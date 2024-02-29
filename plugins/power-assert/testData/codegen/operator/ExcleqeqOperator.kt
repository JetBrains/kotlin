fun box() = expectThrowableMessage {
    assert(1 !== 1)
} + "\n\n" + expectThrowableMessage {
    // Test that we don't just search for `!==` in the expression.
    assert(" !== " !== " !== ")
} + "\n\n" + expectThrowableMessage {
    // Test multiline case
    assert(
        " !== "

                !==

                " !== "
    )
} + "\n\n" + expectThrowableMessage {
    // Test that we don't assume whitespaces around the infix operator
    assert(1/*!==*/!==/*!==*/1)
} + "\n\n" + expectThrowableMessage {
    // Test nested `!==`
    assert((1 !== 1) !== false)
}
