package inlineFunPackage

inline fun foo(f: () -> Unit) {
    null!!
    f()
}
