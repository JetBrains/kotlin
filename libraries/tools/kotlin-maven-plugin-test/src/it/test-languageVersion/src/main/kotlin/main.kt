sealed class Base

class Derived : Base()

fun useApiFrom11() {
    mapOf<Any, Any>().toMutableMap()
}