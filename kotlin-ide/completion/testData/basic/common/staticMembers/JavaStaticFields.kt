fun foo() {
    SOU<caret>
}

// INVOCATION_COUNT: 2
// EXIST_JAVA_ONLY: { allLookupStrings: "SOUTH, getSOUTH", itemText: "GridBagConstraints.SOUTH", tailText: " (java.awt)", typeText: "Int", attributes: "" }
// EXIST_JAVA_ONLY: { allLookupStrings: "SOUTHWEST, getSOUTHWEST", itemText: "GridBagConstraints.SOUTHWEST", tailText: " (java.awt)", typeText: "Int", attributes: "" }
// ABSENT: serialVersionUID
