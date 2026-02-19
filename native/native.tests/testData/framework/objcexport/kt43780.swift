import Kt

private func testObject() throws {
    let object = KT43780TestObject.shared
    try assertEquals(actual: object.x, expected: 5)
    try assertEquals(actual: object.y, expected: 6)
    try assertEquals(actual: object.shared, expected: "shared")
    try assertEquals(actual: object.Shared, expected: "Shared")
    try assertTrue(object === KT43780TestObject())
}

private func testCompanionObject() throws {
    let object = KT43780TestClassWithCompanion.companion
    try assertEquals(actual: object.z, expected: 7)
    try assertTrue(object === KT43780TestClassWithCompanion.Companion())
    try assertTrue(object === KT43780TestClassWithCompanion.Companion.shared)
}

private func testNameClash() throws {
    let object = Shared.shared
    try assertEquals(actual: object.x, expected: 8)
    try assertTrue(object === Shared())
    let object2 = Companion.companion
    try assertEquals(actual: object2.x, expected: 9)
    try assertTrue(object2 === Companion.Companion())
    let object3 = Companion()
    try assertEquals(actual: object3.t, expected: 10)
    let object4 = Companion()
    try assertEquals(actual: object4.t, expected: 10)
    try assertTrue(object3 !== object4)

    let object5 = KT43780Enum.Companion()
    try assertEquals(actual: object5.x, expected: 11)
    try assertTrue(object5 === KT43780Enum.Companion())
    let enumEntry : KT43780Enum = KT43780Enum.companion
    try assertEquals(actual: enumEntry.name, expected: "COMPANION")
    try assertEquals(actual: KT43780Enum.otherEntry.name, expected: "OTHER_ENTRY")
}

// Reported as https://youtrack.jetbrains.com/issue/KT-47462
private func testUnexposedCompanion() throws {
    // Just trying to ensure that the classes are accessible:
    try assertEquals(actual: 13, expected: ClassWithInternalCompanion().y)
    try assertEquals(actual: 15, expected: ClassWithPrivateCompanion().y)
    // and this doesn't make the companions accessible as well.
}


class Kt43780Tests : SimpleTestProvider {
    override init() {
        super.init()

        test("testObject", testObject)
        test("testCompanionObject", testCompanionObject)
        test("testNameClash", testNameClash)
        test("testUnexposedCompanion", testUnexposedCompanion)
    }
}
