// "Create secondary constructor" "true"

interface T

class A: T

fun test() {
    val t: T = A(<caret>1)
}