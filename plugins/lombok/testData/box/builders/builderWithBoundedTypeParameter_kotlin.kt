// ISSUES: KT-90824

import lombok.Builder
import kotlin.test.assertEquals

@Builder
class BoundedTypeParameter<T : CharSequence>(val value: T)

// Force 'BoundedTypeParameterBuilder' symbol to be generated during SUPER_TYPES phase.
// This forces the type parameter bounds to be resolved by the Lombok plugin (KT-90824).
class BoundedTypeParameterBuilderSubclass : BoundedTypeParameter.BoundedTypeParameterBuilder<String>()

// A bound that mentions the type parameter it bounds. The builder class copies the entity's type parameters,
// and a copy whose bound still points at the original owner's parameter is out of scope everywhere it is used.
@Builder
class SelfReferentialBound<T : Comparable<T>>(val value: T)

// Force 'SelfReferentialBoundBuilder' symbol to be generated during SUPER_TYPES phase.
// This forces the type parameter bounds to be resolved by the Lombok plugin (KT-90824).
class SelfReferentialBoundBuilderSubclass : SelfReferentialBound.SelfReferentialBoundBuilder<Int>()

// A bound that mentions a type parameter declared after the one it bounds.
@Builder
class ForwardReferentialBound<A : Comparable<B>, B>(val first: A, val second: B)

// Force 'ForwardReferentialBoundBuilder' symbol to be generated during SUPER_TYPES phase.
// This forces the type parameter bounds to be resolved by the Lombok plugin (KT-90824).
class ForwardReferentialBoundBuilderSubclass : ForwardReferentialBound.ForwardReferentialBoundBuilder<Key, String>()

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
    assertEquals("value", BoundedTypeParameter.builder<String>().value("value").build().value)
    assertEquals("value", BoundedTypeParameterBuilderSubclass().value("value").build().value)
    assertEquals(1, SelfReferentialBound.builder<Int>().value(1).build().value)
    assertEquals(1, SelfReferentialBoundBuilderSubclass().value(1).build().value)
    assertEquals("second", ForwardReferentialBound.builder<Key, String>().first(Key()).second("second").build().second)
    assertEquals("second", ForwardReferentialBoundBuilderSubclass().first(Key()).second("second").build().second)

    val wrapper = GenericWrapper()
    assertEquals(1, wrapper.wrapWCBBuilder<Int>().item(1).build().item)
    assertEquals(1, wrapper.wrapCount)

    val test = BoundedTypeParameter.builder<String>().value("OK").build()
    return test.value
}
