fun C.extFunForC(){}
fun D.extFunForD(){}
fun Any.extFunForAny(){}

open class C {
    fun foo() {
        if (this is D) {
            <caret>
        }
    }
}

class D : C

// EXIST: { itemText: "extFunForD", attributes: "bold" }
// EXIST: { itemText: "extFunForC", attributes: "" }
// EXIST: { itemText: "extFunForAny", attributes: "" }
