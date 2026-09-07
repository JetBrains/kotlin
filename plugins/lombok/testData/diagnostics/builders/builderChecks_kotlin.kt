// WITH_STDLIB

import lombok.AccessLevel
import lombok.Builder
import lombok.Singular

@Builder
class WithPlainInitializer(
    val name: String = <!BUILDER_WILL_IGNORE_INITIALIZING_EXPRESSION!>"default"<!>,
)

@Builder
class WithMissingDefaultInitializer(
    <!BUILDER_DEFAULT_REQUIRES_INITIALIZING_EXPRESSION!>@Builder.Default<!>
    val name: String,
)

@Builder
class Farm(
    <!CANNOT_SINGULARIZE_NAME!>@Singular<!>
    val sheep: List<String>,
)

@Builder
class Container(
    <!UNSUPPORTED_SINGULAR_TYPE!>@Singular("thing")<!>
    val things: Array<String>,
)

@Builder
class MixedDefaultAndSingular(
    <!BUILDER_DEFAULT_AND_SINGULAR_MIXED!>@Builder.Default<!>
    @Singular
    val items: List<String> = emptyList(),
)

// No diagnostics expected: builder-eligible properties used correctly.
@Builder
class CleanWidget(
    val id: Int,
    @Builder.Default
    val name: String = "default",
    @Singular
    val tags: List<String>,
)

// A builder field is a primary constructor value parameter, `val` or not, so a parameter declared without one
// is checked like any other: its own default value is still ignored by the builder, and `@Singular`
// (`@Target(FIELD, PARAMETER)`) still lands on it. `@Builder.Default` cannot - it is `@Target(FIELD)` and such
// a parameter has no field - which Kotlin's own annotation-target checker rejects before this checker runs.
@Builder
class ParametersWithoutVal(
    plain: String = <!BUILDER_WILL_IGNORE_INITIALIZING_EXPRESSION!>"default"<!>,
    <!CANNOT_SINGULARIZE_NAME!>@Singular<!> sheep: List<String>,
    <!UNSUPPORTED_SINGULAR_TYPE!>@Singular("thing")<!> things: Array<String>,
) {
    val summary: String = plain + sheep.size + things.size
}

// A property declared in the class body is not a builder field at all - `build()` has only the primary
// constructor to call - so nothing on one is reported: not its initializer, which the builder never ignores
// because it never sets the property, and not a `@Singular` or `@Builder.Default` that cannot reach a builder
// field in the first place. Every one of these would have been reported when the builder was built out of the
// class's properties.
@Builder
class BodyPropertiesAreNotBuilderFields(val id: Int, extra: String) {
    val derived: String = extra + id

    @Builder.Default // TODO: KT-89218 should be reported (annotation has no effect)
    val defaulted: Int = 1

    @Singular // TODO: KT-89218 should be reported (annotation has no effect)
    val sheep: List<String> = emptyList()

    @Singular("thing") // TODO: KT-89218 should be reported (annotation has no effect)
    val things: Array<String> = emptyArray()
}

// `toBuilder()` fills each builder field from the entity's property of that name, so a parameter the class
// declares no property for leaves it nothing to read. Lombok rejects the same shape - "cannot find symbol:
// variable <name>" on the annotation - for a method and a constructor builder alike, and offers
// `@Builder.ObtainVia` to point elsewhere, which isn't implemented here.
@Builder(toBuilder = true)
class ToBuilderWithoutProperty(val kept: Int, <!TO_BUILDER_CANNOT_OBTAIN!>lost: Int<!>) {
    val doubled: Int = lost * 2
}

// A property declared by hand is enough - it need not be a promoted one.
@Builder(toBuilder = true)
class ToBuilderWithBodyProperty(val kept: Int, mirrored: Int) {
    val mirrored: Int = mirrored
}

// The parameters of a constructor or a method builder can never be `val`, so the property has to be there
// already. `SecondaryMatching` has one, the others do not.
class ToBuilderOnConstructor(val str: String, val int: Int) {
    @Builder(toBuilder = true)
    constructor(str: String) : this(str, -1)

    @Builder(toBuilder = true, builderClassName = "OtherBuilder")
    constructor(<!TO_BUILDER_CANNOT_OBTAIN!>other: Long<!>) : this("empty", other.toInt())
}

class ToBuilderOnMemberMethod {
    var name: String = ""
    var seen: Int = 0

    @Builder(toBuilder = true)
    fun init(name: String, <!TO_BUILDER_CANNOT_OBTAIN!>extra: Int<!>) {
        this.name = name
        this.seen = extra
    }
}

// Without `toBuilder` nothing has to be obtainable: the mismatch only matters to the round-trip.
class NoToBuilderNeedsNothing(val kept: Int) {
    @Builder
    constructor(other: Long) : this(other.toInt())
}

// A class-level `@Builder` builds out of the primary constructor and nothing else, so a class that declares
// none leaves `build()` with nothing to call. Real Lombok never meets this shape - a Java class always has a
// constructor over all of its fields - and the Kotlin way out is to annotate a constructor instead.
<!BUILDER_REQUIRES_PRIMARY_CONSTRUCTOR!>@Builder<!>
class NoPrimaryConstructor {
    val y: Int

    constructor(x: Int) {
        y = x
    }
}

// The same class written the supported way: a constructor builder has parameters of its own to build from,
// so the missing primary constructor costs it nothing.
class NoPrimaryConstructorWithBuilderOnConstructor {
    val y: Int

    @Builder
    constructor(x: Int) {
        y = x
    }
}

// `@Builder` on a secondary constructor: only `@Singular` is checkable (`@Builder.Default`
// is `@Target(FIELD)`, so it can't land on a bare constructor parameter at all).
class ConstructorSingularCannotSingularize(val id: Int) {
    @Builder
    constructor(id: Int, <!CANNOT_SINGULARIZE_NAME!>@Singular<!> sheep: List<String>) : this(id)
}

class ConstructorParameterDefaultIgnored(val id: Int, val extra: Int) {
    @Builder
    constructor(id: Int = <!BUILDER_WILL_IGNORE_INITIALIZING_EXPRESSION!>0<!>) : this(id, -1)
}

@Builder(access = AccessLevel.PROTECTED)
class BuilderAccessLevelProtected(val id: Int)

// `@Builder` needs a constructor to call and a companion object to host the `builder()` factory, so it supports
// nothing but a non-local class. Lombok narrows the annotation the same way and reports "@Builder is only
// supported on classes, records, constructors, and methods." for the first three.
<!ANNOTATION_HAS_NO_EFFECT!>@Builder<!>
interface BuilderInterface

<!ANNOTATION_HAS_NO_EFFECT!>@Builder<!>
annotation class BuilderAnnotationClass

// An enum constructor takes the synthetic name and ordinal parameters, so a generated `build()` wouldn't find the
// one it calls: it used to fail with `NoSuchMethodError` at run time, KT-87871.
<!ANNOTATION_HAS_NO_EFFECT!>@Builder<!>
enum class BuilderEnum(val id: Int) { A(1) }

// An object has no constructor to call.
<!ANNOTATION_HAS_NO_EFFECT!>@Builder<!>
object BuilderObject

// An inner class's constructor takes the outer instance as its dispatch receiver, and the generated `build()`
// - a member of the builder class, which holds no such instance - has no way to pass one: the JVM backend used
// to fail on the call outright, KT-88852. Lombok refuses the shape too ("@Builder is not supported on
// non-static nested classes"); a nested class is what works, in Kotlin as in Java.
class OuterOfBuilderInner {
    <!ANNOTATION_IS_NOT_SUPPORTED_ON_CLASS!>@Builder<!>
    inner class BuilderInner(val value: Int)

    @Builder
    class BuilderNested(val value: Int)
}

// An abstract or sealed class cannot be instantiated, so the `build()` that calls its constructor failed with
// `InstantiationError` at run time, KT-88814. Lombok reports it too: "BuilderExample is abstract; cannot be
// instantiated". `@SuperBuilder` is what builds such a hierarchy in Java, and it is not supported on a Kotlin
// class at all.
<!ANNOTATION_IS_NOT_SUPPORTED_ON_CLASS!>@Builder<!>
abstract class BuilderAbstract(val id: Int, val name: String) {
    abstract fun describe(): String
}

<!ANNOTATION_IS_NOT_SUPPORTED_ON_CLASS!>@Builder<!>
sealed class BuilderSealed(val id: Int)

// An `open` class is instantiable, so it builds like any other.
@Builder
open class BuilderOpen(val id: Int)

@Builder(access = <!UNSUPPORTED_ACCESS_LEVEL!>AccessLevel.PACKAGE<!>) // Prohibited, KT-88337
class BuilderAccessLevelPackage(val id: Int)

@Builder(access = <!UNSUPPORTED_ACCESS_LEVEL!>AccessLevel.<!DEPRECATION!>MODULE<!><!>) // Prohibited, KT-88337
class BuilderAccessLevelModule(val id: Int)

fun test() {
    BuilderAccessLevelProtected.<!INVISIBLE_REFERENCE!>builder<!>()
    BuilderInterface.<!UNRESOLVED_REFERENCE!>builder<!>() // Nothing is generated, KT-87871
    BuilderAnnotationClass.<!UNRESOLVED_REFERENCE!>builder<!>() // Nothing is generated, KT-87871
    BuilderEnum.<!UNRESOLVED_REFERENCE!>builder<!>() // Nothing is generated
    BuilderObject.<!UNRESOLVED_REFERENCE!>builder<!>()
    OuterOfBuilderInner.BuilderInner.<!UNRESOLVED_REFERENCE!>builder<!>() // Nothing is generated, KT-88852
    OuterOfBuilderInner.BuilderNested.builder().value(1).build()
    BuilderAbstract.<!UNRESOLVED_REFERENCE!>builder<!>() // Nothing is generated, KT-88814
    BuilderSealed.<!UNRESOLVED_REFERENCE!>builder<!>() // Nothing is generated, KT-88814
    BuilderOpen.builder().id(1).build()

   // Local classes can't have a companion object to host `builder()`, exactly as for `@NoArgsConstructor`.
    <!ANNOTATION_HAS_NO_EFFECT!>@Builder<!>
    class BuilderLocal(val id: Int)

    BuilderLocal.<!UNRESOLVED_REFERENCE!>builder<!>()
}

// `builder()` goes into a companion object, and a nested class of that name leaves nowhere to put it, KT-88276.
@Builder
class BuilderWithNestedCompanionClass(val id: Int) {
    class <!COMPANION_OBJECT_IS_NOT_GENERATED!>Companion<!>
}

// A declaration the parser cannot read a name off gets the special name `<no name provided>`,
// which no builder member can be named after. Nothing is generated for it, and nothing crashes.
@Builder
class IncompleteProperty(val<!SYNTAX!><!> )

@Builder(setterPrefix = "with")
class IncompletePrefixedProperty(val<!SYNTAX!><!> )

fun useIncomplete() {
    IncompleteProperty.builder().build()
    IncompletePrefixedProperty.builder().build()
}
