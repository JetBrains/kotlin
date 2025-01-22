import SpecialTypes
import KotlinRuntime
import Testing

private func assertEqualStrings(actual: String, expected: String) throws {
    try #require(actual == expected)
    try #require(actual.utf16.count == expected.utf16.count)
    try #require(zip(actual.utf16, expected.utf16).allSatisfy(==))
}

@Test
func testStrings() throws {
    let swiftString = "Hello, World!"
    let kotlinConstantString = getConstantString()
    try assertEqualStrings(actual: kotlinConstantString, expected: swiftString)

    try #require(string != swiftString)
    string = swiftString
    try assertEqualStrings(actual: string, expected: swiftString)

    try #require(areStringsEqual(lhs: string, rhs: swiftString))
    try #require(!areStringsTheSame(lhs: string, rhs: swiftString))

    // Swift.String is a value type which prevents identity retention
    try #require(!areStringsTheSame(lhs: string, rhs: string))
}

@Test
func testDataObject() throws {
    let obj = DemoDataObject.shared
    let objDescription = obj.description

    try assertEqualStrings(actual: obj.description, expected: stringDescribingDataObject())
    try assertEqualStrings(actual: "\(obj)", expected: obj.description)
    try assertEqualStrings(actual: "\(obj)", expected: "DemoDataObject")
}

@Test
func testWeirdStrings() throws {
    let asciiString = "Hello, World!"
    try assertEqualStrings(actual: predefinedASCIIString, expected: asciiString);
    try #require(isPredefinedASCIIString(str: asciiString))

    let bmpString = "Привет, Мир!"
    try assertEqualStrings(actual: predefinedBMPString, expected: bmpString);
    try #require(isPredefinedBMPString(str: bmpString))

    let unicodeString = "👋, 🌎"
    try assertEqualStrings(actual: predefinedUnicodeString, expected: unicodeString);
    try #require(isPredefinedUnicodeString(str: unicodeString))
}