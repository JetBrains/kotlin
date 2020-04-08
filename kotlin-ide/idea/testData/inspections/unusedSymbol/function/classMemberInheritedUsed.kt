open class Klass {
    fun used() {
    }
}

class Subklass: Klass()

fun main(args: Array<String>) {
    Subklass().used()
}