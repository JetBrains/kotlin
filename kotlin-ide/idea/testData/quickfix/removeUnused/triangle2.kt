// "Safe delete 'something'" "false"
// ACTION: Convert function to property
// ACTION: Convert member to extension
// ACTION: Convert to block body
// ACTION: Move to companion object
// ACTION: Specify return type explicitly

interface Inter {
    fun something(): String
}

abstract class Abstract {
    open fun <caret>something() = "hi"
}

class Test: Abstract(), Inter {
    override fun something() = "bye"
}
