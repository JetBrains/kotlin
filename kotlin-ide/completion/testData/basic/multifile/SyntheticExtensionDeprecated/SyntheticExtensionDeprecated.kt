fun foo(javaClass: JavaClass) {
    javaClass.<caret>
}

// WITH_ORDER
// EXIST: { lookupString: "something2", attributes: "bold" }
// EXIST: { lookupString: "something3", attributes: "bold" }
// EXIST: { lookupString: "something1", attributes: "bold strikeout" }
// EXIST: { lookupString: "something4", attributes: "bold strikeout" }
