import Testing
import StdlibUsages

@Test
func testByteArray() throws {
    let byteArray = produceByteArray()
    try #require(getElementAt(byteArray, index: 0) == 1)
}