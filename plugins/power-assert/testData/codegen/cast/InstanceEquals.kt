fun box() = expectThrowableMessage {
    assert(null is String)
} + "\n\n" + expectThrowableMessage {
    // Test that we don't just search for `is` in the expression.
    assert(!(" is " is String))
} + "\n\n" + expectThrowableMessage {
    // Test multiline case
    assert(!(
        " is "

                is

                String
    ))
} + "\n\n" + expectThrowableMessage {
    // Test that we don't assume whitespaces around the operator
    assert(null/*is*/is/*is*/String)
} + "\n\n" + expectThrowableMessage {
    // Test nested `is`
    assert(!((null is String) is Boolean))
}
