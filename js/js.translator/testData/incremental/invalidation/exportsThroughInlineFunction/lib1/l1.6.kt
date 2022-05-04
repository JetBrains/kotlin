fun foo(flag: Boolean = false): String {
    if (flag) lib1Foo()
    return "hello 6"
}

inline fun fooInline() = "hello inline 3"
