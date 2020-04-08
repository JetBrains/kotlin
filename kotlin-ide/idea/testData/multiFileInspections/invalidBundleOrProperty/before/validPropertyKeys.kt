fun test() {
    K.message("foo.bar", "arg")
    K.message("foo.bar", "arg1", "arg2")
    J.message("foo.bar", "arg")
    J.message("foo.bar", "arg1", "arg2")
    K.message3("test")
    K.message3("tests")
}