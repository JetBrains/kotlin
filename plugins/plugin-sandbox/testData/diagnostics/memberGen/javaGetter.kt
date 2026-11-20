// RUN_PIPELINE_TILL: CODEGEN
// FIR_DUMP
// FILE: main.kt
fun test() {
    val foo = B().foo
}

// FILE: B.java
@org.jetbrains.kotlin.plugin.sandbox.JavaClassWithGeneratedGetter
public class B {
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaFunction, javaProperty, javaType, localProperty, propertyDeclaration */
