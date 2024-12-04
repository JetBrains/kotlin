import Foundation
import Kt

// -------- Tests --------

func testFloatVector() throws {
    let vector: SIMD4<Float> = VectorKt.createVector(f0: 1, f1: 2, f2: 3, f3: 4)

    try assertEquals(actual: vector.x, expected: 1)
    try assertEquals(actual: vector.y, expected: 2)
    try assertEquals(actual: vector.z, expected: 3)
    try assertEquals(actual: vector.w, expected: 4)

    try assertEquals(actual: VectorKt.sumVectorFloat(vector), expected: 10.0)
}

func testIntVector() throws {
    let vector: SIMD4<Float> = VectorKt.createVector(i0: 0, i1: 1, i2: 2, i3: 3)

    let vectorInt: SIMD4<Int32> = unsafeBitCast(vector, to: SIMD4<Int32>.self)
    try assertEquals(actual: vectorInt.x, expected: 0)
    try assertEquals(actual: vectorInt.y, expected: 1)
    try assertEquals(actual: vectorInt.z, expected: 2)
    try assertEquals(actual: vectorInt.w, expected: 3)

    try assertEquals(actual: VectorKt.sumVectorInt(vector), expected: 6)
}

func testVectorBoxing() throws {
    let nullVector: Any? = VectorKt.createNullableVector(isNull: true)
    try assertNil(nullVector)
    try assertEquals(actual: VectorKt.sumNullableVectorInt(nullVector), expected: 0)

    let nonNullVector: Any? = VectorKt.createNullableVector(isNull: false)
    try assertFalse(nonNullVector == nil)
    try assertEquals(actual: VectorKt.sumNullableVectorInt(nonNullVector), expected: 10)
}

func testProperty() throws {
    let vector = VectorKt.createVector(i0: 1, i1: 2, i2: 3, i3: 4)
    try assertFalse(VectorKt.vector == vector)
    VectorKt.vector = vector
    try assertTrue(VectorKt.vector == vector)

}

// -------- Execution of the test --------

class VectorTests : SimpleTestProvider {
    override init() {
        super.init()

        test("testFloatVector", testFloatVector)
        test("testIntVector", testIntVector)
        test("testVectorBoxing", testVectorBoxing)
        test("testProperty", testProperty)
    }
}
