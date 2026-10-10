package com.team.editor.common.character;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import com.team.editor.common.crdt.character.CharId;
import com.team.editor.common.crdt.character.CharacterCrdtEngine;
import com.team.editor.common.model.UserId;

class CharacterCrdtEngineTest {

    @Test
    void shouldInsertCharactersLocally() {
        UserId alice = new UserId("Alice");
        CharacterCrdtEngine engine = new CharacterCrdtEngine(alice);

        CharId c1 = engine.insert(null, 'A');
        CharId c2 = engine.insert(c1, 'B');
        CharId c3 = engine.insert(c2, 'C');

        assertEquals("ABC", engine.getVisibleText());
    }

@Test
    void shouldHandleConcurrentInsertionsWithDeterministicOrder()
    {
        UserId u1 = new UserId("Alice");
        UserId u2 = new UserId("Bob");
        CharacterCrdtEngine alice = new CharacterCrdtEngine(u1);
        CharacterCrdtEngine bob = new CharacterCrdtEngine(u2);
        
        CharId a1 = alice.insert(null, 'A');
        CharId b1 = bob.insert(null, 'B');

        bob.applyInsert(a1, null, 'A');
        alice.applyInsert(b1, null, 'B');

        assertEquals("BA", alice.getVisibleText());
        assertEquals("BA", bob.getVisibleText());
    }

    @Test
    void shouldHandleDeletionsWithTombstones()
    {
        UserId alice = new UserId("Alice");
        CharacterCrdtEngine engine = new CharacterCrdtEngine(alice);

        CharId c1 = engine.insert(null, 'A');
        CharId c2 = engine.insert(c1, 'B');
        engine.applyDelete(c1);

        assertEquals("B", engine.getVisibleText());
        assertFalse(engine.getTree().getChar(c1).isVisible());
        assertTrue(engine.getTree().getChar(c2).isVisible());
    }

    @Test
    void shouldHandleOutofOrderDelivery() {
        UserId alice = new UserId("Alice");
        UserId bob = new UserId("Bob");
        CharacterCrdtEngine aliceEngine = new CharacterCrdtEngine(alice);
        CharacterCrdtEngine bobEngine = new CharacterCrdtEngine(bob);

        CharId a1 = aliceEngine.insert(null, 'A');
        CharId a2 = aliceEngine.insert(a1, 'B');

        bobEngine.applyInsert(a2, a1, 'B');
        assertEquals("", bobEngine.getVisibleText()); // A is missing

        bobEngine.applyInsert(a1, null, 'A');
        assertEquals("AB", bobEngine.getVisibleText());
    }
}