class C(p: Int) {
    constructor(pString: String, pInt: Int) : this(<caret>)
}

// EXIST: pInt
// ABSENT: pString
