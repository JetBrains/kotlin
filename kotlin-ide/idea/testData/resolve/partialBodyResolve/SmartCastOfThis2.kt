class C {
    fun foo(){}
}

fun Any.f() {
    if (this !is C) return
    this.<caret>foo()
}