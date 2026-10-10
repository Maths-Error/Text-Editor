# Phase 3 Demo Checklist

Use this as the practical final-demo run sheet. Do not treat an item as ready until it has been tried on the real app, not only unit tested.

## Before The Demo

- [ ] Run `mvn test` from the repo root and record the result.
- [ ] Start MongoDB with `docker compose up -d mongo`.
- [ ] Start the server from `server/` with Maven.
- [ ] Confirm the server port is `8082`.
- [ ] Start two client windows.
- [ ] Use simple display names such as `Omar` and `Biso`.

## Document Lifecycle

- [ ] Create a new document from the dashboard.
- [ ] Confirm the editor opens for the owner.
- [ ] Confirm the document appears in the owner's document list.
- [ ] Close and reopen the client, then list documents again to show persistence.
- [ ] Open the saved document by selecting it from the list.
- [ ] Rename the document if rename UI is implemented; otherwise say rename is backend/API only.
- [ ] Delete a document owned by the user and confirm it disappears from the list.
- [ ] Try deleting a document as a non-owner if that path is easy to demo.

## Share Codes And Roles

- [ ] Owner can see both editor and viewer codes.
- [ ] Editor code opens the same document for another user with edit permission.
- [ ] Viewer code opens the same document for another user in read-only mode.
- [ ] Viewer cannot see share codes.
- [ ] Viewer cannot type, paste, save, or format text.
- [ ] Owner/editor changes are visible to viewer through WebSocket relay.

## Live Editing

- [ ] Owner and editor join the same document session.
- [ ] Typing in one editor appears in the other.
- [ ] Newlines still sync.
- [ ] Delete still syncs.
- [ ] Formatting syncs if formatting is included in the demo.
- [ ] Closing one editor updates presence for the remaining user.

## Persistence And Snapshots

- [ ] Type text in a document.
- [ ] Save snapshot.
- [ ] Close and reopen the document.
- [ ] Confirm the saved text loads.
- [ ] If conflict handling is implemented, demo or test stale snapshot save rejection.
- [ ] If conflict handling is not implemented, do not claim conflicts are handled.

## Import And Export

- [ ] Import a `.txt` file from the dashboard.
- [ ] Confirm a document is created from the imported file.
- [ ] Save or verify the imported snapshot is saved automatically.
- [ ] Reopen the imported document.
- [ ] Export the visible text to a `.txt` file from the editor.
- [ ] Open the exported file and confirm the text matches.

## Final Talking Points

- [ ] Explain that MongoDB stores document metadata and snapshots.
- [ ] Explain that `documentId` is used as the WebSocket `sessionId`.
- [ ] Explain that the server resolves editor/viewer permissions from document access.
- [ ] Explain that CRDT/live editing was preserved and integrated, not rewritten.
- [ ] Mention honest limitations: operation logs are runtime-only unless implemented later; snapshot conflict handling must match the actual code.

## Known Risks To Check Before Demo

- [ ] README instructions are updated to the current port and MongoDB setup.
- [ ] The server does not require arbitrary `demo-room` WebSocket joins anymore; a document must exist first.
- [ ] MongoDB is running before the server starts.
- [ ] Placeholder tests are not mistaken for implemented coverage.

