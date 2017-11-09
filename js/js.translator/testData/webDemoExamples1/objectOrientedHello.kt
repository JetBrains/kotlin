class Greeter(name: String) {
    val name = name
    fun greet() {
        println("Hello, ${name}!");
    }
}

fun main(args: Array<String>) {
    Greeter(args[0]).greet()
}