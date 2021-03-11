fun String.extFunForString(): Int = 0
fun Any.extFunForAny(): Int = 0

class C {
    fun foo(): Int {
        return "".<caret>
    }
}

// EXIST: { itemText: "extFunForString", attributes: "bold" }
// EXIST: { itemText: "extFunForAny", attributes: "" }
