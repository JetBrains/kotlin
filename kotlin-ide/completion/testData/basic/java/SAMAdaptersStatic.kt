fun foo() {
    javax.swing.SwingUtilities.invoke<caret>
}

// EXIST: { lookupString: "invokeLater", itemText: "invokeLater", tailText: "(doRun: Runnable!)", typeText: "Unit" }
// EXIST: { lookupString: "invokeLater", itemText: "invokeLater", tailText: " {...} (doRun: (() -> Unit)!)", typeText: "Unit" }
