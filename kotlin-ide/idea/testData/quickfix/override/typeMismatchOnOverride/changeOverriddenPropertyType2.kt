// "Change type of base property 'A.x' to 'String'" "true"
interface A {
    var x: Int
}

interface B {
    var x: String
}

interface C : A, B {
    override var x: String<caret>
}
/* FIR_COMPARISON */