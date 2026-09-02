// WITH_STDLIB

import lombok.Builder

@Builder
class BoundedTypeParameter<T : CharSequence>(val value: T)

fun test() {
    BoundedTypeParameter.builder<<!UPPER_BOUND_VIOLATED!>Int<!>>().value(1).build()
}
