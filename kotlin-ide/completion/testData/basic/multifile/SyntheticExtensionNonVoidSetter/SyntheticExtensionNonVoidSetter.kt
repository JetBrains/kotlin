fun foo(javaClass: JavaClass) {
    javaClass.<caret>
}

// EXIST: { lookupString: "something", attributes: "bold" }
// EXIST: { lookupString: "setSomething", attributes: "bold" }
// ABSENT: getSomething