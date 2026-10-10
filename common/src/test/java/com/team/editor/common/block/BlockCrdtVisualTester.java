package com.team.editor.common.block;
import com.team.editor.common.crdt.block.*;
import com.team.editor.common.model.UserId;
import java.util.List;

public class BlockCrdtVisualTester {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("     BLOCK CRDT LAYER --- VISUAL TESTING ---      ");
        System.out.println("==================================================\n");

        runTest1_BuildBigSentence();
        runTest2_Commutativity_OutOfOrderSync();
        runTest3_ConcurrentSiblingInsertion();
        runTest4_ConcurrentDeleteAndInsert_GhostParent();
        runTest5_Idempotency_DuplicateMessages();
        runTest6_DeepNesting_TombstoneChains();
        runTest7_TheOrphanChild_MissingParent();
        runTest8_MassConcurrentStressTest();
        runTest9_SplitBrainNetworkPartition();
        runTest10_ConcurrentParentDeletionAndChildInsertion();
        runTest11_BlockSplit_NetworkRaceCondition();
        runTest12_ThreeWayConcurrentTie();
        runTest13_CopyAndPasteBlock();
        runTest14_MoveBlock();
        runTest15_Layer2Integration();

        System.out.println("==================================================");
        System.out.println("   --- ALL TESTS COMPLETED --- MADE BY BISO ---   ");
        System.out.println("==================================================");
    }

    private static void runTest1_BuildBigSentence() {
        System.out.println("--- TEST 1: Basic Insert & Build Big Sentence ---");
        BlockCrdtEngine engine = new BlockCrdtEngine(new UserId("Alice"));
        BlockId b1 = insertText(engine, null, "Collaborative editing ");
        BlockId b2 = insertText(engine, b1, "is deeply fascinating, ");
        BlockId b3 = insertText(engine, b2, "[DELETE ME] "); 
        BlockId b4 = insertText(engine, b2, "but mathematically complex, ");
        engine.delete(b3);
        printTestConclusion("Test 1", engine.getVisibleBlocks().size() == 3, engine);
    }

    private static void runTest2_Commutativity_OutOfOrderSync() {
        System.out.println("\n--- TEST 2: Commutativity (Out-of-Order Delivery) ---");
        UserId bob = new UserId("Bob");
        UserId charlie = new UserId("Charlie");
        BlockId bBob = new BlockId(bob, 1);
        BlockId bCharlie = new BlockId(charlie, 1);
        
        BlockCrdtEngine engineA = new BlockCrdtEngine(new UserId("EngineA"));
        engineA.applyInsert(bBob, null);
        engineA.applyInsert(bCharlie, bBob);
        engineA.pasteTextIntoBlock(bBob, null, "especially when handling ");
        engineA.pasteTextIntoBlock(bCharlie, null, "concurrent offline changes. ");
        
        BlockCrdtEngine engineB = new BlockCrdtEngine(new UserId("EngineB"));
        engineB.applyInsert(bCharlie, bBob); 
        engineB.applyInsert(bBob, null);     
        engineB.pasteTextIntoBlock(bBob, null, "especially when handling ");
        engineB.pasteTextIntoBlock(bCharlie, null, "concurrent offline changes. ");
        
        printTestConclusion("Test 2", compareTrees(engineA.getVisibleBlocks(), engineB.getVisibleBlocks()), engineB);
    }

    private static void runTest3_ConcurrentSiblingInsertion() {
        System.out.println("\n--- TEST 3: Concurrent Sibling Insertion (Tie-Breaking) ---");
        BlockCrdtEngine aliceEngine = new BlockCrdtEngine(new UserId("Alice"));
        BlockCrdtEngine bobEngine = new BlockCrdtEngine(new UserId("Bob"));
        
        BlockId root = insertText(aliceEngine, null, "This test suite ");
        bobEngine.applyInsert(root, null);
        bobEngine.pasteTextIntoBlock(root, null, "This test suite ");
        
        BlockId bAlice = insertText(aliceEngine, root, "proves our CRDT ");
        BlockId bBob = insertText(bobEngine, root, "is absolutely bulletproof! ");
        
        aliceEngine.applyInsert(bBob, root);
        aliceEngine.pasteTextIntoBlock(bBob, null, "is absolutely bulletproof! ");
        
        bobEngine.applyInsert(bAlice, root);
        bobEngine.pasteTextIntoBlock(bAlice, null, "proves our CRDT ");
        
        printTestConclusion("Test 3", compareTrees(aliceEngine.getVisibleBlocks(), bobEngine.getVisibleBlocks()), aliceEngine);
    }

    private static void runTest4_ConcurrentDeleteAndInsert_GhostParent() {
        System.out.println("\n--- TEST 4: Concurrent Delete & Insert (Ghost Parent) ---");
        BlockCrdtEngine aliceEngine = new BlockCrdtEngine(new UserId("Alice"));
        BlockCrdtEngine bobEngine = new BlockCrdtEngine(new UserId("Bob"));
        
        BlockId root = insertText(aliceEngine, null, "The old redundant intro. ");
        bobEngine.applyInsert(root, null);
        
        aliceEngine.delete(root);
        BlockId bobChild = insertText(bobEngine, root, "Collaborative editing ");
        
        bobEngine.applyDelete(root); 
        aliceEngine.applyInsert(bobChild, root);
        aliceEngine.pasteTextIntoBlock(bobChild, null, "Collaborative editing ");
        
        printTestConclusion("Test 4", aliceEngine.getVisibleBlocks().size() == 1, aliceEngine);
    }

    private static void runTest5_Idempotency_DuplicateMessages() {
        System.out.println("\n--- TEST 5: Idempotency (Duplicate Messages) ---");
        BlockCrdtEngine engine = new BlockCrdtEngine(new UserId("Alice"));
        BlockId bBob = new BlockId(new UserId("Bob"), 1);
        
        engine.applyInsert(bBob, null);
        engine.pasteTextIntoBlock(bBob, null, "is deeply fascinating, ");
        engine.applyInsert(bBob, null); 
        engine.applyInsert(bBob, null); 
        
        printTestConclusion("Test 5", engine.getVisibleBlocks().size() == 1, engine);
    }

    private static void runTest6_DeepNesting_TombstoneChains() {
        System.out.println("\n--- TEST 6: Deep Nesting & Tombstone Chains ---");
        BlockCrdtEngine engine = new BlockCrdtEngine(new UserId("Alice"));
        BlockId l1 = insertText(engine, null, "Level 1 ");
        BlockId l2 = insertText(engine, l1, "Level 2 ");
        BlockId l3 = insertText(engine, l2, "Level 3 ");
        BlockId l4 = insertText(engine, l3, "Level 4 (Survivor) ");
        
        engine.delete(l2);
        engine.delete(l3);
        
        List<BlockNode> visible = engine.getVisibleBlocks();
        printTestConclusion("Test 6", visible.size() == 2 && visible.get(1).getId().equals(l4), engine);
    }

    private static void runTest7_TheOrphanChild_MissingParent() {
        System.out.println("\n--- TEST 7: The Orphan Child (Missing Parent) ---");
        BlockCrdtEngine engine = new BlockCrdtEngine(new UserId("Alice"));
        BlockId parent = new BlockId(new UserId("Bob"), 1);
        BlockId child = new BlockId(new UserId("Bob"), 2);
        
        engine.applyInsert(child, parent);
        engine.applyInsert(parent, null);
        engine.pasteTextIntoBlock(parent, null, "Parent ");
        engine.pasteTextIntoBlock(child, null, "Child ");
        
        printTestConclusion("Test 7", engine.getVisibleBlocks().size() == 2, engine);
    }

    private static void runTest8_MassConcurrentStressTest() {
        System.out.println("\n--- TEST 8: Mass Concurrent Stress Test ---");
        BlockCrdtEngine alice = new BlockCrdtEngine(new UserId("Alice"));
        BlockCrdtEngine bob = new BlockCrdtEngine(new UserId("Bob"));
        
        BlockId root = insertText(alice, null, "Start: ");
        bob.applyInsert(root, null);
        bob.pasteTextIntoBlock(root, null, "Start: ");
        
        BlockId a1 = insertText(alice, root, "A1 ");
        BlockId b1 = insertText(bob, root, "B1 ");
        BlockId a2 = insertText(alice, a1, "A2 ");
        BlockId b2 = insertText(bob, b1, "B2 ");
        BlockId a3 = insertText(alice, a2, "A3 ");
        
        alice.applyInsert(b1, root);
        alice.pasteTextIntoBlock(b1, null, "B1 ");
        alice.applyInsert(b2, b1);
        alice.pasteTextIntoBlock(b2, null, "B2 ");
        
        bob.applyInsert(a1, root);
        bob.pasteTextIntoBlock(a1, null, "A1 ");
        bob.applyInsert(a2, a1);
        bob.pasteTextIntoBlock(a2, null, "A2 ");
        bob.applyInsert(a3, a2);
        bob.pasteTextIntoBlock(a3, null, "A3 ");
        
        printTestConclusion("Test 8", compareTrees(alice.getVisibleBlocks(), bob.getVisibleBlocks()), alice);
    }

    private static void runTest9_SplitBrainNetworkPartition() {
        System.out.println("\n--- TEST 9: Split-Brain Network Partition ---");
        BlockCrdtEngine alice = new BlockCrdtEngine(new UserId("Alice"));
        BlockCrdtEngine bob = new BlockCrdtEngine(new UserId("Bob"));
        
        BlockId root = insertText(alice, null, "Document Root. ");
        bob.applyInsert(root, null);
        bob.pasteTextIntoBlock(root, null, "Document Root. ");
        
        BlockId a1 = insertText(alice, root, "Alice paragraph 1. ");
        BlockId a2 = insertText(alice, a1, "Alice paragraph 2. ");
        BlockId b1 = insertText(bob, root, "Bob paragraph 1. ");
        BlockId b2 = insertText(bob, b1, "Bob paragraph 2. ");
        
        bob.delete(root); 
        
        bob.applyInsert(a1, root);
        bob.pasteTextIntoBlock(a1, null, "Alice paragraph 1. ");
        bob.applyInsert(a2, a1);
        bob.pasteTextIntoBlock(a2, null, "Alice paragraph 2. ");
        
        alice.applyInsert(b1, root);
        alice.pasteTextIntoBlock(b1, null, "Bob paragraph 1. ");
        alice.applyInsert(b2, b1);
        alice.pasteTextIntoBlock(b2, null, "Bob paragraph 2. ");
        alice.applyDelete(root);
        
        printTestConclusion("Test 9", compareTrees(alice.getVisibleBlocks(), bob.getVisibleBlocks()), alice);
    }

    private static void runTest10_ConcurrentParentDeletionAndChildInsertion() {
        System.out.println("\n--- TEST 10: Concurrent Parent Deletion & Child Insertion ---");
        BlockCrdtEngine alice = new BlockCrdtEngine(new UserId("Alice"));
        BlockCrdtEngine bob = new BlockCrdtEngine(new UserId("Bob"));
        
        BlockId parentBlock = insertText(alice, null, "[Parent Block] ");
        bob.applyInsert(parentBlock, null);
        bob.pasteTextIntoBlock(parentBlock, null, "[Parent Block] ");
        
        alice.delete(parentBlock); 
        BlockId bobsChild = insertText(bob, parentBlock, "Bob's late addition! "); 
        
        alice.applyInsert(bobsChild, parentBlock);
        alice.pasteTextIntoBlock(bobsChild, null, "Bob's late addition! ");
        bob.applyDelete(parentBlock);
        
        boolean success = alice.getVisibleBlocks().size() == 1 && compareTrees(alice.getVisibleBlocks(), bob.getVisibleBlocks());
        printTestConclusion("Test 10", success, alice);
    }

    private static void runTest11_BlockSplit_NetworkRaceCondition() {
        System.out.println("\n--- TEST 11: Block Split & Network Race Condition ---");
        BlockCrdtEngine alice = new BlockCrdtEngine(new UserId("Alice"));
        BlockCrdtEngine bob = new BlockCrdtEngine(new UserId("Bob"));

        BlockId root = insertText(alice, null, "[Doc Start] ");
        bob.applyInsert(root, null);

        BlockId targetBlock = insertText(alice, root, "Lines 1-10 ");
        BlockId splitSibling = alice.splitBlock(targetBlock);
        alice.pasteTextIntoBlock(splitSibling, null, "Line 11 (Overflow) ");

        bob.applySplit(targetBlock, splitSibling);

        boolean isBobSafeBeforeTarget = bob.getVisibleBlocks().size() == 1; 

        bob.applyInsert(targetBlock, root);
        bob.pasteTextIntoBlock(targetBlock, null, "Lines 1-10 ");
        bob.pasteTextIntoBlock(splitSibling, null, "Line 11 (Overflow) ");

        boolean isBobSyncedAfterTarget = bob.getVisibleBlocks().size() == 3;

        boolean success = isBobSafeBeforeTarget 
                       && isBobSyncedAfterTarget 
                       && compareTrees(alice.getVisibleBlocks(), bob.getVisibleBlocks());
                       
        printTestConclusion("Test 11", success, bob);
    }

    private static void runTest12_ThreeWayConcurrentTie() {
        System.out.println("\n--- TEST 12: Three-Way Concurrent Tie-Breaker ---");
        BlockCrdtEngine alice = new BlockCrdtEngine(new UserId("Alice"));
        BlockCrdtEngine bob = new BlockCrdtEngine(new UserId("Bob"));
        BlockCrdtEngine charlie = new BlockCrdtEngine(new UserId("Charlie"));
        
        BlockId root = insertText(alice, null, "Root ");
        bob.applyInsert(root, null);
        charlie.applyInsert(root, null);
        
        BlockId a1 = insertText(alice, root, "Alice's Block ");
        BlockId b1 = insertText(bob, root, "Bob's Block ");
        BlockId c1 = insertText(charlie, root, "Charlie's Block ");
        
        alice.applyInsert(b1, root);
        alice.applyInsert(c1, root);
        
        bob.applyInsert(a1, root);
        bob.applyInsert(c1, root);
        
        charlie.applyInsert(a1, root);
        charlie.applyInsert(b1, root);
        
        boolean success = compareTrees(alice.getVisibleBlocks(), bob.getVisibleBlocks()) && compareTrees(bob.getVisibleBlocks(), charlie.getVisibleBlocks());
        printTestConclusion("Test 12", success, alice);
    }

    private static void runTest13_CopyAndPasteBlock() {
        System.out.println("\n--- TEST 13: Copy and Paste an Entire Block ---");
        BlockCrdtEngine alice = new BlockCrdtEngine(new UserId("Alice"));
        
        BlockId root = insertText(alice, null, "Introduction. ");
        BlockId importantQuote = insertText(alice, root, "\"CRDTs are magic.\" ");
        BlockId conclusion = insertText(alice, root, "Conclusion. ");
        BlockId pastedQuote = alice.copyBlock(importantQuote, conclusion);
        boolean success = alice.getVisibleBlocks().size() == 4;
        printTestConclusion("Test 13", success, alice);
    }

    private static void runTest14_MoveBlock() {
        System.out.println("\n--- TEST 14: Move Block (Reorder within Document) ---");
        BlockCrdtEngine alice = new BlockCrdtEngine(new UserId("Alice"));
        BlockCrdtEngine bob = new BlockCrdtEngine(new UserId("Bob"));
        
        BlockId section1 = insertText(alice, null, "[Section 1] ");
        BlockId misplacedParagraph = insertText(alice, section1, "This belongs in Section 2! ");
        BlockId section2 = insertText(alice, null, "[Section 2] ");
        
        bob.applyInsert(section1, null);
        bob.applyInsert(misplacedParagraph, section1);
        bob.applyInsert(section2, null);
        BlockId movedParagraph = alice.moveBlock(misplacedParagraph, section2);
        bob.applyDelete(misplacedParagraph);
        bob.applyInsert(movedParagraph, section2);
        bob.pasteTextIntoBlock(movedParagraph, null, "This belongs in Section 2! ");

        boolean success = alice.getVisibleBlocks().size() == 3 
                       && compareTrees(alice.getVisibleBlocks(), bob.getVisibleBlocks());
        
        printTestConclusion("Test 14", success, alice);
    }


    private static void runTest15_Layer2Integration() {
        System.out.println("\n--- TEST 15: Layer 1 & 2 Text Integration ---");
        BlockCrdtEngine engine = new BlockCrdtEngine(new UserId("Alice"));
        BlockId sourceBlock = insertText(engine, null, "I am the original text. ");
        BlockId targetBlock = insertText(engine, null, "Target: ");
        engine.copyContentBetweenBlocks(sourceBlock, targetBlock, null);
    
        BlockId clonedBlock = engine.copyBlock(sourceBlock, null);
        BlockNode clonedNode = engine.getVisibleBlocks().stream()
                .filter(b -> b.getId().equals(clonedBlock))
                .findFirst().orElseThrow();
                
        String clonedText = clonedNode.getInnerTree().generateVisibleText();

        boolean success = "I am the original text. ".equals(clonedText);
        printTestConclusion("Test 15", success, engine);
    }

    // ========================
    //  --- HELPER METHODS --- 
    // ========================
    
    private static BlockId insertText(BlockCrdtEngine engine, BlockId parent, String text) {
        BlockId id = engine.insert(parent);
        if (id != null && text != null && !text.isEmpty()) {
            engine.pasteTextIntoBlock(id, null, text);
        }
        return id;
    }

    private static void printTestConclusion(String testName, boolean success, BlockCrdtEngine engine) {
        List<BlockNode> blocks = engine.getVisibleBlocks();
        
        System.out.println("  [Tree Structure]");
        if (blocks.isEmpty()) {
            System.out.println("   (Document is empty)");
        } else {
            for (int i = 0; i < blocks.size(); i++) {
                BlockNode node = blocks.get(i);
                boolean isRoot = node.getParentId() == null || node.getParentId().getUserId().getValue().equals("ROOT");
                String indent = isRoot ? "  ├── " : "  └── ";
                
                String text = node.getInnerTree().generateVisibleText();
                
                System.out.println(String.format("   %d.%s%s (ID: %s)", i + 1, indent, text.trim(), node.getId()));
            }
        }

        System.out.print("  [Resulting Sentence]: \"");
        for (BlockNode node : blocks) {
            System.out.print(node.getInnerTree().generateVisibleText());
        }
        System.out.println("\"");

        if (success) {
            System.out.println("  => [SUCCESS] " + testName + " passed!");
        } else {
            System.err.println("  => [FAILED] " + testName + " failed!");
        }
    }

    private static boolean compareTrees(List<BlockNode> tree1, List<BlockNode> tree2) {
        if (tree1.size() != tree2.size()) return false;
        for (int i = 0; i < tree1.size(); i++) {
            if (!tree1.get(i).getId().equals(tree2.get(i).getId())) {
                return false;
            }
        }
        return true;
    }
}