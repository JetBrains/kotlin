fun box() = expectThrowableMessage {
    assert("Name" in listOf("Hello", "World"))
} + expectThrowableMessage {
    // Test that we don't just search for `in` in the expression.
    assert(" in " in listOf("Hello", "World"))
} + expectThrowableMessage {
    // Test multiline case
    assert(
        " in "

                        in

                   listOf("Hello", "World")
    )
} + expectThrowableMessage {
    // Test that we don't assume whitespaces around the infix operator
    assert("Name"/*in*/in/*in*/listOf("Hello", "World"))
} + expectThrowableMessage {
    // Test nested `in`
    assert(("Name" in listOf("Hello", "World")) in listOf(true))
}
