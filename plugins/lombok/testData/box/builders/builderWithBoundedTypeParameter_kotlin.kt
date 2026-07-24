// WITH_STDLIB

import lombok.Builder

@Builder
class BoundedTypeParameter<T : CharSequence>(val value: T)

fun box(): String {
    val test = BoundedTypeParameter.builder<String>().value("OK").build()
    return test.value
}
