fun test(i: Int) {
    var fn: () -> String

    <caret>if (i == 1) {
        fn = { "foo" }
    } else if (i == 2) {
        fn = { "bar" }
    } else {
        fn = { "baz" }
    }
}