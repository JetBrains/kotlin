import XCTest
import Shared
import Subproject

final class iosAppTests: XCTestCase {

    func testFoo() {
        let result = foo()
        XCTAssertEqual(result, 321, "foo() should return the expected result")
    }

    func testBar() {
        let result = bar()
        XCTAssertEqual(result, 123, "bar() should return the expected result")
    }

    func testFoobar() {
        let param: Swift.Int32 = 42
        let result = foobar(param: param)
        XCTAssertEqual(result, 486, "foobar() should return the expected result for the given parameter")
    }

    func testSubprojectFoo() {
        let result = libraryFoo()
        XCTAssertEqual(result, 123456, "libraryFoo() should return the expected result")
    }
}
