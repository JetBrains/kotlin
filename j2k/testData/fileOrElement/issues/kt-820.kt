package demo

class Container {
    var myInt = 1
}

class One {
    default object {
        var myContainer = Container()
    }
}

class Test {
    fun test() {
        val b = One.myContainer.myInt.toByte()
    }
}