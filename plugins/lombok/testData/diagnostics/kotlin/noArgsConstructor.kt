import lombok.NoArgsConstructor

open class C {
    <!ANNOTATION_HAS_NO_EFFECT!>@NoArgsConstructor<!>
    companion object
}

<!ANNOTATION_HAS_NO_EFFECT!>@NoArgsConstructor<!>
object O

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
class B(x: String) : A(x)

@NoArgsConstructor
class D(x: String) : A(x), <!MANY_CLASSES_IN_SUPERTYPE_LIST!>C<!>()

fun test() {
    <!NO_VALUE_FOR_PARAMETER!>B<!>() // Don't generate no-args constructor because delegated no-args constructor is missing.
    <!NO_VALUE_FOR_PARAMETER!>D<!>()
}
