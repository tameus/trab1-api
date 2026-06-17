# Distributed Messaging System

A distributed, federated messaging system built for the **Distributed Systems** course (2025/26) at **Faculdade de Ciências e Tecnologia, Universidade Nova de Lisboa (FCT-UNL)**.

Developed by **Mateus Albergaria** and **Francisco Gututui**.

The system works similarly to email: users have accounts in a domain, and messages can be sent across domains. Each domain runs independent servers that communicate with each other through REST or gRPC.

## Architecture

The system is composed of three types of servers per domain:

- **Users Server** — manages user accounts for the domain (CRUD + search)
- **Messages Server** — manages the domain's mailboxes and handles cross-domain message forwarding
- **Gateway Server** — optional reverse proxy that forwards client requests to the appropriate domain server

Clients always interact with the servers of their own domain. When a message is sent to a user in another domain, the local Messages server is responsible for forwarding it asynchronously.

```
Domain A                          Domain B
┌──────────────────┐              ┌──────────────────┐
│  Users Server    │              │  Users Server    │
│  Messages Server │◄────────────►│  Messages Server │
│  Gateway Server  │              │  Gateway Server  │
└──────────────────┘              └──────────────────┘
         ▲                                 ▲
         │                                 │
      Clients                           Clients
```

## Key Features Implemented

### Dual-Protocol Support (REST + gRPC)
Every service is implemented in both REST (JAX-RS / Jersey) and gRPC (Protocol Buffers). A generic client factory inspects the discovered service URI (`http://` vs `grpc://`) and transparently routes requests to the correct client implementation. This means REST and gRPC servers can coexist in the same system and communicate with each other.

### Multicast Service Discovery
Servers announce themselves periodically via UDP multicast (`226.226.226.226:2266`) using the format `ServiceName@domain\t<uri>`. Clients block until a minimum number of service replicas have been discovered before attempting requests. No external registry (Consul, Eureka, etc.) is used — the discovery mechanism is fully custom-built.

### Asynchronous Cross-Domain Message Delivery
When a message is sent to users in remote domains, delivery is decoupled from the client's request. The `postMessage` operation returns immediately after persisting the message locally. Each remote domain has its own independent retry queue backed by a thread pool, so a fault on one domain never delays delivery to others. Retries run with 500ms backoff until the destination is reachable.

### Fault Tolerance and Timeout Notifications
If a remote domain is unreachable for longer than a configurable threshold (default: 90 seconds), the server stops retrying and places a failure notification in the sender's inbox with subject `FAILED TO SEND <mid> TO <user>: TIMEOUT`. For messages sent to users that simply don't exist, a `UNKNOWN USER` notification is sent immediately.

### Concurrency Control
- Message persistence uses `synchronized (messageId.intern())` to prevent duplicate entries under concurrent requests
- Inbox operations are protected to maintain consistency when multiple threads process the same user simultaneously
- Discovery uses `ConcurrentHashMap` for thread-safe URI caching

### Cross-Domain Message Deletion
When a user deletes a message (within a 30-second window), the deletion is propagated asynchronously to all other domains that hold a copy of that message. A `DeletedMessage` table tracks which messages have been deleted, ensuring that in-flight deliveries of already-deleted messages are correctly discarded upon arrival.

### Persistent Storage with Hibernate ORM
All entities (users, messages, inboxes, deleted messages) are persisted to an HSQLDB database via Hibernate. This makes the system stateful across restarts and supports concurrent transactional access.

## Tech Stack

| Layer | Technology |
|---|---|
| REST | Jersey 4.0.2 + JDK HTTP Server |
| RPC | gRPC 1.79.0 + Protocol Buffers 4.33.5 |
| Service Discovery | Custom UDP Multicast |
| Persistence | Hibernate 7.2.4 + HSQLDB 2.7.4 |
| Build & Packaging | Maven + Docker (Fabric8 plugin) |
| Language | Java 17 |

## Project Structure

```
src/sd2526/trab/
├── api/               # Service interfaces, data models, REST/gRPC API definitions
├── clients/           # REST and gRPC client implementations with retry logic
├── server/
│   ├── rest/          # JAX-RS REST server and resource classes
│   ├── grpc/          # gRPC server and service controller classes
│   ├── gateway/       # Gateway reverse-proxy server
│   └── java/          # Core business logic (JavaMessages, JavaUsers)
├── persistence/       # Hibernate ORM helper
└── Discovery.java     # Multicast discovery engine
```

## REST API

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/rest/users` | Create a user |
| GET | `/rest/users/{name}` | Get user info |
| PUT | `/rest/users/{name}` | Update user |
| DELETE | `/rest/users/{name}` | Delete user |
| GET | `/rest/users` | Search users |
| POST | `/rest/messages` | Send a message |
| GET | `/rest/messages/mbox/{name}` | List inbox (with optional search) |
| GET | `/rest/messages/mbox/{name}/{mid}` | Get a message |
| DELETE | `/rest/messages/mbox/{name}/{mid}` | Remove from inbox |
| DELETE | `/rest/messages/{name}/{mid}` | Delete message globally |

## Building

```bash
mvn clean compile package docker:build
```