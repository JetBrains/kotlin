// "Change type of base property 'A.x' to 'Any'" "true"
interface A {
    val x: CharSequence
}

class B(override val x: Any<caret>) : A
/* FIR_COMPARISON */