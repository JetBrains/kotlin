// "Implement members" "true"
// WITH_RUNTIME
abstract class C {
    abstract val p: String?
    abstract val q: Int
    abstract fun test()
}

<caret>class MyImpl3(
    override val p: String? = null
) : C() {
    override val q: Int = 0
}