// "Safe delete 'something'" "true"

abstract class Abstract {
    open fun <caret>something() = "hi"
}

class Test: Abstract() {
}
