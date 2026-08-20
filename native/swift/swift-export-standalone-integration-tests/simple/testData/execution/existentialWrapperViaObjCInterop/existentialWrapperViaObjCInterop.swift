import ExistentialWrapperViaObjCInterop
import Testing
import ObjectiveC
import Foundation

/// Bridging a Kotlin object to Objective-C has to produce a wrapper that carries the object itself: storing
/// the value in an `id`-typed Objective-C container and reading it back must yield the same Kotlin object.
@Test
func testKotlinObjectSurvivesObjCRoundTrip() throws {
    try #require(exportedRoundTripsThroughObjC())
    try #require(hiddenRoundTripsThroughObjC())
    try #require(hiddenChildRoundTripsThroughObjC())
    try #require(unitRoundTripsThroughObjC())
}

/// Crossing into Objective-C resolves the wrapper class through `classWrapperFor()`, which follows the Kotlin
/// class hierarchy, rather than through `protocolWrapperFor()`, which would yield an existential.
@Test
func testWrapperClassFollowsKotlinHierarchy() throws {
    try #require(hiddenChildSharesWrapperClassWithExported())
    try #require(hiddenUsesDistinctWrapperClass())
}

/// A Kotlin object keeps one wrapper across repeated conversions to Objective-C.
@Test
func testWrapperIsReusedAcrossConversions() throws {
    try #require(existentialWrapperIsCached())
    try #require(boundWrapperIsCached())
    try #require(exportedWrapperIsCached())
}

/// A Swift-created instance of an exported class already carries its bound bridge wrapper, so crossing into
/// Objective-C has to resolve back to that very instance instead of allocating a second wrapper.
@Test
func testExportedInstanceSurvivesObjCRoundTrip() throws {
    let swiftCreated = Exported()
    try #require(roundTripExportedThroughObjC(value: swiftCreated) === swiftCreated)

    let kotlinCreated = makeExported()
    try #require(roundTripExportedThroughObjC(value: kotlinCreated) === kotlinCreated)
}

/// Here the Objective-C class associated with the Kotlin type is the Swift subclass itself, while the
/// resolved wrapper class is `Exported`'s bound bridge. Neither descends from the other, which is exactly
/// what a receiver-constrained wrapper lookup cannot express.
@Test
func testSwiftSubclassSurvivesObjCRoundTrip() throws {
    class SwiftSubclass: Exported {}

    let subclassInstance = SwiftSubclass()
    let returned = roundTripExportedThroughObjC(value: subclassInstance)
    try #require(returned === subclassInstance)
    try #require(returned is SwiftSubclass)
}

@Test
func testRuntimeSupportIsLinked() throws {
    let selector = NSSelectorFromString("_Kotlin_SwiftExport_wrapIntoExistential:")
    try #require(class_getClassMethod(NSObject.self, selector) != nil)
}
