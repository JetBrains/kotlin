// FIR_IDENTICAL
// WITH_STDLIB

// FILE: BoundedTypeParameter.java
import lombok.Builder;
import lombok.Getter;

@Builder
public class BoundedTypeParameter<T extends CharSequence> {
    T value;
}

// FILE: test.kt
fun test() {
    BoundedTypeParameter.builder<<!UPPER_BOUND_VIOLATED!>Int<!>>().value(1).build()
}