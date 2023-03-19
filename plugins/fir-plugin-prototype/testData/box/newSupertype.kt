
package foo

import org.jetbrains.kotlin.fir.plugin.MyInterfaceSupertype

interface MyInterface {
    fun foo() {}
}

@MyInterfaceSupertype
class FinalClassWithOverride {
    override fun foo() {}
}

@MyInterfaceSupertype
class AnnotatedClassWithExplicitInheritance : MyInterface {
    override fun foo() {}
}

fun test(x: MyInterface) {
    x.foo()
}

fun box(): String {
    val x = FinalClassWithOverride()
    x.foo()
    test(x)

    val y = AnnotatedClassWithExplicitInheritance()
    y.foo()
    test(y)

    return "OK"
}
