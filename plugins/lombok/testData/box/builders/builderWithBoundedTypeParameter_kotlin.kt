// IGNORE_BACKEND_K2: ANY

import lombok.Builder
import kotlin.test.assertEquals

@Builder
class BoundedTypeParameter<T : CharSequence>(val value: T)

// A bound that mentions the type parameter it bounds. The builder class copies the entity's type parameters,
// and a copy whose bound still points at the original owner's parameter is out of scope everywhere it is used.
@Builder
class SelfReferentialBound<T : Comparable<T>>(val value: T)

// A bound that mentions a type parameter declared after the one it bounds.
@Builder
class ForwardReferentialBound<A : Comparable<B>, B>(val first: A, val second: B)

class Key : Comparable<String> {
    override fun compareTo(other: String): Int = 0
}

class Wrapped<T>(val item: T)

class GenericWrapper {
    var wrapCount: Int = 0

    @Builder(builderClassName = "WrapWCBBuilder", builderMethodName = "wrapWCBBuilder")
    fun <T : Comparable<T>> wrapWithComparableBound(item: T): Wrapped<T> {
        wrapCount++
        return Wrapped(item)
    }
}

fun box(): String {
    assertEquals(1, SelfReferentialBound.builder<<!UPPER_BOUND_VIOLATED!>Int<!>>().value(1).build().value)
    assertEquals("second", ForwardReferentialBound.builder<<!UPPER_BOUND_VIOLATED!>Key<!>, String>().first(Key()).second("second").build().second)

    val wrapper = GenericWrapper()
    assertEquals(1, wrapper.wrapWCBBuilder<<!UPPER_BOUND_VIOLATED!>Int<!>>().item(1).build().item)
    assertEquals(1, wrapper.wrapCount)

    val test = BoundedTypeParameter.builder<String>().value("OK").build()
    return test.value
}
