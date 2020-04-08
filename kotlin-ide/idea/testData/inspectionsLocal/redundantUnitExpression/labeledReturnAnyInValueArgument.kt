// PROBLEM: none

fun foo(f: () -> Unit, g: () -> Any) {}

fun test() {
    foo({ return@foo Unit }, { return@foo Unit<caret> })
}