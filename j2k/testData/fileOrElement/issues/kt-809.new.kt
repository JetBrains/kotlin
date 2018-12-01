package demo

internal class Container {
    var myInt = 1
}

internal object One {
    var myContainer: Container? = Container()
}

internal class IntContainer(i: Int)

internal class Test {
    fun putInt(i: Int) {}
    fun test() {
        putInt(One.myContainer!!.myInt)
        IntContainer(One.myContainer!!.myInt)
    }
}