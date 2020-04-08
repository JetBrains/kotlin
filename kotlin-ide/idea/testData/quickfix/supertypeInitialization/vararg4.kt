// "Add constructor parameters from A(Array<out String>)" "true"
open class A(vararg strings: String = arrayOf("a", "b"))

class B : A<caret>