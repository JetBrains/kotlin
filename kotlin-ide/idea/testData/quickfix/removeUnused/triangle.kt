// "Safe delete 'something'" "false"
// ACTION: Convert function to property
// ACTION: Convert member to extension
// ACTION: Convert to block body
// ACTION: Move to companion object
// ACTION: Specify return type explicitly
// ACTION: Make open

interface Inter {
    fun something(): String
}

abstract class Abstract {
    fun <caret>something() = "hi"
}

class Test: Abstract(), Inter {
}
