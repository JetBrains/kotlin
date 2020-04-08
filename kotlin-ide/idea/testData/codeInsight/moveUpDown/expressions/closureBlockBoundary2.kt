// MOVE: down
fun foo() {
    bar {
        /**/ val foo = 1
        println("foo=")
        <caret>println(foo) /**/
    }
}