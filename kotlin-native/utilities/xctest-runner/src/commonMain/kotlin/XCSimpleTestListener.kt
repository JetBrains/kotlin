import platform.Foundation.NSBundle
import platform.darwin.NSObject
import platform.darwin.NSUInteger
import platform.XCTest.*

class XCSimpleTestListener : NSObject(), XCTestObservationProtocol {
    override fun testBundleDidFinish(testBundle: NSBundle) {
        println("testBundleDidFinish: $testBundle")
    }

    override fun testBundleWillStart(testBundle: NSBundle) {
        println("testBundleWillStart: $testBundle")
    }

    override fun testCase(testCase: XCTestCase, didRecordIssue: XCTIssue) {
        println("testCase: $testCase ${didRecordIssue.compactDescription}")
    }

    override fun testCase(testCase: XCTestCase, didRecordExpectedFailure: XCTExpectedFailure) {
        println("testCase: $testCase ${didRecordExpectedFailure.failureReason}")
    }

    override fun testCase(testCase: XCTestCase, didFailWithDescription: String, inFile: String?, atLine: NSUInteger) {
        println("testCase: $testCase $didFailWithDescription @ $inFile:$atLine")
    }

    override fun testCaseDidFinish(testCase: XCTestCase) {
        println("testCaseDidFinish: $testCase")
    }

    override fun testCaseWillStart(testCase: XCTestCase) {
        println("testCaseWillStart: $testCase")
    }

    override fun testSuite(testSuite: XCTestSuite, didRecordIssue: XCTIssue) {
        println("testSuite: ${testSuite.name} ${didRecordIssue.compactDescription}")
    }

    override fun testSuite(testSuite: XCTestSuite, didRecordExpectedFailure: XCTExpectedFailure) {
        println("testSuite: ${testSuite.name} ${didRecordExpectedFailure.failureReason}")
    }

    override fun testSuite(
            testSuite: XCTestSuite,
            didFailWithDescription: String,
            inFile: String?,
            atLine: NSUInteger
    ) {
        println("testSuite: ${testSuite.name} $didFailWithDescription @ $inFile:$atLine")
    }

    override fun testSuiteDidFinish(testSuite: XCTestSuite) {
        println("testSuiteDidFinish: ${testSuite.name}")
    }

    override fun testSuiteWillStart(testSuite: XCTestSuite) {
        println("testSuiteWillStart: ${testSuite.name}")
    }
}