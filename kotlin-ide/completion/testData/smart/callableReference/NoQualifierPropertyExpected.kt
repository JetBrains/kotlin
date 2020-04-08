import kotlin.reflect.KProperty

fun foo(property: KProperty<Int>) {
}

fun bar() {
    foo(<caret>)
}

val vInt = 0
val vString = ""
fun fInt() = 0

// EXIST: { lookupString: "::vInt", itemText: "::vInt", tailText: " (<root>)", typeText: "Int" }
// ABSENT: ::vString
// ABSENT: ::fInt
