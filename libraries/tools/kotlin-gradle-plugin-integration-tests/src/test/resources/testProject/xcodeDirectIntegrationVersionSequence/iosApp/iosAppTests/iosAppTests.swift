import XCTest
import Kotlin

final class iosAppTests: XCTestCase {

    func test() throws {
        let expectedValue = ProcessInfo.processInfo.environment["EXPECTED_TEST_VALUE"]
        let actualValue = Version().value()

        print("Expected test value: \(expectedValue)")
        print("Actual test value: \(actualValue)")

        XCTAssertNotNil(expectedValue)
        XCTAssertEqual(
            actualValue,
            expectedValue
        )
    }

}
