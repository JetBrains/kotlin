// MOVE: up

fun foo(x: Boolean) {
    when (x) {
        false -> {
            <caret>// test
        }
        true -> {

        }
        else -> {

        }
    }
}
