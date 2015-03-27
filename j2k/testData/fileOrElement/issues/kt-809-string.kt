package demo

class Container {
    var myString = "1"
}

object One {
    var myContainer = Container()
}

class StringContainer(s: String)

class Test {
    fun putString(s: String) {
    }

    fun test() {
        putString(One.myContainer.myString)
        StringContainer(One.myContainer.myString)
    }
}