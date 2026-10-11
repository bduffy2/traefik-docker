# Traefik v3 & Spring Boot 3 Dynamic Discovery Proof-of-Concept

This repository provides a complete, runnable Docker Compose proof-of-concept demonstrating **Traefik v3** acting as an edge reverse proxy and load balancer for two **Spring Boot 3 (Java 21)** REST microservices (`orders-service` and `catalog-service`) using automatic dynamic discovery via Docker container labels.

---

## Architecture Overview

```
                          +-------------------------------------------------------+
                          |                     Host Machine                      |
                          |   :80 (HTTP Traffic)       :8080 (Web Dashboard)      |
                          +------------+---------------------------+--------------+
                                       |                           |
                                       v                           v
                    +----------------------------------------------------------------+
                    |                 Traefik v3 Edge Proxy (Container)              |
                    |           - Reads /var/run/docker.sock (read-only)             |
                    |           - Auto-discovers backend routes from labels          |
                    +-------------------+----------------------------+---------------+
                                        | Bridge Network             |
                                        | (internal-net)             |
                                        v                            v
                       PathPrefix('/orders')            PathPrefix('/catalog')
                                        |                            |
                     +------------------+----+       +---------------+----------------+
                     |  orders-service       |       |  catalog-service               |
                     |  - Port: 8080 (no host|       |  - Port: 8080 (no host         |
                     |    port binding)      |       |    port binding)               |
                     |  - Context: /orders   |       |  - Context: /catalog           |
                     |  - Scales to N nodes  |       +--------------------------------+
                     +-----------------------+
```

### Key Architectural Highlights:
1. **Zero Host Port Collision**: Neither `orders-service` nor `catalog-service` exposes ports to the host machine. Both listen internally on port `8080` within a private Docker bridge network (`internal-net`).
2. **Dynamic Service Discovery**: Traefik continuously monitors the Docker socket (`/var/run/docker.sock`) and registers/unregisters backends in real time without restarting Traefik.
3. **Multi-Stage Container Builds**: Both Spring Boot services utilize multi-stage Dockerfiles (`gradle:8.8-jdk21-alpine` -> `eclipse-temurin:21-jre-alpine`). No local JDK or Gradle installation is needed to build and run the services.
4. **Context Path & Header Awareness**: Both applications configure `server.servlet.context-path` and `server.forward-headers-strategy=framework` to seamlessly process reverse-proxied requests and `X-Forwarded-*` headers.

---

## Directory Structure

```
.
├── docker-compose.yml
├── README.md
└── apps/
    ├── orders-service/
    │   ├── build.gradle
    │   ├── settings.gradle
    │   ├── gradlew
    │   ├── gradlew.bat
    │   ├── gradle/
    │   │   └── wrapper/
    │   │       ├── gradle-wrapper.jar
    │   │       └── gradle-wrapper.properties
    │   ├── Dockerfile
    │   └── src/
    │       └── main/
    │           ├── java/com/example/orders/
    │           │   ├── OrdersApplication.java
    │           │   └── controller/OrdersController.java
    │           └── resources/
    │               └── application.yml
    └── catalog-service/
        ├── build.gradle
        ├── settings.gradle
        ├── gradlew
        ├── gradlew.bat
        ├── gradle/
        │   └── wrapper/
        │       ├── gradle-wrapper.jar
        │       └── gradle-wrapper.properties
        ├── Dockerfile
        └── src/
            └── main/
                ├── java/com/example/catalog/
                │   ├── CatalogApplication.java
                │   └── controller/CatalogController.java
                └── resources/
                    └── application.yml
```

---

## Services & Endpoints

| Service | Context Path | Endpoint | Description |
| :--- | :--- | :--- | :--- |
| **Traefik Dashboard** | `/` | `http://localhost:8080/dashboard/` | Real-time web UI showing active routers, services, and containers |
| **Orders Service** | `/orders` | `GET http://localhost/orders/health` | Service health status and container instance ID |
| **Orders Service** | `/orders` | `GET http://localhost/orders/summary` | Order count, total revenue, sample orders, and instance ID |
| **Catalog Service** | `/catalog` | `GET http://localhost/catalog/health` | Service health status and container instance ID |
| **Catalog Service** | `/catalog` | `GET http://localhost/catalog/items` | Product catalog items list and instance ID |

---

## Getting Started

### 1. Build and Start the Stack

Run the following command from the root directory:

```bash
docker compose up -d --build
```

### 2. Verify Container Status

```bash
docker compose ps
```

All three services (`traefik`, `orders-service`, and `catalog-service`) will be reported in an `Up` state.

---

## Verification via `curl`

### 1. Verify Orders Service Endpoints

```bash
# Health check
curl -i http://localhost/orders/health
```

Expected JSON response:
```json
{
  "service": "orders-service",
  "status": "UP",
  "instanceId": "3491ba6df583",
  "timestamp": "2026-10-11T19:30:00Z"
}
```

```bash
# Orders summary
curl -i http://localhost/orders/summary
```

Expected JSON response:
```json
{
  "service": "orders-service",
  "instanceId": "3491ba6df583",
  "totalOrders": 152,
  "totalRevenue": 24890.75,
  "currency": "USD",
  "recentOrders": [
    {"id": "ORD-1001", "customer": "Alice", "amount": 129.99, "status": "COMPLETED"},
    {"id": "ORD-1002", "customer": "Bob", "amount": 54.5, "status": "SHIPPED"},
    {"id": "ORD-1003", "customer": "Charlie", "amount": 310.0, "status": "PROCESSING"}
  ]
}
```

---

### 2. Verify Catalog Service Endpoints

```bash
# Health check
curl -i http://localhost/catalog/health
```

Expected JSON response:
```json
{
  "service": "catalog-service",
  "status": "UP",
  "instanceId": "f9087c53d9e2",
  "timestamp": "2026-10-11T19:30:05Z"
}
```

```bash
# Catalog items
curl -i http://localhost/catalog/items
```

Expected JSON response:
```json
{
  "service": "catalog-service",
  "instanceId": "f9087c53d9e2",
  "count": 4,
  "items": [
    {"id": "ITEM-101", "name": "Mechanical Keyboard", "category": "Electronics", "price": 89.99, "inStock": true},
    {"id": "ITEM-102", "name": "Ergonomic Mouse", "category": "Electronics", "price": 49.5, "inStock": true},
    {"id": "ITEM-103", "name": "4K Ultra-HD Monitor", "category": "Displays", "price": 379.0, "inStock": false},
    {"id": "ITEM-104", "name": "Noise-Cancelling Headphones", "category": "Audio", "price": 199.95, "inStock": true}
  ]
}
```

---

### 3. Verify Traefik Web Dashboard

Open your browser or run curl against the Traefik API/dashboard:

```bash
curl -i http://localhost:8080/api/rawdata
```

Or open in a browser:
- `http://localhost:8080/dashboard/`

You will see the active HTTP routers (`orders@docker` and `catalog@docker`) and their corresponding load-balanced backend services.

---

## Horizontal Scaling & Automatic Load Balancing Demo

Traefik v3 dynamically detects when Docker containers scale up or down without any configuration changes or proxy reloads.

### Step 1: Scale `orders-service` to 2 instances

```bash
docker compose up -d --scale orders-service=2
```

### Step 2: Query the endpoint multiple times

Execute requests in rapid succession to observe Traefik's round-robin load balancer cycling between container hostnames:

```bash
for i in {1..4}; do curl -s http://localhost/orders/health | grep -o '"instanceId":"[^"]*"'; done
```

On Windows PowerShell:
```powershell
1..4 | ForEach-Object { (Invoke-RestMethod http://localhost/orders/health).instanceId }
```

Sample Output demonstrating load balancing:
```text
"instanceId":"3491ba6df583"
"instanceId":"8f9218d6a71e"
"instanceId":"3491ba6df583"
"instanceId":"8f9218d6a71e"
```

### Step 3: Scale back down to 1 instance

```bash
docker compose up -d --scale orders-service=1
```

Traefik instantly removes the terminated container from the routing pool with zero dropped requests for new incoming connections.

---

## Tear Down

To stop and remove all containers, networks, and volumes:

```bash
docker compose down
```
