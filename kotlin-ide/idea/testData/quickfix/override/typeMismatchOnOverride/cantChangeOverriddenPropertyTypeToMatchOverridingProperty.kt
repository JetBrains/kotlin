// "class org.jetbrains.kotlin.idea.quickfix.ChangeVariableTypeFix" "false"
// ERROR: Type of 'x' doesn't match the type of the overridden var-property 'public abstract var x: Int defined in A'
interface A {
    var x: Int
}

interface B {
    var x: Any
}

interface C : A, B {
    override var x: String<caret>
}
/* FIR_COMPARISON */