// ISSUE: KT-90516
// FILE: CustomBuilder.kt

// Resolving this supertype generates `Box.BoxBuilder`, which copies the type parameter of `Box` together with its
// bound, not resolved yet at the SUPER_TYPES phase
class CustomBuilder : Box.<!UNRESOLVED_REFERENCE!>BoxBuilder<!><String>()

fun usage(): Box<String> = CustomBuilder().<!UNRESOLVED_REFERENCE!>value<!>("OK").build()

// FILE: Box.kt

import lombok.Builder

<!ANNOTATION_IS_NOT_SUPPORTED!>@Builder<!>
class Box<T : Comparable<T>>(val value: T)
