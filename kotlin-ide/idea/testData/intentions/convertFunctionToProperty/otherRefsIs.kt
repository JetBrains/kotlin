// WITH_RUNTIME
package p

import p.isFoo

class A(val n: Int)

fun A.<caret>isFoo(): Boolean = n > 1

fun test() {
    val t = A::isFoo
}