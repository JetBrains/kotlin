val paramVal = 12
fun foo(paramTest: Int = 12)
fun paramFun() {}

fun test() {
    // Type '=', completion should be finishied
    foo(param<caret>)
}
