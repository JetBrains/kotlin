fun main(args : Array<String>) {
    cases("Hello")
    cases(1)
    cases(MyClass())
    cases("hello")
}

fun cases(obj : Any) {
  when(obj) {
    1          -> System.out?.println("One")
    "Hello"    -> System.out?.println("Greeting")
    !is String -> System.out?.println("Not a string")
    else       -> System.out?.println("Unknown")
  }
}

class MyClass() {
}
