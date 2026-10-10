package com.team.editor.common.character;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;

import com.team.editor.common.crdt.character.CharId;
import com.team.editor.common.crdt.character.CharacterTree;
import com.team.editor.common.model.UserId;

class CharacterTreeTest {

    @Test
    void shouldInsertCharactersCorrectly() {
        CharacterTree tree = new CharacterTree();
        UserId user = new UserId("user-1");
        CharId c1 = new CharId(user, 1);
        CharId c2 = new CharId(user, 2);

        tree.insert(c1, null, 'A');
        tree.insert(c2, c1, 'B');

        assertEquals('A', tree.getChar(c1).getValue());
        assertEquals('B', tree.getChar(c2).getValue());
        assertEquals(1, tree.getChar(c1).getChildren().size());
    }

    @Test
    void shouldSortSiblingsAccordingToOrderingRule() {
        CharacterTree tree = new CharacterTree();
        UserId u1 = new UserId("Alice");
        
        CharId clock1 = new CharId(u1, 1);
        CharId clock10 = new CharId(u1, 10);
        tree.insert(clock1, null, '1');
        tree.insert(clock10, null, '0');
        
        assertEquals("01", tree.generateVisibleText());

        CharacterTree tieTree = new CharacterTree();
        CharId aId = new CharId(new UserId("alice"), 5);
        CharId bId = new CharId(new UserId("bob"), 5);
        tieTree.insert(bId, null, 'B');
        tieTree.insert(aId, null, 'A');
    
        assertEquals("BA", tieTree.generateVisibleText());
    }

    @Test
    void shouldMaintainDfsOrder() {
        CharacterTree tree = new CharacterTree();
        UserId user = new UserId("user-1");
        CharId c1 = new CharId(user, 1);
        CharId c1_1 = new CharId(user, 2);
        CharId c2 = new CharId(user, 3);

        tree.insert(c1, null, 'A');
        tree.insert(c2, null, 'C');
        tree.insert(c1_1, c1, 'B');

        assertEquals("CAB", tree.generateVisibleText());
    }

    @Test
    void shouldHandleTombstonesInVisibleText() {
        CharacterTree tree = new CharacterTree();
        CharId c1 = new CharId(new UserId("user-1"), 1);
        CharId c2 = new CharId(new UserId("user-1"), 2);
        
        tree.insert(c1, null, 'A');
        tree.insert(c2, c1, 'B');
        
        tree.delete(c1);

        assertEquals("B", tree.generateVisibleText());
        assertFalse(tree.getChar(c1).isVisible());
    }
}