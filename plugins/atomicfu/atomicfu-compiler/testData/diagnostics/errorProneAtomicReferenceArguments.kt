// RUN_PIPELINE_TILL: FRONTEND
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
    <!ATOMIC_LOCALS_ARE_FORBIDDEN!>val<!> foo = atomic<Any>(Foo(1u))
    foo.compareAndSet(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>bar<!>, <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>Foo(2u)<!>)
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, asExpression, classDeclaration, classReference, flexibleType,
functionDeclaration, localProperty, nullableType, primaryConstructor, propertyDeclaration, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeParameter, unsignedLiteral, value */
