// PROBLEM: none
// WITH_RUNTIME
fun findCredentials() = 1

fun println(i: Int) {}

fun test() {
    val data = run {
        val credentials = findCredentials()

        class Foo {
            val <caret>foundCredentials = credentials
        }

        Foo()
    }

    val foundCredentials = data.foundCredentials
}