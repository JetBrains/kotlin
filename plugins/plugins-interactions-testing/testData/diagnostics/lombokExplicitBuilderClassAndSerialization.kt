// ENABLE_SERIALIZATION
// ENABLE_LOMBOK
// WITH_STDLIB
// ISSUE: KT-90516

import lombok.Builder

<!ANNOTATION_IS_NOT_SUPPORTED!>@Builder<!>
class Dog(val name: String) {
    // The setters are built from the primary constructor parameter types while the supertypes of this builder
    // class are still being resolved
    class DogBuilder
}

fun box(): String = if (Dog.<!UNRESOLVED_REFERENCE!>builder<!>().name("Rex").build().name == "Rex") "OK" else "Fail"
