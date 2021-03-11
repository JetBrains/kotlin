open class B(p: Int)

class C : B {
    constructor(pInt: Int, pString: String) : super(<caret>)
}

// EXIST: pInt
// ABSENT: pString
