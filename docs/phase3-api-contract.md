# Phase 3 API And Protocol Contract

## 1. Title And Scope

This contract defines the Phase 3 document-management API and its integration with the existing live collaboration WebSocket flow.

Phase 3 covers:

- Persistent document lifecycle: create, list, open, rename, and delete.
- Share-code based access with separate editor and viewer codes.
- Role resolution for owners, editors, and viewers.
- Persistent CRDT snapshot storage.
- The handoff from HTTP document APIs to the existing `/ws/collab` live session.

This document is a request/response and protocol agreement only. It does not implement Java logic, add dependencies, or replace the current WebSocket/live-edit behavior.

## 2. Design Rules

- `documentId` is the canonical persistent document identifier.
- `documentId` is also used as the WebSocket `sessionId`.
- The client must not choose its final role in Phase 3. The server resolves role from ownership, document access, or share code.
- Viewer users can open and join a document, but they cannot send edit operations.
- Viewers cannot see `editorCode` or `viewerCode`.
- The CRDT snapshot is the canonical saved state.
- `visibleText` or `visibleTextPreview` may be used only as preview, export, or debug output. It is not the source of truth.
- Existing WebSocket messages must remain flat JSON messages with `messageType` at the top level.
- Do not replace the current WebSocket protocol with a nested envelope such as `{ "type": "...", "payload": { ... } }`.

## 3. Shared Field Conventions

| Field | Type | Notes |
| --- | --- | --- |
| `documentId` | `string` | Persistent document id. Also used as WebSocket `sessionId`. |
| `title` | `string` | Human-readable document title. |
| `ownerId` | `{ "value": string }` | Owner user id using the existing typed id JSON shape. |
| `userId` | `{ "value": string }` | Requesting or joining user id using the existing typed id JSON shape. |
| `role` | `"OWNER" \| "EDITOR" \| "VIEWER"` | Resolved document role for HTTP document APIs. |
| `canEdit` | `boolean` | Whether this user may send edit operations. |
| `canShare` | `boolean` | Whether this user may view or distribute share codes. |
| `createdAt` | ISO-8601 string | Example: `2026-05-02T12:00:00Z`. |
| `updatedAt` | ISO-8601 string | Updated when metadata or persisted content changes. |
| `editorCode` | `string`, nullable, or omitted | Present only when the requester is allowed to share. |
| `viewerCode` | `string`, nullable, or omitted | Present only when the requester is allowed to share. |
| `snapshot` | `object` | CRDT-form saved state. This is canonical persistence state. |
| `snapshotVersion` | `number` | Monotonic version for saved snapshots. Used to prevent stale saves. |
| `visibleTextPreview` | optional `string` | Derived text preview for lists/search/debug. Not canonical. |

## 4. HTTP Endpoint Summary

| Method | Path | Purpose | Who Can Call It | Main Response DTO |
| --- | --- | --- | --- | --- |
| `POST` | `/api/documents` | Create a new document. | Any valid user. | `CreateDocumentResponse` |
| `GET` | `/api/documents?userId={userId}` | List documents visible to a user. | The listed user. | `DocumentSummaryDTO` |
| `GET` | `/api/documents/{documentId}?userId={userId}` | Open a document by id. | Owner, known collaborator, or otherwise authorized user. | `OpenDocumentResponse` |
| `POST` | `/api/documents/open-by-code` | Open a document through an editor or viewer share code. | Any valid user with a valid share code. | `OpenDocumentResponse` |
| `PUT` | `/api/documents/{documentId}/snapshot` | Save the latest CRDT snapshot. | Owner or editor. | `SaveSnapshotResponse` |
| `PATCH` | `/api/documents/{documentId}` | Rename a document. | Owner or another role explicitly allowed to rename. | `RenameDocumentResponse` |
| `DELETE` | `/api/documents/{documentId}` | Delete a document. | Owner only. | `DeleteDocumentResponse` |
| `GET` | `/api/documents/{documentId}/share-codes?requesterUserId={userId}` | Retrieve editor and viewer share codes. | Owner or explicitly allowed editor. | `ShareCodeResponse` |
| `POST` | `/api/documents/import-txt` | Optional: create/import from plain text. | Not required for first implementation. | TBD |
| `GET` | `/api/documents/{documentId}/export-txt` | Optional: export visible text. | Not required for first implementation. | TBD |

## 5. Detailed Endpoint Contracts

### A. Create Document

`POST /api/documents`

Creates a persistent document, assigns the requester as owner, generates both share codes, and stores the initial CRDT snapshot.

Request:

```json
{
  "ownerId": {
    "value": "omar"
  },
  "title": "Untitled Document",
  "initialSnapshot": null
}
```

Response:

```json
{
  "documentId": "doc_abc123",
  "title": "Untitled Document",
  "ownerId": {
    "value": "omar"
  },
  "role": "OWNER",
  "canEdit": true,
  "canShare": true,
  "editorCode": "EDT-9K2P4Q",
  "viewerCode": "VWR-7M8X1A",
  "snapshot": {
    "formatVersion": 1,
    "blocks": []
  },
  "snapshotVersion": 0,
  "createdAt": "2026-05-02T12:00:00Z",
  "updatedAt": "2026-05-02T12:00:00Z"
}
```

Permission rules:

- Any valid `ownerId` may create a document.
- The creator becomes `OWNER`.
- Owners can edit and share.

Important error cases:

- `INVALID_REQUEST`: Missing `ownerId`, invalid title, or malformed `initialSnapshot`.
- `INVALID_SNAPSHOT`: `initialSnapshot` is present but not a valid CRDT snapshot.
- `INTERNAL_ERROR`: Document or share-code creation fails unexpectedly.

### B. List Documents

`GET /api/documents?userId=omar`

Returns documents the user owns or can access.

Response:

```json
{
  "documents": [
    {
      "documentId": "doc_abc123",
      "title": "Untitled Document",
      "ownerId": {
        "value": "omar"
      },
      "role": "OWNER",
      "canEdit": true,
      "canShare": true,
      "visibleTextPreview": "Hello world...",
      "createdAt": "2026-05-02T12:00:00Z",
      "updatedAt": "2026-05-02T12:10:00Z"
    }
  ]
}
```

Permission rules:

- A user may list only documents visible to that `userId`.
- Share codes are not included in list responses.
- `visibleTextPreview` is derived from the CRDT snapshot or runtime state.

Important error cases:

- `INVALID_REQUEST`: Missing or invalid `userId`.
- `INTERNAL_ERROR`: Listing fails unexpectedly.

### C. Open Document By Id

`GET /api/documents/{documentId}?userId=omar`

Loads document metadata and the canonical CRDT snapshot for an authorized user.

Response:

```json
{
  "documentId": "doc_abc123",
  "title": "Untitled Document",
  "ownerId": {
    "value": "omar"
  },
  "role": "OWNER",
  "canEdit": true,
  "canShare": true,
  "editorCode": "EDT-9K2P4Q",
  "viewerCode": "VWR-7M8X1A",
  "snapshot": {
    "formatVersion": 1,
    "blocks": []
  },
  "snapshotVersion": 0,
  "createdAt": "2026-05-02T12:00:00Z",
  "updatedAt": "2026-05-02T12:10:00Z"
}
```

Permission rules:

- Owners receive role `OWNER`, `canEdit: true`, and `canShare: true`.
- Editors receive role `EDITOR`, `canEdit: true`, and `canShare` according to server policy.
- Viewers receive role `VIEWER`, `canEdit: false`, `canShare: false`, and must not receive share codes.

Important error cases:

- `INVALID_REQUEST`: Missing or invalid `userId`.
- `DOCUMENT_NOT_FOUND`: No document exists for `documentId`.
- `PERMISSION_DENIED`: The user is not allowed to open this document.

### D. Open Document By Share Code

`POST /api/documents/open-by-code`

Resolves a share code to a document and role, records or confirms the user's access, and returns the document snapshot.

Request:

```json
{
  "userId": {
    "value": "biso"
  },
  "displayName": "Biso",
  "shareCode": "EDT-9K2P4Q"
}
```

Editor-code response:

```json
{
  "documentId": "doc_abc123",
  "title": "Untitled Document",
  "ownerId": {
    "value": "omar"
  },
  "role": "EDITOR",
  "canEdit": true,
  "canShare": false,
  "snapshot": {
    "formatVersion": 1,
    "blocks": []
  },
  "snapshotVersion": 0,
  "createdAt": "2026-05-02T12:00:00Z",
  "updatedAt": "2026-05-02T12:10:00Z"
}
```

Viewer-code response:

```json
{
  "documentId": "doc_abc123",
  "title": "Untitled Document",
  "ownerId": {
    "value": "omar"
  },
  "role": "VIEWER",
  "canEdit": false,
  "canShare": false,
  "snapshot": {
    "formatVersion": 1,
    "blocks": []
  },
  "snapshotVersion": 0,
  "createdAt": "2026-05-02T12:00:00Z",
  "updatedAt": "2026-05-02T12:10:00Z"
}
```

Permission rules:

- `EDT-...` codes resolve to `EDITOR`.
- `VWR-...` codes resolve to `VIEWER`.
- Do not include `editorCode` or `viewerCode` in open-by-code responses unless the resolved user is allowed to share.
- Viewers must never receive share codes.

Important error cases:

- `INVALID_REQUEST`: Missing `userId`, `displayName`, or `shareCode`.
- `SHARE_CODE_NOT_FOUND`: No active document share code matches the request.
- `DOCUMENT_NOT_FOUND`: The share code exists but its document no longer exists.
- `PERMISSION_DENIED`: The code is revoked or the user is blocked.

### E. Save Document Snapshot

`PUT /api/documents/{documentId}/snapshot`

Saves the latest client-produced CRDT snapshot into persistent storage. The WebSocket still handles live operation relay; this endpoint stores the latest durable snapshot.

Request:

```json
{
  "requesterUserId": {
    "value": "omar"
  },
  "baseSnapshotVersion": 0,
  "snapshot": {
    "formatVersion": 1,
    "blocks": []
  }
}
```

Response:

```json
{
  "documentId": "doc_abc123",
  "snapshotVersion": 1,
  "updatedAt": "2026-05-02T12:20:00Z"
}
```

Permission rules:

- Owners and editors can save snapshots.
- Viewers cannot save snapshots.
- Unknown users cannot save snapshots.

Version rules:

- The client must send `baseSnapshotVersion` from the latest create, open, or save response.
- The server saves only when `baseSnapshotVersion` equals the current stored `snapshotVersion`.
- After every successful save, the server increments `snapshotVersion` by 1.
- If `baseSnapshotVersion` is stale, the server returns `SNAPSHOT_CONFLICT` with HTTP `409`.
- Clients should not send multiple snapshot saves for the same document in parallel.
- After a successful save, clients should update their local `snapshotVersion` from the response.

Important error cases:

- `INVALID_REQUEST`: Missing requester, missing snapshot, invalid document id, or negative `baseSnapshotVersion`.
- `DOCUMENT_NOT_FOUND`: No document exists for `documentId`.
- `PERMISSION_DENIED`: Requester is not owner or editor.
- `SNAPSHOT_CONFLICT`: Stored snapshot was updated by another save first.

### F. Rename Document

`PATCH /api/documents/{documentId}`

Renames a document and updates document metadata.

Request:

```json
{
  "requesterUserId": {
    "value": "omar"
  },
  "title": "New Title"
}
```

Response:

```json
{
  "documentId": "doc_abc123",
  "title": "New Title",
  "updatedAt": "2026-05-02T12:15:00Z"
}
```

Permission rules:

- Owner can rename.
- Editors may rename only if the implementation explicitly grants that permission.
- Viewers cannot rename.

Important error cases:

- `INVALID_REQUEST`: Missing requester or invalid title.
- `DOCUMENT_NOT_FOUND`: No document exists for `documentId`.
- `PERMISSION_DENIED`: Requester cannot rename this document.

### G. Delete Document

`DELETE /api/documents/{documentId}`

Deletes a document and prevents later opens or joins for that document.

Request:

```json
{
  "requesterUserId": {
    "value": "omar"
  }
}
```

Response:

```json
{
  "documentId": "doc_abc123",
  "deleted": true
}
```

If DELETE request bodies are inconvenient in Spring or HTTP clients, the implementation may use:

```text
DELETE /api/documents/{documentId}?requesterUserId=omar
```

Permission rules:

- Owner only.
- Editors and viewers cannot delete.
- The implementation should decide how active WebSocket users are notified or disconnected after deletion.

Important error cases:

- `INVALID_REQUEST`: Missing requester.
- `DOCUMENT_NOT_FOUND`: No document exists for `documentId`.
- `PERMISSION_DENIED`: Requester is not the owner.

### H. Get Share Codes

`GET /api/documents/{documentId}/share-codes?requesterUserId=omar`

Returns both document share codes to users who are allowed to share.

Response for owner or allowed editor:

```json
{
  "documentId": "doc_abc123",
  "editorCode": "EDT-9K2P4Q",
  "viewerCode": "VWR-7M8X1A",
  "canShare": true
}
```

Viewer response:

```json
{
  "errorCode": "VIEWER_CANNOT_VIEW_SHARE_CODES",
  "message": "Viewers cannot view document share codes."
}
```

Permission rules:

- Owners can view both codes.
- Editors can view codes only if server policy sets `canShare: true`.
- Viewers must receive a `403` error and must not receive either share code.

Important error cases:

- `INVALID_REQUEST`: Missing requester.
- `DOCUMENT_NOT_FOUND`: No document exists for `documentId`.
- `VIEWER_CANNOT_VIEW_SHARE_CODES`: Requester is a viewer.
- `PERMISSION_DENIED`: Requester is otherwise not allowed to view codes.

## 6. WebSocket Integration Contract

After create, open-by-id, or open-by-code succeeds, the client should join the existing WebSocket endpoint:

```text
ws://localhost:8080/ws/collab
```

Join message:

```json
{
  "messageType": "JOIN_SESSION_REQUEST",
  "sessionId": "doc_abc123",
  "userId": {
    "value": "omar"
  },
  "displayName": "Omar",
  "role": "EDITOR"
}
```

Rules:

- `sessionId` must equal `documentId`.
- The `role` field in `JOIN_SESSION_REQUEST` is temporary compatibility with Phase 2.
- Phase 3 server integration should eventually resolve role from the document/open result instead of trusting the client.
- Until full integration is complete, the client may pass the role returned by create/open/open-by-code.
- Because the current WebSocket role enum is editor/viewer oriented, users with document role `OWNER` may join the live edit flow as `EDITOR` while retaining `OWNER` for HTTP document permissions.

The WebSocket path remains `/ws/collab`. The message shape remains flat with `messageType` at the top level.

## 7. Operation Message Contract

Operation messages keep the current flat style:

```json
{
  "messageType": "OPERATION",
  "sessionId": "doc_abc123",
  "senderId": {
    "value": "omar"
  },
  "operationKind": "INSERT_TEXT",
  "payload": {
    "blockId": "...",
    "characterOffset": 0,
    "text": "A"
  }
}
```

Rules:

- Viewers must receive operations from editors.
- Viewers must be rejected if they send `OPERATION`.
- The server should continue relaying accepted operations to other users.
- The server should not echo an operation back to the sender.
- The server should record accepted operations in runtime state and later persist the updated CRDT snapshot.
- The operation payload remains flexible JSON because current `OperationMessage` uses `operationKind` plus `JsonNode payload`.

## 8. Snapshot DTO Shape

Minimal flexible CRDT snapshot:

```json
{
  "formatVersion": 1,
  "blocks": [],
  "metadata": {
    "createdBy": {
      "value": "omar"
    }
  }
}
```

Rules:

- `blocks` should eventually contain block CRDT and character CRDT state.
- Keep the snapshot shape flexible because the exact CRDT internal representation may evolve.
- Do not store only `visibleText` as canonical document state.
- Text previews and exports must be derived from the CRDT snapshot or live CRDT state.

## 9. Error Response Shape

Standard HTTP error response:

```json
{
  "errorCode": "DOCUMENT_NOT_FOUND",
  "message": "Document was not found."
}
```

Expected error codes:

| Error Code | Meaning |
| --- | --- |
| `INVALID_REQUEST` | Request JSON, query parameters, or required fields are invalid. |
| `DOCUMENT_NOT_FOUND` | The requested document does not exist. |
| `SHARE_CODE_NOT_FOUND` | The submitted share code does not exist or is inactive. |
| `PERMISSION_DENIED` | The requester is not allowed to perform the action. |
| `VIEWER_CANNOT_EDIT` | A viewer attempted to edit. |
| `VIEWER_CANNOT_VIEW_SHARE_CODES` | A viewer attempted to view share codes. |
| `DUPLICATE_USER` | A user is already joined to the same live session. |
| `SESSION_NOT_JOINED` | A WebSocket client sent a session message before joining. |
| `INVALID_SNAPSHOT` | A CRDT snapshot is missing, malformed, or unsupported. |
| `SNAPSHOT_CONFLICT` | A save used an old `baseSnapshotVersion`; the client should reload first. |
| `INTERNAL_ERROR` | Unexpected server failure. |

WebSocket errors may continue to use the existing `ErrorMessage` DTO shape with `errorCode` and `message`.

## 10. DTO Mapping Plan

| Contract Area | Proposed DTO Class | Notes |
| --- | --- | --- |
| Create document request | `CreateDocumentRequest` | Contains `ownerId`, `title`, and optional `initialSnapshot`. |
| Create document response | `CreateDocumentResponse` | Full document open response for the owner, including share codes. |
| Open document request | `OpenDocumentRequest` | Useful for code-based open or service-layer calls; GET by id may use query params directly. |
| Open document response | `OpenDocumentResponse` | Full document metadata, resolved role, permissions, snapshot, and optional share codes. |
| Save snapshot request | `SaveSnapshotRequest` | Contains `requesterUserId`, `baseSnapshotVersion`, and `snapshot`. |
| Save snapshot response | `SaveSnapshotResponse` | Contains `documentId`, new `snapshotVersion`, and `updatedAt`. |
| Rename document request | `RenameDocumentRequest` | Contains `requesterUserId` and `title`. |
| Delete document request | `DeleteDocumentRequest` | Contains `requesterUserId`; query-param fallback is allowed. |
| Document list item | `DocumentSummaryDto` | Compact list response without share codes or full snapshot. |
| Share codes response | `ShareCodeResponse` | Contains `documentId`, `editorCode`, `viewerCode`, and `canShare`. |
| CRDT snapshot | `CrdtSnapshotDto` | Flexible transport-safe CRDT-form state. |

## 11. Implementation Notes For The Team

- Biso should build UI against this contract.
- Abdo should write DTO serialization tests against this contract.
- Omar should implement backend services against this contract.
- 7oss should use this document for demo/API docs and import/export coordination.
- Do not block the team on the final MongoDB implementation before agreeing on these request/response shapes.
- The first implementation can use whichever persistence layer is ready, but it must preserve the contract that the canonical saved document is CRDT-form snapshot data.

## 12. Out Of Scope For First Contract

These items are intentionally later work:

- Version history.
- Reconnection operation replay beyond the existing live-session operation history behavior.
- Full operation log API.
- Comments.
- Rich formatting export beyond what the current CRDT supports.
