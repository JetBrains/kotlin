import delegates.crashMe

class Model(private val factory: () -> Unit) {
    var crashMe1 by crashMe(factory)
}

fun main(args: Array<String>) {
    Model({ println("crashMe") })
}