// "Remove 'suspend' modifier from all functions in hierarchy" "true"
open class A {
    open suspend fun foo() {

    }

    open fun foo(n: Int) {

    }
}

open class B : A() {
    override fun <caret>foo() {

    }

    override fun foo(n: Int) {

    }
}

open class B1 : A() {
    override suspend fun foo() {

    }

    override fun foo(n: Int) {

    }
}

open class C : B() {
    override fun foo() {

    }

    override fun foo(n: Int) {

    }
}

open class C1 : B() {
    override fun foo() {

    }

    override fun foo(n: Int) {

    }
}