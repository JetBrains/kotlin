// RENDER_PREDICATES
// NEVER_VALIDATE

open class Baz()

class PrimitiveFields(val a: Int, var b: Int)

class ReferenceField(val pf: PrimitiveFields) : Baz()

class Recursive(val next: Recursive?)

fun <!VIPER_TEXT!>useClasses<!>(rf: ReferenceField, rec: Recursive) { }

open class A() {
    val x: Int = 1
    var y: Int = 2
}
open class B() : A()
class C() : B()

fun <!VIPER_TEXT!>threeLayersHierarchy<!>(c: C) { }

fun <!VIPER_TEXT!>listHierarchy<!>(xs: MutableList<Int>) { }
