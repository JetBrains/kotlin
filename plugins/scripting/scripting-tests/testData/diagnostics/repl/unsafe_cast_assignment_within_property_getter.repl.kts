// SNIPPET

var x: Int? = null
x = 1
val y: Int <!INITIALIZER_TYPE_MISMATCH!>=<!> x

val foo: String
    get() {
        x = null
        return ""
    }

val z: Int <!INITIALIZER_TYPE_MISMATCH!>=<!> x
