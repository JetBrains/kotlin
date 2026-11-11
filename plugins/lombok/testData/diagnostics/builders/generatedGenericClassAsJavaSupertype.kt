// ISSUE: KT-91174
// DISABLE_WITH_PARSER: Psi
// WITH_STDLIB
// FILE: test/Owner.kt
package test

import lombok.Builder

@Builder
class Owner<T>(val value: T)

// FILE: test/Child.java
package test;

public class Child extends Owner.OwnerBuilder<String> {}

// FILE: test/main.kt
package test

fun asBuilder(child: Child): Owner.OwnerBuilder<String> = child
fun buildOwner(child: Child): Owner<String> = child.build()
