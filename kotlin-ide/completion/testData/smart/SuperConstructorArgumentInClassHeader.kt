open class B(p: Int)

val pInt = 0
val pString = ""

class C : B(<caret>)

// EXIST: pInt
// ABSENT: pString
