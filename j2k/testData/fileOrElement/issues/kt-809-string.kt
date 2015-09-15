package demo

internal class Container {
    internal var myString = "1"
}

internal object One {
    internal var myContainer = Container()
}

internal class StringContainer internal constructor(s: String)

internal class Test {
    internal fun putString(s: String) {
    }

    internal fun test() {
        putString(One.myContainer.myString)
        StringContainer(One.myContainer.myString)
    }
}