package ppp

class C {
    fun xxx(vararg s: String, p: Int) = ""

    fun foo() {
        xx<caret>
    }
}

fun C.xxx(vararg s: String, p: Int) = 1
fun Any.xxx(vararg s: String, c: Char) = 1
fun C.xxx(vararg s: String, c: Char) = 1

// EXIST: { lookupString: "xxx", itemText: "xxx", tailText: "(vararg s: String, p: Int)", typeText: "String" }
// EXIST: { lookupString: "xxx", itemText: "xxx", tailText: "(vararg s: String, c: Char) for C in ppp", typeText: "Int" }
// NOTHING_ELSE
