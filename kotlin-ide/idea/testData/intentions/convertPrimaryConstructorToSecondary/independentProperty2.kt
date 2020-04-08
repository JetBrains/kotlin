object Storage {
    val hello = "Hello"
}

class Hello<caret>(val x: String) {
    val y = Storage.hello

    val z = x + y
}
