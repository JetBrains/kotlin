package some

fun foo(i: Int) = 1
fun test() {
    foo(B<caret>)
}

/* KT-3779, KT-2821 */
// INVOCATION_COUNT: 2
// EXIST: Bar