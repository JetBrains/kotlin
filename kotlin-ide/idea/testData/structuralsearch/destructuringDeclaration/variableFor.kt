fun eat(vararg elements: Any) { elements.hashCode() }

data class Person(val name: String, val age: Int)

fun checkLambda(block: (Person) -> Unit) { block(Person("a", 1)) }

fun main() {
    val person = Person("Bob", 2)
    val (name, age) = person
    eat(name, age)
    checkLambda { (n, a) -> eat(n, a) }
    val personList = Array(1, { person })
    <warning descr="SSR">for ((n, a) in personList) { eat(n, a) }</warning>
}