# Project Structure

## Overview

This project is split into three main modules: `common`, `client`, and `server`.

Phase 1 mainly focused on the CRDT foundation inside `common`, including block editing, character editing, document composition, shared operations, and typed IDs.

Phase 2 added the package structure needed for collaboration features such as UI, sessions, WebSocket communication, cursor tracking, and shared protocol DTOs.

## Top-Level Structure

```text
collab-text-editor
|-- .gitignore
|-- README.md
|-- docs/
|   |-- architecture/
|   |   `-- project-structure.md
|   |-- decisions/
|   |-- protocol/
|   `-- crdt-rules.md
|-- common/
|   |-- pom.xml
|   `-- src/
|       |-- main/
|       `-- test/
|-- client/
|   |-- pom.xml
|   `-- src/
|       |-- main/
|       `-- test/
`-- server/
    |-- .gitattributes
    |-- HELP.md
    |-- mvnw
    |-- mvnw.cmd
    |-- pom.xml
    |-- .mvn/
    `-- src/
        |-- main/
        `-- test/
```

## Detailed Module Tree

### `common`

```text
common/
|-- pom.xml
`-- src/
    |-- main/
    |   `-- java/
    |       `-- com/team/editor/common/
    |           |-- package-info.java
    |           |-- crdt/
    |           |   |-- block/
    |           |   |   |-- BlockCrdtEngine.java
    |           |   |   |-- BlockId.java
    |           |   |   |-- BlockNode.java
    |           |   |   `-- BlockTree.java
    |           |   |-- character/
    |           |   |   |-- CharacterCrdtEngine.java
    |           |   |   |-- CharacterOrdering.java
    |           |   |   |-- CharacterTree.java
    |           |   |   |-- CharId.java
    |           |   |   `-- CharNode.java
    |           |   `-- document/
    |           |       `-- DocumentCrdt.java
    |           |-- model/
    |           |   |-- DocumentId.java
    |           |   `-- UserId.java
    |           |-- ops/
    |           |   |-- block/
    |           |   |   |-- DeleteBlockOp.java
    |           |   |   `-- InsertBlockOp.java
    |           |   `-- character/
    |           |       |-- DeleteCharOp.java
    |           |       `-- InsertCharOp.java
    |           `-- protocol/
    |               |-- package-info.java
    |               |-- dto/
    |               |   |-- CursorUpdateMessage.java
    |               |   |-- ErrorMessage.java
    |               |   |-- JoinSessionRequest.java
    |               |   |-- JoinSessionResponse.java
    |               |   |-- OperationMessage.java
    |               |   |-- PresenceUpdateMessage.java
    |               |   `-- package-info.java
    |               `-- message/
    |                   |-- CollaborationMessage.java
    |                   `-- MessageType.java
    `-- test/
        `-- java/
            `-- com/team/editor/common/
                |-- block/
                |   |-- BlockCrdtEngineTest.java
                |   |-- BlockCrdtVisualTester.java
                |   |-- BlockIdTest.java
                |   |-- BlockNodeTest.java
                |   `-- BlockTreeTest.java
                |-- cdt/
                |   `-- document/
                |-- character/
                |   |-- CharacterCrdtEngineTest.java
                |   |-- CharacterCrdtVisualTester.java
                |   |-- CharacterOrderingTest.java
                |   |-- CharacterTreeTest.java
                |   |-- CharIdTest.java
                |   `-- CharNodeTest.java
                |-- crdt/
                |   `-- document/
                |       `-- DocumentCrdtIntegrationTest.java
                |-- document/
                |   `-- DocumentCrdtTest.java
                `-- model/
                    |-- DocumentIdTest.java
                    `-- UserIdTest.java
```

### `client`

```text
client/
|-- pom.xml
`-- src/
    |-- main/
    |   `-- java/
    |       `-- com/team/editor/client/
    |           |-- ClientApp.java
    |           |-- controller/
    |           |   |-- EditorController.java
    |           |   `-- SessionController.java
    |           |-- cursor/
    |           |   |-- CursorManager.java
    |           |   `-- RemoteCursor.java
    |           |-- editor/
    |           |   `-- package-info.java
    |           |-- network/
    |           |   `-- package-info.java
    |           |-- renderer/
    |           |   |-- DocumentRenderer.java
    |           |   `-- package-info.java
    |           |-- service/
    |           |   `-- package-info.java
    |           |-- session/
    |           |   |-- ClientSession.java
    |           |   `-- SessionRole.java
    |           |-- state/
    |           |   |-- ConnectionState.java
    |           |   `-- EditorState.java
    |           |-- ui/
    |           |   |-- ActiveUsersPanel.java
    |           |   |-- ConnectionStatusBar.java
    |           |   |-- EditorView.java
    |           |   `-- JoinSessionView.java
    |           `-- websocket/
    |               |-- ClientWebSocketHandler.java
    |               |-- OutboundMessageDispatcher.java
    |               `-- package-info.java
    `-- test/
        `-- java/
            `-- com/team/editor/client/
                `-- package-info.java
```

### `server`

```text
server/
|-- .gitattributes
|-- HELP.md
|-- mvnw
|-- mvnw.cmd
|-- pom.xml
|-- .mvn/
|   `-- wrapper/
|       `-- maven-wrapper.properties
`-- src/
    |-- main/
    |   |-- java/
    |   |   |-- .gitkeep
    |   |   `-- com/team/editor/server/
    |   |       |-- EditorServerApplication.java
    |   |       |-- package-info.java
    |   |       |-- config/
    |   |       |   |-- DatabaseConfig.java
    |   |       |   |-- ServerConfig.java
    |   |       |   |-- WebSocketConfig.java
    |   |       |   `-- package-info.java
    |   |       |-- persistence/
    |   |       |   `-- package-info.java
    |   |       |-- service/
    |   |       |   |-- DocumentService.java
    |   |       |   |-- OperationRelayService.java
    |   |       |   |-- PermissionService.java
    |   |       |   |-- PresenceService.java
    |   |       |   |-- SessionJoinService.java
    |   |       |   `-- package-info.java
    |   |       |-- session/
    |   |       |   |-- CollaborationSession.java
    |   |       |   |-- ConnectedUser.java
    |   |       |   |-- SessionRegistry.java
    |   |       |   `-- package-info.java
    |   |       |-- validation/
    |   |       |   `-- MessageValidator.java
    |   |       `-- websocket/
    |   |           |-- CollaborationWebSocketHandler.java
    |   |           `-- package-info.java
    |   `-- resources/
    |       |-- application.properties
    |       |-- static/
    |       `-- templates/
    `-- test/
        `-- java/
            `-- com/team/editor/server/
                |-- EditorServerApplicationTests.java
                `-- package-info.java
```

### `tests`

```text
common/src/test/java/com/team/editor/common/
|-- block/
|   |-- BlockCrdtEngineTest.java
|   |-- BlockCrdtVisualTester.java
|   |-- BlockIdTest.java
|   |-- BlockNodeTest.java
|   `-- BlockTreeTest.java
|-- cdt/
|   `-- document/
|-- character/
|   |-- CharacterCrdtEngineTest.java
|   |-- CharacterCrdtVisualTester.java
|   |-- CharacterOrderingTest.java
|   |-- CharacterTreeTest.java
|   |-- CharIdTest.java
|   `-- CharNodeTest.java
|-- crdt/
|   `-- document/
|       `-- DocumentCrdtIntegrationTest.java
|-- document/
|   `-- DocumentCrdtTest.java
`-- model/
    |-- DocumentIdTest.java
    `-- UserIdTest.java

client/src/test/java/com/team/editor/client/
`-- package-info.java

server/src/test/java/com/team/editor/server/
|-- EditorServerApplicationTests.java
`-- package-info.java
```

## Module Details

### common

The `common` module is the shared base of the project.

- `crdt/block` holds the block-level CRDT classes and the main block tree pieces.
- `crdt/character` holds the character-level CRDT classes, ordering rules, and character IDs.
- `crdt/document` combines lower-level CRDT pieces into a document-level structure.
- `model` holds shared typed IDs such as `UserId` and `DocumentId`.
- `ops` holds shared operation classes for block and character changes.
- `protocol/message` holds the shared message contract and message type enum.
- `protocol/dto` holds the transport DTOs used for session joins, operations, cursor updates, presence updates, and error messages.

### client

The `client` module contains the client-side structure for the editor and collaboration UI.

- `ClientApp.java` is the current client application entry point.
- `controller` is meant for editor and session control flow.
- `cursor` is meant for remote cursor tracking on the client side.
- `renderer` is meant for document rendering responsibilities.
- `session` is meant for client-side session data and roles.
- `state` is meant for editor state and connection state.
- `ui` is meant for views and UI panels such as the editor view, join session view, active users panel, and connection status bar.
- `websocket` is meant for client-side collaboration message sending and receiving.

The folders `editor`, `network`, and `service` still exist and currently look like placeholder folders because they only contain `package-info.java`.

### server

The `server` module contains the Spring Boot backend structure.

- `EditorServerApplication.java` is the Spring Boot application entry point.
- `config` holds configuration classes such as database setup, general server config, and WebSocket config.
- `service` holds service-layer classes for document work and collaboration-related services.
- `session` holds session-related classes such as `CollaborationSession`, `ConnectedUser`, and `SessionRegistry`.
- `validation` currently holds `MessageValidator.java`.
- `websocket` holds the server-side collaboration WebSocket handler structure.
- `persistence` currently exists as a placeholder-only package with `package-info.java`.

## Phase 1 Foundation

The shared CRDT work is already implemented mainly inside `common`.

- Block CRDT logic is present in `crdt/block`.
- Character CRDT logic is present in `crdt/character`.
- Document composition is present in `crdt/document`.
- Typed IDs such as `UserId`, `DocumentId`, `BlockId`, and `CharId` already exist.
- Shared operations already exist in `ops/block` and `ops/character`.
- The project also already has a good amount of tests in `common/src/test`, including CRDT engine tests, ID tests, ordering tests, and visual test helpers.

## Phase 2 Additions

Phase 2 added the package structure needed for collaboration work, even though much of the implementation is still light.

- UI structure was added in the client through `ui`, `renderer`, `controller`, and `state`.
- Session management structure was added through `client/session` and `server/session`.
- WebSocket communication structure was added through `client/websocket`, `server/websocket`, and `server/config/WebSocketConfig`.
- Shared protocol messages were added through `common/protocol/message` and `common/protocol/dto`.
- Cursor tracking structure was added through `client/cursor` and `CursorUpdateMessage`.
- Collaboration services were added through the new service classes in `server/service`.

## Notes

Some folders are still placeholders and may be refined later as the project grows.

Right now, `common` is the most complete module because it already contains the CRDT foundation and most of the real tests.

`client` and `server` now have a clearer Phase 2 structure, but many classes are still scaffolding and will need real implementation later.

There is also a small cleanup note in the test tree: `common/src/test/java/com/team/editor/common/cdt` looks suspicious next to the existing `crdt` test folder and may be a naming mistake that should be reviewed later.
