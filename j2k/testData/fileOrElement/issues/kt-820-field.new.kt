package demo

internal class Container {
    var myInt = 1
}

internal object One {
    var myContainer: Container? = Container()
}

internal class Test {
    var b = One.myContainer!!.myInt.toByte()
}