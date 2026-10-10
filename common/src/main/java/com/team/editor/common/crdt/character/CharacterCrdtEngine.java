package com.team.editor.common.crdt.character;
import com.team.editor.common.model.UserId;
import java.util.*;

// handles all the typing and syncing logic for individual characters

public final class CharacterCrdtEngine
{
    private final CharacterTree tree;
    private final UserId userId;
    private long clock;
    private final Map<CharId, List<PendingInsert>> orphanBuffer;

    // set up the tree and start the counter at 0
    public CharacterCrdtEngine(UserId userId)
    {
        this.tree = new CharacterTree();
        this.userId = userId;
        this.clock = 0;
        this.orphanBuffer = new HashMap<>();
    }

    public CharId insert(CharId parentId, char value) {
        clock++;
        CharId newId = new CharId(userId, clock);
        tree.insert(newId, parentId, value);
        processOrphans(newId);
        return newId;
    }

    // handle a character typed by someone else
    // keep our clock in sync and put in orphanage it if its parent hasn't downloaded yet
    public void applyInsert(CharId id, CharId parentId, char value) {
        if (id.getUserId().equals(userId)) {
            clock = Math.max(clock, id.getClock());
        }

        try {
            tree.insert(id, parentId, value);
            processOrphans(id);
        } catch (IllegalStateException e) {
            orphanBuffer.computeIfAbsent(parentId, k -> new ArrayList<>()).add(new PendingInsert(id, value));
        }
    }

    public void applyDelete(CharId id) {
        tree.delete(id);
    }
    public void delete(CharId id) {
        tree.delete(id);
    }
    public void applyRestore(CharId id) {
        tree.restore(id);
    }
    public void restore(CharId id) {
        tree.restore(id);
    }

    // swap out a character by trashing the old one and puttingg the new one below it
    public CharId replace(CharId targetId, char newValue) {
        delete(targetId);
        return insert(targetId, newValue);
    }

    public CharId pasteText(CharId startParentId, String text) {
        CharId currentParent = startParentId;
        for (char c : text.toCharArray()) {
            currentParent = insert(currentParent, c);
        }
        return currentParent;
    }

    // check if any delayed network characters were waiting for this parent to arrive

    private void processOrphans(CharId newlyInsertedParentId)
    {
        if (orphanBuffer.containsKey(newlyInsertedParentId))
        {
            List<PendingInsert> orphans = orphanBuffer.remove(newlyInsertedParentId);
            for (PendingInsert orphan : orphans) {
                applyInsert(orphan.id, newlyInsertedParentId, orphan.value);
            }
        }
    }


    public String getVisibleText() { return tree.generateVisibleText(); }
    public CharacterTree getTree() { return tree; }

    private static class PendingInsert {
        final CharId id;
        final char value;

        PendingInsert(CharId id, char value) {
            this.id = id;
            this.value = value;
        }
    }
}
