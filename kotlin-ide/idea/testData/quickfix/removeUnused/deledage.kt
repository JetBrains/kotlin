// "Safe delete 'something'" "false"
// ACTION: Convert function to property
// ACTION: Convert to block body
// ACTION: Specify return type explicitly

interface Inter {
    fun something(): String
}

class Impl : Inter {
    override fun <caret>something() = "hi"
}

class Test: Inter by Impl() {
}
