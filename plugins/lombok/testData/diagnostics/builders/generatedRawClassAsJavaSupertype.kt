// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-91174
// WITH_STDLIB
// FILE: test/Owner.kt
package test

import lombok.Builder

@Builder
class Owner<T>(val value: T)

// FILE: test/Child.java
package test;

public class Child extends Owner.OwnerBuilder {}

// FILE: test/main.kt
package test

fun asBuilder(child: Child): Owner.OwnerBuilder<*> = child
fun buildOwner(child: Child): Owner<*> = child.build()

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, functionDeclaration, javaType, nullableType, primaryConstructor,
propertyDeclaration, starProjection, typeParameter */
