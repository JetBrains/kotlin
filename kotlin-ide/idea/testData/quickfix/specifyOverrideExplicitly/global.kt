// "Specify override for 'foo(): Unit' explicitly" "true"

interface A {
    fun foo()
}

open class B : A {
    override fun foo() {}
}

fun bar(): A = null!!

val a: A = bar()

class C<caret>() : B(), A by a