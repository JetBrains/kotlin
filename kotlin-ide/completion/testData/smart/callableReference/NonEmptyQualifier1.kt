class C {
    fun xf1(){}
    fun xf1(s: String){}
    fun xf2(i: Int){}
}

fun C.xfe1(){}
fun C.xfe1(s: String){}
fun C.xfe2(i: Int){}


fun foo(p: C.(String) -> Unit){}

fun bar() {
    foo(C::<caret>)
}

// EXIST: { lookupString:"xf1", itemText:"xf1", tailText: "(s: String)", typeText: "Unit" }
// EXIST: { lookupString:"xfe1", itemText:"xfe1", tailText: "(s: String) for C in <root>", typeText: "Unit" }
// NOTHING_ELSE
