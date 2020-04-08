interface I : suspend () -> Unit

fun xfoo(p: suspend () -> Unit) {}

fun test(action: suspend () -> Unit, i: I, notSuspend: () -> Unit) {
    xf<caret>
}

// EXIST: { itemText:"xfoo", tailText:" {...} (p: suspend () -> Unit) (<root>)", typeText:"Unit" }
// EXIST: { itemText:"xfoo", tailText:"(action) (<root>)", typeText:"Unit" }
// EXIST: { itemText:"xfoo", tailText:"(i) (<root>)", typeText:"Unit" }
// NOTHING_ELSE