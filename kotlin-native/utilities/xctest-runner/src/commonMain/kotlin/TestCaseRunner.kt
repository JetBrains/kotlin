import kotlinx.cinterop.*

import platform.Foundation.*
import platform.UniformTypeIdentifiers.UTTypeSourceCode
import platform.XCTest.*
import platform.objc.*

import kotlin.native.internal.ExportForCppRuntime
import kotlin.native.internal.test.GeneratedSuites
import kotlin.native.internal.test.TestCase
import kotlin.native.internal.test.TestSuite

@ExportObjCClass(name = "Kotlin/Native::Test")
class TestCaseRunner(
        invocation: NSInvocation,
        private val testName: String,
        private val testCase: TestCase
) : XCTestCase(invocation) {
    // Sets XCTest to continue running after failure to match Kotlin Test
    override fun continueAfterFailure(): Boolean = true

    private val ignored = testCase.ignored || testCase.suite.ignored

    @ObjCAction
    fun run() {
        if (ignored) {
            // TODO: XCTSkip() should be used instead, but https://youtrack.jetbrains.com/issue/KT-43719
            //  just skip it for now as no one catches the _XCTSkipFailureException
            //  023-01-02 20:06:56.016 xctest[76004:10364894] *** Terminating app due to uncaught exception '_XCTSkipFailureException', reason: 'Test skipped'
//            _XCTSkipHandler(testName, 0, "Test $testName is ignored")
            return
        }
        try {
            testCase.doRun()
        } catch (throwable: Throwable) {
            val type = when (throwable) {
                is AssertionError -> XCTIssueTypeAssertionFailure
                else -> XCTIssueTypeUncaughtException
            }
            val stackTrace = throwable.getStackTrace()
            val failedStackLine = stackTrace.first {
                // try to filter out kotlin.Exceptions and kotlin.test.Assertion inits
                !it.contains("kfun:kotlin.")
            }
            // Find path and line number
            val matchResult = Regex("^\\d+ +.* \\((.*):(\\d+):.*\\)$").find(failedStackLine)
            val sourceLocation = if (matchResult != null) {
                val (file, line) = matchResult.destructured
                XCTSourceCodeLocation(file, line.toLong())
            } else {
                XCTSourceCodeLocation()
            }

            @Suppress("CAST_NEVER_SUCCEEDS")
            val stackAsPayload = (stackTrace.joinToString("\n") as? NSString)
                    ?.dataUsingEncoding(NSUTF8StringEncoding)
            val stackTraceAttachment = XCTAttachment.attachmentWithUniformTypeIdentifier(
                    identifier = UTTypeSourceCode.identifier,
                    name = "Kotlin stacktrace (full)",
                    payload = stackAsPayload,
                    userInfo = null
            )

            val issue = XCTIssue(
                    type = type,
                    compactDescription = "$throwable in $testName",
                    detailedDescription = "Caught exception $throwable in $testName (caused by ${throwable.cause})",
                    sourceCodeContext = XCTSourceCodeContext(
                            callStackAddresses = throwable.getStackTraceAddresses(),
                            location = sourceLocation
                    ),
                    associatedError = null,
                    attachments = listOf(stackTraceAttachment)
            )
            testRun?.recordIssue(issue) ?: error("TestRun for the test $testName not found")
        }
    }

    override fun setUp() {
        super.setUp()
        if (!ignored) testCase.doBefore()
    }

    override fun tearDown() {
        if (!ignored) testCase.doAfter()
        super.tearDown()
    }

    override fun description(): String = buildString {
        append(testName)
        if (ignored) append("(ignored)")
    }

    // TODO: file an issue for null in this::class.simpleName
    //  "${this::class.simpleName}::$testName" leads to "null::$testName"
    // TODO: should the name be "-[$className $testName]" or not?
    override fun name() = testName

    companion object : XCTestCaseMeta(), XCTestSuiteExtensionsProtocolMeta {
        //region: XCTestSuiteExtensionsProtocolMeta extensions
        /**
         * These are from the XCTestCase extension and are not available by default.
         * See `@interface XCTestCase (XCTestSuiteExtensions)` in `XCTestCase.h` header file.
         * Issue: https://youtrack.jetbrains.com/issue/KT-40426
         */

        override fun defaultTestSuite(): XCTestSuite? = defaultTestSuite

        // TODO: setUp() and tearDown() methods are required for tests with @Before/AfterClass annotations
        //  testSuites should be generated one-to-one with each suite run by the own TestCaseRunner
        override fun setUp() {
            println("setUp method from $this")
            Throwable().printStackTrace()
        }

        override fun tearDown() {
            // FIXME: it looks like it's never invoked and hence methods aren't disposed
            //  do we need to dispose them at all or just check that we don't add them twice
            println("tearDown after all")
            disposeRunMethods()
        }
        //endregion

        /**
         * Used if the test suite is generated as a default one from methods extracted by the XCTest from the
         * runner that extends XCTestCase and is exported to ObjC.
         */
        override fun testCaseWithInvocation(invocation: NSInvocation?): XCTestCase {
            error("""
                This should not happen by default.
                Got invocation: ${invocation?.description}
                with selector @sel(${NSStringFromSelector(invocation?.selector)})
                """.trimIndent()
            )
        }

        // region: Dynamic run methods creation
        private fun createRunMethod(selector: SEL) {
            // Note: must be disposed off with imp_removeBlock
            val result = class_addMethod(
                    cls = this.`class`(),
                    name = selector,
                    imp = imp_implementationWithBlock(this::runner),
                    types = "v@:" // replace with method_getTypeEncoding()
            )
            check(result) {
                "Was unable to add method with selector $selector"
            }
        }

        private fun dispose(selector: SEL) {
            val imp = class_getMethodImplementation(
                    cls = this.`class`(),
                    name = selector
            )
            imp_removeBlock(imp)
        }

        private fun disposeRunMethods() {
            createTestMethodsNames().forEach {
                val selector = NSSelectorFromString(it)
                dispose(selector)
            }
        }

        @Suppress("UNUSED_PARAMETER")
        private fun runner(runner: TestCaseRunner, _sel: SEL) {
            runner.run()
        }
        //endregion

        @OptIn(ExperimentalStdlibApi::class)
        private fun createTestMethodsNames(): List<String> = GeneratedSuites.suites
                .flatMap { testSuite ->
                    testSuite.testCases.values
                            .map { "$testSuite.${it.name}" }
                }

        /**
         * Create Test invocations for each test method to make them resolvable by the XCTest's
         * @see NSInvocation
         */
        override fun testInvocations(): List<NSInvocation> = createTestMethodsNames().map {
            val selector = NSSelectorFromString(it)
            createRunMethod(selector)
            this.instanceMethodSignatureForSelector(selector)?.let { signature ->
                val invocation = NSInvocation.invocationWithMethodSignature(signature)
                invocation.setSelector(selector)
                invocation
            } ?: error("Not able to create NSInvocation for method $it")
        }
    }
}

internal typealias SEL = COpaquePointer?

class TestSuiteRunner(private val testSuite: TestSuite) : XCTestSuite(testSuite.name) {
    private val ignoredSuite: Boolean
        get() = testSuite.ignored || testSuite.testCases.all { it.value.ignored }

    override fun setUp() {
        super.setUp()
        if (!ignoredSuite) testSuite.doBeforeClass()
    }

    override fun tearDown() {
        if (!ignoredSuite) testSuite.doAfterClass()
        super.tearDown()
    }
}

@ExportForCppRuntime("Konan_create_testSuite")
fun defaultTestSuiteRunner(): XCTestSuite {
    XCTestObservationCenter.sharedTestObservationCenter.addTestObserver(XCSimpleTestListener())
    val nativeTestSuite = XCTestSuite.testSuiteWithName("Kotlin/Native test suite")

    println("Main bundle is: ${NSBundle.mainBundle}")
    NSBundle.allBundles.forEach {
        println("Bundle: $it with principal = ${(it as? NSBundle)?.principalClass()}")
        println((it as? NSBundle)?.infoDictionary?.get("TestKeyToBundle") ?: "Dictionary is null")
    }

    println(":::: Create test suites ::::")
    createTestSuites().forEach {
        println("* Suite '${it.name}' with tests: " + it.tests().joinToString(", ", "[", "]"))
        nativeTestSuite.addTest(it)
    }
    println(":::: Tests created ::::")
    @Suppress("UNCHECKED_CAST")
    (nativeTestSuite.tests as List<XCTest>).forEach {
        println("* Suite '${it.name}' with ${it.testCaseCount} test cases")
    }
    return nativeTestSuite
}

@OptIn(ExperimentalStdlibApi::class)
internal fun createTestSuites(): List<XCTestSuite> {
    val testInvocations = TestCaseRunner.testInvocations()
    return GeneratedSuites.suites
            .map {
                val suite = TestSuiteRunner(it)
                it.testCases.values.map { testCase ->
                    testInvocations
                            .filter { nsInvocation ->
                                NSStringFromSelector(nsInvocation.selector) == "${it.name}.${testCase.name}"
                            }
                            .map { inv ->
                                TestCaseRunner(
                                        invocation = inv,
                                        testName = "${it.name}.${testCase.name}",
                                        testCase = testCase
                                )
                            }.single()
                }.forEach { t ->
                    suite.addTest(t)
                }
                suite
            }
}
