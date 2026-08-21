// DUMP_KT_IR

import lombok.EqualsAndHashCode

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
    assertEquals(true, s1 == s2)
    assertEquals(false, s1 == s3)
    assertEquals(true, s1.hashCode() == s2.hashCode())
    // megaName is excluded so two instances with the same name/age are equal
    assertEquals(true, Simple("Alice", 30) == Simple("Alice", 30))

    val we1 = WithExclude("a", "b1")
    val we2 = WithExclude("a", "b2")
    assertEquals(true, we1 == we2)
    assertEquals(true, we1.hashCode() == we2.hashCode())

    assertEquals(true, OnlyIncluded("yes", "no") == OnlyIncluded("yes", "different"))
    assertEquals(false, OnlyIncluded("yes", "no") == OnlyIncluded("no", "no"))

    // Check that generated `equals` and `hashCode` are used instead of default ones:
    // The last parameter should be excluded because it's marked with `@EqualsAndHashCode.Exclude`
    // If default implementaiton was used, the classes would be different.
    val d1 = DataClassWithExclude("Alice", 30, 'a')
    val d2 = DataClassWithExclude("Alice", 30, 'b')
    assertEquals(true, d1 == d2)
    assertEquals(true, d1.hashCode() == d2.hashCode())
    val d3 = DataClassWithExclude("Alice", 31, 'a')
    assertEquals(false, d1 == d3)

    val p1 = PlainDataClass("x", 1)
    val p2 = PlainDataClass("x", 1)
    assertEquals(true, p1 == p2)
    assertEquals(true, p1.hashCode() == p2.hashCode())

    assertEquals(true, SingletonObject == SingletonObject)
    assertEquals(true, ObjectWithProperties == ObjectWithProperties)
    // Calling hashCode must not throw.
    SingletonObject.hashCode()
    ObjectWithProperties.hashCode()

    assertEquals(true, WithNullable(null, 1) == WithNullable(null, 1))
    assertEquals(false, WithNullable("a", 1) == WithNullable(null, 1))
    // hashCode does not NPE on a null property
    WithNullable(null, 1).hashCode()

    // KT-88532: `optionalId = 0` and `optionalId = null` are not equal, so they must not share a hash.
    assertEquals(false, SingleNullableInt(0) == SingleNullableInt(null))
    assertEquals(true, SingleNullableInt(0).hashCode() != SingleNullableInt(null).hashCode())

    assertEquals(false, SingleNullableString("") == SingleNullableString(null))
    assertEquals(true, SingleNullableString("").hashCode() != SingleNullableString(null).hashCode())

    assertEquals(false, TwoNullableInts(0, 1) == TwoNullableInts(null, 1))
    assertEquals(true, TwoNullableInts(0, 1).hashCode() != TwoNullableInts(null, 1).hashCode())

    // Equal instances still agree, null properties included.
    assertEquals(true, SingleNullableInt(null) == SingleNullableInt(null))
    assertEquals(true, SingleNullableInt(null).hashCode() == SingleNullableInt(null).hashCode())
    assertEquals(true, TwoNullableInts(null, null).hashCode() == TwoNullableInts(null, null).hashCode())

    assertEquals(true, Empty() == Empty())
    // The accumulator Lombok starts every `hashCode` from, with nothing folded into it.
    assertEquals(1, Empty().hashCode())

    val cd1 = CallSuperDerived("x")
    val cd2 = CallSuperDerived("x")
    assertEquals(true, cd1 == cd2)
    assertEquals(true, cd1.hashCode() == cd2.hashCode())

    assertEquals(true, WithComputedProperties("X") == WithComputedProperties("X"))

    @EqualsAndHashCode
    class LocalClass(val x: Int)
    assertEquals(true, LocalClass(7) == LocalClass(7))
    assertEquals(false, LocalClass(7) == LocalClass(8))

    // The enum keeps the identity comparison it inherits from `java.lang.Enum`, KT-88507.
    assertEquals(true, Color.RED == Color.RED)
    assertEquals(false, Color.RED == Color.GREEN)
    assertEquals(true, Color.RED.hashCode() == Color.RED.hashCode())

    return "OK"
}
