package demo

internal class Container {
    var myString = "1"
}

internal object One {
    var myContainer = Container()
}

internal class StringContainer(s: String)

internal class Test {
    fun putString(s: String) {}
    fun test() {
        putString(One.myContainer.myString)
        StringContainer(One.myContainer.myString)
    }
}