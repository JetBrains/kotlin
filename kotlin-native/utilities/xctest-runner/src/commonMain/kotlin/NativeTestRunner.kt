/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

import kotlinx.cinterop.*
import kotlin.native.internal.test.*
import platform.Foundation.*
import platform.Foundation.NSError
import platform.Foundation.NSInvocation
import platform.Foundation.NSString
import platform.Foundation.NSMethodSignature
import platform.UniformTypeIdentifiers.UTTypeSourceCode
import platform.XCTest.*
import platform.objc.*

/**
 * An XCTest equivalent of the K/N TestCase.
 */
@ExportObjCClass(name = "Kotlin/Native::Test")
class XCTestCaseRunner(
    invocation: NSInvocation,
    val testName: String,
    val testCase: TestCase,
) : XCTestCase(invocation) {
    // Sets XCTest to continue running after failure to match Kotlin Test
    override fun continueAfterFailure(): Boolean = true

    private val ignored = testCase.ignored || testCase.suite.ignored

    @ObjCAction
    fun run() {
        if (ignored) {
            // FIXME: It is not possible to use XCTSkip() due to the KT-43719 and not implemented exception importing.
            //  Using `_XCTSkipHandler(...)` fails with
            //   Uncaught Kotlin exception: kotlinx.cinterop.ForeignException: _XCTSkipFailureException:: Test skipped
            //  _XCTSkipHandler(testName, 0U, "Test $testName is ignored")
            //  So, just skip the test. It will be seen as passed in XCode, but K/N TestListener should correctly process that.
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
            val stackAsPayload = (stackTrace.joinToString("\n") as? NSString)?.dataUsingEncoding(NSUTF8StringEncoding)
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
                associatedError = NSErrorWithKotlinException(throwable),
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
        /**
         * This method is invoked by the XCTest when it discovered XCTestCase instance
         * that contains test method.
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

        // region Dynamic run methods creation

        /**
         * Creates and adds method to the metaclass with implementation block
         * that holds an XCTestCase instance to be run
         */
        private fun createRunMethod(selector: SEL) {
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

        /**
         * Disposes the implementation block for given selector
         */
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

        // TODO: Clean up those methods? When/where should this be invoked?
        private fun disposeRunMethods() {
            testMethodsNames().forEach {
                val selector = NSSelectorFromString(it)
                dispose(selector)
            }
        }

        @Suppress("UNUSED_PARAMETER")
        private fun runner(runner: XCTestCaseRunner, cmd: SEL) = runner.run()
        // endregion

        /**
         * Creates Test invocations for each test method to make them resolvable by the XCTest machinery.
         *
         * @see NSInvocation
         */
        override fun testInvocations(): List<NSInvocation> = testMethodsNames().map {
            val selector = NSSelectorFromString(it)
            createRunMethod(selector)
            this.instanceMethodSignatureForSelector(selector)?.let { signature ->
                // Those casts can never succeed ¯\_(ツ)_/¯
                @Suppress("CAST_NEVER_SUCCEEDS")
                val invocation = NSInvocation.invocationWithMethodSignature(signature as NSMethodSignature)
                invocation.setSelector(selector)
                invocation
            } ?: error("Not able to create NSInvocation for method $it")
        }
    }
}

private typealias SEL = COpaquePointer?

/**
 * This is a NSError-wrapper of Kotlin exception used to pass it through the XCTIssue.
 * See [NativeTestObserver] for the usage.
 */
internal class NSErrorWithKotlinException(val kotlinException: Throwable) : NSError(NSCocoaErrorDomain, NSValidationErrorMinimum, null)

/**
 * XCTest equivalent of K/N TestSuite.
 */
class XCTestSuiteRunner(val testSuite: TestSuite) : XCTestSuite(testSuite.name) {
    private val ignoredSuite: Boolean
        get() = testSuite.ignored || testSuite.testCases.all { it.value.ignored }

    override fun setUp() {
        if (!ignoredSuite) testSuite.doBeforeClass()
    }

    override fun tearDown() {
        if (!ignoredSuite) testSuite.doAfterClass()
    }
}
