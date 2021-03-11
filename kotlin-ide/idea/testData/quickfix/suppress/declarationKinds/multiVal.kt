// "Suppress 'REDUNDANT_NULLABLE' for initializer " "true"

fun foo() {
    val (a, b) = Pair<String?<caret>?, String>("", "")
}

data class Pair<A, B>(val a: A, val b: B)