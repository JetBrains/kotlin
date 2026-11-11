// ISSUE: KT-91173
// DISABLE_WITH_PARSER: Psi

// FILE: test/Owner.java
package test;

import lombok.Builder;

@Builder
public class Owner {
    private final String name;
}

// FILE: test/Child.java
package test;

public class Child extends Owner.OwnerBuilder {}

// FILE: test/main.kt
package test

fun asBuilder(child: Child): Owner.OwnerBuilder = child
