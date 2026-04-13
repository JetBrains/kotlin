// SNIPPET

var x: Int? = null
x = 1
val y: Int <!INITIALIZER_TYPE_MISMATCH!>=<!> x

var foo: String = ""
    set(value) {
        x = null
        field = value
    }

val z: Int <!INITIALIZER_TYPE_MISMATCH!>=<!> x
