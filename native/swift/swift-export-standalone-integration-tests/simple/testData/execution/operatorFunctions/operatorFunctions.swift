// Swift code to test the exported Kotlin operator functions

import OperatorFunctions
import Testing

@Test
func testBinaryOperators() throws {
    let v1 = OperatorFunctions.Vector(x: 1, y: 2)
    let v2 = OperatorFunctions.Vector(x: 3, y: 4)

    // Test plus operator
    let sum = v1.plus(other: v2)
    try #require(sum.x == 4)
    try #require(sum.y == 6)

    // Test minus operator
    let diff = v1.minus(other: v2)
    try #require(diff.x == -2)
    try #require(diff.y == -2)

    // Test times operator
    let scaled = v1.times(scalar: 2)
    try #require(scaled.x == 2)
    try #require(scaled.y == 4)

    // Test div operator
    let divided = v1.div(scalar: 1)
    try #require(divided.x == 1)
    try #require(divided.y == 2)
}

@Test
func testUnaryOperators() throws {
    let v1 = OperatorFunctions.Vector(x: 1, y: 2)

    // Test unary minus operator
    let negated = v1.unaryMinus()
    try #require(negated.x == -1)
    try #require(negated.y == -2)
}

@Test
func testComparisonOperators() throws {
    let v1 = OperatorFunctions.Vector(x: 1, y: 2)
    let v2 = OperatorFunctions.Vector(x: 3, y: 4)

    // Test comparison operator
    let comparison = v1.compareTo(other: v2)
    try #require(comparison < 0) // v1 magnitude is less than v2 magnitude
}

@Test
func testIndexingOperators() throws {
    let v1 = OperatorFunctions.Vector(x: 1, y: 2)

    // Test indexing operators
    let x = try v1.get(index: 0)
    let y = try v1.get(index: 1)

    try #require(x == 1)
    try #require(y == 2)

    // Test out of bounds index
    try #require(throws: Error.self) {
        _ = try v1.get(index: 2)
    }
}
