package foo

class Greeter(private val name: String) {
    val greeting: String
            get() = "Hello $name!"
}