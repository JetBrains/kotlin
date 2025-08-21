typealias MyTypeAlias = org.jetbrains.kotlin.plugin.sandbox.AllOpen

@MyTypeAlias
annotation class MyAnno

@MyAnno
class A {
    fun foo() {

    }
}

@MyAnno
class B : <!FINAL_SUPERTYPE!>A<!>() {
    <!OVERRIDING_FINAL_MEMBER!>override<!> fun foo() {

    }
}
