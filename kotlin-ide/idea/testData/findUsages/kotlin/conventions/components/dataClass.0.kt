// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtParameter
// OPTIONS: usages
package pack

data class A(val <caret>n: Int, val s: String, val o: Any) {
    fun f() {
        "a".apply {
            this@A.toString()
        }
    }
}

abstract class X : Comparable<A>

fun A.ext1() {
    val (x, y) = getThis()
}

/**
 * Doc-comment reference 1: [A]
 * Doc-comment reference 2: [ext1]
 */
fun List<A>.ext1() {
    val (x, y) = get(0)
}

fun <T> T.getThis(): T = this
