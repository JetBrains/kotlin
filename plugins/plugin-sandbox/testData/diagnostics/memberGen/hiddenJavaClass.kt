// FILE: JavaClass.java
@org.jetbrains.kotlin.plugin.sandbox.JavaClassWithHiddenNested
public class JavaClass {
}

// FILE: main.kt
class Nested {
    fun foo() = "OK"
}

class MyClass : JavaClass() {
    fun check(n: Nested): String = n.foo()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, javaType, stringLiteral */
