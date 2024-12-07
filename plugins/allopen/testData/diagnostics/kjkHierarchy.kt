// FIR_IDENTICAL
// ISSUE: KT-68557

// FILE: D.kt
class D : C()

// FILE: C.java
public class C extends B {}

// FILE: B.kt
open class B : A()

// FILE: A.kt
open class A
