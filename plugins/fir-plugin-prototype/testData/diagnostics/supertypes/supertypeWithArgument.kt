package foo

import org.jetbrains.kotlin.fir.plugin.SupertypeWithTypeArgument

interface InterfaceWithArgument<T> {
    fun generate(): T = null!!
}

@SupertypeWithTypeArgument(String::class)
class TopLevelType

class Nested

abstract class Base {
    class Nested
}

class Derived : Base() {
    @SupertypeWithTypeArgument(Nested::class)
    class TypeFromSupertype

    @SupertypeWithTypeArgument(foo.Nested::class)
    class QualifiedType
}

@SupertypeWithTypeArgument(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!><!UNRESOLVED_REFERENCE!>UnresolvedClass<!>::class<!>)
class UnresolvedType

fun takeString(x: String) {}
fun takeTopLevelNested(x: Base.Nested) {}
fun takeInnerNested(x: Nested) {}

fun test_1(x: TopLevelType) {
    takeString(x.generate())
}

fun test_2(x: Derived.TypeFromSupertype) {
    takeTopLevelNested(x.generate())
}

fun test_3(x: Derived.QualifiedType) {
    takeInnerNested(x.generate())
}
