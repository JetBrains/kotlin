import lombok.NoArgsConstructor

open class C {
    <!ANNOTATION_HAS_NO_EFFECT!>@NoArgsConstructor<!>
    companion object
}

<!ANNOTATION_HAS_NO_EFFECT!>@NoArgsConstructor<!>
object O

<!ANNOTATION_HAS_NO_EFFECT!>@NoArgsConstructor<!> // isn't applicable to interface unlike `@NoArg` from noarg plugin
interface I

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

@NoArgsConstructor // TODO: KT-86651 ("Constructor without parameters is already defined")
class J()

@NoArgsConstructor(staticName = "create") // TODO: KT-86651 ("Constructor without parameters is already defined")
class F() {
    companion object {
        fun create(): F = F()
    }
}

const val myStaticName: String = "make"

@NoArgsConstructor(staticName = myStaticName) // TODO: KT-86816
class K()

@NoArgsConstructor(staticName = "!@#$%^&*()") // TODO: KT-86816
class L()

fun test() {
    <!NO_VALUE_FOR_PARAMETER!>B<!>() // Don't generate no-args constructor because delegated no-args constructor is missing.
    <!NO_VALUE_FOR_PARAMETER!>D<!>() // Don't generate no-args constructor because there are multiple super classes (`MANY_CLASSES_IN_SUPERTYPE_LIST`)
    H() // Valid case: H has implicit `Any` call
    J()
    F()
    K()
    L()
}
