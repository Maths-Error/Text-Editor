# Phase 3 CRDT Persistence Analysis

## Summary

The server currently only relays WebSocket edit operations between connected clients. It does not apply `OperationMessage` objects to a server-side `DocumentCrdt`, and it does not produce an updated `CrdtSnapshotDto` after edits.

The server does keep an in-memory operation history inside each `CollaborationSession`, but that history is only used while the server process is running. It is not saved to MongoDB, and it disappears when the server restarts.

The safest next step is Path B: add a snapshot sync HTTP endpoint in Batch 6B. This lets the client, which already applies CRDT operations locally, send the latest CRDT snapshot to the server for MongoDB storage.

## Current Operation Flow

WebSocket operations are received in `CollaborationWebSocketHandler.handleOperationMessage`.

The current flow is:

1. The handler validates the JSON shape with `MessageValidator`.
2. The handler converts the JSON into an `OperationMessage`.
3. The handler gets the joined user and room from `SessionJoinService.getJoinedSessionContext`.
4. The handler overwrites `sessionId` and `senderId` using the trusted joined WebSocket context.
5. The handler checks edit permission using `PermissionService.canEdit`.
6. If the user is an editor, the handler calls `CollaborationSession.recordOperation`.
7. The handler calls `OperationRelayService.relayOperation` to send the operation to other connected users.

The server stores accepted operations only in:

```text
CollaborationSession.operationHistory
```

That storage is in memory only. It is useful for replaying recent operations to a late joiner in the same live server run, but it is not durable persistence.

The server does not currently:

- Load a `DocumentCrdt` for the document.
- Apply the operation to a server-side CRDT.
- Convert the operation history into a snapshot.
- Save an updated snapshot back to `MongoDocumentRecord`.
- Save operations in `OperationLogRecord` or `OperationLogRepository`.

## Current CRDT Support

The common module has real CRDT classes:

- `DocumentCrdt`
- `BlockCrdtEngine`
- `BlockTree`
- `BlockNode`
- `CharacterCrdtEngine`
- `CharacterTree`
- `CharNode`
- `BlockId`
- `CharId`

The CRDT code supports low-level operations such as:

- Insert block.
- Delete block.
- Split block.
- Insert character into a block.
- Delete character from a block.
- Replace character.
- Paste text into a block.
- Render visible text.

The operation DTOs also exist:

- `InsertBlockOp`
- `DeleteBlockOp`
- `InsertCharOp`
- `DeleteCharOp`
- `BlockOpPayload<T>`
- `OperationKind`
- `OperationMessage`

However, the CRDT classes do not currently provide a clear snapshot API. I did not find methods like:

- `toSnapshot`
- `fromSnapshot`
- `loadSnapshot`
- `createSnapshot`
- `restoreSnapshot`

The server also does not currently have a mapper that converts every `OperationMessage` into calls on a server-side `DocumentCrdt`.

The client does have operation-apply logic in `EditorController.handleRemoteOperation`. It parses `OperationMessage` payloads and calls the CRDT engine for supported operation kinds:

- `CHARACTER_INSERT`
- `CHARACTER_DELETE`
- `BLOCK_INSERT`
- `BLOCK_DELETE`

That client logic is helpful, but it is not currently shared as a server-side service.

Because of this, `OperationMessage` cannot be safely and directly converted into a persisted server snapshot yet without adding new mapping and snapshot code.

## Current Snapshot Support

`CrdtSnapshotDto` currently has these fields:

- `int formatVersion`
- `JsonNode blocks`
- `JsonNode metadata`

It is intentionally flexible. It can hold CRDT-shaped JSON, but it does not define exactly how blocks, characters, tombstones, parent ids, clocks, and metadata must be serialized.

`MongoDocumentRecord` stores:

```java
private CrdtSnapshotDto snapshot;
```

The document HTTP APIs already return this snapshot:

- `CreateDocumentResponse` includes `snapshot`.
- `OpenDocumentResponse` includes `snapshot`.
- `DocumentMapper` reads `document.getSnapshot()` and puts it into responses.

When a document is created without an initial snapshot, `DocumentCommandService` creates a default `CrdtSnapshotDto` with `formatVersion = 1`.

There is no code that updates the snapshot after WebSocket editing. After users edit through `/ws/collab`, MongoDB still has the old snapshot.

## Recommended Implementation Path

Recommended path: Path B, Snapshot sync endpoint.

Add a simple HTTP endpoint later:

```text
PUT /api/documents/{documentId}/snapshot
```

The request should include:

- `requesterUserId`
- `snapshot`

The server should:

1. Validate `documentId`.
2. Validate `requesterUserId`.
3. Load the document from `DocumentRepository`.
4. Reject missing or deleted documents.
5. Use `DocumentAccessService` to allow OWNER and EDITOR.
6. Reject VIEWER with `PERMISSION_DENIED` or `VIEWER_CANNOT_EDIT`.
7. Save the submitted `CrdtSnapshotDto` into `MongoDocumentRecord.snapshot`.
8. Update `updatedAt`.
9. Save through `DocumentRepository`.

Open document endpoints can keep returning `MongoDocumentRecord.snapshot` as they already do.

## Why This Path Is Recommended

Path A, server-side CRDT persistence, is more correct long term, but it is not the safest next step right now.

The server has CRDT classes available, but it does not yet have:

- A server runtime `DocumentCrdt` per document.
- A snapshot serializer.
- A snapshot loader.
- A complete server-side operation mapper.
- Tests proving that server-side replay matches client-side CRDT state.

Adding all of that at once would touch the WebSocket path and CRDT internals. That is risky for the current project stage because live editing is already working.

Path C, operation log first, is also not the best next step. There are placeholder files for operation logging, but no real operation log implementation yet. Storing operations first would still leave the team needing a replay-to-snapshot implementation later.

Path B is smaller and safer because:

- It keeps WebSocket behavior unchanged.
- It uses the existing Mongo `snapshot` field.
- It uses the existing HTTP document access checks.
- It lets the client remain the place where CRDT operations are already applied.
- It gives a strong course demo story: create, edit, save snapshot, restart server, reopen document.

## Batch 6B Plan

Batch 6B should implement snapshot sync with the smallest safe surface area.

Suggested files to add or update:

- Add a common DTO such as `SaveSnapshotRequest`.
- Add a method in `DocumentManagementController` for `PUT /api/documents/{documentId}/snapshot`.
- Add a method in `DocumentCommandService` such as `saveSnapshot`.
- Add validation in `DocumentRequestValidator`.
- Reuse `DocumentAccessService` to allow owners and editors only.
- Reuse `DocumentRepository.save` to persist the updated `MongoDocumentRecord`.
- Add focused service/controller tests using `InMemoryDocumentRepository`.

The WebSocket flow should not be changed in Batch 6B.

The client can later call this endpoint after edits, on a save button, on close, or on a simple timer.

## Risks and Limitations

Path B has one main limitation: the server trusts the client to send a correct CRDT snapshot.

That is acceptable for this course project stage if:

- Only OWNER and EDITOR can save snapshots.
- VIEWER cannot save snapshots.
- The client already uses the shared CRDT classes.
- The server keeps validating document access.

Possible things that could break:

- If the client sends an old snapshot after another editor made newer edits, it could overwrite newer state.
- If two editors save at almost the same time, the last save wins.
- If the client snapshot shape is inconsistent, the server will store inconsistent CRDT JSON unless validation is added.

The fastest path is Path B.

The most correct long-term path is Path A, but it needs proper snapshot serialization/loading and server-side operation application first.

The best course demo path is Path B because it proves persistence after restart without risking the working live relay.

## Final Recommendation

Do not implement server-side CRDT snapshot generation from WebSocket operations yet.

For Batch 6B, implement Path B: a simple snapshot sync endpoint that saves a client-provided `CrdtSnapshotDto` into `MongoDocumentRecord.snapshot`.

This matches the current codebase best because the server is currently a live relay plus access-control layer, while the client is the place that already applies CRDT operations to document state.

## Batch 6B Update

Batch 6B implemented the recommended snapshot sync endpoint with `snapshotVersion` optimistic concurrency. The server still does not apply WebSocket operations to a server-side CRDT; it stores client-provided snapshots through the HTTP save endpoint.
