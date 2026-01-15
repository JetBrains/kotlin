// SNIPPET

var x: Int? = null
x = 1
val y: Int <!INITIALIZER_TYPE_MISMATCH!>=<!> x
val z: Int <!INITIALIZER_TYPE_MISMATCH!>=<!> x

fun foo() {
    x = null
}
