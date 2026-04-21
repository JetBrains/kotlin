import lombok.ToString

@ToString
class Simple(val name: String, val age: Int)

@ToString(includeFieldNames = false)
class NoFieldNames(val x: Int, val y: Int)

@ToString
class WithExclude(val a: String, @ToString.Exclude val b: String)

@ToString
class WithIncludeCustomName(@ToString.Include(name = "myName") val x: String, val y: Int)

@ToString(onlyExplicitlyIncluded = true)
class OnlyIncluded(@ToString.Include val included: String, val excluded: String)

@ToString
class WithExistingToString(val x: Int) {
    override fun toString(): String = "custom"
}

@ToString
class WithExistingNonConflictingToString(val x: Int) {
    fun toString(p: Boolean): String = if (p) x.toString() else ""
}

@ToString
class WithComputedProperties {
    val computedProp: String get() = "computed"
}

@ToString
class WithImplicitReturnTypeProperty {
    val implicitReturnTypeProp = "implicit return type"
}

@ToString
class WithNonConflictingExtensionFunction(val a: Int) {
    fun WithNonConflictingExtensionFunction.toString(): String = "Ext"
}

@ToString
class WithNonConflictingContextualFunction(val b: String) {
    context(p: WithNonConflictingContextualFunction)
    fun toString(): String = "Contex"
}

@ToString
open class CallSuperBase(val baseProp: Int)

@ToString(callSuper = true)
class CallSuperDerived(val ownProp: String) : CallSuperBase(10)

@ToString(callSuper = true)
class CallSuperWithOnlyAnyParent(val x: Int)

fun box(): String {
    assertEquals("Simple(name=Alice, age=30)", Simple("Alice", 30).toString())
    assertEquals("NoFieldNames(1, 2)", NoFieldNames(1, 2).toString())
    assertEquals("WithExclude(a=hello)", WithExclude("hello", "world").toString())
    assertEquals("WithIncludeCustomName(myName=foo, y=42)", WithIncludeCustomName("foo", 42).toString())
    assertEquals("OnlyIncluded(included=yes)", OnlyIncluded("yes", "no").toString())
    assertEquals("custom", WithExistingToString(5).toString())
    assertEquals("WithExistingNonConflictingToString(x=5)", WithExistingNonConflictingToString(5).toString())
    assertEquals("WithComputedProperties()", WithComputedProperties().toString())
    assertEquals("WithImplicitReturnTypeProperty(implicitReturnTypeProp=implicit return type)", WithImplicitReturnTypeProperty().toString())
    assertEquals("WithNonConflictingExtensionFunction(a=6)", WithNonConflictingExtensionFunction(6).toString())
    assertEquals("WithNonConflictingContextualFunction(b=str)", WithNonConflictingContextualFunction("str").toString())

    @ToString()
    class LocalClass(val prop: String)
    assertEquals("LocalClass(prop=TestLocalClass)", LocalClass("TestLocalClass").toString())

    assertEquals("CallSuperBase(baseProp=10)", CallSuperBase(10).toString())
    assertEquals("CallSuperDerived(super=CallSuperBase(baseProp=10), ownProp=hello)", CallSuperDerived("hello").toString())
    assertEquals("CallSuperWithOnlyAnyParent(x=5)", CallSuperWithOnlyAnyParent(5).toString())

    return "OK"
}
