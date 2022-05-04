fun foo(): String {
    if (false) lib1Foo()
    return "hello 5"
}

inline fun fooInline() = "hello inline 3"
