@_spi(kotlinx$cinterop$ExperimentalForeignApi) import Main
import FooKit
import KotlinRuntime
import Testing

@Test
func testReexportedCinteropTypeRoundTrip() {
    let foo = Foo(payload: 7)!
    #expect(payloadTriple(x: foo) == 21)
    #expect(foo.doubled() == 14)
    #expect(barValuePlusOne(x: foo) == 8)
}

/// An ObjC/Swift class returned as Kotlin `Any` keeps its original Swift type (KT-90924).
@Test
func testReturnFooAsAny() {
    let foo = Foo(payload: 7)!
    let result = returnFooAsAny(x: foo)
    #expect(result is Foo)
    #expect((result as! Foo) === foo)
}

/// A Swift implementation of a re-exported ObjC protocol returned as Kotlin `Any` keeps its
/// original Swift type (KT-90924).
@Test
func testReturnSwiftBarAsAny() {
    class SwiftBar : Bar {
        func barValue() -> Int32 { 0 }
    }

    let bar = SwiftBar()
    let result = returnBarAsAny(x: bar)
    #expect(result is Bar)
    #expect((result as! SwiftBar) === bar)
}

/// A foreign ObjC/Swift object survives a Kotlin `Any` round trip with its identity intact.
@Test
func testForeignObjCObjectKeepsIdentityThroughAny() {
    let foo = Foo(payload: 9)!
    let result = anyIdentity(x: foo)
    #expect(result is Foo)
    #expect((result as! Foo) === foo)
}

/// A private Kotlin implementation of a re-exported ObjC protocol keeps going through the Kotlin
/// wrapper machinery: it surfaces as a KotlinExistential (a KotlinBase descendant), not as the raw
/// registered ObjC object (KT-90924).
@Test
func testKotlinImplementationOfObjCProtocolIsWrapped() {
    let result = returnKotlinBarAsAny()
    #expect(result is KotlinBase)
    // Current gap (KT-90924 follow-up): conformance extensions of _KotlinExistential are only
    // generated for Kotlin-declared protocols, so the wrapper does not conform to the foreign
    // `Bar` protocol yet. Flip this assertion when that machinery exists.
    #expect(!(result is Bar))
}

/// What the widened `Any` parameter actually accepts (KT-90924): previously-supported values
/// keep their exact old path; exotic Swift values must not crash the bridge.
@Test
func testSwiftValueTypesThroughAny() {
    struct Point { var x: Int32; var y: Int32 }

    // A custom struct: boxed as an opaque Swift object, surfaces back as the same box.
    let point = Point(x: 1, y: 2)
    let boxedPoint = anyIdentity(x: point)
    print("PROBE struct: kotlin sees \(describeAny(x: boxedPoint))")
    #expect((boxedPoint as? Point)?.x == 1)

    // Swift `Int` (not `_KotlinBridgeable`-conforming): bridges to NSNumber, which the runtime
    // converts into a Kotlin number matching the NSNumber width — `Long` for 64-bit Swift Int.
    let intResult = anyIdentity(x: 7)
    print("PROBE int: kotlin sees \(describeAny(x: intResult))")
    #expect((intResult as? Int64) == 7)

    // A tuple: the most exotic case.
    let tupleResult = anyIdentity(x: (1, 2) as Any)
    print("PROBE tuple: kotlin sees \(describeAny(x: tupleResult))")
    #expect(tupleResult != nil)
}

/// Values passed through `Any` parameters must not be pinned by the bridge after Swift drops
/// them (KT-90924).
@Test
func testForeignObjectPassedThroughAnyParamIsReleased() throws {
    try autoreleasepool {
        let foo = Foo(payload: 3)!
        #expect(trackThroughAny(x: foo) == 0)
    }
    try #require(!isTrackedThroughAnyAlive())
}

@Test
func testStringPassedThroughAnyParamIsReleased() throws {
    try autoreleasepool {
        #expect(trackThroughAny(x: "leak probe") == 0)
    }
    try #require(!isTrackedThroughAnyAlive())
}
