fun foo() {
    myFor@
    for (i in 1..10) {
        myWhile@
        while (x()) {
            myDo@
            do {
                continue@<caret>
            } while (y())
        }
    }
}

// EXIST: { lookupString: "continue@myDo", itemText: "continue", tailText: "@myDo", attributes: "bold" }
// EXIST: { lookupString: "continue@myWhile", itemText: "continue", tailText: "@myWhile", attributes: "bold" }
// EXIST: { lookupString: "continue@myFor", itemText: "continue", tailText: "@myFor", attributes: "bold" }
// NOTHING_ELSE
