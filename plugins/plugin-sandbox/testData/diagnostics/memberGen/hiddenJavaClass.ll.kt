// LL_FIR_DIVERGENCE
// KT-77114: Source (& plugin) Java classes cannot be hidden in the Analysis API
// LL_FIR_DIVERGENCE
// FILE: JavaClass.java
@org.jetbrains.kotlin.plugin.sandbox.JavaClassWithHiddenNested
public class JavaClass {
}

// FILE: main.kt
class Nested {
    fun foo() = "OK"
}

class MyClass : JavaClass() {
    fun check(n: <!DEPRECATION_ERROR!>Nested<!>): String = n.<!UNRESOLVED_REFERENCE!>foo<!>()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, javaType, stringLiteral */
