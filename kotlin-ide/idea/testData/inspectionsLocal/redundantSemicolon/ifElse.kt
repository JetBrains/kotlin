// PROBLEM: none
// WITH_RUNTIME

fun test() {
    if (true) println("hello") else<caret>;
    println("hi")
}