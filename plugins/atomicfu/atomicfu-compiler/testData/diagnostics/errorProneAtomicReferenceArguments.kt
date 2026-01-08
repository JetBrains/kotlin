// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-82375
// FULL_JDK
// DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE
// WITH_STDLIB

// FILE: FooValueClass.kt

@JvmInline
value class Foo(val value: UInt)

// FILE: K.kt

import kotlinx.atomicfu.*

fun testTypealiasedKotlin(bar: Foo) {
    val foo = atomic<Any>(Foo(1u))
    foo.compareAndSet(<!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>bar<!>, <!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>Foo(2u)<!>)
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, asExpression, classDeclaration, classReference, flexibleType,
functionDeclaration, localProperty, nullableType, primaryConstructor, propertyDeclaration, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeParameter, unsignedLiteral, value */
