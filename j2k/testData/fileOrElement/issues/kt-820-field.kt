package demo

class Container {
    var myInt = 1
}

class One {
    companion object {
        var myContainer = Container()
    }
}

class Test {
    var b = One.myContainer.myInt.toByte()
}