// WITH_RUNTIME
package p

import p.foo

class A(val n: Int)

fun A.<caret>foo(): Boolean = n > 1

fun test() {
    val t = A::foo
}