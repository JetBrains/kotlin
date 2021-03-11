fun foo(thread: Thread) {
    thread.<caret>
}

// INVOCATION_COUNT: 2
// EXIST: { lookupString: "priority", itemText: "priority", tailText: " (from getPriority()/setPriority())", typeText: "Int" }
// EXIST: getPriority
// EXIST: setPriority
