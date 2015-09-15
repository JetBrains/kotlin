package demo

internal class Container {
    internal var myInt = 1
}

internal object One {
    internal var myContainer = Container()
}

internal class IntContainer internal constructor(i: Int)

internal class Test {
    internal fun putInt(i: Int) {
    }

    internal fun test() {
        putInt(One.myContainer.myInt)
        IntContainer(One.myContainer.myInt)
    }
}