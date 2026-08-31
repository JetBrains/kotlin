// ISSUE: KT-87563
// MUTE_LL_FIR: inconsistent resolution behavior, but it doesn't make sense to fix unless IDEA-393430 is fixed
// because it's SO on Java code that's reproducable without Kotlin.

// FILE: X1.java

import lombok.experimental.SuperBuilder;

@SuperBuilder
class X1 extends X2 {
    public String field1;
}

// FILE: X2.java

import lombok.experimental.SuperBuilder;

@SuperBuilder
class X2 extends X1 {
    public int field2;
}

// FILE: test.kt

fun test() {
    X1.builder().field1("str1").<!UNRESOLVED_REFERENCE!>field2<!>(1).build()
    X2.builder().field2(2).<!UNRESOLVED_REFERENCE!>field1<!>("str2").build()
}
