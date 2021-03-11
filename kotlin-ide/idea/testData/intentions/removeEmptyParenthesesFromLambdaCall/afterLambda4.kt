// IS_APPLICABLE: false

fun Unit.returnFun3(fn: (Unit) -> Unit): ((Unit) -> Unit) -> Unit = {}

fun test() {
    Unit.returnFun3 {} ()<caret> {}
}