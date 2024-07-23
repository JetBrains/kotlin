import SpecialTypes
import KotlinRuntime

private func assertEqualStrings(actual: String, expected: String) throws {
    try assertEquals(actual: actual, expected: expected)
    try assertEquals(actual: actual.utf16.count, expected: expected.utf16.count)
    try assertTrue(zip(actual.utf16, expected.utf16).allSatisfy(==))
}

func testStrings() throws {
    let swiftString = "Hello, World!"
    let kotlinConstantString = getConstantString()
    try assertEqualStrings(actual: kotlinConstantString, expected: swiftString)

    try assertFalse(string == swiftString)
    string = swiftString
    try assertEqualStrings(actual: string, expected: swiftString)

    try assertTrue(areStringsEqual(lhs: string, rhs: swiftString))
    try assertFalse(areStringsTheSame(lhs: string, rhs: swiftString))

    // Swift.String is a value type which prevents identity retention
    try assertFalse(areStringsTheSame(lhs: string, rhs: string))
}

func testDataObject() throws {
    let obj = DemoDataObject.shared
    let objDescription = obj.description

    try assertEqualStrings(actual: obj.description, expected: stringDescribingDataObject())
    try assertEqualStrings(actual: "\(obj)", expected: obj.description)
    try assertEqualStrings(actual: "\(obj)", expected: "DemoDataObject")
}

func testWeirdStrings() throws {
    let asciiString = "Hello, World!"
    try assertEqualStrings(actual: predefinedASCIIString, expected: asciiString);
    try assertTrue(isPredefinedASCIIString(str: asciiString))

    let bmpString = "Привет, Мир!"
    try assertEqualStrings(actual: predefinedBMPString, expected: bmpString);
    try assertTrue(isPredefinedBMPString(str: bmpString))

    let unicodeString = "👋, 🌎"
    try assertEqualStrings(actual: predefinedUnicodeString, expected: unicodeString);
    try assertTrue(isPredefinedUnicodeString(str: unicodeString))
}

class SpecialTypesTests : TestProvider {
    var tests: [TestCase] = []

    init() {
        providers.append(self)
        tests = [
            TestCase(name: "testStrings", method: withAutorelease(testStrings)),
            TestCase(name: "testWeirdStrings", method: withAutorelease(testWeirdStrings)),
            TestCase(name: "testDataObject", method: withAutorelease(testDataObject)),
        ]
    }
}