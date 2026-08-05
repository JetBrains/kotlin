import ExistentialWrapperViaObjCInterop
import Testing
import ObjectiveC
import Foundation

@Test
func testExistentialWrapperViaObjCInterop() throws {
    try #require(crossNonExportedClassIntoObjC() == false)
}

@Test
func testRuntimeSupportIsLinked() throws {
    let selector = NSSelectorFromString("_Kotlin_SwiftExport_wrapIntoExistential:")
    try #require(class_getClassMethod(NSObject.self, selector) != nil)
}
