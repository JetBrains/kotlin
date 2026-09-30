import FullValueClasses

func testFullValueClasses() throws {
    // A single-field full value class is exported like an inline class, as its underlying type.
    try assertEquals(actual: FullValueClassesKt.twice(meters: 21), expected: 42)
    try assertEquals(actual: FullValueClassesKt.sum(point: Point(x: 1, y: 2)), expected: 3)
}

// -------- Execution of the test --------

class FullValueClassesTests : SimpleTestProvider {
    override init() {
        super.init()

        test("fullValueClasses", testFullValueClasses)
    }
}
