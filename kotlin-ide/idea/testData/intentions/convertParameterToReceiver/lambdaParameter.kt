// IS_APPLICABLE: false

val foo = { <caret>s: String, n: Int -> s.length - n/2 > 1 }

fun test() {
    foo("1", 2)
}