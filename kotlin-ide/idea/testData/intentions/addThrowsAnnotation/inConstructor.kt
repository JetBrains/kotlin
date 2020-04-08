// WITH_RUNTIME

class FooException : Exception()

class Test {
    constructor() {
        <caret>throw FooException()
    }
}