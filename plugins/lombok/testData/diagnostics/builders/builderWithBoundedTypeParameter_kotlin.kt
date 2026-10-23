// WITH_STDLIB

import lombok.Builder

<!ANNOTATION_IS_NOT_SUPPORTED!>@Builder<!>
class BoundedTypeParameter<T : CharSequence>(val value: T)

fun test() {
    BoundedTypeParameter.<!UNRESOLVED_REFERENCE!>builder<!><Int>().value(1).build()
}
