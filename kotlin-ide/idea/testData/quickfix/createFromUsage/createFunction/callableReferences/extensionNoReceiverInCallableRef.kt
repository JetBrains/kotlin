// "Create function 'foo'" "true"
fun <T, U> T.map(f: T.() -> U) = f()

fun consume(s: String) {}

fun test() {
    consume(1.map(::<caret>foo))
}