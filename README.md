# Collaborative Text Editor

A Java-based real-time collaborative plain text editor built for the APT project.
The repository is a Maven multi-module project with three modules: `common`, `client`, and `server`.

## Collaborators

| Collaborators |
| --- |
| Omar Hamdy |
| Bassel Alaa |
| Abdelrahman Salah |
| Hossam Bakr |

## Prerequisites

| Tool | Recommended Version | Purpose |
| --- | --- | --- |
| Java JDK | `17` | Build and run all modules |
| Apache Maven | `3.9+` | Build and test from the repo root; run the backend from `server/` |
| Git | Latest stable | Clone and update the project |
| IDE | IntelliJ IDEA or VS Code | Optional, but helpful for development |

## Modules

- `common`: shared CRDT logic, IDs, operations, and protocol DTO/message types
- `client`: Java client/editor-side code and client WebSocket flow
- `server`: Spring Boot backend with the collaboration WebSocket endpoint

## Recommended Quick Start

Run these commands from the repository root.

### 1. Build everything

```bash
mvn clean install
```

This builds all three modules in the correct order.

### 2. Run tests

```bash
mvn test
```

### 3. Start the server

```bash
cd server
mvn spring-boot:run
```

Run this after the project has already been built from the repository root.

The server starts on `http://localhost:8080`.

### 4. Run the client (optional)

```bash
mvn -pl client -am exec:java -Dexec.mainClass="com.team.editor.client.ClientApp"
```

## Alternative Commands

Use these only when you want module-specific work or do not have Maven installed globally.

### Module-specific Maven commands

Build only `common`:

```bash
mvn -pl common -am clean install
```

Build only `server` and what it depends on:

```bash
mvn -pl server -am clean package
```

Test only `client` and what it depends on:

```bash
mvn -pl client -am test
```

### Maven wrapper

If Maven is installed, use normal `mvn` commands from the repository root.

If Maven is not installed, use the checked-in wrapper inside `server/` and point it to the root `pom.xml`.

Windows:

```powershell
cd server
.\mvnw.cmd -f ..\pom.xml test
```

Unix-like shells:

```bash
cd server
./mvnw -f ../pom.xml test
```

You can replace `test` with other Maven goals such as `clean install`.

## Basic WebSocket Collaboration Test

This is the simplest way to verify the current collaboration flow.

### 1. Start the server

```bash
cd server
mvn spring-boot:run
```

### 2. Connect a WebSocket client

Use Postman, Insomnia, browser devtools, or `wscat`, and connect to:

```text
ws://localhost:8080/ws/collab
```

### 3. Send a join request

Send this JSON message from the first client:

```json
{
  "messageType": "JOIN_SESSION_REQUEST",
  "sessionId": "demo-room",
  "userId": {
    "value": "alice"
  },
  "displayName": "Alice",
  "role": "EDITOR"
}
```

### 4. Verify join success

The same socket should receive a `JOIN_SESSION_RESPONSE` message with:

- `"messageType": "JOIN_SESSION_RESPONSE"`
- `"accepted": true`
- `"sessionId": "demo-room"`
- `"assignedUserId"` containing the joined user id

### 5. Open a second client

Connect a second WebSocket client to the same endpoint and send another join request:

```json
{
  "messageType": "JOIN_SESSION_REQUEST",
  "sessionId": "demo-room",
  "userId": {
    "value": "bob"
  },
  "displayName": "Bob",
  "role": "EDITOR"
}
```

### 6. Verify presence

The first client should receive a `PRESENCE_UPDATE` showing that Bob joined the same session.

### 7. Send an operation

From Alice's client, send:

```json
{
  "messageType": "OPERATION",
  "sessionId": "demo-room",
  "senderId": {
    "value": "alice"
  },
  "operationKind": "INSERT_TEXT",
  "payload": "{\"text\":\"hello\"}"
}
```

### 8. Verify relay

- Bob's client should receive that `OPERATION` message
- Alice's client should not receive the same operation back

Note: the current protocol uses flat JSON messages with `messageType` at the top level. It does not currently use a nested `payload` envelope for join requests.

## Tech Stack

| Layer | Technologies |
| --- | --- |
| Language | Java `17` |
| Build Tool | Maven multi-module build |
| Backend | Spring Boot, Spring Web MVC, Spring WebSocket |
| Database | H2 in-memory database |
| Real-Time Communication | WebSocket |
| Data Handling | Jackson |
| Testing | JUnit 5, Spring test starters |
| Core Collaboration Model | CRDT-based document editing |

## Current Status

- Phase 1 CRDT foundation is implemented mainly in `common`
- Phase 2 collaboration, session, and WebSocket structure exists across `client`, `server`, and `common`
- Persistence and fuller integration are planned for a later phase
