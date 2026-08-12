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
