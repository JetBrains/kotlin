abstract class Base(val x: String)

class Derived : Base {
    constructor(x: String<caret>): super(x)
}