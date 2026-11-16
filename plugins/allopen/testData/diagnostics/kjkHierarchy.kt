// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-68557

// FILE: D.kt
class D : C()

// FILE: C.java
public class C extends B {}

// FILE: B.kt
open class B : A()

// FILE: A.kt
open class A

/* GENERATED_FIR_TAGS: classDeclaration, javaType */
