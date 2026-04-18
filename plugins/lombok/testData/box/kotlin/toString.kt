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

fun box(): String {
    Simple("Alice", 30).toString().let { if (it != "Simple(name=Alice, age=30)") return it }
    NoFieldNames(1, 2).toString().let { if (it != "NoFieldNames(1, 2)") return it }
    WithExclude("hello", "world").toString().let { if (it != "WithExclude(a=hello)") return it }
    WithIncludeCustomName("foo", 42).toString().let { if (it != "WithIncludeCustomName(myName=foo, y=42)") return it }
    OnlyIncluded("yes", "no").toString().let { if (it != "OnlyIncluded(included=yes)") return it }
    WithExistingToString(5).toString().let { if (it != "custom") return it }
    WithExistingNonConflictingToString(5).toString().let { if (it != "WithExistingNonConflictingToString(x=5)") return it }
    WithComputedProperties().toString().let { if (it != "WithComputedProperties()") return it }
    WithImplicitReturnTypeProperty().toString().let { if (it != "WithImplicitReturnTypeProperty(implicitReturnTypeProp=implicit return type)") return it }

    @ToString()
    class LocalClass(val prop: String)
    LocalClass("TestLocalClass").toString().let { if (it != "LocalClass(prop=TestLocalClass)") return it }

    return "OK"
}
