/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlinx.cinterop.*

import platform.Foundation.*
import platform.UniformTypeIdentifiers.UTTypeSourceCode
import platform.XCTest.*
import platform.objc.*

import kotlin.native.internal.test.GeneratedSuites
import kotlin.native.internal.test.TestCase
import kotlin.native.internal.test.TestSuite

@ExportObjCClass(name = "Kotlin/Native::Test")
class XCTestCaseRunner(
    invocation: NSInvocation,
    private val testName: String,
    private val testCase: TestCase,
) : XCTestCase(invocation) {
    // Sets XCTest to continue running after failure to match Kotlin Test
    override fun continueAfterFailure(): Boolean = true

    private val ignored = testCase.ignored || testCase.suite.ignored

    @ObjCAction
    fun run() {
        if (ignored) {
            // It is not possible to use XCTSkip() due to KT-43719 and not implemented exception importing.
            // Using `_XCTSkipHandler(...)` fails with
            //   Uncaught Kotlin exception: kotlinx.cinterop.ForeignException: _XCTSkipFailureException:: Test skipped
            // _XCTSkipHandler(testName, 0U, "Test $testName is ignored")
            // So, just skip the test for now (TODO)
            return
        }
        try {
            testCase.doRun()
        } catch (throwable: Throwable) {
            // First of all, just print it
            val stackTrace = throwable.getStackTrace()
            println(throwable)
            println(stackTrace.filter { it.contains("kfun:") }.joinToString("\n"))

            val type = when (throwable) {
                is AssertionError -> XCTIssueTypeAssertionFailure
                else -> XCTIssueTypeUncaughtException
            }
            val failedStackLine = stackTrace.first {
                // try to filter out kotlin.Exceptions and kotlin.test.Assertion inits
                !it.contains("kfun:kotlin.")
            }
            // Find path and line number to create source location
            val matchResult = Regex("^\\d+ +.* \\((.*):(\\d+):.*\\)$").find(failedStackLine)
            val sourceLocation = if (matchResult != null) {
                val (file, line) = matchResult.destructured
                XCTSourceCodeLocation(file, line.toLong())
            } else {
                // No debug info to get the path. Still have to record location
                XCTSourceCodeLocation(testCase.suite.name, 0L)
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
                detailedDescription = buildString {
                    appendLine("Test '$testName' from '${testCase.suite.name}' failed with $throwable")
                    throwable.cause?.let { appendLine("(caused by ${throwable.cause})") }
                },
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
        if (!ignored) testCase.doBefore()
    }

    override fun tearDown() {
        if (!ignored) testCase.doAfter()
    }

    override fun description(): String = buildString {
        append(testName)
        if (ignored) append("(ignored)")
    }

    override fun name() = testName

    companion object : XCTestCaseMeta() {
        /*
         * Used if the test suite is generated as a default one from methods extracted by the XCTest from the
         * runner that extends XCTestCase and is exported to ObjC.
         */
        override fun testCaseWithInvocation(invocation: NSInvocation?): XCTestCase {
            error(
                """
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
                types = "v@:" // See ObjC' type encodings: v (returns void), @ (id self), : (SEL _cmd)
            )
            check(result) {
                "Internal error: was unable to add method with selector $selector"
            }
        }

        private fun dispose(selector: SEL) {
            val imp = class_getMethodImplementation(
                cls = this.`class`(),
                name = selector
            )
            val result = imp_removeBlock(imp)
            check(result) {
                "Internal error: was unable to remove block for $selector"
            }
        }

        // TODO: need to clean up those methods. When/where should this be invoked?
        private fun disposeRunMethods() {
            testMethodsNames.forEach {
                val selector = NSSelectorFromString(it)
                dispose(selector)
            }
        }

        @Suppress("UNUSED_PARAMETER")
        private fun runner(runner: XCTestCaseRunner, cmd: SEL) = runner.run()
        //endregion

        /**
         * Create Test invocations for each test method to make them resolvable by the XCTest's machinery
         * @see NSInvocation
         */
        override fun testInvocations(): List<NSInvocation> = testMethodsNames.map {
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

private typealias SEL = COpaquePointer?

class XCTestSuiteRunner(private val testSuite: TestSuite) : XCTestSuite(testSuite.name) {
    private val ignoredSuite: Boolean
        get() = testSuite.ignored || testSuite.testCases.all { it.value.ignored }

    override fun setUp() {
        if (!ignoredSuite) testSuite.doBeforeClass()
    }

    override fun tearDown() {
        if (!ignoredSuite) testSuite.doAfterClass()
    }
}

private val testMethodsNames: List<String> =
    GeneratedSuites.suites.flatMap { testSuite ->
        testSuite.testCases.values.map { "$testSuite.${it.name}" }
    }

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "unused")
@kotlin.native.internal.ExportForCppRuntime("Konan_create_testSuite")
internal fun setupXCTestSuite(): XCTestSuite {
    val nativeTestSuite = XCTestSuite.testSuiteWithName("Kotlin/Native test suite")

    // Create and add tests to the main suite
    createTestSuites().forEach {
        nativeTestSuite.addTest(it)
    }

    // Tests created (self-check)
    @Suppress("UNCHECKED_CAST")
    check(GeneratedSuites.suites.size == (nativeTestSuite.tests as List<XCTest>).size) {
        "The amount of generated XCTest suites should be equal to Kotlin test suites"
    }

    return nativeTestSuite
}

private fun createTestSuites(): List<XCTestSuite> {
    val testInvocations = XCTestCaseRunner.testInvocations()
    return GeneratedSuites.suites.map { suite ->
        val xcSuite = XCTestSuiteRunner(suite)
        suite.testCases.values.map { testCase ->
            testInvocations.filter {
                it.selectorString() == "${suite.name}.${testCase.name}"
            }.map { invocation ->
                XCTestCaseRunner(
                    invocation = invocation,
                    testName = "${suite.name}.${testCase.name}",
                    testCase = testCase
                )
            }.single()
        }.forEach {
            xcSuite.addTest(it)
        }
        xcSuite
    }
}

private fun NSInvocation.selectorString() = NSStringFromSelector(selector)
