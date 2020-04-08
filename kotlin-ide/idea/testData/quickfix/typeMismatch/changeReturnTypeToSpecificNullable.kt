// "Change return type of enclosing function 'foo' to 'Int?'" "true"

fun foo(): String {
    val n: Int? = 1
    return <caret>n
}