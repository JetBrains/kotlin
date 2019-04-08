/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.testing.tcsmc

import jetbrains.buildServer.messages.serviceMessages.TestFinished
import jetbrains.buildServer.messages.serviceMessages.TestStarted
import jetbrains.buildServer.messages.serviceMessages.TestSuiteFinished
import jetbrains.buildServer.messages.serviceMessages.TestSuiteStarted
import org.junit.Test

class TestFailureTest : TCServiceMessagesClientTest() {
    @Test
    fun testJs() {
        assertEvents(
            """
STARTED SUITE root // root
  STARTED SUITE  // root/
    STARTED TEST displayName: Test, classDisplayName: , className: , name: Test // root//Test
      FAILURE AssertionError: Expected value to be true.
    at AssertionError_init_0 (mpplib2/build/tmp/expandedArchives/kotlin-stdlib-js-1.3-SNAPSHOT.jar_730a1b227513cf16a9b639e009a985fc/kotlin/exceptions.kt:102:37)
    at DefaultJsAsserter.failWithMessage_0 (mpplib2/build/tmp/expandedArchives/kotlin-test-js-1.3-SNAPSHOT.jar_d60f1e6d0dd94843a03bf98a569bbb73/src/main/kotlin/kotlin/test/DefaultJsAsserter.kt:80:19)
    at DefaultJsAsserter.assertTrue_o10pc4${'$'} (mpplib2/build/tmp/expandedArchives/kotlin-test-js-1.3-SNAPSHOT.jar_d60f1e6d0dd94843a03bf98a569bbb73/src/main/kotlin/kotlin/test/DefaultJsAsserter.kt:60:13)
    at DefaultJsAsserter.assertTrue_4mavae${'$'} (mpplib2/build/tmp/expandedArchives/kotlin-test-js-1.3-SNAPSHOT.jar_d60f1e6d0dd94843a03bf98a569bbb73/src/main/kotlin/kotlin/test/DefaultJsAsserter.kt:67:9)
    at assertTrue_0 (mpplib2/build/tmp/expandedArchives/kotlin-test-js-1.3-SNAPSHOT.jar_d60f1e6d0dd94843a03bf98a569bbb73/Assertions.kt:36:21)
    at SampleTestsJS.testHello (mpplib2/src/jsTest/kotlin/sample/SampleTestsJS.kt:9:9)
    at mpplib2/build/js_test_node_modules/mpplib2_test.js:59:38
    at Object.fn [as test] (mpplib2/build/tmp/expandedArchives/src/KotlinTestRunner.ts:12:25)
    at Object.test (mpplib2/build/tmp/expandedArchives/src/KotlinTestTeamCityReporter.ts:80:28)
    at test (mpplib2/build/tmp/expandedArchives/kotlin-test-js-1.3-SNAPSHOT.jar_d60f1e6d0dd94843a03bf98a569bbb73/src/main/kotlin/kotlin/test/TestApi.kt:57:15) // root//Test
    COMPLETED FAILURE // root//Test
  COMPLETED FAILURE // root/
COMPLETED FAILURE // root
        """
        ) {
            serviceMessage(TestSuiteStarted(""))
            serviceMessage(TestStarted("Test", false, null))
            serviceMessage(
                "testFailed",
                mapOf(
                    "name" to "Test",
                    "message" to "Expected value to be true",
                    "details" to """AssertionError: Expected value to be true.
    at AssertionError_init_0 (mpplib2/build/tmp/expandedArchives/kotlin-stdlib-js-1.3-SNAPSHOT.jar_730a1b227513cf16a9b639e009a985fc/kotlin/exceptions.kt:102:37)
    at DefaultJsAsserter.failWithMessage_0 (mpplib2/build/tmp/expandedArchives/kotlin-test-js-1.3-SNAPSHOT.jar_d60f1e6d0dd94843a03bf98a569bbb73/src/main/kotlin/kotlin/test/DefaultJsAsserter.kt:80:19)
    at DefaultJsAsserter.assertTrue_o10pc4$ (mpplib2/build/tmp/expandedArchives/kotlin-test-js-1.3-SNAPSHOT.jar_d60f1e6d0dd94843a03bf98a569bbb73/src/main/kotlin/kotlin/test/DefaultJsAsserter.kt:60:13)
    at DefaultJsAsserter.assertTrue_4mavae$ (mpplib2/build/tmp/expandedArchives/kotlin-test-js-1.3-SNAPSHOT.jar_d60f1e6d0dd94843a03bf98a569bbb73/src/main/kotlin/kotlin/test/DefaultJsAsserter.kt:67:9)
    at assertTrue_0 (mpplib2/build/tmp/expandedArchives/kotlin-test-js-1.3-SNAPSHOT.jar_d60f1e6d0dd94843a03bf98a569bbb73/Assertions.kt:36:21)
    at SampleTestsJS.testHello (mpplib2/src/jsTest/kotlin/sample/SampleTestsJS.kt:9:9)
    at mpplib2/build/js_test_node_modules/mpplib2_test.js:59:38
    at Object.fn [as test] (mpplib2/build/tmp/expandedArchives/src/KotlinTestRunner.ts:12:25)
    at Object.test (mpplib2/build/tmp/expandedArchives/src/KotlinTestTeamCityReporter.ts:80:28)
    at test (mpplib2/build/tmp/expandedArchives/kotlin-test-js-1.3-SNAPSHOT.jar_d60f1e6d0dd94843a03bf98a569bbb73/src/main/kotlin/kotlin/test/TestApi.kt:57:15)"""
                )
            )
            serviceMessage(TestFinished("Test", 0))
            serviceMessage(TestSuiteFinished(""))
        }
    }

    @Test
    fun testNative() {
        treatFailedTestOutputAsStacktrace = true

        assertEvents(
            """
STARTED SUITE root // root
  STARTED SUITE  // root/
    STARTED TEST displayName: Test, classDisplayName: , className: , name: Test // root//Test
      FAILURE Expected <7>, actual <42>
AssertionError: Expected value to be true.
    at AssertionError_init_0 (mpplib2/build/tmp/expandedArchives/kotlin-stdlib-js-1.3-SNAPSHOT.jar_730a1b227513cf16a9b639e009a985fc/kotlin/exceptions.kt:102:37)
    at DefaultJsAsserter.failWithMessage_0 (mpplib2/build/tmp/expandedArchives/kotlin-test-js-1.3-SNAPSHOT.jar_d60f1e6d0dd94843a03bf98a569bbb73/src/main/kotlin/kotlin/test/DefaultJsAsserter.kt:80:19)
    at DefaultJsAsserter.assertTrue_o10pc4${'$'} (mpplib2/build/tmp/expandedArchives/kotlin-test-js-1.3-SNAPSHOT.jar_d60f1e6d0dd94843a03bf98a569bbb73/src/main/kotlin/kotlin/test/DefaultJsAsserter.kt:60:13)
    at DefaultJsAsserter.assertTrue_4mavae${'$'} (mpplib2/build/tmp/expandedArchives/kotlin-test-js-1.3-SNAPSHOT.jar_d60f1e6d0dd94843a03bf98a569bbb73/src/main/kotlin/kotlin/test/DefaultJsAsserter.kt:67:9)
    at assertTrue_0 (mpplib2/build/tmp/expandedArchives/kotlin-test-js-1.3-SNAPSHOT.jar_d60f1e6d0dd94843a03bf98a569bbb73/Assertions.kt:36:21)
    at SampleTestsJS.testHello (mpplib2/src/jsTest/kotlin/sample/SampleTestsJS.kt:9:9)
    at mpplib2/build/js_test_node_modules/mpplib2_test.js:59:38
    at Object.fn [as test] (mpplib2/build/tmp/expandedArchives/src/KotlinTestRunner.ts:12:25)
    at Object.test (mpplib2/build/tmp/expandedArchives/src/KotlinTestTeamCityReporter.ts:80:28)
    at test (mpplib2/build/tmp/expandedArchives/kotlin-test-js-1.3-SNAPSHOT.jar_d60f1e6d0dd94843a03bf98a569bbb73/src/main/kotlin/kotlin/test/TestApi.kt:57:15)
 // root//Test
    COMPLETED FAILURE // root//Test
  COMPLETED FAILURE // root/
COMPLETED FAILURE // root
        """
        ) {
            serviceMessage(TestSuiteStarted(""))
            serviceMessage(TestStarted("Test", false, null))
            regularText(
                """AssertionError: Expected value to be true.
    at AssertionError_init_0 (mpplib2/build/tmp/expandedArchives/kotlin-stdlib-js-1.3-SNAPSHOT.jar_730a1b227513cf16a9b639e009a985fc/kotlin/exceptions.kt:102:37)
    at DefaultJsAsserter.failWithMessage_0 (mpplib2/build/tmp/expandedArchives/kotlin-test-js-1.3-SNAPSHOT.jar_d60f1e6d0dd94843a03bf98a569bbb73/src/main/kotlin/kotlin/test/DefaultJsAsserter.kt:80:19)
    at DefaultJsAsserter.assertTrue_o10pc4$ (mpplib2/build/tmp/expandedArchives/kotlin-test-js-1.3-SNAPSHOT.jar_d60f1e6d0dd94843a03bf98a569bbb73/src/main/kotlin/kotlin/test/DefaultJsAsserter.kt:60:13)
    at DefaultJsAsserter.assertTrue_4mavae$ (mpplib2/build/tmp/expandedArchives/kotlin-test-js-1.3-SNAPSHOT.jar_d60f1e6d0dd94843a03bf98a569bbb73/src/main/kotlin/kotlin/test/DefaultJsAsserter.kt:67:9)
    at assertTrue_0 (mpplib2/build/tmp/expandedArchives/kotlin-test-js-1.3-SNAPSHOT.jar_d60f1e6d0dd94843a03bf98a569bbb73/Assertions.kt:36:21)
    at SampleTestsJS.testHello (mpplib2/src/jsTest/kotlin/sample/SampleTestsJS.kt:9:9)
    at mpplib2/build/js_test_node_modules/mpplib2_test.js:59:38
    at Object.fn [as test] (mpplib2/build/tmp/expandedArchives/src/KotlinTestRunner.ts:12:25)
    at Object.test (mpplib2/build/tmp/expandedArchives/src/KotlinTestTeamCityReporter.ts:80:28)
    at test (mpplib2/build/tmp/expandedArchives/kotlin-test-js-1.3-SNAPSHOT.jar_d60f1e6d0dd94843a03bf98a569bbb73/src/main/kotlin/kotlin/test/TestApi.kt:57:15)"""
            )
            serviceMessage(
                "testFailed",
                mapOf(
                    "name" to "Test",
                    "message" to "Expected <7>, actual <42>"
                )
            )
            serviceMessage(TestFinished("Test", 0))
            serviceMessage(TestSuiteFinished(""))
        }
    }
}