/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.internal

import org.junit.Test
import kotlin.test.assertEquals

class NodeJsStackTraceParserKtTest {
    @Test
    fun parseNodeJsStackTrace() {
        val parsed = parseNodeJsStackTrace(
            """
AssertionError: Expected value to be true.
    at AssertionError_init_0 (/Users/jetbrains/IdeaProjects/mpplib2/build/tmp/expandedArchives/kotlin-stdlib-js-1.3-SNAPSHOT.jar_730a1b227513cf16a9b639e009a985fc/kotlin/exceptions.kt:102:37)
    at DefaultJsAsserter.failWithMessage_0 (/Users/jetbrains/IdeaProjects/mpplib2/build/tmp/expandedArchives/kotlin-test-js-1.3-SNAPSHOT.jar_d60f1e6d0dd94843a03bf98a569bbb73/src/main/kotlin/kotlin/test/DefaultJsAsserter.kt:80:19)
    at DefaultJsAsserter.assertTrue_o10pc4${'$'} (/Users/jetbrains/IdeaProjects/mpplib2/build/tmp/expandedArchives/kotlin-test-js-1.3-SNAPSHOT.jar_d60f1e6d0dd94843a03bf98a569bbb73/src/main/kotlin/kotlin/test/DefaultJsAsserter.kt:60:13)
    at DefaultJsAsserter.assertTrue_4mavae${'$'} (/Users/jetbrains/IdeaProjects/mpplib2/build/tmp/expandedArchives/kotlin-test-js-1.3-SNAPSHOT.jar_d60f1e6d0dd94843a03bf98a569bbb73/src/main/kotlin/kotlin/test/DefaultJsAsserter.kt:67:9)
    at assertTrue_0 (/Users/jetbrains/IdeaProjects/mpplib2/build/tmp/expandedArchives/kotlin-test-js-1.3-SNAPSHOT.jar_d60f1e6d0dd94843a03bf98a569bbb73/Assertions.kt:36:21)
    at DeepPackageTest${'$'}Inner${'$'}Inner2${'$'}Inner3.testHello (/Users/jetbrains/IdeaProjects/mpplib2/src/commonTest/kotlin/sample/a/DeepPackageTest.kt:16:13)
    at /Users/jetbrains/IdeaProjects/mpplib2/build/js_test_node_modules/mpplib2_test.js:93:48
    at Object.fn [as test] (/Users/jetbrains/IdeaProjects/mpplib2/build/tmp/expandedArchives/src/KotlinTestRunner.ts:12:25)
    at Object.test (/Users/jetbrains/IdeaProjects/mpplib2/build/tmp/expandedArchives/src/KotlinTestTeamCityReporter.ts:80:28)
    at test (/Users/jetbrains/IdeaProjects/mpplib2/build/tmp/expandedArchives/kotlin-test-js-1.3-SNAPSHOT.jar_d60f1e6d0dd94843a03bf98a569bbb73/src/main/kotlin/kotlin/test/TestApi.kt:57:15)            """.trimIndent()
        ).toString()

        assertEquals(
            """
NodeJsStackTrace(
message="AssertionError: Expected value to be true.",
stacktrace=[
NodeJsStackTraceElement(className=AssertionError, methodName=init, fileName=/Users/jetbrains/IdeaProjects/mpplib2/build/tmp/expandedArchives/kotlin-stdlib-js-1.3-SNAPSHOT.jar_730a1b227513cf16a9b639e009a985fc/kotlin/exceptions.kt, lineNumber=102, colNumber=37)
NodeJsStackTraceElement(className=DefaultJsAsserter, methodName=failWithMessage, fileName=/Users/jetbrains/IdeaProjects/mpplib2/build/tmp/expandedArchives/kotlin-test-js-1.3-SNAPSHOT.jar_d60f1e6d0dd94843a03bf98a569bbb73/src/main/kotlin/kotlin/test/DefaultJsAsserter.kt, lineNumber=80, colNumber=19)
NodeJsStackTraceElement(className=DefaultJsAsserter, methodName=assertTrue, fileName=/Users/jetbrains/IdeaProjects/mpplib2/build/tmp/expandedArchives/kotlin-test-js-1.3-SNAPSHOT.jar_d60f1e6d0dd94843a03bf98a569bbb73/src/main/kotlin/kotlin/test/DefaultJsAsserter.kt, lineNumber=60, colNumber=13)
NodeJsStackTraceElement(className=DefaultJsAsserter, methodName=assertTrue, fileName=/Users/jetbrains/IdeaProjects/mpplib2/build/tmp/expandedArchives/kotlin-test-js-1.3-SNAPSHOT.jar_d60f1e6d0dd94843a03bf98a569bbb73/src/main/kotlin/kotlin/test/DefaultJsAsserter.kt, lineNumber=67, colNumber=9)
NodeJsStackTraceElement(className=null, methodName=assertTrue, fileName=/Users/jetbrains/IdeaProjects/mpplib2/build/tmp/expandedArchives/kotlin-test-js-1.3-SNAPSHOT.jar_d60f1e6d0dd94843a03bf98a569bbb73/Assertions.kt, lineNumber=36, colNumber=21)
NodeJsStackTraceElement(className=Inner3, methodName=testHello, fileName=/Users/jetbrains/IdeaProjects/mpplib2/src/commonTest/kotlin/sample/a/DeepPackageTest.kt, lineNumber=16, colNumber=13)
NodeJsStackTraceElement(className=null, methodName=null, fileName=/Users/jetbrains/IdeaProjects/mpplib2/build/js_test_node_modules/mpplib2_test.js, lineNumber=93, colNumber=48)
NodeJsStackTraceElement(className=Object, methodName=test, fileName=/Users/jetbrains/IdeaProjects/mpplib2/build/tmp/expandedArchives/src/KotlinTestRunner.ts, lineNumber=12, colNumber=25)
NodeJsStackTraceElement(className=Object, methodName=test, fileName=/Users/jetbrains/IdeaProjects/mpplib2/build/tmp/expandedArchives/src/KotlinTestTeamCityReporter.ts, lineNumber=80, colNumber=28)
NodeJsStackTraceElement(className=null, methodName=test, fileName=/Users/jetbrains/IdeaProjects/mpplib2/build/tmp/expandedArchives/kotlin-test-js-1.3-SNAPSHOT.jar_d60f1e6d0dd94843a03bf98a569bbb73/src/main/kotlin/kotlin/test/TestApi.kt, lineNumber=57, colNumber=15)
])
            """.trim(), parsed
        )
    }
}