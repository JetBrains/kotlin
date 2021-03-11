fun eat(vararg elements: Any) { elements.hashCode() }

data class Person(val name: String, val age: Int)

fun checkLambda(block: (Person) -> Unit) { block(Person("a", 1)) }

fun main() {
    val person = Person("Bob", 2)
    val (name, age) = person
    eat(name, age)
    checkLambda <warning descr="SSR">{ (n, a) -> eat(n, a) }</warning>
    val personList = listOf(person)
    for ((n, a) in personList) { eat(n, a) }
}