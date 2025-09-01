// FIR_IDENTICAL
// ISSUE: KT-74687
// JDK_KIND: FULL_JDK_17

// FILE: RecordBuilder.java

import lombok.Builder;

@Builder
record RecordBuilder(String prop) {
    RecordBuilder(String prop, int prop2) {
        this(prop);
    }
}

// FILE: test.kt

fun box(): String {
    return RecordBuilder.builder().<!UNRESOLVED_REFERENCE!>prop<!>("OK").build().prop
}
