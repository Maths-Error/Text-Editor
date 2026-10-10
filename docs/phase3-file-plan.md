# Phase 3 File Plan

> Superseded note, 2026-05-07: this file is now historical. The current source of truth is `docs/phase3-current-audit.md`, because most proposed Phase 3 files below now exist and need status tracking rather than another placeholder-file proposal.

Generated on `2026-04-26` from repo root:

`D:\CUFE\Semesters\Spring 2026\APT\Project\collab-text-editor`

No Phase 3 logic, Java code, or placeholder classes were added by this plan. The scoped inspection found `104` existing files before this planning file was created.

## 1. Current Project Tree

Excluded folders: `target/`, `.git/`, `.idea/`, `.vscode/`, `node_modules/`, `out/`.

Placeholder marker: `[placeholder package: package-info.java only]`.

```text
collab-text-editor/
|-- .gitignore
|-- pom.xml
|-- README.md
|-- client/
|   |-- pom.xml
|   `-- src/
|       |-- main/
|       |   `-- java/
|       |       `-- com/
|       |           `-- team/
|       |               `-- editor/
|       |                   `-- client/
|       |                       |-- ClientApp.java
|       |                       |-- controller/
|       |                       |   |-- EditorController.java
|       |                       |   `-- SessionController.java
|       |                       |-- cursor/
|       |                       |   |-- CursorManager.java
|       |                       |   `-- RemoteCursor.java
|       |                       |-- editor/ [placeholder package: package-info.java only]
|       |                       |   `-- package-info.java
|       |                       |-- network/ [placeholder package: package-info.java only]
|       |                       |   `-- package-info.java
|       |                       |-- renderer/
|       |                       |   `-- DocumentRenderer.java
|       |                       |-- service/ [placeholder package: package-info.java only]
|       |                       |   `-- package-info.java
|       |                       |-- session/
|       |                       |   |-- ClientSession.java
|       |                       |   `-- SessionRole.java
|       |                       |-- state/
|       |                       |   |-- ConnectionState.java
|       |                       |   `-- EditorState.java
|       |                       |-- ui/
|       |                       |   |-- ActiveUsersPanel.java
|       |                       |   |-- ConnectionStatusBar.java
|       |                       |   |-- EditorView.java
|       |                       |   `-- JoinSessionView.java
|       |                       `-- websocket/
|       |                           |-- ClientWebSocketHandler.java
|       |                           `-- OutboundMessageDispatcher.java
|       `-- test/
|           `-- java/
|               `-- com/
|                   `-- team/
|                       `-- editor/
|                           `-- client/
|                               |-- package-info.java
|                               |-- controller/
|                               |   |-- EditorControllerTest.java
|                               |   `-- SessionControllerTest.java
|                               |-- state/
|                               |   `-- EditorStateTest.java
|                               `-- websocket/
|                                   `-- ClientWebSocketHandlerTest.java
|-- common/
|   |-- pom.xml
|   `-- src/
|       |-- main/
|       |   `-- java/
|       |       `-- com/
|       |           `-- team/
|       |               `-- editor/
|       |                   `-- common/
|       |                       |-- crdt/
|       |                       |   |-- block/
|       |                       |   |   |-- BlockCrdtEngine.java
|       |                       |   |   |-- BlockId.java
|       |                       |   |   |-- BlockNode.java
|       |                       |   |   `-- BlockTree.java
|       |                       |   |-- character/
|       |                       |   |   |-- CharacterCrdtEngine.java
|       |                       |   |   |-- CharacterOrdering.java
|       |                       |   |   |-- CharacterTree.java
|       |                       |   |   |-- CharId.java
|       |                       |   |   `-- CharNode.java
|       |                       |   `-- document/
|       |                       |       `-- DocumentCrdt.java
|       |                       |-- model/
|       |                       |   |-- DocumentId.java
|       |                       |   `-- UserId.java
|       |                       |-- ops/
|       |                       |   |-- block/
|       |                       |   |   |-- DeleteBlockOp.java
|       |                       |   |   |-- InsertBlockOp.java
|       |                       |   |   `-- SplitBlockOp.java
|       |                       |   `-- character/
|       |                       |       |-- DeleteCharOp.java
|       |                       |       `-- InsertCharOp.java
|       |                       `-- protocol/
|       |                           |-- dto/
|       |                           |   |-- BlockOpPayload.java
|       |                           |   |-- CursorUpdateMessage.java
|       |                           |   |-- ErrorMessage.java
|       |                           |   |-- JoinSessionRequest.java
|       |                           |   |-- JoinSessionResponse.java
|       |                           |   |-- OperationMessage.java
|       |                           |   `-- PresenceUpdateMessage.java
|       |                           `-- message/
|       |                               |-- CollaborationMessage.java
|       |                               |-- MessageType.java
|       |                               |-- PresenceEventType.java
|       |                               `-- SessionRole.java
|       `-- test/
|           `-- java/
|               `-- com/
|                   `-- team/
|                       `-- editor/
|                           `-- common/
|                               |-- block/
|                               |   |-- BlockCrdtEngineTest.java
|                               |   |-- BlockCrdtVisualTester.java
|                               |   |-- BlockIdTest.java
|                               |   |-- BlockNodeTest.java
|                               |   `-- BlockTreeTest.java
|                               |-- character/
|                               |   |-- CharacterCrdtEngineTest.java
|                               |   |-- CharacterCrdtVisualTester.java
|                               |   |-- CharacterOrderingTest.java
|                               |   |-- CharacterTreeTest.java
|                               |   |-- CharIdTest.java
|                               |   `-- CharNodeTest.java
|                               |-- crdt/
|                               |   `-- document/
|                               |       `-- DocumentCrdtIntegrationTest.java
|                               |-- document/
|                               |   `-- DocumentCrdtTest.java
|                               `-- model/
|                                   |-- DocumentIdTest.java
|                                   `-- UserIdTest.java
|-- docs/
|   |-- architecture/
|   |   `-- project-structure.md
|   |-- CUFE docs/
|   |   |-- [CMPS211] Project Document (S2026).pdf
|   |   `-- Collaborative Text Editing App.pdf
|   |-- crdt-rules.md
|   |-- phase3-file-plan.md
|   |-- project-context-for-codex.md
|   `-- review-abdo-phase2.md
`-- server/
    |-- .gitattributes
    |-- .mvn/
    |   `-- wrapper/
    |       `-- maven-wrapper.properties
    |-- HELP.md
    |-- mvnw
    |-- mvnw.cmd
    |-- pom.xml
    `-- src/
        |-- main/
        |   |-- java/
        |   |   |-- .gitkeep
        |   |   `-- com/
        |   |       `-- team/
        |   |           `-- editor/
        |   |               `-- server/
        |   |                   |-- EditorServerApplication.java
        |   |                   |-- config/
        |   |                   |   |-- DatabaseConfig.java
        |   |                   |   |-- ServerConfig.java
        |   |                   |   `-- WebSocketConfig.java
        |   |                   |-- persistence/ [placeholder package: package-info.java only]
        |   |                   |   `-- package-info.java
        |   |                   |-- service/
        |   |                   |   |-- DocumentService.java
        |   |                   |   |-- OperationRelayService.java
        |   |                   |   |-- PermissionService.java
        |   |                   |   |-- PresenceService.java
        |   |                   |   `-- SessionJoinService.java
        |   |                   |-- session/
        |   |                   |   |-- CollaborationSession.java
        |   |                   |   |-- ConnectedUser.java
        |   |                   |   `-- SessionRegistry.java
        |   |                   |-- validation/
        |   |                   |   `-- MessageValidator.java
        |   |                   `-- websocket/
        |   |                       `-- CollaborationWebSocketHandler.java
        |   `-- resources/
        |       `-- application.properties
        `-- test/
            `-- java/
                `-- com/
                    `-- team/
                        `-- editor/
                            `-- server/
                                |-- EditorServerApplicationTests.java
                                |-- package-info.java
                                `-- websocket/
                                    `-- CollaborationWebSocketHandlerTest.java
```

## 2. Current Important Existing Files

### common

- `common/pom.xml`: Defines shared module dependencies, including Jackson and JUnit.
- `common/src/main/java/com/team/editor/common/model/DocumentId.java`: Shared typed document identifier.
- `common/src/main/java/com/team/editor/common/model/UserId.java`: Shared typed user identifier.
- `common/src/main/java/com/team/editor/common/crdt/block/BlockId.java`: CRDT block identifier based on user and logical clock.
- `common/src/main/java/com/team/editor/common/crdt/block/BlockNode.java`: Node state for a block, including its inner character tree.
- `common/src/main/java/com/team/editor/common/crdt/block/BlockTree.java`: Tree structure for visible and tombstoned blocks.
- `common/src/main/java/com/team/editor/common/crdt/block/BlockCrdtEngine.java`: Main block-level CRDT API and block-scoped character operation bridge.
- `common/src/main/java/com/team/editor/common/crdt/character/CharId.java`: CRDT character identifier based on user and logical clock.
- `common/src/main/java/com/team/editor/common/crdt/character/CharNode.java`: Node state for a character, including tombstone and children.
- `common/src/main/java/com/team/editor/common/crdt/character/CharacterTree.java`: Character CRDT tree with visible-text generation.
- `common/src/main/java/com/team/editor/common/crdt/character/CharacterOrdering.java`: Deterministic ordering rules for sibling characters.
- `common/src/main/java/com/team/editor/common/crdt/character/CharacterCrdtEngine.java`: Character-level local and remote operation engine.
- `common/src/main/java/com/team/editor/common/crdt/document/DocumentCrdt.java`: Document composition wrapper around block CRDT state.
- `common/src/main/java/com/team/editor/common/ops/character/InsertCharOp.java`: Shared record for inserting one character.
- `common/src/main/java/com/team/editor/common/ops/character/DeleteCharOp.java`: Shared record for deleting one character.
- `common/src/main/java/com/team/editor/common/ops/block/InsertBlockOp.java`: Shared record for inserting one block.
- `common/src/main/java/com/team/editor/common/ops/block/DeleteBlockOp.java`: Shared record for deleting one block.
- `common/src/main/java/com/team/editor/common/ops/block/SplitBlockOp.java`: Shared record for a block split operation, currently not wired through the WebSocket flow.
- `common/src/main/java/com/team/editor/common/protocol/message/CollaborationMessage.java`: Common interface that exposes each DTO message type.
- `common/src/main/java/com/team/editor/common/protocol/message/MessageType.java`: Top-level message categories used by client and server routing.
- `common/src/main/java/com/team/editor/common/protocol/message/PresenceEventType.java`: Shared enum for join/leave presence events.
- `common/src/main/java/com/team/editor/common/protocol/message/SessionRole.java`: Shared editor/viewer role enum used by server protocol DTOs.
- `common/src/main/java/com/team/editor/common/protocol/dto/JoinSessionRequest.java`: DTO for a client asking to join a collaboration session.
- `common/src/main/java/com/team/editor/common/protocol/dto/JoinSessionResponse.java`: DTO for server join acceptance/rejection response.
- `common/src/main/java/com/team/editor/common/protocol/dto/OperationMessage.java`: DTO for operation relay, currently `operationKind` string plus `JsonNode` payload.
- `common/src/main/java/com/team/editor/common/protocol/dto/BlockOpPayload.java`: Generic wrapper for block-scoped character operation payloads.
- `common/src/main/java/com/team/editor/common/protocol/dto/CursorUpdateMessage.java`: DTO for cursor position broadcasts.
- `common/src/main/java/com/team/editor/common/protocol/dto/PresenceUpdateMessage.java`: DTO for user joined/left presence updates.
- `common/src/main/java/com/team/editor/common/protocol/dto/ErrorMessage.java`: DTO for validation and transport errors.

### client

- `client/pom.xml`: Defines client dependencies on `common`, Jackson, FlatLaf, and JUnit.
- `client/src/main/java/com/team/editor/client/ClientApp.java`: Swing application entry point and look-and-feel setup.
- `client/src/main/java/com/team/editor/client/controller/SessionController.java`: Owns client session state and forwards WebSocket events to listeners.
- `client/src/main/java/com/team/editor/client/controller/EditorController.java`: Converts local UI edits into CRDT operations and applies remote operations.
- `client/src/main/java/com/team/editor/client/websocket/ClientWebSocketHandler.java`: Opens the Java WebSocket connection, serializes outbound DTOs, and parses inbound messages.
- `client/src/main/java/com/team/editor/client/websocket/OutboundMessageDispatcher.java`: Small outbound transport abstraction used by controllers.
- `client/src/main/java/com/team/editor/client/state/EditorState.java`: Holds local client CRDT document state and offset mapping helpers.
- `client/src/main/java/com/team/editor/client/state/ConnectionState.java`: Tracks disconnected, connecting, connected, joined, and error states.
- `client/src/main/java/com/team/editor/client/session/ClientSession.java`: Stores current client session id, user id, display name, and role.
- `client/src/main/java/com/team/editor/client/session/SessionRole.java`: Client-local role enum; duplicates the shared `common` role concept.
- `client/src/main/java/com/team/editor/client/ui/JoinSessionView.java`: Initial Swing UI for entering session id and username.
- `client/src/main/java/com/team/editor/client/ui/EditorView.java`: Main Swing editor window, text pane, local edit interception, cursor sending, and refresh.
- `client/src/main/java/com/team/editor/client/ui/ActiveUsersPanel.java`: Displays active users in the current session.
- `client/src/main/java/com/team/editor/client/ui/ConnectionStatusBar.java`: Displays current connection/session state.
- `client/src/main/java/com/team/editor/client/cursor/CursorManager.java`: Paints remote cursor highlights in the Swing editor.
- `client/src/main/java/com/team/editor/client/cursor/RemoteCursor.java`: Stores username, color, and offset for one remote cursor.
- `client/src/main/java/com/team/editor/client/renderer/DocumentRenderer.java`: Rendering boundary interface; currently only an interface shell.
- `client/src/main/java/com/team/editor/client/editor/package-info.java`: Placeholder-only package.
- `client/src/main/java/com/team/editor/client/network/package-info.java`: Placeholder-only package.
- `client/src/main/java/com/team/editor/client/service/package-info.java`: Placeholder-only package suitable for Phase 3 client services.

### server

- `server/pom.xml`: Defines Spring Boot server dependencies and direct dependency on `common`.
- `server/src/main/java/com/team/editor/server/EditorServerApplication.java`: Spring Boot server entry point.
- `server/src/main/java/com/team/editor/server/config/WebSocketConfig.java`: Registers the `/ws/collab` WebSocket endpoint.
- `server/src/main/java/com/team/editor/server/config/ServerConfig.java`: Provides the shared server `ObjectMapper` bean.
- `server/src/main/java/com/team/editor/server/config/DatabaseConfig.java`: Empty database config shell; likely to be replaced or supplemented for MongoDB.
- `server/src/main/resources/application.properties`: Current H2/JDBC properties and Spring application name.
- `server/src/main/java/com/team/editor/server/websocket/CollaborationWebSocketHandler.java`: Receives raw WebSocket JSON, validates message type, routes join/operation/cursor messages, and sends errors.
- `server/src/main/java/com/team/editor/server/session/SessionRegistry.java`: In-memory registry for active collaboration sessions.
- `server/src/main/java/com/team/editor/server/session/CollaborationSession.java`: In-memory room state with connected users and operation history.
- `server/src/main/java/com/team/editor/server/session/ConnectedUser.java`: Server-side participant record with user id, display name, role, and socket session.
- `server/src/main/java/com/team/editor/server/service/SessionJoinService.java`: Validates joins, creates sessions, stores socket-user mapping, and handles disconnect cleanup.
- `server/src/main/java/com/team/editor/server/service/OperationRelayService.java`: Serializes and relays operation/cursor messages to other connected users.
- `server/src/main/java/com/team/editor/server/service/PresenceService.java`: Broadcasts user joined/left presence updates.
- `server/src/main/java/com/team/editor/server/service/PermissionService.java`: Resolves editor/viewer roles and rejects viewer edit operations.
- `server/src/main/java/com/team/editor/server/service/DocumentService.java`: Empty service shell; this is a likely Phase 3 expansion point.
- `server/src/main/java/com/team/editor/server/validation/MessageValidator.java`: Validates raw JSON, message type, join, operation, and cursor payload shape.
- `server/src/main/java/com/team/editor/server/persistence/package-info.java`: Placeholder-only persistence package.

## 3. Phase 3 Requirements That Need New Files

Assumption for this plan: Phase 3 should probably use MongoDB because CRDT-shaped documents, nested blocks/chars, snapshots, and operation logs fit document storage better than a relational schema for this project size.

| Requirement | New files likely needed | Notes |
| --- | --- | --- |
| MongoDB persistence | Mongo config, document records, repositories, persistence tests | Current server has H2 config but no actual persistence implementation. |
| Document create/open/rename/delete | Document request/response DTOs, server document command/query services, client document controller/UI | Current join flow uses arbitrary session ids and has no document metadata lifecycle. |
| CRDT snapshot save/load | Snapshot DTO/record, mapper, repository, runtime service | Server needs a durable CRDT form, not plain rendered text. |
| Operation log if needed | Operation log record/repository and replay tests | Useful for late join, reconnect, audit, and recovery; can be a support track after snapshot basics. |
| Share codes | Share-code record/repository/service and response DTOs | Codes should resolve document id and role server-side. |
| Editor/viewer permissions | Document access service and role-aware join/open responses | Current server role check exists, but role is still requested by client instead of resolved from code. |
| Import `.txt` | Client file import service and possibly server import helper | Import should create CRDT operations/state from text instead of storing plain text canonically. |
| Export `.txt` | Client export service and maybe server export endpoint/helper | Export can use visible text derived from CRDT state. |
| Client document management UI | Dashboard/list/open/create/rename/delete/share UI classes | Current client starts directly at a join-session form. |
| Tests | Common DTO serialization tests, server document/persistence/share tests, client document/file tests | Existing tests cover CRDT and live relay but not Phase 3 document lifecycle. |
| Docs/demo checklist | Mongo setup, API/protocol notes, demo checklist | Needed so the team can run a consistent final demo. |

## 4. Proposed New Files To Add Later

Proposed new file count: `48`.

Do not create these yet.

| Module | New file path | Purpose | Owner suggestion | Blocking risk |
| --- | --- | --- | --- | --- |
| common | `common/src/main/java/com/team/editor/common/protocol/message/OperationKind.java` | Shared enum replacing string operation kinds. | Member 3 / Persistence Hardening + Protocol/Tests | HIGH |
| common | `common/src/main/java/com/team/editor/common/protocol/dto/document/CreateDocumentRequest.java` | Request DTO for creating a document. | Omar / Backend Lead | HIGH |
| common | `common/src/main/java/com/team/editor/common/protocol/dto/document/CreateDocumentResponse.java` | Response DTO with created document metadata and codes if allowed. | Omar / Backend Lead | HIGH |
| common | `common/src/main/java/com/team/editor/common/protocol/dto/document/OpenDocumentRequest.java` | Request DTO for opening a document via id or share code. | Omar / Backend Lead | HIGH |
| common | `common/src/main/java/com/team/editor/common/protocol/dto/document/OpenDocumentResponse.java` | Response DTO carrying metadata, role, and initial document state reference. | Omar / Backend Lead | HIGH |
| common | `common/src/main/java/com/team/editor/common/protocol/dto/document/RenameDocumentRequest.java` | Request DTO for renaming an existing document. | Omar / Backend Lead | MEDIUM |
| common | `common/src/main/java/com/team/editor/common/protocol/dto/document/DeleteDocumentRequest.java` | Request DTO for deleting an existing document. | Omar / Backend Lead | MEDIUM |
| common | `common/src/main/java/com/team/editor/common/protocol/dto/document/DocumentSummaryDto.java` | Compact document listing item for client dashboards. | Member 2 / Client UI Lead | MEDIUM |
| common | `common/src/main/java/com/team/editor/common/protocol/dto/document/ShareCodeResponse.java` | DTO for editor/viewer sharing codes without exposing them to viewers. | Omar / Backend Lead | HIGH |
| common | `common/src/main/java/com/team/editor/common/protocol/dto/persistence/CrdtSnapshotDto.java` | Transport-safe representation of CRDT snapshot data. | Omar / Backend Lead | HIGH |
| common | `common/src/main/java/com/team/editor/common/protocol/dto/persistence/OperationLogEntryDto.java` | Transport-safe representation of persisted operation log entries. | Member 3 / Persistence Hardening + Protocol/Tests | MEDIUM |
| server | `server/src/main/java/com/team/editor/server/config/MongoConfig.java` | MongoDB connection/configuration entry point. | Omar / Backend Lead | HIGH |
| server | `server/src/main/java/com/team/editor/server/persistence/MongoDocumentRecord.java` | Mongo document metadata and ownership/share references. | Omar / Backend Lead | HIGH |
| server | `server/src/main/java/com/team/editor/server/persistence/CrdtSnapshotRecord.java` | Mongo record for serialized CRDT snapshots. | Omar / Backend Lead | HIGH |
| server | `server/src/main/java/com/team/editor/server/persistence/OperationLogRecord.java` | Mongo record for accepted operation log entries. | Member 3 / Persistence Hardening + Protocol/Tests | MEDIUM |
| server | `server/src/main/java/com/team/editor/server/persistence/ShareCodeRecord.java` | Mongo record mapping share codes to document id and role. | Omar / Backend Lead | HIGH |
| server | `server/src/main/java/com/team/editor/server/repository/DocumentRepository.java` | Repository for document metadata lookup/save/delete. | Omar / Backend Lead | HIGH |
| server | `server/src/main/java/com/team/editor/server/repository/CrdtSnapshotRepository.java` | Repository for snapshot save/load. | Omar / Backend Lead | HIGH |
| server | `server/src/main/java/com/team/editor/server/repository/OperationLogRepository.java` | Repository for operation append/replay/dedupe support. | Member 3 / Persistence Hardening + Protocol/Tests | MEDIUM |
| server | `server/src/main/java/com/team/editor/server/repository/ShareCodeRepository.java` | Repository for editor/viewer code lookup and uniqueness checks. | Omar / Backend Lead | HIGH |
| server | `server/src/main/java/com/team/editor/server/document/DocumentCommandService.java` | Backend create/rename/delete/save command logic. | Omar / Backend Lead | HIGH |
| server | `server/src/main/java/com/team/editor/server/document/DocumentQueryService.java` | Backend open/list/load query logic. | Omar / Backend Lead | HIGH |
| server | `server/src/main/java/com/team/editor/server/document/DocumentRuntimeService.java` | Runtime bridge between live sessions, CRDT state, snapshots, and operation logs. | Omar / Backend Lead | HIGH |
| server | `server/src/main/java/com/team/editor/server/document/DocumentMapper.java` | Converts between CRDT/domain state, DTOs, and Mongo records. | Omar / Backend Lead | HIGH |
| server | `server/src/main/java/com/team/editor/server/document/DocumentManagementController.java` | HTTP or message endpoint for document create/open/rename/delete actions. | Omar / Backend Lead | MEDIUM |
| server | `server/src/main/java/com/team/editor/server/document/ShareCodeService.java` | Generates and resolves editor/viewer share codes. | Omar / Backend Lead | HIGH |
| server | `server/src/main/java/com/team/editor/server/document/DocumentAccessService.java` | Central authorization checks for owner/editor/viewer actions. | Omar / Backend Lead | HIGH |
| server | `server/src/main/java/com/team/editor/server/importexport/TextImportService.java` | Server-side helper if import is processed on backend. | Member 4 / Import-Export + QA/Docs | LOW |
| server | `server/src/main/java/com/team/editor/server/importexport/TextExportService.java` | Server-side helper if export is processed on backend. | Member 4 / Import-Export + QA/Docs | LOW |
| server | `server/src/main/java/com/team/editor/server/validation/DocumentRequestValidator.java` | Validation for document lifecycle and share-code requests. | Member 3 / Persistence Hardening + Protocol/Tests | MEDIUM |
| client | `client/src/main/java/com/team/editor/client/controller/DocumentController.java` | Client-side controller for document create/open/rename/delete/share flow. | Member 2 / Client UI Lead | HIGH |
| client | `client/src/main/java/com/team/editor/client/service/DocumentClientService.java` | Client transport/service wrapper for document management requests. | Member 2 / Client UI Lead | HIGH |
| client | `client/src/main/java/com/team/editor/client/service/DocumentSessionService.java` | Tracks selected document, resolved role, and active document metadata. | Member 2 / Client UI Lead | HIGH |
| client | `client/src/main/java/com/team/editor/client/ui/DocumentDashboardView.java` | Main document list/create/open screen before entering editor. | Member 2 / Client UI Lead | HIGH |
| client | `client/src/main/java/com/team/editor/client/ui/DocumentToolbar.java` | Editor toolbar for rename/delete/share/import/export actions. | Member 2 / Client UI Lead | MEDIUM |
| client | `client/src/main/java/com/team/editor/client/ui/ShareCodesDialog.java` | Dialog for showing editor/viewer codes only to allowed users. | Member 2 / Client UI Lead | MEDIUM |
| client | `client/src/main/java/com/team/editor/client/file/TextImportService.java` | Reads `.txt` files and prepares import text for CRDT conversion. | Member 4 / Import-Export + QA/Docs | LOW |
| client | `client/src/main/java/com/team/editor/client/file/TextExportService.java` | Writes visible document text to `.txt`. | Member 4 / Import-Export + QA/Docs | LOW |
| common-test | `common/src/test/java/com/team/editor/common/protocol/DocumentDtoSerializationTest.java` | Verifies new document DTO JSON compatibility. | Member 3 / Persistence Hardening + Protocol/Tests | MEDIUM |
| server-test | `server/src/test/java/com/team/editor/server/document/DocumentCommandServiceTest.java` | Tests create/rename/delete command behavior. | Omar / Backend Lead | HIGH |
| server-test | `server/src/test/java/com/team/editor/server/document/ShareCodeServiceTest.java` | Tests share-code generation, lookup, and collision behavior. | Member 3 / Persistence Hardening + Protocol/Tests | MEDIUM |
| server-test | `server/src/test/java/com/team/editor/server/persistence/MongoPersistenceTest.java` | Tests Mongo record save/load behavior or repository contract. | Member 3 / Persistence Hardening + Protocol/Tests | MEDIUM |
| server-test | `server/src/test/java/com/team/editor/server/document/DocumentAccessServiceTest.java` | Tests viewer/editor/owner permissions. | Member 3 / Persistence Hardening + Protocol/Tests | MEDIUM |
| client-test | `client/src/test/java/com/team/editor/client/controller/DocumentControllerTest.java` | Tests client document workflow state transitions. | Member 2 / Client UI Lead | MEDIUM |
| client-test | `client/src/test/java/com/team/editor/client/file/TextImportExportServiceTest.java` | Tests import/export file helpers without UI dependency. | Member 4 / Import-Export + QA/Docs | LOW |
| docs | `docs/mongodb-setup.md` | MongoDB setup and troubleshooting notes for the team. | Member 3 / Persistence Hardening + Protocol/Tests | LOW |
| docs | `docs/phase3-api-contract.md` | Documents document-management and share-code request/response contracts. | Member 4 / Import-Export + QA/Docs | MEDIUM |
| docs | `docs/phase3-demo-checklist.md` | Final demo checklist covering persistence, share codes, roles, import/export, and live editing. | Member 4 / Import-Export + QA/Docs | LOW |

## 5. Suggested Package Structure For Phase 3

Useful package folders to add later, only when creating the real placeholders or implementation:

```text
common/src/main/java/com/team/editor/common/protocol/dto/document/
common/src/main/java/com/team/editor/common/protocol/dto/persistence/

server/src/main/java/com/team/editor/server/config/
server/src/main/java/com/team/editor/server/persistence/
server/src/main/java/com/team/editor/server/repository/
server/src/main/java/com/team/editor/server/document/
server/src/main/java/com/team/editor/server/importexport/

client/src/main/java/com/team/editor/client/controller/
client/src/main/java/com/team/editor/client/service/
client/src/main/java/com/team/editor/client/ui/
client/src/main/java/com/team/editor/client/file/

common/src/test/java/com/team/editor/common/protocol/
server/src/test/java/com/team/editor/server/document/
server/src/test/java/com/team/editor/server/persistence/
client/src/test/java/com/team/editor/client/file/
```

Package guidance:

- Use `server.document` for document lifecycle/business flow, not the existing generic `server.service` package.
- Use `server.persistence` for Mongo record classes and `server.repository` for repository interfaces/classes.
- Keep client file import/export in `client.file` so it does not block Member 2's UI work.
- Keep shared document DTOs in `common.protocol.dto.document` so both client and server can use them.
- Keep CRDT snapshot/operation-log DTOs in `common.protocol.dto.persistence` only if they truly cross module boundaries.

## 6. Files That Should Probably Stay Untouched

These files are currently part of the working live-edit/WebSocket path and should not be rewritten wholesale during Phase 3:

- `common/src/main/java/com/team/editor/common/crdt/block/BlockCrdtEngine.java`
- `common/src/main/java/com/team/editor/common/crdt/block/BlockTree.java`
- `common/src/main/java/com/team/editor/common/crdt/character/CharacterCrdtEngine.java`
- `common/src/main/java/com/team/editor/common/crdt/character/CharacterTree.java`
- `common/src/main/java/com/team/editor/common/crdt/document/DocumentCrdt.java`
- `common/src/main/java/com/team/editor/common/ops/character/InsertCharOp.java`
- `common/src/main/java/com/team/editor/common/ops/character/DeleteCharOp.java`
- `common/src/main/java/com/team/editor/common/ops/block/InsertBlockOp.java`
- `common/src/main/java/com/team/editor/common/ops/block/DeleteBlockOp.java`
- `common/src/main/java/com/team/editor/common/protocol/dto/OperationMessage.java`
- `client/src/main/java/com/team/editor/client/websocket/ClientWebSocketHandler.java`
- `client/src/main/java/com/team/editor/client/controller/SessionController.java`
- `client/src/main/java/com/team/editor/client/controller/EditorController.java`
- `client/src/main/java/com/team/editor/client/state/EditorState.java`
- `client/src/main/java/com/team/editor/client/ui/EditorView.java`
- `server/src/main/java/com/team/editor/server/websocket/CollaborationWebSocketHandler.java`
- `server/src/main/java/com/team/editor/server/service/OperationRelayService.java`
- `server/src/main/java/com/team/editor/server/service/SessionJoinService.java`
- `server/src/main/java/com/team/editor/server/service/PresenceService.java`
- `server/src/main/java/com/team/editor/server/session/CollaborationSession.java`
- `server/src/main/java/com/team/editor/server/session/SessionRegistry.java`
- `server/src/main/java/com/team/editor/server/validation/MessageValidator.java`

Allowed later edits should be narrow integration changes, such as calling new document runtime/persistence services after protocol and document boundaries are defined. Avoid replacing the working relay path with a large rewrite.

## 7. Uncertainty And Suspicious Duplicates

- `common/src/main/java/com/team/editor/common/protocol/message/SessionRole.java` and `client/src/main/java/com/team/editor/client/session/SessionRole.java` duplicate the same editor/viewer concept.
- `server/pom.xml` uses artifact id `editor-server`, while the module path is `server`; this works in Maven but can confuse command naming.
- `server/src/main/java/com/team/editor/server/config/DatabaseConfig.java` and `server/src/main/resources/application.properties` are still H2/JDBC-oriented while Phase 3 is expected to use MongoDB.
- `server/src/main/java/com/team/editor/server/service/DocumentService.java` is currently empty and may either be expanded or superseded by `server.document.*`.
- Placeholder-only packages exist in `client.editor`, `client.network`, `client.service`, and `server.persistence`.
- `docs/project-context-for-codex.md` exists inside the repo, while a separate external context file also exists at `D:\CUFE\Semesters\Spring 2026\APT\Project\ChatGPT context file\project-context-for-codex.md`; keep them intentionally synchronized if both are used.
- `SplitBlockOp.java` exists in `common`, but no current WebSocket send/apply path was found for split operations.

## Next Prompt: Add Placeholder Files Only

```text
You are working on my APT Collaborative Text Editor Java Maven project at:
D:\CUFE\Semesters\Spring 2026\APT\Project\collab-text-editor

Read docs/phase3-file-plan.md first.

Task: add the proposed Phase 3 files as placeholders only.

Rules:
- Do not implement Phase 3 business logic.
- Do not modify existing working Java code unless needed only to compile package declarations/imports for placeholders.
- Create only the proposed files listed in "Proposed New Files To Add Later".
- Each new Java file should contain the correct package declaration and a minimal class/interface/record/enum skeleton.
- Add TODO comments describing the intended responsibility.
- Keep placeholder methods empty or omitted unless a minimal method is required for compilation.
- Do not wire placeholders into the running app yet.
- Do not add MongoDB dependencies yet unless the placeholders cannot compile without them; prefer plain placeholder classes that avoid external dependencies.
- After creating placeholders, run `mvn test` and report the exact result.

Preserve the existing WebSocket/live-edit path. This placeholder task is only to prepare the file structure for Phase 3 team distribution.
```
