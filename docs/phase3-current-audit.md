# Phase 3 Current Audit

## A. Audit Metadata

- Audit date/time: `2026-05-07 14:21:31 +03:00`
- Repo path: `D:\CUFE\Semesters\Spring 2026\APT\Project\collab-text-editor`
- Branch: `main`
- Git status at audit start: `M server/src/main/java/com/team/editor/server/websocket/CollaborationWebSocketHandler.java`
- Total current file count excluding `target/`, `.git/`, `.idea/`, `.vscode/`, `node_modules/`, and `out/`: `177`
- Maven test result: `SUCCESS` from `mvn test` at `2026-05-07T14:25:37+03:00`

Notes:

- The modified WebSocket handler was already dirty before this audit. It was inspected but not changed.
- The repository had `176` counted files before creating this audit document; it has `177` after adding `docs/phase3-current-audit.md`.
- `docs/phase3-file-plan.md` is now a historical plan, not the source of truth. Updating it fully would be destructive because most of its proposed files now exist and need a status-based audit instead of another copy of the old proposal.
- Test summary: reactor modules `collab-text-editor`, `common`, `client`, and `editor-server` all succeeded. Test counts were `common: 72`, `client: 60`, `editor-server: 36`, total `168`, with `0` failures, `0` errors, and `0` skipped.
- Important test log noise: `EditorServerApplicationTests` logged a MongoDB monitor-thread `MongoSocketOpenException` for `localhost:27017`, but the Spring context and build still succeeded. One client test intentionally printed `java.lang.IllegalArgumentException: boom` while testing serialization failure handling; it did not fail the test suite.

## B. Current Project Tree

Placeholder marker: `[placeholder package: package-info.java only]`.

Placeholder-only files are called out after the tree because some packages now contain real files plus old `package-info.java` files.

```text
collab-text-editor/
|-- .github/
|   `-- java-upgrade/
|       |-- hooks/
|       |   `-- scripts/
|       |       |-- recordToolUse.ps1
|       |       `-- recordToolUse.sh
|       `-- .gitignore
|-- client/
|   |-- src/
|   |   |-- main/
|   |   |   `-- java/
|   |   |       `-- com/
|   |   |           `-- team/
|   |   |               `-- editor/
|   |   |                   `-- client/
|   |   |                       |-- config/
|   |   |                       |   `-- ClientConfig.java
|   |   |                       |-- controller/
|   |   |                       |   |-- DocumentController.java
|   |   |                       |   |-- EditorController.java
|   |   |                       |   `-- SessionController.java
|   |   |                       |-- cursor/
|   |   |                       |   |-- CursorManager.java
|   |   |                       |   `-- RemoteCursor.java
|   |   |                       |-- editor/ [placeholder package: package-info.java only]
|   |   |                       |   `-- package-info.java
|   |   |                       |-- file/
|   |   |                       |   |-- TextExportService.java
|   |   |                       |   `-- TextImportService.java
|   |   |                       |-- network/ [placeholder package: package-info.java only]
|   |   |                       |   `-- package-info.java
|   |   |                       |-- renderer/
|   |   |                       |   `-- DocumentRenderer.java
|   |   |                       |-- service/
|   |   |                       |   |-- ClientApiException.java
|   |   |                       |   |-- DocumentClientService.java
|   |   |                       |   |-- DocumentSessionService.java
|   |   |                       |   `-- package-info.java
|   |   |                       |-- session/
|   |   |                       |   |-- ClientSession.java
|   |   |                       |   `-- SessionRole.java
|   |   |                       |-- state/
|   |   |                       |   |-- ConnectionState.java
|   |   |                       |   `-- EditorState.java
|   |   |                       |-- ui/
|   |   |                       |   |-- ActiveUsersPanel.java
|   |   |                       |   |-- ConnectionStatusBar.java
|   |   |                       |   |-- DocumentDashboardView.java
|   |   |                       |   |-- DocumentToolbar.java
|   |   |                       |   |-- EditorView.java
|   |   |                       |   |-- JoinSessionView.java
|   |   |                       |   `-- ShareCodesDialog.java
|   |   |                       |-- websocket/
|   |   |                       |   |-- ClientWebSocketHandler.java
|   |   |                       |   `-- OutboundMessageDispatcher.java
|   |   |                       `-- ClientApp.java
|   |   `-- test/
|   |       `-- java/com/team/editor/client/
|   |           |-- config/ClientConfigTest.java
|   |           |-- controller/
|   |           |   |-- DocumentControllerTest.java
|   |           |   |-- EditorControllerTest.java
|   |           |   `-- SessionControllerTest.java
|   |           |-- file/TextImportExportServiceTest.java
|   |           |-- scratch/
|   |           |-- service/
|   |           |   |-- ClientApiExceptionTest.java
|   |           |   `-- DocumentSessionServiceTest.java
|   |           |-- state/EditorStateTest.java
|   |           |-- websocket/ClientWebSocketHandlerTest.java
|   |           `-- package-info.java
|   `-- pom.xml
|-- common/
|   |-- src/
|   |   |-- main/java/com/team/editor/common/
|   |   |   |-- crdt/
|   |   |   |   |-- block/
|   |   |   |   |   |-- BlockCrdtEngine.java
|   |   |   |   |   |-- BlockId.java
|   |   |   |   |   |-- BlockNode.java
|   |   |   |   |   `-- BlockTree.java
|   |   |   |   |-- character/
|   |   |   |   |   |-- CharacterCrdtEngine.java
|   |   |   |   |   |-- CharacterOrdering.java
|   |   |   |   |   |-- CharacterTree.java
|   |   |   |   |   |-- CharId.java
|   |   |   |   |   `-- CharNode.java
|   |   |   |   `-- document/DocumentCrdt.java
|   |   |   |-- model/
|   |   |   |   |-- DocumentId.java
|   |   |   |   `-- UserId.java
|   |   |   |-- ops/
|   |   |   |   |-- block/
|   |   |   |   |   |-- DeleteBlockOp.java
|   |   |   |   |   |-- InsertBlockOp.java
|   |   |   |   |   `-- SplitBlockOp.java
|   |   |   |   `-- character/
|   |   |   |       |-- DeleteCharOp.java
|   |   |   |       `-- InsertCharOp.java
|   |   |   `-- protocol/
|   |   |       |-- dto/
|   |   |       |   |-- document/
|   |   |       |   |   |-- CreateDocumentRequest.java
|   |   |       |   |   |-- CreateDocumentResponse.java
|   |   |       |   |   |-- DeleteDocumentRequest.java
|   |   |       |   |   |-- DeleteDocumentResponse.java
|   |   |       |   |   |-- DocumentRole.java
|   |   |       |   |   |-- DocumentSummaryDto.java
|   |   |       |   |   |-- ListDocumentsResponse.java
|   |   |       |   |   |-- OpenDocumentRequest.java
|   |   |       |   |   |-- OpenDocumentResponse.java
|   |   |       |   |   |-- RenameDocumentRequest.java
|   |   |       |   |   |-- RenameDocumentResponse.java
|   |   |       |   |   |-- SaveSnapshotRequest.java
|   |   |       |   |   |-- SaveSnapshotResponse.java
|   |   |       |   |   `-- ShareCodeResponse.java
|   |   |       |   |-- persistence/
|   |   |       |   |   |-- CrdtSnapshotDto.java
|   |   |       |   |   `-- OperationLogEntryDto.java
|   |   |       |   |-- BlockOpPayload.java
|   |   |       |   |-- CursorUpdateMessage.java
|   |   |       |   |-- ErrorMessage.java
|   |   |       |   |-- JoinSessionRequest.java
|   |   |       |   |-- JoinSessionResponse.java
|   |   |       |   |-- OperationMessage.java
|   |   |       |   `-- PresenceUpdateMessage.java
|   |   |       `-- message/
|   |   |           |-- CollaborationMessage.java
|   |   |           |-- MessageType.java
|   |   |           |-- OperationKind.java
|   |   |           |-- PresenceEventType.java
|   |   |           `-- SessionRole.java
|   |   `-- test/java/com/team/editor/common/
|   |       |-- block/
|   |       |-- character/
|   |       |-- crdt/document/DocumentCrdtIntegrationTest.java
|   |       |-- document/DocumentCrdtTest.java
|   |       |-- model/
|   |       `-- protocol/DocumentDtoSerializationTest.java
|   `-- pom.xml
|-- docs/
|   |-- architecture/project-structure.md
|   |-- CUFE docs/Collaborative Text Editing App.pdf
|   |-- crdt-rules.md
|   |-- mongodb-setup.md
|   |-- phase3-api-contract.md
|   |-- phase3-client-integration-audit.md
|   |-- phase3-crdt-persistence-analysis.md
|   |-- phase3-current-audit.md
|   |-- phase3-demo-checklist.md
|   `-- phase3-file-plan.md
|-- server/
|   |-- .mvn/wrapper/maven-wrapper.properties
|   |-- src/
|   |   |-- main/
|   |   |   |-- java/
|   |   |   |   |-- .gitkeep
|   |   |   |   `-- com/team/editor/server/
|   |   |   |       |-- config/
|   |   |   |       |   |-- DatabaseConfig.java
|   |   |   |       |   |-- MongoConfig.java
|   |   |   |       |   |-- MongoConnectionLogger.java
|   |   |   |       |   |-- ServerConfig.java
|   |   |   |       |   `-- WebSocketConfig.java
|   |   |   |       |-- document/
|   |   |   |       |   |-- DocumentAccessService.java
|   |   |   |       |   |-- DocumentApiException.java
|   |   |   |       |   |-- DocumentApiExceptionHandler.java
|   |   |   |       |   |-- DocumentCommandService.java
|   |   |   |       |   |-- DocumentManagementController.java
|   |   |   |       |   |-- DocumentMapper.java
|   |   |   |       |   |-- DocumentQueryService.java
|   |   |   |       |   |-- DocumentRuntimeService.java
|   |   |   |       |   `-- ShareCodeService.java
|   |   |   |       |-- importexport/
|   |   |   |       |   |-- TextExportService.java
|   |   |   |       |   `-- TextImportService.java
|   |   |   |       |-- persistence/
|   |   |   |       |   |-- CrdtSnapshotRecord.java
|   |   |   |       |   |-- MongoDocumentRecord.java
|   |   |   |       |   |-- OperationLogRecord.java
|   |   |   |       |   |-- package-info.java
|   |   |   |       |   `-- ShareCodeRecord.java
|   |   |   |       |-- repository/
|   |   |   |       |   |-- CrdtSnapshotRepository.java
|   |   |   |       |   |-- DocumentRepository.java
|   |   |   |       |   |-- InMemoryDocumentRepository.java
|   |   |   |       |   |-- MongoDocumentRepository.java
|   |   |   |       |   |-- OperationLogRepository.java
|   |   |   |       |   `-- ShareCodeRepository.java
|   |   |   |       |-- service/
|   |   |   |       |   |-- DocumentService.java
|   |   |   |       |   |-- OperationRelayService.java
|   |   |   |       |   |-- PermissionService.java
|   |   |   |       |   |-- PresenceService.java
|   |   |   |       |   `-- SessionJoinService.java
|   |   |   |       |-- session/
|   |   |   |       |-- validation/
|   |   |   |       `-- websocket/CollaborationWebSocketHandler.java
|   |   |   `-- resources/application.properties
|   |   `-- test/java/com/team/editor/server/
|   |       |-- document/
|   |       |   |-- DocumentAccessServiceTest.java
|   |       |   |-- DocumentBatch1ServiceTest.java
|   |       |   |-- DocumentBatch2ServiceTest.java
|   |       |   |-- DocumentCommandServiceTest.java
|   |       |   |-- DocumentSnapshotServiceTest.java
|   |       |   `-- ShareCodeServiceTest.java
|   |       |-- persistence/MongoPersistenceTest.java
|   |       `-- websocket/CollaborationWebSocketHandlerTest.java
|   |-- .gitattributes
|   |-- HELP.md
|   |-- mvnw
|   |-- mvnw.cmd
|   `-- pom.xml
|-- .gitignore
|-- docker-compose.yml
|-- pom.xml
`-- README.md
```

Placeholder-only files/packages:

- `client/src/main/java/com/team/editor/client/editor/package-info.java`
- `client/src/main/java/com/team/editor/client/network/package-info.java`
- `client/src/main/java/com/team/editor/client/ui/DocumentToolbar.java`
- `common/src/main/java/com/team/editor/common/protocol/dto/persistence/OperationLogEntryDto.java`
- `server/src/main/java/com/team/editor/server/document/DocumentRuntimeService.java`
- `server/src/main/java/com/team/editor/server/importexport/TextImportService.java`
- `server/src/main/java/com/team/editor/server/importexport/TextExportService.java`
- `server/src/main/java/com/team/editor/server/persistence/CrdtSnapshotRecord.java`
- `server/src/main/java/com/team/editor/server/persistence/OperationLogRecord.java`
- `server/src/main/java/com/team/editor/server/persistence/ShareCodeRecord.java`
- `server/src/main/java/com/team/editor/server/repository/CrdtSnapshotRepository.java`
- `server/src/main/java/com/team/editor/server/repository/OperationLogRepository.java`
- `server/src/main/java/com/team/editor/server/repository/ShareCodeRepository.java`
- `server/src/main/java/com/team/editor/server/service/DocumentService.java`
- `client/src/test/java/com/team/editor/client/controller/DocumentControllerTest.java`
- `server/src/test/java/com/team/editor/server/document/DocumentAccessServiceTest.java`
- `server/src/test/java/com/team/editor/server/document/DocumentCommandServiceTest.java`
- `server/src/test/java/com/team/editor/server/document/ShareCodeServiceTest.java`
- `server/src/test/java/com/team/editor/server/persistence/MongoPersistenceTest.java`

## C. What Changed Since Old Phase 3 Plan

Added files and directories since the old file plan:

- Top-level support: `docker-compose.yml`, `.github/java-upgrade/**`.
- Docs: `docs/mongodb-setup.md`, `docs/phase3-api-contract.md`, `docs/phase3-client-integration-audit.md`, `docs/phase3-crdt-persistence-analysis.md`, `docs/phase3-current-audit.md`.
- Common protocol: `OperationKind`, document DTOs, `CrdtSnapshotDto`, and placeholder `OperationLogEntryDto`.
- Server persistence/config/API: Mongo config, Mongo document record/repository, document controller/services/mapper/exception handling, document validation, placeholder operation/share-code/snapshot records and repositories.
- Server WebSocket integration: `SessionJoinService` now resolves WebSocket role from stored document access and rejects missing documents.
- Client document flow: `DocumentController`, `DocumentClientService`, `DocumentSessionService`, `DocumentDashboardView`, `ShareCodesDialog`, import/export helpers, save/export/read-only UI wiring in `EditorView` and `EditorController`.
- Tests: document DTO serialization, document batch service tests, snapshot tests, client config/session/file tests, and more editor/session tests.

Removed or missing compared with the old plan/tree:

- `docs/project-context-for-codex.md` is referenced by the old plan but is not present.
- `docs/review-abdo-phase2.md` is referenced by the old tree but is not present.
- `docs/CUFE docs/[CMPS211] Project Document (S2026).pdf` was listed in the old tree but is not present in the current filtered file list.
- Some `package-info.java` files referenced by `docs/architecture/project-structure.md` are not present in current packages.

Moved/renamed files:

- No clear Java package move was found. Most current Phase 3 files match the old proposed paths.
- The server module directory is still `server`, while its Maven artifact id is `editor-server`.

Files that changed role:

- `server/src/main/java/com/team/editor/server/persistence/package-info.java` is no longer the only persistence file; `server.persistence` now contains one implemented record and several placeholder records.
- `client/src/main/java/com/team/editor/client/service/package-info.java` is no longer a placeholder-only package because `DocumentClientService`, `DocumentSessionService`, and `ClientApiException` now exist.
- `ClientApp.java` now starts `DocumentDashboardView`, not the older direct join-session UI.
- `OperationMessage` now uses shared `OperationKind`, not only a raw string.
- `SessionJoinService` now depends on `DocumentRepository` and `DocumentAccessService`, making WebSocket join document-aware.

Outdated assumptions from the old plan:

- The old plan says the Phase 3 files should be added later; most have already been added.
- The old plan says persistence is not implemented; Mongo document persistence is partially implemented.
- The old plan says client document management UI does not exist; a dashboard now exists and is the app entry point.
- The old plan says share-code DTO/service files are future work; basic share-code generation and open-by-code exist.
- The old plan still treats `client.service` and `server.persistence` as placeholder-only packages; they are not.

## D. Current Phase 3 Readiness

| Requirement | Current status | Evidence from files | Remaining work | Suggested owner |
| --- | --- | --- | --- | --- |
| persistent database storage | PARTIAL | `server/pom.xml` has Mongo dependency; `MongoConfig`, `MongoDocumentRepository`, `MongoDocumentRecord`, `application.properties`, `docker-compose.yml` exist. | Prove with real Mongo integration test, remove/clarify old H2 config, decide indexes/unique codes, document startup accurately. | Omar |
| document create | DONE | `DocumentCommandService.createDocument`, `DocumentManagementController`, `CreateDocumentRequest/Response`, `DocumentBatch1ServiceTest`. | Add controller/API test if time allows. | Omar |
| document list | PARTIAL | `DocumentQueryService.listDocuments` calls `findByOwnerId`; mapper returns owner summaries only. | Include editor/viewer accessible documents if that is required for dashboard after share-code access. | Omar + Client |
| document open by id | PARTIAL | `openDocumentById`, `DocumentAccessService.canOpenById`, tests for owner/editor/viewer after code. | Confirm expected behavior for users who only have a share code but have not opened by code before. | Omar |
| document open by share code | DONE | `openDocumentByCode` stores editor/viewer user id and returns role-specific response; `DocumentBatch2ServiceTest`. | Add controller-level test and optional code revocation policy if needed. | Omar |
| document rename | PARTIAL | `renameDocument` exists and owner-only access is implemented. | Tests appear placeholder-only in `DocumentCommandServiceTest`; dashboard rename UI is not wired. | Omar + Client |
| document delete | PARTIAL | `deleteDocument` soft-deletes; dashboard delete exists; owner-only check. | Active WebSocket users are not notified/disconnected; tests need to cover delete behavior. | Omar + Client |
| share code generation | DONE | `ShareCodeService`, generated `EDT-` and `VWR-` codes, codes stored on `MongoDocumentRecord`. | Collision loop should fail explicitly after max attempts; no separate `ShareCodeRecord` is used. | Omar |
| viewer/editor permission handling | PARTIAL | `DocumentAccessService`; `SessionJoinService` resolves role from stored access; viewer operation rejection remains in WebSocket handler. | Add real tests for document-aware WebSocket join; decide if editors may share or rename. | Omar |
| CRDT snapshot save/load | PARTIAL | `SaveSnapshotRequest/Response`, `DocumentCommandService.saveSnapshot`, `EditorState.toSnapshotDto/loadSnapshot`, `EditorController.saveSnapshot`. | Snapshot is text-block DTO derived from visible text, not a full durable dump of internal CRDT ids/tombstones. | Omar + CRDT owner |
| snapshot version/conflict handling | MISSING | DTO has `baseSnapshotVersion`; repository increments `snapshotVersion`. | Current save ignores `baseSnapshotVersion`; tests even accept stale and negative versions. Implement 409 conflict before demo if required. | Omar |
| WebSocket integration with documentId/sessionId | PARTIAL | `SessionJoinService` requires stored document by session id and maps owner/editor to editor role. | Existing README WebSocket demo with `demo-room` is now stale; add tests for document-aware join and ensure client open flow is stable. | Omar |
| operation persistence or runtime replay | PARTIAL | `CollaborationSession.recordOperation` and `OperationRelayService.replayOperationHistoryToUser` provide runtime replay only. | No durable operation log; `OperationLogRecord`, `OperationLogRepository`, and `OperationLogEntryDto` are placeholders. | Backend |
| import .txt | PARTIAL | `client.file.TextImportService`; dashboard import creates doc, converts text to snapshot, saves snapshot. | No dedicated server import API; UI path needs manual demo verification. | Client + QA |
| export .txt | PARTIAL | `client.file.TextExportService`; `EditorController.exportTextToFile`; `EditorView` export button visible for all roles. | No server export API; manual UI verification needed. | Client + QA |
| client dashboard/document UI | PARTIAL | `DocumentDashboardView`, `DocumentController`, app entry point updated. | Rename UI missing; list only owner docs; some no-op methods remain in dashboard. | Client |
| share codes UI | PARTIAL | `ShareCodesDialog`; owner codes shown in editor sidebar. | Dashboard share-code display flow is limited; verify viewers never see codes. | Client |
| viewer read-only UI behavior | PARTIAL | `EditorView.setDocumentInfo` disables text pane and hides save/format controls; `EditorController` blocks edit/cursor operations for viewer. | Need end-to-end test/manual demo with viewer share code. | Client |
| tests | PARTIAL | Many CRDT, WebSocket, DTO, service, import/export tests exist. Placeholder tests remain for controller, access, command, share, Mongo persistence. | Add missing tests for delete/rename/controller/API/Mongo/WebSocket document join/conflict. | QA |
| README/run instructions | PARTIAL | README exists but says server is on `8080`, database is H2, and WebSocket demo uses arbitrary `demo-room`. | Update to `8082`, Mongo/docker, HTTP document flow before WebSocket join. | Docs |
| final demo checklist | PARTIAL | `docs/phase3-demo-checklist.md` existed as placeholder and is updated by this audit. | Run checklist manually after next implementation/bug-fix batch. | QA |

## E. Updated Implementation Plan

### Batch 1: blocking backend/data model work

- Goal: Make persistence and snapshot semantics honest before adding more UI behavior.
- Files to create/modify: `DocumentRepository`, `MongoDocumentRepository`, `InMemoryDocumentRepository`, `DocumentCommandService`, `DocumentRequestValidator`, `DocumentSnapshotServiceTest`, `MongoPersistenceTest`, `application.properties`, `README.md`.
- Files to avoid touching: CRDT engines, `EditorView`, live relay code unless a test proves a narrow need.
- Tests to run: `mvn -pl server -am test`, then root `mvn test`.
- Blocking risk: HIGH because current snapshot conflict handling is missing and Mongo integration is not proven by tests.
- Recommended owner: Omar.

### Batch 2: HTTP document APIs

- Goal: Finish and test create/list/open/rename/delete/share-code HTTP behavior as the stable contract for the client.
- Files to create/modify: `DocumentManagementController`, `DocumentCommandService`, `DocumentQueryService`, `DocumentAccessService`, `DocumentMapper`, `DocumentRequestValidator`, document service tests.
- Files to avoid touching: WebSocket handler and CRDT code unless endpoint tests require a DTO adjustment.
- Tests to run: server document tests and root `mvn test`.
- Blocking risk: HIGH for list/delete/rename because client flow depends on clean API behavior.
- Recommended owner: Omar.

### Batch 3: WebSocket integration with document sessions/roles

- Goal: Preserve the working live-edit path while making document id and server-resolved role reliable.
- Files to create/modify: `SessionJoinService`, `CollaborationWebSocketHandlerTest`, possibly `MessageValidator`.
- Files to avoid touching: `OperationRelayService`, `CollaborationSession`, CRDT engines, client editor mutation logic.
- Tests to run: `server/src/test/.../websocket/CollaborationWebSocketHandlerTest.java`, client `SessionControllerTest`, root `mvn test`.
- Blocking risk: HIGH because the current WebSocket path is working and easy to break.
- Recommended owner: Omar + WebSocket owner.

### Batch 4: client document UI/service

- Goal: Make dashboard, open-by-code, open-list, delete, share-code, and read-only flows demo-ready.
- Files to create/modify: `DocumentDashboardView`, `DocumentController`, `DocumentClientService`, `DocumentSessionService`, `ShareCodesDialog`, `DocumentControllerTest`.
- Files to avoid touching: server internals, CRDT internals, low-level WebSocket parser.
- Tests to run: `mvn -pl client -am test`, then root `mvn test`.
- Blocking risk: MEDIUM because current UI is implemented but has untested branches and missing rename.
- Recommended owner: Client owner.

### Batch 5: import/export

- Goal: Keep `.txt` import/export simple, local, and demonstrable.
- Files to create/modify: `client/file/TextImportService.java`, `client/file/TextExportService.java`, `DocumentController`, `EditorController`, `TextImportExportServiceTest`.
- Files to avoid touching: server-side placeholder import/export unless the team decides to expose HTTP endpoints.
- Tests to run: client file tests and root `mvn test`.
- Blocking risk: MEDIUM because import currently saves a text-block snapshot and depends on snapshot save correctness.
- Recommended owner: QA/docs + Client.

### Batch 6: tests and final demo polish

- Goal: Remove placeholder tests or fill them, update docs, and rehearse the final demo.
- Files to create/modify: placeholder test files, `README.md`, `docs/mongodb-setup.md`, `docs/phase3-demo-checklist.md`, `docs/phase3-api-contract.md`.
- Files to avoid touching: working runtime code unless tests reveal a real bug.
- Tests to run: root `mvn test`; manual server/client/Mongo demo.
- Blocking risk: MEDIUM because docs currently disagree with code.
- Recommended owner: QA/docs.

## F. Files That Should Stay Untouched Or Only Narrowly Edited

These files are part of the working CRDT and live-edit path. They should not be rewritten during Phase 3 implementation:

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
- `client/src/main/java/com/team/editor/client/controller/EditorController.java`
- `client/src/main/java/com/team/editor/client/controller/SessionController.java`
- `client/src/main/java/com/team/editor/client/state/EditorState.java`
- `client/src/main/java/com/team/editor/client/ui/EditorView.java`
- `client/src/main/java/com/team/editor/client/websocket/ClientWebSocketHandler.java`
- `server/src/main/java/com/team/editor/server/websocket/CollaborationWebSocketHandler.java`
- `server/src/main/java/com/team/editor/server/service/OperationRelayService.java`
- `server/src/main/java/com/team/editor/server/service/SessionJoinService.java`
- `server/src/main/java/com/team/editor/server/service/PresenceService.java`
- `server/src/main/java/com/team/editor/server/session/CollaborationSession.java`
- `server/src/main/java/com/team/editor/server/session/SessionRegistry.java`
- `server/src/main/java/com/team/editor/server/validation/MessageValidator.java`

Reason: these files already carry the live edit, cursor, operation replay, validation, and CRDT behavior. Phase 3 should add document persistence and access checks around this path, not replace it.

## G. Updated Proposed New Files

Only files still needed after the audit are listed here. Most old proposed files already exist.

| Path | Purpose | Module | Priority | Owner suggestion | Blocking risk | Exists or create |
| --- | --- | --- | --- | --- | --- | --- |
| `server/src/test/java/com/team/editor/server/persistence/MongoDocumentRepositoryIntegrationTest.java` | Prove real Mongo document save/find/share-code/snapshot behavior, preferably with local Mongo or a clearly skipped profile. | server | HIGH | Omar/QA | HIGH | Needs creation |
| `server/src/test/java/com/team/editor/server/document/DocumentManagementControllerTest.java` | Cover HTTP status codes and JSON contracts for create/list/open/code/share/rename/delete/save. | server | HIGH | Omar/QA | HIGH | Needs creation |
| `server/src/test/java/com/team/editor/server/websocket/DocumentAwareJoinServiceTest.java` | Test join by document id, owner/editor/viewer role resolution, and rejection for unknown users/deleted docs. | server | HIGH | WebSocket owner | HIGH | Needs creation |
| `client/src/test/java/com/team/editor/client/controller/DocumentControllerWorkflowTest.java` | Cover client document-session state transitions without launching full Swing UI. | client | MEDIUM | Client owner | MEDIUM | Needs creation |
| `docs/phase3-manual-demo-results.md` | Record the final manual demo run with exact server/client/Mongo steps and screenshots/observations if needed. | docs | LOW | QA/docs | LOW | Needs creation |

Existing placeholder files that should either be implemented narrowly or deleted later:

- `server/src/main/java/com/team/editor/server/document/DocumentRuntimeService.java`
- `server/src/main/java/com/team/editor/server/repository/OperationLogRepository.java`
- `server/src/main/java/com/team/editor/server/persistence/OperationLogRecord.java`
- `common/src/main/java/com/team/editor/common/protocol/dto/persistence/OperationLogEntryDto.java`
- `client/src/main/java/com/team/editor/client/ui/DocumentToolbar.java`
- `server/src/test/java/com/team/editor/server/persistence/MongoPersistenceTest.java`
- `client/src/test/java/com/team/editor/client/controller/DocumentControllerTest.java`

## H. Docs To Update

- `README.md`: update server port from `8080` to `8082`, database from H2-only to Mongo + H2 leftover, quick start with `docker compose up -d mongo`, and WebSocket demo to create/open a document before joining by `documentId`.
- `docs/architecture/project-structure.md`: current tree is stale; it misses Phase 3 packages and still mentions placeholder packages that now have real files.
- `docs/phase3-file-plan.md`: should be treated as superseded by this audit. A full rewrite is not worth the churn.
- `docs/phase3-api-contract.md`: mostly useful, but must be reconciled with current behavior where snapshot conflict handling is not implemented and list currently returns owner docs only.
- `docs/mongodb-setup.md`: verify container names against `docker-compose.yml` and current Spring Boot property names.
- `docs/crdt-rules.md`: encoding appears corrupted in the arrows; update only if docs cleanup is in scope.

## I. Questions For Omar

1. Should `baseSnapshotVersion` become a real conflict check before demo, returning `SNAPSHOT_CONFLICT`/409, or should the contract be changed to say last save wins?
2. Should the document list show documents opened through editor/viewer share codes, or only documents owned by the current user?
3. After a document is deleted, should active WebSocket clients be disconnected/notified immediately, or is blocking future opens enough for the project demo?
4. Is the current text-block snapshot (`CLIENT_TEXT_BLOCKS_V1`) acceptable for Phase 3, or does the demo require storing full internal CRDT ids/tombstones?

## J. Next Recommended Codex Prompt

```text
You are working on my APT Collaborative Text Editor Java Maven project at:
D:\CUFE\Semesters\Spring 2026\APT\Project\collab-text-editor

Read docs/phase3-current-audit.md first.

Task: implement only Batch 1 from the audit: make snapshot persistence semantics and backend data model behavior safe.

Rules:
- Do not rewrite CRDT engines.
- Do not rewrite the existing WebSocket/live-edit path.
- Keep edits narrow to document persistence, snapshot save validation/conflict behavior, and tests.
- Decide from the audit whether to implement real baseSnapshotVersion conflict checks or update the contract/tests to last-save-wins; prefer real conflict checks unless Omar says otherwise.
- Fill or replace placeholder tests only where needed for this batch.
- Run mvn test and report exact results.
```
