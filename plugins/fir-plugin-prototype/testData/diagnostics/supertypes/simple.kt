package foo

import org.jetbrains.kotlin.fir.plugin.MyInterfaceSupertype

interface MyInterface {
    fun foo()
}

@MyInterfaceSupertype
abstract class AbstractClass

@MyInterfaceSupertype
class FinalClassWithOverride {
    override fun foo() {}
}

@MyInterfaceSupertype
<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class FinalClassWithoutOverride<!> {
    // should be error
}

class NotAnnotatedWithOverride {
    // should be error
    <!NOTHING_TO_OVERRIDE!>override<!> fun foo() {}
}

@MyInterfaceSupertype
class AnnotatedClassWithExplicitInheritance : MyInterface {
    override fun foo() {}
}

fun test_1(x: AbstractClass) {
    x.foo()
}

fun test_2(x: FinalClassWithOverride) {
    x.foo()
}

fun test_3(x: FinalClassWithoutOverride) {
    x.foo()
}
