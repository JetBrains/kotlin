fun box() = expectThrowableMessage {
    assert("Hello" !in listOf("Hello", "World"))
} + "\n\n" + expectThrowableMessage {
    // Test that we don't just search for `!in` in the expression.
    assert(" !in " !in listOf(" !in "))
} + "\n\n" + expectThrowableMessage {
    // Test multiline case
    assert(
        " !in "

                !in

                listOf(" !in ")
    )
} + "\n\n" + expectThrowableMessage {
    // Test that we don't assume whitespaces around the infix operator
    assert("Hello"/*!in*/!in/*!in*/listOf("Hello", "World"))
} + "\n\n" + expectThrowableMessage {
    // Test nested `!in`
    assert(("Hello" !in listOf("Hello", "World")) !in listOf(false))
}
