package packJs

class <caret>ClassUsageJs {
    init {
        listOf("example")
        arrayOf("another")
        Pair(1, "2")
    }

    @JsName("bar")
    fun foo() {

    }
}

class Foo