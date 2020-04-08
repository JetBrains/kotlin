class Foo(f: () -> Unit)

fun main(args: String) {
    Foo(fun<caret>() {
        val p = 1
    })
}