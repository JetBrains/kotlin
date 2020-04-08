// "Remove redundant receiver parameter" "true"
fun <caret>Any.foo() {

}

fun test() {
    1.foo()
}