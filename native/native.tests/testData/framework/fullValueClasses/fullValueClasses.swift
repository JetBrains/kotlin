import FullValueClasses

func testFullValueClasses() throws {
    // A single-field full value class is not an inline class: it is exported as a class, like a multi-field one.
    try assertEquals(actual: FullValueClassesKt.twice(meters: Meters(value: 21)).value, expected: 42)
    // An inline value class is exported as its underlying type.
    try assertEquals(actual: FullValueClassesKt.twiceInline(meters: 21), expected: 42)
    try assertEquals(actual: FullValueClassesKt.sum(point: Point(x: 1, y: 2)), expected: 3)
}

// -------- Execution of the test --------

class FullValueClassesTests : SimpleTestProvider {
    override init() {
        super.init()

        test("fullValueClasses", testFullValueClasses)
    }
}
