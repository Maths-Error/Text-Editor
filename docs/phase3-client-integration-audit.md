# Phase 3 Client Integration Audit

## Summary

The current client is a Swing live-collaboration client from the earlier phase. It can open a join window, connect to the WebSocket endpoint, join a manually typed session, open an editor window, create local CRDT operations, send them over WebSocket, and apply remote operations.

Phase 3 document management is not integrated yet. The client has placeholder classes for document APIs, document session metadata, dashboard UI, share-code UI, toolbar UI, import, and export, but those classes do not contain implementation. There is no HTTP document API client, no persisted `documentId` workflow, no snapshot save/load workflow, no stored `snapshotVersion`, and no role-based UI restriction.

The best path is to keep the existing Swing editor and CRDT operation flow, then add Phase 3 in small batches around it. The first real implementation batch should be LAN-ready server URL configuration, because both HTTP and WebSocket need the same configurable server address.

## Current Client Architecture

The client is Swing-based with console logging.

- `ClientApp` starts Swing on the event dispatch thread and opens `JoinSessionView`.
- `JoinSessionView` collects a session id and username, then asks `SessionController` to connect and join.
- `SessionController` owns `ClientWebSocketHandler`, stores a `ClientSession`, and broadcasts WebSocket events to UI listeners.
- `ClientWebSocketHandler` uses Java's built-in `java.net.http.HttpClient` WebSocket support.
- `EditorView` is the main Swing editor window with a `JTextPane`, connection status bar, and active users panel.
- `EditorController` converts local text edits into CRDT operations and applies remote operations into `EditorState`.
- `EditorState` owns the in-memory `DocumentCrdt`.
- Phase 3 classes exist as placeholders: `DocumentController`, `DocumentClientService`, `DocumentSessionService`, `DocumentDashboardView`, `DocumentToolbar`, `ShareCodesDialog`, `TextImportService`, and `TextExportService`.

## Current Startup Flow

`ClientApp` applies `FlatDarkLaf`, creates a `SessionController`, creates a `JoinSessionView`, and calls `view.start()`.

The first screen is `JoinSessionView`. It shows:

- `Session ID`, defaulting to `demo-room`
- `Username`, defaulting to `user` plus a random number
- `Join Session` button

When the button is clicked:

- `JoinSessionView` reads the typed session id and username.
- It calls `controller.connect()`.
- It calls `controller.joinSession(sessionId, currentUsername)`.
- After a successful `JOIN_SESSION_RESPONSE`, it hides the join window.
- It creates `EditorState` with the username and typed session id.
- It creates `EditorController`.
- It creates and shows `EditorView`.

The current startup flow is not document-oriented. It joins a manually entered live session directly. `userId` and `displayName` are both effectively the username typed into `JoinSessionView`. There is no Phase 3 dashboard first, no create/open/list screen first, and no HTTP open/create before WebSocket join.

## Current HTTP Support

There is no implemented HTTP client for the Phase 3 backend document APIs.

`DocumentClientService` exists but is only a TODO placeholder. It does not support:

- create document
- open by id
- list documents
- open by share code
- get share codes
- rename document
- delete document
- save snapshot

The only current use of `java.net.http.HttpClient` is inside `ClientWebSocketHandler` for WebSocket connection creation. There is no `RestTemplate`, and no other HTTP transport layer is present in the client.

There is no client-side server base URL setting. The WebSocket URL is hardcoded as:

```text
ws://localhost:8080/ws/collab
```

Because of this, the client is not LAN-demo ready. A client laptop cannot point at the server laptop IP without editing source code.

## Current WebSocket Support

`ClientWebSocketHandler` opens the WebSocket connection using Java `HttpClient.newWebSocketBuilder()`.

`SessionController` sends `JOIN_SESSION_REQUEST` through `webSocketHandler.sendJoinRequest(request)`.

`EditorController` sends operation messages through `sessionController.sendOperation(msg)`, which forwards to `ClientWebSocketHandler.sendOperation(message)`.

`ClientWebSocketHandler.handleIncoming(String json)` receives incoming WebSocket JSON, reads `messageType`, deserializes the message, and dispatches it to the listener.

Incoming messages are then routed like this:

- `JOIN_SESSION_RESPONSE` goes to `SessionController.onJoinSessionResponse`.
- `ERROR` goes to `SessionController.onError`.
- `PRESENCE_UPDATE` goes to `JoinSessionView.onPresenceUpdate`.
- `CURSOR_UPDATE` goes to `EditorController.handleRemoteCursor`.
- `OPERATION` goes to `EditorController.handleRemoteOperation`.

Current session behavior:

- `sessionId` is manually entered in `JoinSessionView`.
- It is not randomly generated.
- It is not based on a backend `documentId`.
- `ClientSession` role is locally hardcoded to `EDITOR`.
- The actual `JoinSessionRequest` uses the constructor without role, so the outgoing JSON does not intentionally include the HTTP-resolved role.
- The client does not use the role returned by HTTP create/open because HTTP create/open is not integrated.
- WebSocket localhost is hardcoded and cannot be changed from the UI or config.

## Current State Management

Current session state is split across a few classes.

- `ClientSession` stores `sessionId`, `userId`, `displayName`, and a client-local `SessionRole`.
- `SessionController` stores the active `ClientSession` and `ConnectionState`.
- `EditorState` stores the `DocumentCrdt` built with a `DocumentId` created from the current session id.
- `JoinSessionView` temporarily stores the current username and pending incoming messages until the editor view is created.

There is no current class that stores Phase 3 document metadata:

- no canonical `documentId` separate from old `sessionId`
- no document title
- no owner id
- no HTTP `DocumentRole`
- no `canEdit`
- no `canShare`
- no editor/viewer share codes
- no `snapshotVersion`

`DocumentSessionService` is the right existing placeholder to extend for selected document metadata and resolved role. `ClientSession` should continue to represent the live WebSocket session, but it should be filled from document open/create results instead of manual room input.

## Current Editor and CRDT Support

Local editing happens in `EditorView`.

`EditorView` uses a `DocumentFilter` on `JTextPane` to intercept inserts, deletes, replacements, paste, cut, and newline handling. It forwards local changes to `EditorController`.

Operations are created in `EditorController`:

- character insert creates `InsertCharOp` inside `BlockOpPayload`
- character delete creates `DeleteCharOp` inside `BlockOpPayload`
- paste sends repeated character insert operations
- range delete sends repeated character delete operations
- newline creates a block insert operation

Remote operations are applied in `EditorController.handleRemoteOperation`.

The client does maintain in-memory CRDT state through `EditorState`, `DocumentCrdt`, and `BlockCrdtEngine`. It can render visible text with `editorState.getText()`.

The client cannot currently produce a real `CrdtSnapshotDto` from its current document state. It also cannot load a `CrdtSnapshotDto` into `EditorState`.

Missing pieces for snapshot integration:

- A simple mapper from current CRDT blocks/chars/tombstones into `CrdtSnapshotDto.blocks`.
- A simple loader from `CrdtSnapshotDto` back into `EditorState` or a new `DocumentCrdt`.
- A decision on the exact block/character JSON shape inside the flexible `blocks` list.
- Storage of `snapshotVersion` after create/open/save.
- Save flow that calls `PUT /api/documents/{documentId}/snapshot`.
- Conflict handling for HTTP `409 SNAPSHOT_CONFLICT`.

## Current Import/Export Support

`TextImportService` is a placeholder.

`TextExportService` is a placeholder.

Neither service currently works with visible text or CRDT snapshots.

Import/export should remain client-side for the first Phase 3 integration unless the team later decides to implement the optional backend import/export endpoints. For now:

- Export should write visible text derived from the current CRDT state.
- Import should read text on the client and insert it into the editor through the same simple local-operation path used by paste.
- After import, the user should be able to save the resulting CRDT snapshot through the normal snapshot save flow.

Import/export should not become the canonical persistence path. Snapshot save/load must remain the canonical persisted document state.

## Recommended Client Integration Plan

Client Batch 1: server URL / LAN readiness

- Add one simple configurable server base URL used by both HTTP and WebSocket.
- Prefer a beginner-friendly approach: default to `http://localhost:8080`, allow changing to `http://SERVER_LAPTOP_IP:8080`.
- Derive WebSocket URL from the same setting: `http` to `ws`, plus `/ws/collab`.
- Do this before HTTP methods, because every later batch needs it.

Client Batch 2: HTTP document API integration

- Implement `DocumentClientService` with Java `HttpClient` and Jackson.
- Keep one method per backend endpoint.
- Use existing common DTOs.
- Return DTOs directly and handle simple error messages clearly.
- Do not add dependencies.

Client Batch 3: create/open/list/open-by-code UI flow

- Use `DocumentDashboardView` as the first document-oriented screen after user identity/server URL input.
- Support create document, list documents, open selected document, and open by share code.
- Keep `JoinSessionView` only if it is adapted or replaced as a simple login/server screen.
- Do not let the user manually choose the final WebSocket `sessionId`.

Client Batch 4: WebSocket join using documentId

- After HTTP create/open/open-by-code succeeds, set selected document state.
- Join WebSocket with `sessionId = documentId`.
- Send role from the HTTP response, mapping `OWNER` to WebSocket `EDITOR`.
- Avoid joining WebSocket before HTTP open/create succeeds.

Client Batch 5: role-based editing/viewer restrictions

- Store `canEdit` and `canShare` in `DocumentSessionService`.
- Set `EditorView` editable only when `canEdit` is true.
- Block local operation creation when `canEdit` is false.
- Hide or disable save, import, rename, delete, and share controls based on permissions.

Client Batch 6: snapshot save/load using snapshotVersion

- Load the HTTP response snapshot into `EditorState` before WebSocket join or before showing the editor.
- Store `snapshotVersion` from create/open/open-by-code.
- On save, build `SaveSnapshotRequest` with `baseSnapshotVersion`.
- After save succeeds, update stored `snapshotVersion`.
- On `SNAPSHOT_CONFLICT`, show a simple message telling the user to reopen/reload.

Client Batch 7: import/export integration

- Implement `TextImportService` and `TextExportService`.
- Import plain text through existing local edit methods so CRDT state and WebSocket operations stay consistent.
- Export from `EditorState.getText()`.
- After import, user saves through the normal snapshot save action.

Client Batch 8: final LAN demo checklist

- Fill `docs/phase3-demo-checklist.md` with exact laptop IP setup steps.
- Verify client laptops use the server laptop IP, not localhost.
- Demo owner creates a document.
- Demo editor opens with editor code and can edit.
- Demo viewer opens with viewer code and cannot edit or see share codes.
- Demo save, close/reopen, and snapshot reload.
- Demo import/export if included.

## File-by-File Recommendations

| File | Current role | Keep / Modify / Create / Avoid | Why |
| --- | --- | --- | --- |
| `docs/phase3-api-contract.md` | Source of truth for Phase 3 API and WebSocket handoff. | Keep | Use it as the implementation contract. |
| `docs/phase3-demo-checklist.md` | Placeholder demo checklist. | Modify later | Fill during Batch 8 after client flow is real. |
| `client/pom.xml` | Client dependencies: common, Jackson, FlatLaf, JUnit. | Keep | Already has needed dependencies; no new dependency is needed. |
| `ClientApp.java` | Starts Swing and opens `JoinSessionView`. | Modify | Eventually start with server/user/document flow instead of direct session join. |
| `controller/SessionController.java` | Owns WebSocket handler and live `ClientSession`. | Modify | Keep for WebSocket lifecycle, but accept configurable URL and join using `documentId` plus resolved role. |
| `controller/EditorController.java` | Converts local edits to CRDT operations and applies remote operations. | Modify carefully | Keep existing operation flow; add `canEdit` checks and snapshot conversion hooks without rewriting the editor. |
| `controller/DocumentController.java` | Placeholder for Phase 3 document coordination. | Modify | Good place to coordinate dashboard actions, HTTP service, selected document state, and editor launch. |
| `service/DocumentClientService.java` | Placeholder transport service. | Modify | Implement Java `HttpClient` calls here, one simple method per endpoint. |
| `service/DocumentSessionService.java` | Placeholder selected document metadata service. | Modify | Store `documentId`, title, role, `canEdit`, `canShare`, share codes, snapshot, and `snapshotVersion`. |
| `session/ClientSession.java` | Stores live session id, user id, display name, and local live role. | Modify | Keep for WebSocket session data, but populate from HTTP document result. |
| `session/SessionRole.java` | Client-local duplicate of live roles. | Avoid expanding | Prefer common protocol role for WebSocket messages to avoid role duplication confusion. |
| `state/EditorState.java` | Owns in-memory `DocumentCrdt` and visible text helpers. | Modify | Add simple snapshot load/export support or delegate to a helper while keeping this the editor state boundary. |
| `state/ConnectionState.java` | Simple WebSocket connection enum. | Keep | Enough for current connection UI. |
| `ui/JoinSessionView.java` | Current first screen and direct manual WebSocket join UI. | Modify or replace carefully | Phase 3 should not manually join arbitrary session ids; it should create/open documents first. |
| `ui/EditorView.java` | Main editor window and local text edit capture. | Modify carefully | Add viewer read-only mode and toolbar integration; keep current text edit path. |
| `ui/DocumentDashboardView.java` | Placeholder dashboard. | Modify | Use for create/list/open/open-by-code flow. |
| `ui/DocumentToolbar.java` | Placeholder toolbar. | Modify | Use for save, rename, delete, share, import, export actions. |
| `ui/ShareCodesDialog.java` | Placeholder share-code dialog. | Modify | Show codes only when `canShare` is true. |
| `ui/ConnectionStatusBar.java` | Displays connection state. | Keep | Can be reused as-is. |
| `ui/ActiveUsersPanel.java` | Displays active users. | Keep | Can be reused as-is. |
| `websocket/ClientWebSocketHandler.java` | Opens hardcoded WebSocket, sends/receives flat JSON messages. | Modify | Add configurable WebSocket URI and keep flat message format. |
| `websocket/OutboundMessageDispatcher.java` | Small sending interface. | Keep | Useful boundary for tests/controllers. |
| `file/TextImportService.java` | Placeholder import service. | Modify | Implement simple client-side text file reading later. |
| `file/TextExportService.java` | Placeholder export service. | Modify | Implement simple client-side visible text export later. |
| `renderer/DocumentRenderer.java` | Empty rendering boundary. | Avoid for Phase 3 | Not needed for document API integration. |
| `common/protocol/dto/document/*` | Shared HTTP document DTOs. | Keep | Already match the contract well enough for client use. |
| `common/protocol/dto/persistence/CrdtSnapshotDto.java` | Flexible snapshot DTO. | Keep | Use for save/load; define client block contents consistently. |
| `common/protocol/dto/JoinSessionRequest.java` | WebSocket join DTO with optional role. | Keep | Client should set role after HTTP open/create. |
| `common/protocol/dto/OperationMessage.java` | WebSocket operation DTO. | Keep | Current editor uses it already. |
| `common/protocol/message/*` | Shared message enums and interface. | Keep | Use common `SessionRole` for WebSocket role mapping. |

## Risks and Fixes

| Risk | Why it matters | Recommended fix |
| --- | --- | --- |
| Hardcoded `localhost` WebSocket URL | LAN demo clients will try to connect to themselves instead of the server laptop. | Add configurable server base URL in Batch 1. |
| No HTTP document API client | Client cannot create/open/list/save persistent documents. | Implement `DocumentClientService` in Batch 2. |
| WebSocket joins before HTTP open/create | The client can join a live room with no persistent document or resolved permission. | Make HTTP create/open/open-by-code mandatory before WebSocket join. |
| Manual `sessionId` input | Violates Phase 3 rule that `documentId` must be WebSocket `sessionId`. | Use `documentId` from HTTP response as the only live session id. |
| Role is locally hardcoded to editor | Viewers may still be allowed to type/send operations from the UI. | Store HTTP role/permissions and enforce `canEdit` in UI and controller. |
| `JoinSessionRequest` does not currently set role | Phase 3 compatibility expects role to be sent after HTTP resolution. | Map `OWNER` and `EDITOR` to WebSocket `EDITOR`, `VIEWER` to WebSocket `VIEWER`. |
| Duplicate role enums | `client.session.SessionRole` and `common.protocol.message.SessionRole` can drift. | Prefer common protocol role for WebSocket messages; keep document role separate as `DocumentRole`. |
| No `snapshotVersion` storage | Saves cannot safely use `baseSnapshotVersion`, and conflicts cannot be handled. | Store it in `DocumentSessionService` and update after every successful save. |
| No snapshot serializer/loader | Persistence cannot save/load real CRDT state. | Add a simple, documented CRDT snapshot mapping before save/load UI. |
| Viewer UI still editable | `EditorView` always calls `textPane.setEditable(true)`. | Set editable from `canEdit` and block operation methods too. |
| Import/export placeholders | Demo import/export cannot work yet. | Implement after snapshot save/load, using visible text for export and local operations for import. |
| Import/export may not match CRDT snapshot shape | Saving only visible text would break the contract that snapshot is canonical. | Treat import/export as convenience only; persist CRDT snapshots. |
| Current logs are very noisy | Console output may make demo debugging harder. | Reduce debug prints after integration stabilizes. |

## Final Recommendation

Do not rewrite the client. Keep the current Swing editor, `EditorController`, `EditorState`, and WebSocket message flow as the live-edit foundation.

Start Phase 3 client integration with server URL configuration and LAN readiness. Then implement `DocumentClientService` with Java `HttpClient`, store selected document metadata in `DocumentSessionService`, and only then change the UI flow so create/open/open-by-code happens before WebSocket join.

The most important behavior rule for the first implementation is simple: HTTP document open/create decides the `documentId`, role, permissions, snapshot, and `snapshotVersion`; WebSocket only joins afterward using `documentId` as `sessionId`.
