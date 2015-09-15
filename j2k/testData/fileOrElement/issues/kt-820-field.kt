package demo

internal class Container {
    internal var myInt = 1
}

internal object One {
    internal var myContainer = Container()
}

internal class Test {
    internal var b = One.myContainer.myInt.toByte()
}