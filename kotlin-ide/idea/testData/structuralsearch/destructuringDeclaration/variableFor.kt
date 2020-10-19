data class Person(val name: String, val age: Int)

fun checkLambda(block: (Person) -> Unit) { print(block) }

fun main() {
    val person = Person("Bob", 2)
    val (name, age) = person
    print("$name $age")
    checkLambda { (n, a) -> print("$n $a") }
    val personList = listOf(person)
    <warning descr="SSR">for ((n, a) in personList) { print("$n $a") }</warning>
}