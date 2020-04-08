fun main(args: Array<String>) {
    val b: Base = Derived()
    <caret>val a = 1
}

open class Base {
    fun funInBase() {}

    open fun funWithOverride() { }
    open fun funWithoutOverride() { }

    fun funInBoth() { }
}

open class Intermediate : Base() {
    fun funInIntermediate(){}
}

class Derived : Intermediate() {
    fun funInDerived() { }

    override fun funWithOverride() { }

    fun funInBoth(p: Int) { }
}

// INVOCATION_COUNT: 1
// EXIST: { itemText: "funInBase", tailText: "()", attributes: "bold" }
// EXIST: { itemText: "funWithOverride", tailText: "()", attributes: "bold" }
// EXIST: { itemText: "funWithoutOverride", tailText: "()", attributes: "bold" }
// EXIST: { itemText: "funInDerived", tailText: "()", attributes: "bold" }
// EXIST: { itemText: "funInBoth", tailText: "()", attributes: "bold" }
// EXIST: { itemText: "funInBoth", tailText: "(p: Int)", attributes: "bold" }
// EXIST: { itemText: "funInIntermediate", tailText: "()", attributes: "" }
// NOTHING_ELSE


// RUNTIME_TYPE: Derived