// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-82375
// FULL_JDK
// DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE
// RENDER_DIAGNOSTICS_FULL_TEXT

// FILE: FooValueClass.kt

@JvmInline
value class Foo(val value: UInt)

// FILE: K.kt

import kotlinx.atomicfu.*

fun testKotlinArray(foo: AtomicArray<Any>, bar: Foo) {
    foo[0].compareAndSet(<!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>bar<!>, <!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>Foo(2u)<!>)
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, assignment, classReference, flexibleType, functionDeclaration,
integerLiteral, javaFunction, localProperty, propertyDeclaration */
