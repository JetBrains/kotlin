// "Create secondary constructor" "true"

open class A {
    constructor(x: Int, y: Int)
    constructor(x: Int, y: String)
}

class B : <caret>A(1)