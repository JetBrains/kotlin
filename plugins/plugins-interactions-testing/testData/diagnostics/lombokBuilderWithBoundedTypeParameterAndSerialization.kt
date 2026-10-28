// ISSUE: KT-90516
// ENABLE_SERIALIZATION
// ENABLE_LOMBOK
// WITH_STDLIB

import lombok.Builder

// The builder class copies the type parameter of `Box` together with its bound, which is not resolved yet when the
// builder class is generated while the supertypes are still being resolved
<!ANNOTATION_IS_NOT_SUPPORTED!>@Builder<!>
class Box<T : Comparable<T>>(val value: T)

fun box(): String = Box.<!UNRESOLVED_REFERENCE!>builder<!><String>().value("OK").build().value
