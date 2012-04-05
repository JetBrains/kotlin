package sample

/** A Comment */
class Person(val name: String, val city: String) {
    fun toString(): String = "Person($name, $city)"

    fun hello(): String = "Hello $name"
}