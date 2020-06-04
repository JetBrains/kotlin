// IGNORE_FIR

fun <info textAttributesKey="KOTLIN_FUNCTION_DECLARATION">test</info>() {
    val <info textAttributesKey="KOTLIN_LOCAL_VARIABLE">vect</info> = <info textAttributesKey="KOTLIN_CONSTRUCTOR">MyIterable</info><<info textAttributesKey="KOTLIN_CLASS">Int</info>>()
    <info textAttributesKey="KOTLIN_LOCAL_VARIABLE">vect</info>.<info textAttributesKey="KOTLIN_FUNCTION_CALL">filter</info> { <info textAttributesKey="KOTLIN_CLOSURE_DEFAULT_PARAMETER">it</info> != 2 }.<info textAttributesKey="KOTLIN_FUNCTION_CALL">forEach</info> { <info textAttributesKey="KOTLIN_CLOSURE_DEFAULT_PARAMETER">it</info>.<info textAttributesKey="KOTLIN_FUNCTION_CALL">toString</info>() }
}

class <info textAttributesKey="KOTLIN_CLASS">MyIterable</info><<info textAttributesKey="KOTLIN_TYPE_PARAMETER">T</info>> {
    fun <info textAttributesKey="KOTLIN_FUNCTION_DECLARATION">filter</info>(<warning textAttributesKey="NOT_USED_ELEMENT_ATTRIBUTES"><info textAttributesKey="KOTLIN_PARAMETER">function</info></warning>: (<info textAttributesKey="KOTLIN_TYPE_PARAMETER">T</info>) -> <info textAttributesKey="KOTLIN_CLASS">Boolean</info>) = this
    fun <info textAttributesKey="KOTLIN_FUNCTION_DECLARATION">forEach</info>(<warning textAttributesKey="NOT_USED_ELEMENT_ATTRIBUTES"><info textAttributesKey="KOTLIN_PARAMETER">action</info></warning>: (<info textAttributesKey="KOTLIN_TYPE_PARAMETER">T</info>) -> <info textAttributesKey="KOTLIN_OBJECT">Unit</info>) {
    }
}
