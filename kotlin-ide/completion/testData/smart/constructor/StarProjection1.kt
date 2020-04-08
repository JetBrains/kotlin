class X<D>()

fun test() {
    bar(<caret>)
}

fun bar(foo: X<*>) {}

// EXIST: { lookupString:"X", itemText:"X", tailText:"() (<root>)" }
