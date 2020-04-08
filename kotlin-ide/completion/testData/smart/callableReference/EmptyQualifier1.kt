fun foo(p: () -> Unit){}

fun bar() {
    foo(::xf<caret>)
}

fun xf1(){}
fun xf1(s: String){}
fun xf2(i: Int){}

// EXIST: { lookupString:"xf1", itemText:"xf1", tailText: "() (<root>)", typeText: "Unit" }
// NOTHING_ELSE
