// ISSUE: KT-90562
// FIR_DUMP
// FILE: Dog.kt

package animals

class Dog(val name: String)

// FILE: test.kt

import animals.Dog as Hound
import lombok.Builder

// The builder class name is inferred from the return type: from `Hound` as written when the builder class is
// generated before the types are resolved, but from the `Dog` class it resolves to afterwards
class Kennel {
    companion object {
        <!ANNOTATION_IS_NOT_SUPPORTED!>@Builder<!>
        fun create(name: String): Hound = Hound(name)
    }
}

fun box(): String = if (Kennel.<!UNRESOLVED_REFERENCE!>builder<!>().name("Rex").build().name == "Rex") "OK" else "Fail"
