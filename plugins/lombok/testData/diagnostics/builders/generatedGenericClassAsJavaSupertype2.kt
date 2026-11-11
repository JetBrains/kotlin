// ISSUE: KT-91174
// DISABLE_WITH_PARSER: Psi
// WITH_STDLIB
// FILE: test/Owner.java
package test

import lombok.Builder;

@Builder
public class Owner<T> {
    private final T value;
}

// FILE: test/Child.java
package test;

public class Child extends Owner.OwnerBuilder<String> {}

// FILE: test/main.kt
package test

fun asBuilder(child: Child): Owner.OwnerBuilder<String> = child
fun buildOwner(child: Child): Owner<String> = child.build()
