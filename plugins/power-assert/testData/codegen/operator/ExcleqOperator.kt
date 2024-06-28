fun box() = expectThrowableMessage {
    assert(1 != 1)
} + expectThrowableMessage {
    // Test that we don't just search for `!=` in the expression.
    assert(" != " != " != ")
} + expectThrowableMessage {
    // Test multiline case
    assert(
        " != "

                !=

                " != "
    )
} + expectThrowableMessage {
    // Test that we don't assume whitespaces around the infix operator
    assert(1/*!=*/!=/*!=*/1)
} + expectThrowableMessage {
    // Test nested `!=`
    assert((1 != 1) != false)
}
