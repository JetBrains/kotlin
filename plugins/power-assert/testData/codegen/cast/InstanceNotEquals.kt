fun box() = expectThrowableMessage {
    assert("Hello, world!" !is String)
} + "\n\n" + expectThrowableMessage {
    // Test that we don't just search for `!is` in the expression.
    assert(" !is " !is String)
} + "\n\n" + expectThrowableMessage {
    // Test multiline case
    assert(
        " !is "

                !is

                String
    )
} + "\n\n" + expectThrowableMessage {
    // Test that we don't assume whitespaces around the operator
    assert("Hello, world!"/*!is*/!is/*!is*/String)
} + "\n\n" + expectThrowableMessage {
    // Test nested `!is`
    assert(("Hello, world!" !is String) !is Boolean)
}
