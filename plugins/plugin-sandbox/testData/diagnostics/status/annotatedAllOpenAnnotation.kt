typealias MyTypeAlias = org.jetbrains.kotlin.plugin.sandbox.AllOpen

@MyTypeAlias
annotation class MyAnno

@MyAnno
class A {
    fun foo() {

    }
}

@MyAnno
class B : A() {
    <!OVERRIDING_FINAL_MEMBER!>override<!> fun foo() {

    }
}
