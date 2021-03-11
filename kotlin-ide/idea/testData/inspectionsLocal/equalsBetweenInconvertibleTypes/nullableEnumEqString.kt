// FIX: none
enum class E1

fun test(x: E1?, y: String): Boolean? {
    return x?.<caret>equals(y)
}