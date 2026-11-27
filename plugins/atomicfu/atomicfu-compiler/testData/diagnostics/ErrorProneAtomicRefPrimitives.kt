// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-73508
// FULL_JDK
// RENDER_DIAGNOSTICS_FULL_TEXT

import kotlinx.atomicfu.*

fun test() {
    <!ATOMIC_LOCALS_ARE_FORBIDDEN!>val<!> a = atomic<Int>(127)
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>a.compareAndSet(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>127<!>, <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>128<!>)<!> // true
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>a.compareAndSet(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>128<!>, <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>7777<!>)<!> // false

    <!ATOMIC_LOCALS_ARE_FORBIDDEN!>val<!> aa: AtomicRef<Int>
    aa = a
}

typealias AtomicfuAtomicReference<T> = AtomicRef<T>

fun testTypealiased() {
    <!ATOMIC_LOCALS_ARE_FORBIDDEN!>val<!> aa: AtomicfuAtomicReference<Int>
    aa = atomic<Int>(127)
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>aa.compareAndSet(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>127<!>, <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>128<!>)<!>
}

fun testArray(<!ATOMIC_VALUE_PARAMETERS_ARE_FORBIDDEN!>a<!>: AtomicArray<Int>) {
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>a[0].compareAndSet(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>1<!>, <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>2<!>)<!> // A call on `AtomicRef<Int?>`
}

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, integerLiteral, localProperty, nullableType, propertyDeclaration,
typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter */
