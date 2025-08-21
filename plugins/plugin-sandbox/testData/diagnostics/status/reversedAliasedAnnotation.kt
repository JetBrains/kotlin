@MyTypeAlias
class A {
    fun foo() {

    }
}

@MyTypeAlias
class B : A() {
    <!OVERRIDING_FINAL_MEMBER!>override<!> fun foo() {

    }
}

typealias MyTypeAlias = org.jetbrains.kotlin.plugin.sandbox.AllOpen
