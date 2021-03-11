fun test() {
    K.message("foo.bar")
    J.message("foo.bar")
    K.message(key = "foo.bar", args = 1)
    K.message("foo.bar2", *arrayOf(1, 2))
    K.message2("foo.bar", 1)
}