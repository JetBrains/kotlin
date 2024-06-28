// ISSUE: KT-57654
package kt57654

class A {
    fun foo() {}
}

fun useA(block: A.() -> Unit) {}
