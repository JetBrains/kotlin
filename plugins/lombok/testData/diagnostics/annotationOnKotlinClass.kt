// FIR_IDENTICAL

// FILE: KotlinClass.kt

import lombok.Builder;

@Builder
class KotlinClass {
    companion object
}

// FILE: test.kt

fun test() {
    // Currently "Kotlin compiler ignores Lombok annotations if you use them in Kotlin code."
    val builder: <!UNRESOLVED_REFERENCE!>KotlinClassBuilder<!> = KotlinClass.<!UNRESOLVED_REFERENCE!>builder<!>()
    val builder2: <!UNRESOLVED_REFERENCE!>KotlinClassBuilder<!> = KotlinClass.Companion.<!UNRESOLVED_REFERENCE!>builder<!>()
}
