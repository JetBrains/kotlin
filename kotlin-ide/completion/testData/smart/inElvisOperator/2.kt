fun foo1(): String? = null
fun foo2(): String? = null
fun foo3(): String = ""

fun bar() {
    foo1() ?: <caret>
}

// EXIST: { itemText:"foo3" }
// ABSENT: { itemText:"foo2" }
// EXIST: { itemText:"!! foo2" }
// EXIST: { itemText:"?: foo2" }
