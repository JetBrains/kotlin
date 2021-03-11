// PROBLEM: none
package kotlin

fun test() {
    val x = foo@<caret>{ return@foo Result(true) }
}
