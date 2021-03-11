// IGNORE_FIR

<info descr="null" textAttributesKey="KOTLIN_BUILTIN_ANNOTATION">open</info> class <info descr="null" textAttributesKey="KOTLIN_CLASS">Foo</info> {
    <info descr="null" textAttributesKey="KOTLIN_KEYWORD">constructor</info>(<warning descr="[UNUSED_PARAMETER] Parameter 'i' is never used" textAttributesKey="NOT_USED_ELEMENT_ATTRIBUTES"><info descr="null" textAttributesKey="KOTLIN_PARAMETER">i</info></warning>: <info descr="null" textAttributesKey="KOTLIN_CLASS">Int</info>)
}

class <info descr="null" textAttributesKey="KOTLIN_CLASS">Bar</info> : <info descr="null" textAttributesKey="KOTLIN_CLASS">Foo</info> {
    <error descr="[EXPLICIT_DELEGATION_CALL_REQUIRED] Explicit 'this' or 'super' call is required. There is no constructor in superclass that can be called without arguments" textAttributesKey="ERRORS_ATTRIBUTES"><info descr="null" textAttributesKey="KOTLIN_KEYWORD">constructor</info>(s: String)</error>
}

class <info descr="null" textAttributesKey="KOTLIN_CLASS">F</info>(<warning descr="[UNUSED_PARAMETER] Parameter 'foo' is never used" textAttributesKey="NOT_USED_ELEMENT_ATTRIBUTES"><info descr="null" textAttributesKey="KOTLIN_PARAMETER">foo</info></warning>: <info descr="null" textAttributesKey="KOTLIN_CLASS">String</info>) {
    <error descr="[PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED] Primary constructor call expected" textAttributesKey="ERRORS_ATTRIBUTES"><info descr="null" textAttributesKey="KOTLIN_KEYWORD">constructor</info>()</error> {}
}

<info descr="null" textAttributesKey="KOTLIN_BUILTIN_ANNOTATION">enum</info> class <info descr="null" textAttributesKey="KOTLIN_ENUM">E</info>(val <info descr="null" textAttributesKey="KOTLIN_INSTANCE_PROPERTY">a</info>: <info descr="null" textAttributesKey="KOTLIN_CLASS">String</info>) {
    <info descr="null" textAttributesKey="KOTLIN_ENUM_ENTRY">A</info>;
    <error descr="[PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED] Primary constructor call expected" textAttributesKey="ERRORS_ATTRIBUTES"><info descr="null" textAttributesKey="KOTLIN_KEYWORD">constructor</info>()</error>
}
