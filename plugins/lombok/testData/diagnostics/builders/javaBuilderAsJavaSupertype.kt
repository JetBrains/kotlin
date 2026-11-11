// ISSUE: KT-91173

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
