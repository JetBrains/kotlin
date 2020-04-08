// "Replace with 'A.bar(x)'" "true"

package test

import a.A

@Deprecated("bla", ReplaceWith("A.bar(x)", "a.A"))
fun foo(x: Any) {
}

fun test() {
    A.bar(1)
}