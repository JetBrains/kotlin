package sample

/** A Comment */
public class Person(val name: String, val city: String) {
    fun toString(): String = "Person($name, $city)"

    public fun hello(): String = "Hello $name"
}
