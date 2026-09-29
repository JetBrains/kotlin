@_spi(kotlinx$cinterop$ExperimentalForeignApi) import Main
import FooKit
import Testing

@Test
func testReexportedCinteropTypeRoundTrip() {
    let foo = Foo(payload: 7)!
    #expect(payloadTriple(x: foo) == 21)
    #expect(foo.doubled() == 14)
    #expect(barValuePlusOne(x: foo) == 8)
}
