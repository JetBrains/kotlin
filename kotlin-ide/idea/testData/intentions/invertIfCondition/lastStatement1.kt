fun foo() {
    val x = 2
    <caret>if (x > 1) {
        // invoke bar1&bar2
        bar1()
        bar2()
        // done
    }
}

fun bar1(){}
fun bar2(){}