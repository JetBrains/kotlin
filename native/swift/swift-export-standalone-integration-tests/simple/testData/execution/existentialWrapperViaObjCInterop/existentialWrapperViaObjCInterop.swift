import ExistentialWrapperViaObjCInterop
import Testing

@Test
func testExistentialWrapperViaObjCInterop() throws {
    try #require(crossNonExportedClassIntoObjC() == false)
}
