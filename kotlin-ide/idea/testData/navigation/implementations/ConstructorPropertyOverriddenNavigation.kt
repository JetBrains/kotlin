package testing

open class Test(open var <caret>some: Int)

class OtherTestInConstructor(override var some: Int): Test(some)

class OtherTestInBody(some: Int, other: String): Test(some) {
    override var some: Int = some
}

// REF: (in testing.OtherTestInConstructor).some
// REF: (in testing.OtherTestInBody).some