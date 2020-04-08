// "Replace with 'B'" "true"

class OldClass<T>

@Deprecated("Bad!", ReplaceWith("B"))
class A @Deprecated("Bad!", ReplaceWith("B()")) constructor()

class B

typealias Old = OldClass<<caret>A>

val o: Old = Old()
val a = A() // Usage of A()