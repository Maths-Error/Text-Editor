# Collaborative Undo/Redo Design

This backend/protocol work adds server-controlled collaborative undo and redo for live sessions.

## Behavior

- Undo and redo are document/session-level, not user-level.
- Any connected `EDITOR` can request undo or redo.
- `VIEWER` users cannot request undo or redo.
- The server owns the undo and redo stacks for each `CollaborationSession`.
- Undo pops the latest session operation from the undo stack and broadcasts the server-generated inverse operation.
- Redo re-applies the latest undone operation and moves it back to the undo stack.
- Generated undo/redo operations are broadcast to every open websocket in the room, including the requester.
- Normal live edit operations still use the existing relay path and are sent to everyone except the sender.
- The session keeps the latest 10 undo history entries.
- Generated undo/redo operations are also recorded in operation history so late joiners replay the current document state.
- Empty undo or redo requests are safe no-ops. The server does not send a client error or broadcast an operation when the relevant stack is empty.
- Generated undo/redo operation payloads include `undoRedoMetadata` with the requester id and action. The requesting client uses this to move only its own caret after character insert/delete/restore effects, then sends a normal `CURSOR_UPDATE`. Other clients apply the document change but do not jump their local caret. Formatting undo/redo keeps the current caret stable.
- The Swing client provides Undo and Redo buttons for editors.
- `Ctrl+Z` sends `UNDO_REQUEST`; `Ctrl+Y` and `Ctrl+Shift+Z` send `REDO_REQUEST`.
- The client does not mutate local document state when undo/redo is requested. It only sends the request and waits for the server-generated operation broadcast.

## Current Payload Support

The current Swing client sends typing, deletion, and formatting as collaborative operations:

- `INSERT_CHAR` payloads include `blockId`, inserted char `id`, `parentId`, `value`, and optional initial `formatState`.
- `DELETE_CHAR` payloads include `blockId`, deleted char `id`, deleted char `parentId`, deleted char `value`, and optional deleted character `formatState`.
- `RESTORE_CHAR` payloads include the target char `id` and may include `parentId`, `value`, and `formatState` so restored tombstoned characters keep their style.
- `APPLY_FORMAT` and `REMOVE_FORMAT` payloads keep the old `startOffset` / `endOffset` fields for compatibility, and also include per-character inverse metadata:
  - `format`, such as `BOLD`, `ITALIC`, or `UNDERLINE`
  - `newValue`, the value the operation applies
  - `affectedChars`, with each target carrying `blockId`, `charId`, `previousValue`, and `newValue`

Typed styled characters are atomic. If the user types while bold, italic, or underline typing mode is active, the client sends one `INSERT_CHAR` operation with `formatState`; it does not send a second `APPLY_FORMAT` operation for the new character. Undo removes the whole character, and redo restores the same tombstoned character id with its original formatting.

This means `INSERT_CHAR` can be safely inverted into `DELETE_CHAR`. Redo after undoing an insertion uses tombstone-safe `RESTORE_CHAR`, because replaying `INSERT_CHAR` with an existing CRDT char id would be ignored by the character tree.

`DELETE_CHAR` is inverted with `RESTORE_CHAR`. The CRDT keeps deleted characters as tombstones, so the restore operation makes the existing character id visible again instead of inserting a duplicate character. The delete payload also carries the deleted character value, parent id, and formatting state for inverse metadata and future auditing. The current server requires the deleted character value before treating a delete as undoable; older delete payloads without it return:

```text
UNDO_NOT_SUPPORTED_FOR_OPERATION
```

Formatting undo is implemented without blind toggles. The server builds an inverse formatting operation by swapping each affected character's `previousValue` and `newValue`. This lets undo restore mixed selections correctly, for example when only some characters were already bold before applying bold to the whole selection. Redo replays the original formatting operation.

Standalone formatting operations are still separate undoable actions when formatting is applied to existing selected text.

Safe formatting undo depends on the shared CRDT character ids matching across clients, which is the normal live collaboration path. The client applies server-generated undo/redo formatting operations when it receives the broadcast `APPLY_FORMAT` or `REMOVE_FORMAT` operation. It prefers the `affectedChars` CRDT ids, can still find a character by `charId` if `blockId` is not resolvable locally, and keeps the offset fields as a compatibility fallback for older formatting messages and simple snapshot-based cases. After any text refresh, the Swing client reapplies all CRDT format runs to the `StyledDocument` so redo-generated formatting is visible.

Formatting operations with missing `affectedChars` metadata are still rejected for undo with:

```text
UNDO_NOT_SUPPORTED_FOR_OPERATION
```

## TODO

- Add equivalent inverse metadata for text and block operations before claiming full CRDT undo support. Batch text operations may be decomposed into character operations, but any true range operation still needs its own metadata.
- Consider Mongo operation-log persistence in a later backend batch.
