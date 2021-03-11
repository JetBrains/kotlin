import kotlin.reflect.KProperty1

fun foo(p: KProperty1<String, Int>){}

fun bar() {
    foo(String::xT<caret>)
}

val String.xTopLevelIntVal: Int get()  = 1
val String.xTopLevelStringVal: String get() = "1"
val Any.xTopLevelValOnAny: Int get()  = 1
val Int.xTopLevelValOnInt: Int get()  = 1

// EXIST: { lookupString:"xTopLevelIntVal", itemText:"xTopLevelIntVal", tailText: " for String in <root>", typeText: "Int" }
// EXIST: { lookupString:"xTopLevelValOnAny", itemText:"xTopLevelValOnAny", tailText: " for Any in <root>", typeText: "Int" }
// NOTHING_ELSE
