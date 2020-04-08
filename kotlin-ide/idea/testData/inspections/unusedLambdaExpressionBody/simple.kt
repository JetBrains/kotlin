
fun foo() {
    bar()
    val a = bar()
    if (bar() != null) {
        bar()
    }
    bar()()
}

fun bar() = {

}