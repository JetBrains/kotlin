// DUMP_KT_IR

import lombok.EqualsAndHashCode
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@EqualsAndHashCode
class Simple(val name: String, val age: Int) {
    @EqualsAndHashCode.Exclude
    val megaName: String = "Super $name"
}

@EqualsAndHashCode
class WithExclude(val a: String, @EqualsAndHashCode.Exclude val b: String)

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
class OnlyIncluded(@EqualsAndHashCode.Include val included: String, val excluded: String)

@EqualsAndHashCode
data class DataClassWithExclude(
    val name: String,
    val age: Int,
    @EqualsAndHashCode.Exclude
    val customProp: Char,
)

@EqualsAndHashCode
data class PlainDataClass(val a: String, val b: Int)

// Nothing is generated for an object either: it is a single instance, so the identity comparison it already has
// is exactly what a generated `equals` would amount to, KT-88507.
<!ANNOTATION_HAS_NO_EFFECT!>@EqualsAndHashCode<!>
object SingletonObject

<!ANNOTATION_HAS_NO_EFFECT!>@EqualsAndHashCode<!>
object ObjectWithProperties {
    val version = "2.0"
    val label = "release"
}

@EqualsAndHashCode
class WithNullable(val a: String?, val b: Int)

// A null property must not hash to what a present one can hash to: `0.hashCode()` and `"".hashCode()` are both
// 0, so a null field hashing to 0 collided with them and made non-equal instances share a hash, KT-88532.
@EqualsAndHashCode
class SingleNullableInt(val optionalId: Int?)

@EqualsAndHashCode
class SingleNullableString(val optional: String?)

// The same, on the path that folds several properties into `result`, not the single-property shortcut.
@EqualsAndHashCode
class TwoNullableInts(val first: Int?, val second: Int?)

// An array property is compared and hashed by content, not by identity: Lombok routes one through
// `java.util.Arrays`, deeply for an object array and shallowly for a primitive one, KT-88656.
@EqualsAndHashCode
class WithObjectArray(val array: Array<String>)

@EqualsAndHashCode
class WithPrimitiveArray(val array: IntArray)

@EqualsAndHashCode
class WithNestedArray(val array: Array<Array<String>>)

@EqualsAndHashCode
class WithNullableArray(val array: Array<String>?)

// A `$`-prefixed name is generated or internal by convention, so Lombok leaves such a property out of the
// class's identity unless it is explicitly opted in with `@EqualsAndHashCode.Include`, KT-88636.
@EqualsAndHashCode
class WithDollarPrefixedProperties(
    val regular: String,
    val `$excludedByDefault`: String,
    @EqualsAndHashCode.Include val `$explicitlyIncluded`: String,
)

@EqualsAndHashCode
class Empty

@EqualsAndHashCode
open class CallSuperBase(val baseProp: Int)

@EqualsAndHashCode(callSuper = true)
class CallSuperDerived(val ownProp: String) : CallSuperBase(10)

@EqualsAndHashCode
class WithComputedProperties(val real: String) {
    val computedProp: String get() = "computed"
}

// Nothing is generated: `java.lang.Enum` declares `equals`/`hashCode` final, so a generated one used to fail
// verification and the class didn't even load, KT-88507. `ANNOTATION_HAS_NO_EFFECT` is reported instead.
<!ANNOTATION_HAS_NO_EFFECT!>@EqualsAndHashCode<!>
enum class Color(val hex: String) {
    RED("#FF0000"),
    GREEN("#00FF00")
}

fun box(): String {
    val s1 = Simple("Alice", 30)
    val s2 = Simple("Alice", 30)
    val s3 = Simple("Bob", 30)
    assertEquals(s1, s2)
    assertNotEquals(s1, s3)
    assertEquals(s1.hashCode(), s2.hashCode())
    // megaName is excluded so two instances with the same name/age are equal
    assertEquals(Simple("Alice", 30), Simple("Alice", 30))

    val we1 = WithExclude("a", "b1")
    val we2 = WithExclude("a", "b2")
    assertEquals(we1, we2)
    assertEquals(we1.hashCode(), we2.hashCode())

    assertEquals(OnlyIncluded("yes", "no"), OnlyIncluded("yes", "different"))
    assertNotEquals(OnlyIncluded("yes", "no"), OnlyIncluded("no", "no"))

    // Check that generated `equals` and `hashCode` are used instead of default ones:
    // The last parameter should be excluded because it's marked with `@EqualsAndHashCode.Exclude`
    // If default implementaiton was used, the classes would be different.
    val d1 = DataClassWithExclude("Alice", 30, 'a')
    val d2 = DataClassWithExclude("Alice", 30, 'b')
    assertEquals(d1, d2)
    assertEquals(d1.hashCode(), d2.hashCode())
    val d3 = DataClassWithExclude("Alice", 31, 'a')
    assertNotEquals(d1, d3)

    val p1 = PlainDataClass("x", 1)
    val p2 = PlainDataClass("x", 1)
    assertEquals(p1, p2)
    assertEquals(p1.hashCode(), p2.hashCode())

    assertEquals(SingletonObject, SingletonObject)
    assertEquals(ObjectWithProperties, ObjectWithProperties)
    // Calling hashCode must not throw.
    SingletonObject.hashCode()
    ObjectWithProperties.hashCode()

    assertEquals(WithNullable(null, 1), WithNullable(null, 1))
    assertNotEquals(WithNullable("a", 1), WithNullable(null, 1))
    // hashCode does not NPE on a null property
    WithNullable(null, 1).hashCode()

    // KT-88532: `optionalId = 0` and `optionalId = null` are not equal, so they must not share a hash.
    assertNotEquals(SingleNullableInt(0), SingleNullableInt(null))
    assertNotEquals(SingleNullableInt(0).hashCode(), SingleNullableInt(null).hashCode())

    assertNotEquals(SingleNullableString(""), SingleNullableString(null))
    assertNotEquals(SingleNullableString("").hashCode(), SingleNullableString(null).hashCode())

    assertNotEquals(TwoNullableInts(0, 1), TwoNullableInts(null, 1))
    assertNotEquals(TwoNullableInts(0, 1).hashCode(), TwoNullableInts(null, 1).hashCode())

    // Equal instances still agree, null properties included.
    assertEquals(SingleNullableInt(null), SingleNullableInt(null))
    assertEquals(SingleNullableInt(null).hashCode(), SingleNullableInt(null).hashCode())
    assertEquals(TwoNullableInts(null, null).hashCode(), TwoNullableInts(null, null).hashCode())

    // KT-88656: equal contents in distinct array instances must compare equal and hash alike.
    assertEquals(WithObjectArray(arrayOf("a", "b")), WithObjectArray(arrayOf("a", "b")))
    assertEquals(WithObjectArray(arrayOf("a", "b")).hashCode(), WithObjectArray(arrayOf("a", "b")).hashCode())
    assertNotEquals(WithObjectArray(arrayOf("a", "b")), WithObjectArray(arrayOf("a", "c")))

    assertEquals(WithPrimitiveArray(intArrayOf(1, 2)), WithPrimitiveArray(intArrayOf(1, 2)))
    assertEquals(WithPrimitiveArray(intArrayOf(1, 2)).hashCode(), WithPrimitiveArray(intArrayOf(1, 2)).hashCode())
    assertNotEquals(WithPrimitiveArray(intArrayOf(1, 2)), WithPrimitiveArray(intArrayOf(1, 3)))

    // A one-dimensional object array is already compared deeply, so a nested one needs nothing extra.
    assertEquals(WithNestedArray(arrayOf(arrayOf("a"))), WithNestedArray(arrayOf(arrayOf("a"))))
    assertEquals(WithNestedArray(arrayOf(arrayOf("a"))).hashCode(), WithNestedArray(arrayOf(arrayOf("a"))).hashCode())
    assertNotEquals(WithNestedArray(arrayOf(arrayOf("a"))), WithNestedArray(arrayOf(arrayOf("b"))))

    // `java.util.Arrays` accepts null itself, so a null array needs no separate guard.
    assertEquals(WithNullableArray(null), WithNullableArray(null))
    assertEquals(WithNullableArray(null).hashCode(), WithNullableArray(null).hashCode())
    assertNotEquals(WithNullableArray(null), WithNullableArray(arrayOf("a")))

    // KT-88636: only `$excludedByDefault` differs, so the instances stay equal; `$explicitlyIncluded` counts.
    assertEquals(WithDollarPrefixedProperties("r", "a", "i"), WithDollarPrefixedProperties("r", "b", "i"))
    assertEquals(
        WithDollarPrefixedProperties("r", "a", "i").hashCode(),
        WithDollarPrefixedProperties("r", "b", "i").hashCode()
    )
    assertNotEquals(WithDollarPrefixedProperties("r", "a", "i"), WithDollarPrefixedProperties("r", "a", "j"))
    assertNotEquals(WithDollarPrefixedProperties("r", "a", "i"), WithDollarPrefixedProperties("s", "a", "i"))

    assertEquals(Empty(), Empty())
    // The accumulator Lombok starts every `hashCode` from, with nothing folded into it.
    assertEquals(1, Empty().hashCode())

    val cd1 = CallSuperDerived("x")
    val cd2 = CallSuperDerived("x")
    assertEquals(cd1, cd2)
    assertEquals(cd1.hashCode(), cd2.hashCode())

    assertEquals(WithComputedProperties("X"), WithComputedProperties("X"))

    @EqualsAndHashCode
    class LocalClass(val x: Int)
    assertEquals(LocalClass(7), LocalClass(7))
    assertNotEquals(LocalClass(7), LocalClass(8))

    // The enum keeps the identity comparison it inherits from `java.lang.Enum`, KT-88507.
    assertEquals(Color.RED, Color.RED)
    assertNotEquals(Color.RED, Color.GREEN)
    assertEquals(Color.RED.hashCode(), Color.RED.hashCode())

    return "OK"
}
