data class Person(val name: String, val age: Int)

fun checkLambda(block: (Person) -> Unit) { print(block) }

fun main() {
    val person = Person("Bob", 2)
    val (name, age) = person
    print("$name $age")
    checkLambda <warning descr="SSR">{ (n, a) -> print("$n $a") }</warning>
    val personList = listOf(person)
    for ((n, a) in personList) { print("$n $a") }
}