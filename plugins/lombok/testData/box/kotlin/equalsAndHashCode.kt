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

// KT-89189: `canEqual` must make `equals` symmetric across a hierarchy - a subtype with more state must not
// compare equal to an instance of its looser supertype, in either direction.
@EqualsAndHashCode
open class CanEqualGrandparent(val a: Int)

@EqualsAndHashCode(callSuper = true)
open class CanEqualParent(a: Int, val b: Int) : CanEqualGrandparent(a)

@EqualsAndHashCode(callSuper = true)
class CanEqualChild(a: Int, b: Int, val c: Int) : CanEqualParent(a, b)

// A user-declared `canEqual` matching the generated shape must not be duplicated - generating one on top would
// clash with it on the JVM (CONFLICTING_JVM_DECLARATIONS) - but `equals` must still call it.
@EqualsAndHashCode
open class UserDeclaredCanEqual(val a: Int) {
    open fun canEqual(other: Any?): Boolean = other is UserDeclaredCanEqual
}

// An unrelated overload sharing the name must never be mistaken for the generated `canEqual`, or the generated
// `equals` body ends up calling the wrong overload and throws a ClassCastException at runtime. `open`, so
// `canEqual` is actually generated here (skipped only for a class that is both final and has no non-trivial
// superclass) and coexists with the unrelated overload, exercising the ambiguous-lookup fix.
@EqualsAndHashCode
open class UnrelatedCanEqualOverload(val a: Int) {
    fun canEqual(x: Int): Boolean = x == a
}

// `Any` and `Any?` both erase to `canEqual(Ljava/lang/Object;)Z` on the JVM, so a non-null parameter must be
// recognized as matching the generated shape too, or a second `canEqual` is generated on top of this one.
@EqualsAndHashCode
open class NonNullCanEqual(val a: Int) {
    open fun canEqual(other: Any): Boolean = other is NonNullCanEqual
}

// The same erasure gap reached through an unbounded type parameter rather than `Any` directly: `T` erases to
// `java.lang.Object` just the same.
@EqualsAndHashCode
open class TypeParameterCanEqual<T>(val a: Int) {
    open fun canEqual(other: T): Boolean = other != null
}

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

    // KT-89189: a `CallSuperBase` and a `CallSuperDerived` sharing the same `baseProp` must not compare equal
    // in either direction, unlike the bug where the looser supertype's `equals` accepted the stricter subtype.
    assertEquals(CallSuperBase(10), CallSuperBase(10))
    val callSuperBase = CallSuperBase(10)
    val callSuperDerived = CallSuperDerived("x")
    assertNotEquals(callSuperBase, callSuperDerived)
    assertNotEquals(callSuperDerived, callSuperBase)

    // Same asymmetry check, one level deeper: the override lookup must walk the whole ancestor chain, not just
    // the immediate superclass, or one of the three classes below fails to even compile.
    val grandparent = CanEqualGrandparent(1)
    val parent = CanEqualParent(1, 2)
    val child = CanEqualChild(1, 2, 3)
    assertEquals(CanEqualGrandparent(1), CanEqualGrandparent(1))
    assertEquals(CanEqualParent(1, 2), CanEqualParent(1, 2))
    assertEquals(CanEqualChild(1, 2, 3), CanEqualChild(1, 2, 3))
    assertNotEquals(grandparent, parent)
    assertNotEquals(parent, grandparent)
    assertNotEquals(parent, child)
    assertNotEquals(child, parent)
    assertNotEquals(grandparent, child)
    assertNotEquals(child, grandparent)

    val userDeclared1 = UserDeclaredCanEqual(1)
    val userDeclared2 = UserDeclaredCanEqual(1)
    assertEquals(userDeclared1, userDeclared2)
    assertEquals(true, userDeclared1.canEqual(userDeclared2))

    assertEquals(UnrelatedCanEqualOverload(1), UnrelatedCanEqualOverload(1))
    assertNotEquals(UnrelatedCanEqualOverload(1), UnrelatedCanEqualOverload(2))
    assertEquals(true, UnrelatedCanEqualOverload(1).canEqual(1))
    assertEquals(false, UnrelatedCanEqualOverload(1).canEqual(2))

    val nonNull1 = NonNullCanEqual(1)
    val nonNull2 = NonNullCanEqual(1)
    assertEquals(nonNull1, nonNull2)
    assertEquals(true, nonNull1.canEqual(nonNull2))

    val typeParam1 = TypeParameterCanEqual<String>(1)
    val typeParam2 = TypeParameterCanEqual<String>(1)
    assertEquals(typeParam1, typeParam2)

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
