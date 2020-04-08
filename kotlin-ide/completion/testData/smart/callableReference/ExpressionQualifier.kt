class C {
    fun xf0(s: String){}
    fun xf1(){}
    fun xf1(s: String){}
    fun xf2(i: Int){}
}

fun C.xfe0(s: String){}
fun C.xfe1(){}
fun C.xfe1(s: String){}
fun C.xfe2(i: Int){}

fun Any.anyF(s: String){}
fun String.stringF(s: String){}

fun foo(p: (String) -> Unit){}

fun bar(c: C) {
    foo(c::<caret>)
}

// EXIST: { lookupString:"xf0", itemText:"xf0", tailText: "(s: String)", typeText: "Unit" }
// EXIST: { lookupString:"xf1", itemText:"xf1", tailText: "(s: String)", typeText: "Unit" }
// EXIST: { lookupString:"xfe0", itemText:"xfe0", tailText: "(s: String) for C in <root>", typeText: "Unit" }
// EXIST: { lookupString:"xfe1", itemText:"xfe1", tailText: "(s: String) for C in <root>", typeText: "Unit" }
// EXIST: { lookupString:"anyF", itemText:"anyF", tailText: "(s: String) for Any in <root>", typeText: "Unit" }
// NOTHING_ELSE
