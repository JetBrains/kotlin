package inlineFunInLibrary

inline fun foo(f: () -> Unit) {
    null!!
    f()
}
