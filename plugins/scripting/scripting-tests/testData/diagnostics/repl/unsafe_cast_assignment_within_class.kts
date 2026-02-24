// SNIPPET

var x: Int? = null
x = 1
val y: Int <!INITIALIZER_TYPE_MISMATCH!>=<!> x

class Foo {
    init {
        x = null
    }
}

val z: Int <!INITIALIZER_TYPE_MISMATCH!>=<!> x
