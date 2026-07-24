import lombok.EqualsAndHashCode

@EqualsAndHashCode
class Simple(val name: String, val age: Int) {
    @EqualsAndHashCode.Exclude
    val megaName: String = "Super $name"
}

@EqualsAndHashCode
class WithExclude(val a: String, @EqualsAndHashCode.Exclude val b: String)

@EqualsAndHashCode(exclude = ["b"])
class WithExcludeAttr(val a: String, val b: String)

@EqualsAndHashCode(of = ["a"])
class WithOf(val a: String, val b: String)

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

@EqualsAndHashCode
object SingletonObject

@EqualsAndHashCode
object ObjectWithProperties {
    val version = "2.0"
    val label = "release"
}

@EqualsAndHashCode
class WithNullable(val a: String?, val b: Int)

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

    val wea1 = WithExcludeAttr("a", "x")
    val wea2 = WithExcludeAttr("a", "y")
    assertEquals(true, wea1 == wea2)

    val wo1 = WithOf("same", "x")
    val wo2 = WithOf("same", "y")
    assertEquals(true, wo1 == wo2)
    val wo3 = WithOf("other", "x")
    assertEquals(false, wo1 == wo3)

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

    assertEquals(true, Empty() == Empty())
    assertEquals(0, Empty().hashCode())

    val cd1 = CallSuperDerived("x")
    val cd2 = CallSuperDerived("x")
    assertEquals(true, cd1 == cd2)
    assertEquals(true, cd1.hashCode() == cd2.hashCode())

    assertEquals(true, WithComputedProperties("X") == WithComputedProperties("X"))

    @EqualsAndHashCode
    class LocalClass(val x: Int)
    assertEquals(true, LocalClass(7) == LocalClass(7))
    assertEquals(false, LocalClass(7) == LocalClass(8))

    return "OK"
}
