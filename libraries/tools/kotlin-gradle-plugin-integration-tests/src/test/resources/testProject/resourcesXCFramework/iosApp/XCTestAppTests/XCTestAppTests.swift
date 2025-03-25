import XCTest
@testable import Shared

final class XCTestAppTests: XCTestCase {

    func testRequiredResourcesExist() throws {
        let bundle = Bundle(for: type(of: Greeting()))
        let baseDirectory = "Frameworks/Shared.framework/embedResources"

        let testCases: [(name: String, type: String?, subDirectory: String)] = [
            ("compose-multiplatform", "xml", "drawable"),
            ("IndieFlower-Regular", "ttf", "font"),
            ("strings", "xml", "values"),
            ("commonResource", nil, "files")
        ]

        for testCase in testCases {
            let resourcePath = bundle.path(
                forResource: testCase.name,
                ofType: testCase.type,
                inDirectory: "\(baseDirectory)/\(testCase.subDirectory)"
            )
            XCTAssertNotNil(
                resourcePath,
                "Missing resource: \(testCase.name).\(testCase.type) in directory \(testCase.subDirectory)"
            )
        }
    }
}
