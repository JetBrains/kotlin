// "Remove 'out' modifier" "true"
fun <T> foo(x : T) {}

fun bar() {
    foo<<caret>out Int>(44)
}
