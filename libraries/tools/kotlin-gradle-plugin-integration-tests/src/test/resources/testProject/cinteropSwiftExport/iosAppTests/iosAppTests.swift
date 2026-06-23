import XCTest
import Shared
import FooKit

final class iosAppTests: XCTestCase {

    func testReexportedCinteropTypeRoundTrip() {
        // `Foo` comes from the user-defined `FooKit` cinterop module, re-exported by Swift Export.
        // `magicPlusOne` is a Kotlin function that accepts the cinterop type and is exposed via Shared.
        let foo = Foo()
        let result = magicPlusOne(foo: foo)
        XCTAssertEqual(result, 43, "magicPlusOne(Foo()) should be Foo.magic() + 1")
    }
}
