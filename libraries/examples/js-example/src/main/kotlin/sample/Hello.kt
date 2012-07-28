package sample

public fun main(args: Array<String>): Unit {
    val hello = Hello()
    hello.doSomething()
}

public class Hello {
    var x = 0

    fun doSomething(): Unit {
        x++
    }
}