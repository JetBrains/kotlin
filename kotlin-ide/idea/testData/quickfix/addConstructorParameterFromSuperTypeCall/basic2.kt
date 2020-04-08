// "Add constructor parameter 'x'" "true"
// DISABLE-ERRORS
abstract class A(val x: Int, val y: String, val z: Long)
class B() : A(<caret>)