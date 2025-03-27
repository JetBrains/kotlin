import Testing
import StdlibUsages
import stdlib

@Test
func testByteArray() throws {
    let byteArray = produceByteArray()
    try #require(getElementAt(byteArray, index: 0) == 1)
}

typealias StringBuilder = ExportedKotlinPackages.kotlin.text.StringBuilder

@Test
func testStringBuilder() throws {
    let sb = StringBuilder()
    sb.append(value: "hello")
    try #require(computeStringBuilder(sb: sb) == "hello")
    sb.append(value: " ")
    sb.append(value: Int32(2025))
    try #require(computeStringBuilder(sb: sb) == "hello 2025")
}