
class Main {
    var i = 0

    fun main() {
        i = 5
    }

    fun changeI(): Int {
        main()
        return i
    }
}

fun test1(): Int {
    val main = Main()
    return main.changeI()
}

fun test2(): Int {
    val main = Main()
    return main.i
}

fun test3(): Int {
    return Main().changeI()
}
