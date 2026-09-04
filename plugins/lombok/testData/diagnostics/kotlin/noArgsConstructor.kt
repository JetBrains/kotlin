// WITH_STDLIB

import lombok.AccessLevel
import lombok.NoArgsConstructor

open class C {
    <!ANNOTATION_HAS_NO_EFFECT!>@NoArgsConstructor<!>
    companion object
}

<!ANNOTATION_HAS_NO_EFFECT!>@NoArgsConstructor<!>
object O

// Nothing is generated when `ANNOTATION_HAS_NO_EFFECT` is reported: neither the constructor (which would be
// illegal in an interface) nor the `staticName` factory, so both are unresolved at the use sites below.
<!ANNOTATION_HAS_NO_EFFECT!>@NoArgsConstructor(staticName = "iface")<!> // isn't applicable to interface unlike `@NoArg` from noarg plugin
interface I

<!ANNOTATION_HAS_NO_EFFECT!>@NoArgsConstructor(staticName = "annotationClass")<!>
annotation class AnnotationClass

// any encountered val param requires `force = true`
<!NO_ARGS_CONSTRUCTOR_FORCE_REQUIRED!>@NoArgsConstructor<!>
class WithValParams(val x: Int, var y: String)

// var params do not require force
@NoArgsConstructor
class WithVarParams(var x: Int)

// force = true enables generation for val params without a error
@NoArgsConstructor(force = true)
class WithForce(val x: Int)

// val property with backing field and without initializer requires force
<!NO_ARGS_CONSTRUCTOR_FORCE_REQUIRED!>@NoArgsConstructor<!>
class NoArgsConstructorWithValPropertyAndNoInitializer {
    val str: String

    constructor(param: String) {
        str = param
    }
}

// val property without backing field is ignored
@NoArgsConstructor
class NoArgsConstructorWithValPropertyAndNoBackingField {
    val x: Int
        get() = 42
}

// val property with backing field but with initializer doesn't require force
@NoArgsConstructor
class NoArgsConstructorWithValPropertyAndInitializer {
    val y: Int = 42
}

// A value class *is* its underlying value: there is no instance to initialize field by field, and its
// constructors compile to static `constructor-impl` functions that must return that value. Generating one used
// to fail the JVM backend outright with "Unexpected IR element found during code generation", KT-88705.
<!ANNOTATION_HAS_NO_EFFECT!>@NoArgsConstructor(force = true)<!>
@JvmInline
value class OnValueClass(val value: Int)

// The static factory exists only to call the constructor, so it is not generated either.
<!ANNOTATION_HAS_NO_EFFECT!>@NoArgsConstructor(staticName = "create", force = true)<!>
@JvmInline
value class OnValueClassWithStaticName(val value: Int)

abstract class A(val x: String)

@NoArgsConstructor
class B(x: String) : A(x) // TODO: KT-86651 (NO_NOARG_CONSTRUCTOR_IN_SUPERCLASS)

@NoArgsConstructor
class D(x: String) : A(x), <!MANY_CLASSES_IN_SUPERTYPE_LIST!>C<!>()

interface I2

@NoArgsConstructor
class H(var y: String) : I2

<!NO_ARGS_CONSTRUCTOR_ALREADY_EXISTS!>@NoArgsConstructor<!>
class J()

<!NO_ARGS_CONSTRUCTOR_ALREADY_EXISTS, STATIC_CONSTRUCTOR_ALREADY_EXISTS!>@NoArgsConstructor(staticName = "create")<!>
class F() {
    companion object {
        fun create(): F = F()
    }
}

// The no-args constructor exists only for the static factory in the companion object to call, so when the factory
// cannot be generated neither is it. The name may be taken by a member of the class, which would shadow the factory
// at every unqualified call site...
<!STATIC_CONSTRUCTOR_ALREADY_EXISTS!>@NoArgsConstructor(staticName = "make", force = true)<!>
class StaticNameTakenByMember(val x: Int) {
    fun make(): String = "member"
}

// ...or by the companion object the factory would have been generated into.
<!STATIC_CONSTRUCTOR_ALREADY_EXISTS!>@NoArgsConstructor(staticName = "make", force = true)<!>
class StaticNameTakenInCompanion(val x: Int) {
    companion object {
        fun make(): String = "companion"
    }
}

// An extension is not taking the name: it cannot be called as `make()` and so shadows nothing.
@NoArgsConstructor(staticName = "make", force = true)
class StaticNameTakenByExtensionOnly(val x: Int) {
    fun Int.make(): String = "extension"
}

const val myStaticName: String = "make"

@NoArgsConstructor(staticName = myStaticName) // TODO: KT-86816
class K(var k: Int)

@NoArgsConstructor(staticName = "!@#$%^&*()") // TODO: KT-86816
class L(var l: Int)

@NoArgsConstructor(
    onConstructor = <!ANNOTATION_ARGUMENT_IS_NOT_SUPPORTED!>[]<!>,
)
class UnsupportedArguments(var arg: String)

fun test() {
    <!NO_VALUE_FOR_PARAMETER!>B<!>() // Don't generate no-args constructor because delegated no-args constructor is missing.
    <!NO_VALUE_FOR_PARAMETER!>D<!>() // Don't generate no-args constructor because there are multiple super classes (`MANY_CLASSES_IN_SUPERTYPE_LIST`)
    H() // Valid case: H has implicit `Any` call
    J()
    F()
    K()
    L()

    <!NO_VALUE_FOR_PARAMETER!>StaticNameTakenByMember<!>()
    StaticNameTakenByMember.<!UNRESOLVED_REFERENCE!>make<!>()

    <!NO_VALUE_FOR_PARAMETER!>StaticNameTakenInCompanion<!>()
    StaticNameTakenInCompanion.make()

    StaticNameTakenByExtensionOnly()
    StaticNameTakenByExtensionOnly.make()

    <!NO_VALUE_FOR_PARAMETER!>OnValueClass<!>() // Nothing is generated, KT-88705
    OnValueClassWithStaticName.<!UNRESOLVED_REFERENCE!>create<!>() // Nothing is generated, KT-88705

    I.<!UNRESOLVED_REFERENCE!>iface<!>() // Nothing is generated, KT-87871
    AnnotationClass.<!UNRESOLVED_REFERENCE!>annotationClass<!>() // Nothing is generated, KT-87871
}

@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
class NoArgsConstructorAccessLevelProtected(val x: Int)

@NoArgsConstructor(access = <!UNSUPPORTED_ACCESS_LEVEL!>AccessLevel.PACKAGE<!>, force = true) // Prohibited, KT-88337
class NoArgsConstructorAccessLevelPackage(val x: Int)

@NoArgsConstructor(access = <!UNSUPPORTED_ACCESS_LEVEL!>AccessLevel.<!DEPRECATION!>MODULE<!><!>, force = true) // Prohibited, KT-88337
class NoArgsConstructorAccessLevelModule(val x: Int)

// With `staticName`, `access` instead governs the visibility of the generated static factory function in the
// companion object, not of a constructor - so this goes through the same function-visibility path as `@Log`.
@NoArgsConstructor(access = AccessLevel.PROTECTED, staticName = "protectedCreate", force = true)
class NoArgsConstructorAccessLevelProtectedStatic(val x: Int)

fun testAccessLevels() {
    <!INVISIBLE_REFERENCE!>NoArgsConstructorAccessLevelProtected<!>()
    NoArgsConstructorAccessLevelProtectedStatic.<!INVISIBLE_REFERENCE!>protectedCreate<!>()
}

// Nothing is generated for an inner class, with or without `staticName`. A generated constructor would have to
// keep its delegating call in FIR - `InnerClassesLowering` takes a super-delegating constructor without an
// `IrInstanceInitializerCall` for a `this(...)` delegation - and that call makes fir2ir inline the class's
// property initializers into it, which is what crashed the JVM backend with "No mapping for symbol" (KT-88659).
// A static factory could not exist here anyway: it has no outer instance to construct with, and Lombok's own
// output for the Java equivalent is rejected by `javac`. The noarg plugin refuses an inner class for the same
// lowering, and so does this one now.
class OuterOfInner {
    <!ANNOTATION_HAS_NO_EFFECT!>@NoArgsConstructor(staticName = "make", force = true)<!>
    inner class InnerWithStaticName(val x: Int)

    <!ANNOTATION_HAS_NO_EFFECT!>@NoArgsConstructor(force = true)<!>
    inner class InnerWithoutStaticName(val x: Int)

    // The KT-88659 shape itself, which is what makes the inner case unfixable rather than merely unsupported:
    // `input` is a plain parameter, not a property, so the initializer of `inputValue` can only read the primary
    // constructor's argument. A generated constructor that carries a delegating call gets that initializer
    // inlined into it, where `input` is unbound, and the JVM backend fails with "No mapping for symbol". Leaving
    // the call off and building the body after fir2ir is the way out for every other class, but not for this
    // one: `InnerClassesLowering` reads a super-delegating constructor without that call as a `this(...)`
    // delegation and passes the outer instance to a call with no receiver slot. Neither shape works, so nothing
    // is generated at all.
    <!ANNOTATION_HAS_NO_EFFECT!>@NoArgsConstructor<!>
    inner class InnerWithReferencedParameter(input: Int) {
        var inputValue: Int? = input
    }

    // A nested class is not inner and keeps its generated constructor.
    @NoArgsConstructor(force = true)
    class NestedOfInner(val x: Int)
}

fun testInner(outer: OuterOfInner) {
    OuterOfInner.InnerWithStaticName.<!UNRESOLVED_REFERENCE!>make<!>() // Nothing is generated
    outer.InnerWithStaticName(42) // The declared constructor is untouched
    outer.<!NO_VALUE_FOR_PARAMETER!>InnerWithoutStaticName<!>() // Nothing is generated either
    outer.InnerWithoutStaticName(42)

    outer.<!NO_VALUE_FOR_PARAMETER!>InnerWithReferencedParameter<!>() // Nothing is generated, KT-88659
    outer.InnerWithReferencedParameter(42)

    OuterOfInner.NestedOfInner()
}

// A local class follows the inner one: `ANNOTATION_HAS_NO_EFFECT` has always been reported for it -
// `KotlinTarget.LOCAL_CLASS` is not among the annotation's targets - while the generator generated into it
// anyway, and the local-class repro of KT-88659 crashed just like the top-level one.
fun testLocal() {
    <!ANNOTATION_HAS_NO_EFFECT!>@NoArgsConstructor(force = true)<!>
    class LocalClass(val x: Int) {
        // An inner class of a local class is refused on both counts.
        <!ANNOTATION_HAS_NO_EFFECT!>@NoArgsConstructor(force = true)<!>
        inner class InnerOfLocal(val y: Int)
    }

    // A local class can hold no companion object, so there is nowhere for a static factory to go regardless.
    <!ANNOTATION_HAS_NO_EFFECT!>@NoArgsConstructor(staticName = "make", force = true)<!>
    class LocalClassWithStaticName(val x: Int)

    <!NO_VALUE_FOR_PARAMETER!>LocalClass<!>()
    val local = LocalClass(42) // The declared constructor is untouched
    local.<!NO_VALUE_FOR_PARAMETER!>InnerOfLocal<!>()
    local.InnerOfLocal(42)

    LocalClassWithStaticName.<!UNRESOLVED_REFERENCE!>make<!>()
    LocalClassWithStaticName(42)
}
