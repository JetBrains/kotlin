open class Klass {
    open val used = ":)"
}

class Subklass: Klass() {
    override fun used = ":|"
}

fun main(args: Array<String>) {
    Subklass().used
}