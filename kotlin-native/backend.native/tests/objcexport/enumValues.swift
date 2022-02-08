import Kt

private func testEnumValues() throws {
    let values = EnumLeftRightUpDown.values()

    try assertEquals(actual: values.size, expected: 4)

    try assertSame(actual: values.get(index: 0) as AnyObject, expected: EnumLeftRightUpDown.left)
    try assertSame(actual: values.get(index: 1) as AnyObject, expected: EnumLeftRightUpDown.right)
    try assertSame(actual: values.get(index: 2) as AnyObject, expected: EnumLeftRightUpDown.up)
    try assertSame(actual: values.get(index: 3) as AnyObject, expected: EnumLeftRightUpDown.down)
}

private func testEnumValuesMangled() throws {
    let values = EnumOneTwoThreeValues.values_()

    try assertEquals(actual: values.size, expected: 4)

    try assertSame(actual: values.get(index: 0) as AnyObject, expected: EnumOneTwoThreeValues.one)
    try assertSame(actual: values.get(index: 1) as AnyObject, expected: EnumOneTwoThreeValues.two)
    try assertSame(actual: values.get(index: 2) as AnyObject, expected: EnumOneTwoThreeValues.three)
    try assertSame(actual: values.get(index: 3) as AnyObject, expected: EnumOneTwoThreeValues.values)
}

private func testEnumValuesMangledTwice() throws {
    let values = EnumValuesValues_.values__()

    try assertEquals(actual: values.size, expected: 2)

    try assertSame(actual: values.get(index: 0) as AnyObject, expected: EnumValuesValues_.values)
    try assertSame(actual: values.get(index: 1) as AnyObject, expected: EnumValuesValues_.values_)
}

private func testEnumValuesEmpty() throws {
    try assertEquals(actual: EmptyEnum.values().size, expected: 0)
}

class EnumValuesTests : SimpleTestProvider {
    override init() {
        super.init()

        test("TestEnumValues", testEnumValues)
        test("TestEnumValuesMangled", testEnumValuesMangled)
        test("TestEnumValuesMangledTwice", testEnumValuesMangledTwice)
        test("TestEnumValuesEmpty", testEnumValuesEmpty)
    }
}
