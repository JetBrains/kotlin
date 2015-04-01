package demo

class Container {
    var myInt = 1
}

object One {
    var myContainer = Container()
}

class Test {
    var b = One.myContainer.myInt.toByte()
}