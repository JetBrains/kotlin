/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

import kotlinx.cinterop.*
import kotlin.native.internal.test.*
import platform.Foundation.NSInvocation
import platform.Foundation.NSBundle
import platform.Foundation.NSStringFromSelector
import platform.XCTest.*
import platform.objc.*

// Top level suite name used to hold all Native tests
internal const val TOP_LEVEL_SUITE = "Kotlin/Native test suite"

// Name of the key that contains arguments used to set [TestSettings]
private const val TEST_ARGUMENTS_KEY = "KotlinNativeTestArgs"

/**
 * Stores current settings with the filtered test suites, loggers, and listeners.
 * Test settings should be initialized by the setup method.
 */
private lateinit var testSettings: TestSettings

/**
 * This is an entry-point of XCTestSuites and XCTestCases generation.
 * Function returns the XCTest's top level TestSuite that holds all the test cases
 * with K/N tests.
 * This test suite can be run by either native launcher compiled to bundle or
 * by the other test suite (e.g. compiled as a framework).
 */
@Suppress("unused")
@kotlin.native.internal.ExportForCppRuntime("Konan_create_testSuite")
internal fun setupXCTestSuite(): XCTestSuite {
    val nativeTestSuite = XCTestSuite.testSuiteWithName(TOP_LEVEL_SUITE)

    // Initialize settings with the given args
    val args = testArguments(TEST_ARGUMENTS_KEY)
    testSettings = TestProcessor(GeneratedSuites.suites, args).process()

    check(::testSettings.isInitialized) {
        "Test settings wasn't set. Check provided arguments and test suites"
    }

    // Set test observer that will log test execution
    XCTestObservationCenter.sharedTestObservationCenter.addTestObserver(NativeTestObserver(testSettings))

    if (testSettings.runTests == true) {
        // Generate and add tests to the main suite
        testSettings.testSuites.generate().forEach {
            nativeTestSuite.addTest(it)
        }

        // Tests created (self-check)
        @Suppress("UNCHECKED_CAST")
        check(testSettings.testSuites.size == (nativeTestSuite.tests as List<XCTest>).size) {
            "The amount of generated XCTest suites should be equal to Kotlin test suites"
        }
    }

    return nativeTestSuite
}

/**
 * Gets test arguments from the Info.plist using the provided key to create test settings.
 *
 * @param key a key used in the `Info.plist` file to pass test arguments
 */
private fun testArguments(key: String): Array<String> {
    // As we don't know which bundle we are, iterate through all of them
    val plistTestArgs = NSBundle.allBundles
            .mapNotNull {
                (it as? NSBundle)?.infoDictionary?.get(key)
            }.singleOrNull() as? String
    return plistTestArgs?.split(" ")
            ?.toTypedArray()
            ?: emptyArray<String>()
}

internal val testMethodsNames: List<String>
    get() = testSettings.testSuites.toList()
        .flatMap { testSuite ->
            testSuite.testCases.values.map { it.fullName }
        }

internal val TestCase.fullName get() = "${suite.name}.$name"

private fun Collection<TestSuite>.generate(): List<XCTestSuite> {
    val testInvocations = XCTestCaseWrapper.testInvocations()
    return this.map { suite ->
        val xcSuite = XCTestSuiteWrapper(suite)
        suite.testCases.values.map { testCase ->
            // Produce test case wrapper from the test invocation
            testInvocations.filter {
                it.selectorString() == testCase.fullName
            }.map { invocation ->
                XCTestCaseWrapper(invocation, testCase)
            }.single()
        }.forEach {
            // add test to its test suite wrappper
            xcSuite.addTest(it)
        }
        xcSuite
    }
}

private fun NSInvocation.selectorString() = NSStringFromSelector(selector)
