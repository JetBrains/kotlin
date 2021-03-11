import java.awt.GridBagConstraints.NORTH

fun foo() {
    SOUT<caret>
}

// INVOCATION_COUNT: 1
// EXIST_JAVA_ONLY: { allLookupStrings: "SOUTH, getSOUTH", itemText: "GridBagConstraints.SOUTH", tailText: " (java.awt)", typeText: "Int", attributes: "" }
// ABSENT: { itemText: "SwingConstants.SOUTH" }
