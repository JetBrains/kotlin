data class Person(val name: String, val surname: String)

object Foo {
    @JvmStatic
    fun main(args: Array<String>) {
        val person = Person("M", "S")
        println(person.toString())
    }
}