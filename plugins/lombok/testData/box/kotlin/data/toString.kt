// ISSUE: KT-89188
// DUMP_KT_IR

import lombok.Data
import lombok.ToString
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Data
class Simple(val name: String, val age: Int)

@Data
class WithExclude(val a: String, @ToString.Exclude val b: String)

@Data
class WithIncludeCustomName(@ToString.Include(name = "myName") val x: String, val y: Int)

@Data
class WithExistingToString(val x: Int) {
    override fun toString(): String = "custom"
}

@Data
class WithExistingNonConflictingToString(val x: Int) {
    fun toString(p: Boolean): String = if (p) x.toString() else ""
}

@Data
class WithComputedProperties {
    val computedProp: String get() = "computed"
}

// KT-88410: a skipped property must not leave a separator behind
@Data
class WithComputedPropertyFirst {
    val computedProp: Int get() = 1
    val b = 2
}

@Data
class WithComputedPropertyInTheMiddle {
    val a = 1
    val computedProp: Int get() = 2
    val c = 3
}

@Data
class CallSuperWithComputedPropertyFirst : CallSuperBase(10) {
    val computedProp: Int get() = 1
    val ownProp = 2
}

@Data
class WithImplicitReturnTypeProperty {
    val implicitReturnTypeProp = "implicit return type"
}

@Data
class WithBackingFieldAndGetter {
    val x: Int = 42
        get() = field
}

@Data
class WithNonConflictingExtensionFunction(val a: Int) {
    fun WithNonConflictingExtensionFunction.toString(): String = "Ext"
}

@Data
data class DataClassDefault(val name: String, val age: Int)

<!ANNOTATION_HAS_NO_EFFECT!>@Data<!>
object EmptyObject

<!ANNOTATION_HAS_NO_EFFECT!>@Data<!>
object ObjectWithProperties {
    val version = "2.0"
    val label = "release"
}

@Data
class WithNonConflictingContextualFunction(val b: String) {
    context(p: WithNonConflictingContextualFunction)
    fun toString(): String = "Contex"
}

@Data
class WithRank(
    @ToString.Include(rank = -1) val lowRank: String,
    val defaultRank: String,
    @ToString.Include(rank = 1) val highRank: String,
)

@Data
class WithSameRank(
    @ToString.Include(rank = 2) val a: String,
    @ToString.Include(rank = 2) val b: String,
    val c: String,
)

@Data
open class CallSuperBase(val baseProp: Int)

@Data
class CallSuperDerived(val ownProp: String) : CallSuperBase(10)

// An explicit `@ToString` takes precedence over the one implied by `@Data`.
@Data
@ToString(callSuper = true)
class DataWithExplicitToString(val own: Int) : CallSuperBase(10)

// ISSUE: KT-88419
@Data
class WithArrays {
    val objectArray = arrayOf("a", "b")
    val nestedArray = arrayOf(arrayOf("a"), arrayOf("b"))
    val intArray = intArrayOf(1, 2)
    val charArray = charArrayOf('x', 'y')
    val nullArray: Array<String>? = null
}

// A `$`-prefixed name is generated or internal by convention, so Lombok leaves such a property out unless it is
// explicitly opted in with `@ToString.Include`, KT-88636.
@Data
class WithDollarPrefixedProperties(
    val regular: String,
    val `$excludedByDefault`: String,
    @ToString.Include val `$explicitlyIncluded`: String,
)

fun box(): String {
    assertEquals("Simple(name=Alice, age=30)", Simple("Alice", 30).toString())
    assertEquals("WithExclude(a=hello)", WithExclude("hello", "world").toString())
    assertEquals("WithIncludeCustomName(myName=foo, y=42)", WithIncludeCustomName("foo", 42).toString())
    assertEquals("custom", WithExistingToString(5).toString())
    assertEquals("WithExistingNonConflictingToString(x=5)", WithExistingNonConflictingToString(5).toString())
    assertEquals("WithComputedProperties()", WithComputedProperties().toString())
    assertEquals("WithComputedPropertyFirst(b=2)", WithComputedPropertyFirst().toString())
    assertEquals("WithComputedPropertyInTheMiddle(a=1, c=3)", WithComputedPropertyInTheMiddle().toString())
    assertEquals(
        // With @ToString(callSuper = true) we would have also super=CallSuperBase(baseProp=10)
        "CallSuperWithComputedPropertyFirst(ownProp=2)",
        CallSuperWithComputedPropertyFirst().toString()
    )
    assertEquals("WithImplicitReturnTypeProperty(implicitReturnTypeProp=implicit return type)", WithImplicitReturnTypeProperty().toString())
    assertEquals("WithBackingFieldAndGetter(x=42)", WithBackingFieldAndGetter().toString())
    assertEquals("WithNonConflictingExtensionFunction(a=6)", WithNonConflictingExtensionFunction(6).toString())
    assertEquals("WithNonConflictingContextualFunction(b=str)", WithNonConflictingContextualFunction("str").toString())

    assertEquals("DataClassDefault(name=Alice, age=30)", DataClassDefault("Alice", 30).toString())

    assertEquals("EmptyObject()", EmptyObject.toString())
    assertEquals("ObjectWithProperties(version=2.0, label=release)", ObjectWithProperties.toString())

    @Data
    class LocalClass(val prop: String)
    assertEquals("LocalClass(prop=TestLocalClass)", LocalClass("TestLocalClass").toString())

    assertEquals("WithRank(highRank=hi, defaultRank=mid, lowRank=lo)", WithRank(lowRank = "lo", defaultRank = "mid", highRank = "hi").toString())
    assertEquals("WithSameRank(a=1, b=2, c=3)", WithSameRank(a = "1", b = "2", c = "3").toString())

    assertEquals("CallSuperBase(baseProp=10)", CallSuperBase(10).toString())
    // With @ToString(callSuper = true) we would have also super=CallSuperBase(baseProp=10)
    assertEquals("CallSuperDerived(ownProp=hello)", CallSuperDerived("hello").toString())
    // An explicit `callSuper = true` is honored even against `Any`, whose `toString` is the bare identity hash
    // `Object.toString` renders - "pretty much meaningless", as `@ToString`'s own javadoc puts it, but asked
    // for, and Lombok has no error for it the way `@EqualsAndHashCode` does. The hash rules out `assertEquals`.
    val withExplicitToString = DataWithExplicitToString(5).toString()
    assertEquals("DataWithExplicitToString(super=CallSuperBase(baseProp=10), own=5)", withExplicitToString)

    assertEquals(
        "WithArrays(objectArray=[a, b], nestedArray=[[a], [b]], intArray=[1, 2], charArray=[x, y], nullArray=null)",
        WithArrays().toString()
    )

    assertEquals(
        "WithDollarPrefixedProperties(regular=r, ${'$'}explicitlyIncluded=i)",
        WithDollarPrefixedProperties("r", "e", "i").toString()
    )

    return "OK"
}
