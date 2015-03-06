package demo

class Container {
    var myInt = 1
}

class One {
    default object {
        var myContainer = Container()
    }
}

class IntContainer(i: Int)

class Test {
    fun putInt(i: Int) {
    }

    fun test() {
        putInt(One.myContainer.myInt)
        IntContainer(One.myContainer.myInt)
    }
}