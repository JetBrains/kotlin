fun foo() {
    i<caret>
}

// INVOCATION_COUNT: 2
// EXIST_JAVA_ONLY: { allLookupStrings: "invokeLater", itemText: "SwingUtilities.invokeLater", tailText: "(doRun: Runnable!) (javax.swing)", typeText: "Unit", attributes: "" }
// EXIST_JAVA_ONLY: { allLookupStrings: "invokeAndWait", itemText: "SwingUtilities.invokeAndWait", tailText: "(doRun: Runnable!) (javax.swing)", typeText: "Unit", attributes: "" }
// EXIST_JAVA_ONLY: { allLookupStrings: "invokeLater", itemText: "SwingUtilities.invokeLater", tailText: " {...} (doRun: (() -> Unit)!) (javax.swing)", typeText: "Unit", attributes: "" }
// ABSENT: { itemText: "SwingUtilities.installSwingDropTargetAsNecessary" }
