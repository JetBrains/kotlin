// "Implement members" "true"
// WITH_RUNTIME
interface I {
    fun foo()
}

data <caret>class C(val i: Int) : I {
    fun bar() {}
}