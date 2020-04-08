import kotlin.reflect.KProperty0

fun foo(p: KProperty0<Int>){}

fun bar() {
    foo(::xT<caret>)
}

val xTopLevelIntVal = 1
var xTopLevelIntVar = 1
val xTopLevelStringVal = "1"

// EXIST: { lookupString:"xTopLevelIntVal", itemText:"xTopLevelIntVal", tailText: " (<root>)", typeText: "Int" }
// EXIST: { lookupString:"xTopLevelIntVar", itemText:"xTopLevelIntVar", tailText: " (<root>)", typeText: "Int" }
// NOTHING_ELSE
