// FIR_COMPARISON
interface T {
    fun foo1(){}
    fun foo2(){}
}

class B(worker: T) : T by worker {
    override fun foo1() { }
}

fun foo(b: B) {
    b.<caret>
}

// EXIST: { itemText: "foo1", attributes: "" }
// EXIST: { itemText: "foo2", attributes: "" }
// EXIST: { itemText: "equals", attributes: "" }
