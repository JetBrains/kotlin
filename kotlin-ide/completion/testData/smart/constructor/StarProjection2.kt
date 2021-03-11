class X<D>(d: D)

fun test() {
    bar(<caret>)
}

fun bar(foo: X<*>) {}

// EXIST: { lookupString:"X", itemText:"X", tailText:"(d: D) (<root>)" }
