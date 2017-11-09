fun main(args: Array<String>) {
    cases("Hello")
    cases(1)
    cases(MyClass())
    cases("hello")
}

fun cases(obj: Any) {
    when(obj) {
        1 -> println("One")
        "Hello" -> println("Greeting")
        !is String -> println("Not a string")
        else -> println("Unknown")
    }
}

class MyClass() {
}
