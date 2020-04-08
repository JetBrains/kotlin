// WITH_RUNTIME
fun foo() {
    val list = 1..4

    <caret>for (x in list) { // start of loop
        // comment 1
        var v = x + 1
        // comment 2
        v++
        // end of loop
    }
}