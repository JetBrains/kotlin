// LL_FIR_DIVERGENCE
// Return type mismatch due to KT-91174
// LL_FIR_DIVERGENCE
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

fun asBuilder(child: Child): Owner.OwnerBuilder<String> = <!RETURN_TYPE_MISMATCH("Owner.OwnerBuilder<String>; Child")!>child<!>
fun buildOwner(child: Child): Owner<String> = <!RETURN_TYPE_MISMATCH("Owner<String>; (Owner<Any!>..Owner<*>)")!>child.build()<!>
