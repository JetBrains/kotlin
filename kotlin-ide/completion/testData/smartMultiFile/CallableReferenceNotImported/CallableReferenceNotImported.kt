fun String.foo() {
    bar(::<caret>)
}

fun bar(p: () -> Unit) { }
fun bar(p: String.() -> Unit) { }


// EXIST: topLevelFun
// ABSENT: extFun
