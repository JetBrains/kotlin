// PROBLEM: none

fun test(r: Runnable) {}

fun usage() {
    test(Runnable<caret> a@{ return@a })
}