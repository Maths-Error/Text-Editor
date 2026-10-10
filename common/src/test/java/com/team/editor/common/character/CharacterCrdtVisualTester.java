package com.team.editor.common.character;
import com.team.editor.common.crdt.character.*;
import com.team.editor.common.model.UserId;
import java.util.List;


public class CharacterCrdtVisualTester {

    public static void main(String[] args) {
        System.out.println("===================================================");
        System.out.println("  --- CHARACTER CRDT LAYER --- VISUAL TESTING ---  ");
        System.out.println("===================================================\n");

        runTest1_BasicTyping();
        runTest2_ConcurrentInsertion();
        runTest3_Deletions();
        runTest4_OutOfOrderDelivery();
        runTest5_ComplexTree();
        runTest6_GhostParent_ConcurrentDeleteAndInsert();
        runTest7_PasteTextBulkInsertion();
        runTest8_ReplaceCharacter();

        System.out.println("==================================================");
        System.out.println("   --- ALL TESTS COMPLETED --- MADE BY BISO ---   ");
        System.out.println("==================================================");
    }

    private static void runTest1_BasicTyping() {
        System.out.println("--- TEST 1: Basic Local Typing ---");
        CharacterCrdtEngine engine = new CharacterCrdtEngine(new UserId("Alice"));
        
        CharId c1 = engine.insert(null, 'H');
        CharId c2 = engine.insert(c1, 'e');
        CharId c3 = engine.insert(c2, 'l');
        CharId c4 = engine.insert(c3, 'l');
        CharId c5 = engine.insert(c4, 'o');

        printTestConclusion("Test 1", "Hello".equals(engine.getVisibleText()), engine);
    }

    private static void runTest2_ConcurrentInsertion() {
        System.out.println("\n--- TEST 2: Concurrent Insertion (Tie-Breaking) ---");
        CharacterCrdtEngine alice = new CharacterCrdtEngine(new UserId("Alice"));
        CharacterCrdtEngine bob = new CharacterCrdtEngine(new UserId("Bob"));

        CharId root = alice.insert(null, '>');
        bob.applyInsert(root, null, '>');

        CharId a1 = alice.insert(root, 'A');
        CharId b1 = bob.insert(root, 'B');

        alice.applyInsert(b1, root, 'B');
        bob.applyInsert(a1, root, 'A');

        printTestConclusion("Test 2", alice.getVisibleText().equals(bob.getVisibleText()) && ">AB".equals(alice.getVisibleText()), alice);
    }

    private static void runTest3_Deletions() {
        System.out.println("\n--- TEST 3: Deletions (Tombstones) ---");
        CharacterCrdtEngine engine = new CharacterCrdtEngine(new UserId("Alice"));
        
        CharId c1 = engine.insert(null, 'A');
        CharId c2 = engine.insert(c1, 'B');
        CharId c3 = engine.insert(c2, 'C');
        
        engine.applyDelete(c2);

        printTestConclusion("Test 3", "AC".equals(engine.getVisibleText()), engine);
    }

    private static void runTest4_OutOfOrderDelivery() {
        System.out.println("\n--- TEST 4: Out-of-Order Delivery (Orphans) ---");
        CharacterCrdtEngine alice = new CharacterCrdtEngine(new UserId("Alice"));
        CharacterCrdtEngine bob = new CharacterCrdtEngine(new UserId("Bob"));

        CharId a1 = alice.insert(null, 'X');
        CharId a2 = alice.insert(a1, 'Y');
        bob.applyInsert(a2, a1, 'Y');
        boolean emptyBefore = bob.getVisibleText().isEmpty();
        bob.applyInsert(a1, null, 'X');
        boolean fullAfter = "XY".equals(bob.getVisibleText());

        printTestConclusion("Test 4", emptyBefore && fullAfter, bob);
    }

    private static void runTest5_ComplexTree() {
        System.out.println("\n--- TEST 5: Complex Concurrent Tree ---");
        CharacterCrdtEngine alice = new CharacterCrdtEngine(new UserId("Alice"));
        CharacterCrdtEngine bob = new CharacterCrdtEngine(new UserId("Bob"));

        CharId r = alice.insert(null, 'R');
        bob.applyInsert(r, null, 'R');

        CharId a1 = alice.insert(r, 'a');
        CharId b1 = bob.insert(r, 'b');
        
        alice.applyInsert(b1, r, 'b');
        bob.applyInsert(a1, r, 'a');

        CharId a2 = alice.insert(a1, '1');
        CharId b2 = bob.insert(b1, '2');

        alice.applyInsert(b2, b1, '2');
        bob.applyInsert(a2, a1, '1');
        
        printTestConclusion("Test 5", "Ra1b2".equals(alice.getVisibleText()), alice);
    }

    private static void runTest6_GhostParent_ConcurrentDeleteAndInsert() {
        System.out.println("\n--- TEST 6: Ghost Parent (Concurrent Delete & Insert) ---");
        System.out.println("  (If Alice deletes 'A', but Bob inserts 'B' under 'A', 'B' must survive)");
        
        CharacterCrdtEngine alice = new CharacterCrdtEngine(new UserId("Alice"));
        CharacterCrdtEngine bob = new CharacterCrdtEngine(new UserId("Bob"));

        CharId root = alice.insert(null, 'A');
        bob.applyInsert(root, null, 'A');

        alice.delete(root);
        CharId b1 = bob.insert(root, 'B');

        
        alice.applyInsert(b1, root, 'B');
        bob.applyDelete(root);
        printTestConclusion("Test 6", "B".equals(alice.getVisibleText()) && "B".equals(bob.getVisibleText()), alice);
    }

    private static void runTest7_PasteTextBulkInsertion() {
        System.out.println("\n--- TEST 7: Paste Text (Bulk Insertion) ---");
        CharacterCrdtEngine engine = new CharacterCrdtEngine(new UserId("Alice"));
        
        CharId start = engine.insert(null, '>');
        
        // Simulate Ctrl+V
        engine.pasteText(start, "CRDT_Magic");
        
        printTestConclusion("Test 7", ">CRDT_Magic".equals(engine.getVisibleText()), engine);
    }

    private static void runTest8_ReplaceCharacter() {
        System.out.println("\n--- TEST 8: Replace Character (Typo Fix) ---");
        CharacterCrdtEngine engine = new CharacterCrdtEngine(new UserId("Alice"));
        
        CharId t = engine.insert(null, 't');
        CharId e = engine.insert(t, 'e');
        CharId x = engine.insert(e, 'x');
        CharId d = engine.insert(x, 'd');
        
        engine.replace(d, 't');
        
        printTestConclusion("Test 8", "text".equals(engine.getVisibleText()), engine);
    }

    private static void printTestConclusion(String testName, boolean success, CharacterCrdtEngine engine) {
        String resultText = engine.getVisibleText();
        System.out.println("  [Resulting Text]: \"" + resultText + "\"");

        if (success) {
            System.out.println("  => [SUCCESS] " + testName + " passed.");
        } else {
            System.err.println("  => [FAILED] " + testName + " failed!");
        }
    }
}
