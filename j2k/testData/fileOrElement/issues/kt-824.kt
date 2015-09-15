package demo

internal class Container {
    internal var myBoolean = true
}

internal object One {
    internal var myContainer = Container()
}

internal class Test {
    internal fun test() {
        if (One.myContainer.myBoolean)
            println("Ok")

        val s = if (One.myContainer.myBoolean) "YES" else "NO"

        while (One.myContainer.myBoolean)
            println("Ok")

        do {
            println("Ok")
        } while (One.myContainer.myBoolean)
    }
}