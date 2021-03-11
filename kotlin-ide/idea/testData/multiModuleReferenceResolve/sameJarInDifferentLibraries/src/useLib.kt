package m1

public fun use() {
    library.foo()
}

// SEARCH_TEXT: foo
// REF: (library).foo()
// BINARY: library/LibKt.class
// SRC: libSrc/lib.kt