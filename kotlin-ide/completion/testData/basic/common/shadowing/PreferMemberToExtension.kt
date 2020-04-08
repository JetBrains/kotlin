package ppp

class C {
    val xxx = ""
    fun xxx() = ""
    fun xxx(p: Int) = ""

    fun foo() {
        xx<caret>
    }
}

val C.xxx: Int
    get() = 1

fun C.xxx() = 1
fun C.xxx(p: Int) = 1
fun C.xxx(p: String) = 1

// EXIST: { lookupString: "xxx", itemText: "xxx", tailText: null, typeText: "String" }
// EXIST: { lookupString: "xxx", itemText: "xxx", tailText: "()", typeText: "String" }
// EXIST: { lookupString: "xxx", itemText: "xxx", tailText: "(p: Int)", typeText: "String" }
// EXIST: { lookupString: "xxx", itemText: "xxx", tailText: "(p: String) for C in ppp", typeText: "Int" }
// NOTHING_ELSE
