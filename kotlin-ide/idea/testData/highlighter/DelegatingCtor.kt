<info textAttributesKey="KOTLIN_BUILTIN_ANNOTATION">open</info> class <info textAttributesKey="KOTLIN_CLASS">Foo</info> {
    <info textAttributesKey="KOTLIN_KEYWORD">constructor</info>(<warning descr="[UNUSED_PARAMETER] Parameter 'i' is never used" textAttributesKey="NOT_USED_ELEMENT_ATTRIBUTES"><info textAttributesKey="KOTLIN_PARAMETER">i</info></warning>: <info textAttributesKey="KOTLIN_CLASS">Int</info>)
}

class <info textAttributesKey="KOTLIN_CLASS">Bar</info> : <info textAttributesKey="KOTLIN_CLASS">Foo</info> {
    <error descr="[EXPLICIT_DELEGATION_CALL_REQUIRED] Explicit 'this' or 'super' call is required. There is no constructor in superclass that can be called without arguments" textAttributesKey="ERRORS_ATTRIBUTES"><info textAttributesKey="KOTLIN_KEYWORD">constructor</info>(s: String)</error>
}

class <info textAttributesKey="KOTLIN_CLASS">F</info>(<warning descr="[UNUSED_PARAMETER] Parameter 'foo' is never used" textAttributesKey="NOT_USED_ELEMENT_ATTRIBUTES"><info textAttributesKey="KOTLIN_PARAMETER">foo</info></warning>: <info textAttributesKey="KOTLIN_CLASS">String</info>) {
    <error descr="[PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED] Primary constructor call expected" textAttributesKey="ERRORS_ATTRIBUTES"><info textAttributesKey="KOTLIN_KEYWORD">constructor</info>()</error> {}
}

<info textAttributesKey="KOTLIN_BUILTIN_ANNOTATION">enum</info> class <info textAttributesKey="KOTLIN_ENUM">E</info>(val <info textAttributesKey="KOTLIN_INSTANCE_PROPERTY">a</info>: <info textAttributesKey="KOTLIN_CLASS">String</info>) {
    <info textAttributesKey="KOTLIN_ENUM_ENTRY">A</info>;
    <warning descr="[PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED_IN_ENUM] Primary constructor call expected. It's going to be an error in 1.5." textAttributesKey="WARNING_ATTRIBUTES"><info textAttributesKey="KOTLIN_KEYWORD">constructor</info>()</warning>
}