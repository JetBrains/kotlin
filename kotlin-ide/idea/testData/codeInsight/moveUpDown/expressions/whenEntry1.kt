// MOVE: down

fun foo(x: Boolean) {
    when (x) {
        <caret>// test
        false -> {

        }
        true -> {

        }
        else -> {

        }
    }
}
