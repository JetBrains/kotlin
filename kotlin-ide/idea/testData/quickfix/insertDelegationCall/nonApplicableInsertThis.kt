// "Insert 'this()' call" "true"

open class B(val x: Int)

class A : B {
    constructor(x: String)<caret>

    constructor() : super(1)
}
