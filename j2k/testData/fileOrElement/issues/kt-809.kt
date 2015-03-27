package demo

class Container {
    var myInt = 1
}

object One {
    var myContainer = Container()
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