open class A() {
    open fun foo(value : Int) : Unit = println(value)
    open val bar : Int = 0
}

class C : A() {
    val constant = 42
    // Some comment
    <caret>
    /*
        Some another comment
    */
    fun someAnotherFunction() {

    }
}
