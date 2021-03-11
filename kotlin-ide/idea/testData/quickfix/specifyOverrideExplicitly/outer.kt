// "Specify override for 'foo(): Unit' explicitly" "true"

interface A {
    fun foo()
}

open class B : A {
    override fun foo() {}
}

fun bar(a: A) {
    class C<caret> : B(), A by a
}