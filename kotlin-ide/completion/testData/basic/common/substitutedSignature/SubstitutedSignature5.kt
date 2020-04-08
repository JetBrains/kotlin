fun<T> T.doSomething(t: T): T = t

fun foo(s: String?) {
    s.<caret>
}

// EXIST: { itemText: "doSomething", tailText: "(t: String?) for T in <root>", typeText: "String?" }
