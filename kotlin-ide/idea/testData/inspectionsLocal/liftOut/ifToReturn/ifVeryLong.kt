// PROBLEM: none

fun println(s: String) {}

fun test(n: Int): String {
    <caret>if (n == 1) {
        println("123")
        println("456")
        println("789")
        println("123")
        println("456")
        println("789")
        return "one"
    }
    else if (n == 2) {
        println("123")
        println("456")
        println("789")
        println("123")
        println("456")
        println("789")
        return "two"
    }
    else {
        println("123")
        println("456")
        println("789")
        println("123")
        println("456")
        println("789")
        return "three"
    }
}