fun <info textAttributesKey="KOTLIN_FUNCTION_DECLARATION">test</info>() {
    <info textAttributesKey="KOTLIN_CONSTRUCTOR">Test</info>("text", "text")() // BUG
}

class <info textAttributesKey="KOTLIN_CLASS">Test</info>(val <info textAttributesKey="KOTLIN_INSTANCE_PROPERTY">x</info>: <info textAttributesKey="KOTLIN_CLASS">String</info>, val <info textAttributesKey="KOTLIN_INSTANCE_PROPERTY">y</info>: <info textAttributesKey="KOTLIN_CLASS">String</info>) {
    <info textAttributesKey="KOTLIN_BUILTIN_ANNOTATION">operator</info> fun <info textAttributesKey="KOTLIN_FUNCTION_DECLARATION">invoke</info>() {
    }
}
