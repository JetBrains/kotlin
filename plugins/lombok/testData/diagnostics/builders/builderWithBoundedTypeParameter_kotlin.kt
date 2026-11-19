// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// ISSUES: KT-90824

import lombok.Builder

@Builder
class BoundedTypeParameter<T : CharSequence>(val value: T)

// Force 'BoundedTypeParameterBuilder' symbol to be generated during SUPER_TYPES phase.
// This forces the type parameter bounds to be resolved by the Lombok plugin (KT-90824).
class BoundedTypeParameterBuilderSubclass : BoundedTypeParameter.BoundedTypeParameterBuilder<<!UPPER_BOUND_VIOLATED!>Int<!>>()

@Builder
class SelfBoundedTypeParameter<T : Comparable<T>>(val value: T)

// Force 'SelfBoundedTypeParameterBuilder' symbol to be generated during SUPER_TYPES phase.
// This forces the type parameter bounds to be resolved by the Lombok plugin (KT-90824).
class SelfBoundedTypeParameterBuilderSubclass : SelfBoundedTypeParameter.SelfBoundedTypeParameterBuilder<<!UPPER_BOUND_VIOLATED!>Any<!>>()

@Builder
class ForwardBoundedTypeParameter<A : Comparable<B>, B>(val first: A, val second: B)

// Force 'ForwardBoundedTypeParameterBuilder' symbol to be generated during SUPER_TYPES phase.
// This forces the type parameter bounds to be resolved by the Lombok plugin (KT-90824).
class ForwardBoundedTypeParameterBuilderSubclass : ForwardBoundedTypeParameter.ForwardBoundedTypeParameterBuilder<<!UPPER_BOUND_VIOLATED!>Int<!>, String>()

fun test() {
    BoundedTypeParameter.builder<<!UPPER_BOUND_VIOLATED!>Int<!>>().value(1).build()
    SelfBoundedTypeParameter.builder<<!UPPER_BOUND_VIOLATED!>Any<!>>().value(1).build()
    ForwardBoundedTypeParameter.builder<<!UPPER_BOUND_VIOLATED!>Int<!>, String>().first(1).build()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, primaryConstructor, propertyDeclaration,
typeConstraint, typeParameter */
