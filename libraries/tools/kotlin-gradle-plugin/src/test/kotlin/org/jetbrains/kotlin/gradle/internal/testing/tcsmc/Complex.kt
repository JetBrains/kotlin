package org.jetbrains.kotlin.gradle.internal.testing.tcsmc

import jetbrains.buildServer.messages.serviceMessages.*
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClient
import org.junit.Test

class Complex: TCServiceMessagesClientTest() {
    @Test
    fun testComplexJs() {
        nameOfRootSuiteToReplace = "js"

        assertEvents("""
STARTED SUITE root // root
  STARTED SUITE js // root.js
    STARTED SUITE MyTest // root.js.MyTest
      STARTED TEST displayName: myTest1, classDisplayName: MyTest, className: my.company.product.MyTest, name: myTest1 // root.js.MyTest.my.company.product.MyTest.myTest1
      COMPLETED SUCCESS // root.js.MyTest.my.company.product.MyTest.myTest1
      STARTED TEST displayName: myTest2, classDisplayName: MyTest, className: my.company.product.MyTest, name: myTest2 // root.js.MyTest.my.company.product.MyTest.myTest2
        FAILURE testFailed // root.js.MyTest.my.company.product.MyTest.myTest2
      COMPLETED FAILURE // root.js.MyTest.my.company.product.MyTest.myTest2
      STARTED TEST displayName: myTest3, classDisplayName: MyTest, className: my.company.product.MyTest, name: myTest3 // root.js.MyTest.my.company.product.MyTest.myTest3
      COMPLETED SKIPPED // root.js.MyTest.my.company.product.MyTest.myTest3
      STARTED SUITE MyTestNested // root.js.MyTest.MyTestNested
        STARTED TEST displayName: myTest4, classDisplayName: MyTestNested, className: my.company.product.MyTest.MyTestNested, name: myTest4 // root.js.MyTest.MyTestNested.my.company.product.MyTest.MyTestNested.myTest4
        COMPLETED SUCCESS // root.js.MyTest.MyTestNested.my.company.product.MyTest.MyTestNested.myTest4
        STARTED TEST displayName: myTest5, classDisplayName: MyTestNested, className: my.company.product.MyTest.MyTestNested, name: myTest5 // root.js.MyTest.MyTestNested.my.company.product.MyTest.MyTestNested.myTest5
          FAILURE testFailed // root.js.MyTest.MyTestNested.my.company.product.MyTest.MyTestNested.myTest5
        COMPLETED FAILURE // root.js.MyTest.MyTestNested.my.company.product.MyTest.MyTestNested.myTest5
        STARTED TEST displayName: myTest6, classDisplayName: MyTestNested, className: my.company.product.MyTest.MyTestNested, name: myTest6 // root.js.MyTest.MyTestNested.my.company.product.MyTest.MyTestNested.myTest6
        COMPLETED SKIPPED // root.js.MyTest.MyTestNested.my.company.product.MyTest.MyTestNested.myTest6
        STARTED SUITE MyTestNestedNested // root.js.MyTest.MyTestNested.MyTestNestedNested
          STARTED TEST displayName: myTest7, classDisplayName: MyTestNestedNested, className: my.company.product.MyTest.MyTestNestedNested, name: myTest7 // root.js.MyTest.MyTestNested.MyTestNestedNested.my.company.product.MyTest.MyTestNestedNested.myTest7
          COMPLETED SUCCESS // root.js.MyTest.MyTestNested.MyTestNestedNested.my.company.product.MyTest.MyTestNestedNested.myTest7
          STARTED TEST displayName: myTest8, classDisplayName: MyTestNestedNested, className: my.company.product.MyTest.MyTestNestedNested, name: myTest8 // root.js.MyTest.MyTestNested.MyTestNestedNested.my.company.product.MyTest.MyTestNestedNested.myTest8
            FAILURE testFailed // root.js.MyTest.MyTestNested.MyTestNestedNested.my.company.product.MyTest.MyTestNestedNested.myTest8
          COMPLETED FAILURE // root.js.MyTest.MyTestNested.MyTestNestedNested.my.company.product.MyTest.MyTestNestedNested.myTest8
          STARTED TEST displayName: myTest9, classDisplayName: MyTestNestedNested, className: my.company.product.MyTest.MyTestNestedNested, name: myTest9 // root.js.MyTest.MyTestNested.MyTestNestedNested.my.company.product.MyTest.MyTestNestedNested.myTest9
          COMPLETED SKIPPED // root.js.MyTest.MyTestNested.MyTestNestedNested.my.company.product.MyTest.MyTestNestedNested.myTest9
        COMPLETED FAILURE // root.js.MyTest.MyTestNested.MyTestNestedNested
      COMPLETED FAILURE // root.js.MyTest.MyTestNested
    COMPLETED FAILURE // root.js.MyTest
    STARTED SUITE MyTest2 // root.js.MyTest2
      STARTED TEST displayName: myTest10, classDisplayName: MyTest2, className: my.company.product.MyTest2, name: myTest10 // root.js.MyTest2.my.company.product.MyTest2.myTest10
      COMPLETED SUCCESS // root.js.MyTest2.my.company.product.MyTest2.myTest10
      STARTED TEST displayName: myTest11, classDisplayName: MyTest2, className: my.company.product.MyTest2, name: myTest11 // root.js.MyTest2.my.company.product.MyTest2.myTest11
        FAILURE testFailed // root.js.MyTest2.my.company.product.MyTest2.myTest11
      COMPLETED FAILURE // root.js.MyTest2.my.company.product.MyTest2.myTest11
      STARTED TEST displayName: myTest12, classDisplayName: MyTest2, className: my.company.product.MyTest2, name: myTest12 // root.js.MyTest2.my.company.product.MyTest2.myTest12
      COMPLETED SKIPPED // root.js.MyTest2.my.company.product.MyTest2.myTest12
      STARTED SUITE MyTest2Nested // root.js.MyTest2.MyTest2Nested
        STARTED TEST displayName: myTest13, classDisplayName: MyTestNested, className: my.company.product.MyTest2.MyTestNested, name: myTest13 // root.js.MyTest2.MyTest2Nested.my.company.product.MyTest2.MyTestNested.myTest13
        COMPLETED SKIPPED // root.js.MyTest2.MyTest2Nested.my.company.product.MyTest2.MyTestNested.myTest13
        STARTED TEST displayName: myTest14, classDisplayName: MyTestNested, className: my.company.product.MyTest2.MyTestNested, name: myTest14 // root.js.MyTest2.MyTest2Nested.my.company.product.MyTest2.MyTestNested.myTest14
        COMPLETED SKIPPED // root.js.MyTest2.MyTest2Nested.my.company.product.MyTest2.MyTestNested.myTest14
        STARTED TEST displayName: myTest15, classDisplayName: MyTestNested, className: my.company.product.MyTest2.MyTestNested, name: myTest15 // root.js.MyTest2.MyTest2Nested.my.company.product.MyTest2.MyTestNested.myTest15
        COMPLETED SKIPPED // root.js.MyTest2.MyTest2Nested.my.company.product.MyTest2.MyTestNested.myTest15
        STARTED SUITE MyTest2NestedNested // root.js.MyTest2.MyTest2Nested.MyTest2NestedNested
          STARTED TEST displayName: myTest16, classDisplayName: MyTest2NestedNested, className: my.company.product.MyTest2.MyTest2NestedNested, name: myTest16 // root.js.MyTest2.MyTest2Nested.MyTest2NestedNested.my.company.product.MyTest2.MyTest2NestedNested.myTest16
          COMPLETED SKIPPED // root.js.MyTest2.MyTest2Nested.MyTest2NestedNested.my.company.product.MyTest2.MyTest2NestedNested.myTest16
          STARTED TEST displayName: myTest17, classDisplayName: MyTest2NestedNested, className: my.company.product.MyTest2.MyTest2NestedNested, name: myTest17 // root.js.MyTest2.MyTest2Nested.MyTest2NestedNested.my.company.product.MyTest2.MyTest2NestedNested.myTest17
          COMPLETED SKIPPED // root.js.MyTest2.MyTest2Nested.MyTest2NestedNested.my.company.product.MyTest2.MyTest2NestedNested.myTest17
          STARTED TEST displayName: myTest18, classDisplayName: MyTest2NestedNested, className: my.company.product.MyTest2.MyTest2NestedNested, name: myTest18 // root.js.MyTest2.MyTest2Nested.MyTest2NestedNested.my.company.product.MyTest2.MyTest2NestedNested.myTest18
          COMPLETED SKIPPED // root.js.MyTest2.MyTest2Nested.MyTest2NestedNested.my.company.product.MyTest2.MyTest2NestedNested.myTest18
        COMPLETED SKIPPED // root.js.MyTest2.MyTest2Nested.MyTest2NestedNested
      COMPLETED SKIPPED // root.js.MyTest2.MyTest2Nested
    COMPLETED FAILURE // root.js.MyTest2
  COMPLETED FAILURE // root.js
COMPLETED FAILURE // root
        """) {
            serviceMessage(TestSuiteStarted(""))
            complexFixture()
            serviceMessage(TestSuiteFinished(""))
        }
    }

    @Test
    fun testComplexIos() {
        nameOfRootSuiteToAppend = "ios"

        assertEvents("""
STARTED SUITE root // root
  STARTED SUITE ios // root.ios
    STARTED SUITE MyTest // root.ios.MyTest
      STARTED TEST displayName: myTest1, classDisplayName: MyTest, className: my.company.product.MyTest, name: myTest1 // root.ios.MyTest.my.company.product.MyTest.myTest1
      COMPLETED SUCCESS // root.ios.MyTest.my.company.product.MyTest.myTest1
      STARTED TEST displayName: myTest2, classDisplayName: MyTest, className: my.company.product.MyTest, name: myTest2 // root.ios.MyTest.my.company.product.MyTest.myTest2
        FAILURE testFailed // root.ios.MyTest.my.company.product.MyTest.myTest2
      COMPLETED FAILURE // root.ios.MyTest.my.company.product.MyTest.myTest2
      STARTED TEST displayName: myTest3, classDisplayName: MyTest, className: my.company.product.MyTest, name: myTest3 // root.ios.MyTest.my.company.product.MyTest.myTest3
      COMPLETED SKIPPED // root.ios.MyTest.my.company.product.MyTest.myTest3
      STARTED SUITE MyTestNested // root.ios.MyTest.MyTestNested
        STARTED TEST displayName: myTest4, classDisplayName: MyTestNested, className: my.company.product.MyTest.MyTestNested, name: myTest4 // root.ios.MyTest.MyTestNested.my.company.product.MyTest.MyTestNested.myTest4
        COMPLETED SUCCESS // root.ios.MyTest.MyTestNested.my.company.product.MyTest.MyTestNested.myTest4
        STARTED TEST displayName: myTest5, classDisplayName: MyTestNested, className: my.company.product.MyTest.MyTestNested, name: myTest5 // root.ios.MyTest.MyTestNested.my.company.product.MyTest.MyTestNested.myTest5
          FAILURE testFailed // root.ios.MyTest.MyTestNested.my.company.product.MyTest.MyTestNested.myTest5
        COMPLETED FAILURE // root.ios.MyTest.MyTestNested.my.company.product.MyTest.MyTestNested.myTest5
        STARTED TEST displayName: myTest6, classDisplayName: MyTestNested, className: my.company.product.MyTest.MyTestNested, name: myTest6 // root.ios.MyTest.MyTestNested.my.company.product.MyTest.MyTestNested.myTest6
        COMPLETED SKIPPED // root.ios.MyTest.MyTestNested.my.company.product.MyTest.MyTestNested.myTest6
        STARTED SUITE MyTestNestedNested // root.ios.MyTest.MyTestNested.MyTestNestedNested
          STARTED TEST displayName: myTest7, classDisplayName: MyTestNestedNested, className: my.company.product.MyTest.MyTestNestedNested, name: myTest7 // root.ios.MyTest.MyTestNested.MyTestNestedNested.my.company.product.MyTest.MyTestNestedNested.myTest7
          COMPLETED SUCCESS // root.ios.MyTest.MyTestNested.MyTestNestedNested.my.company.product.MyTest.MyTestNestedNested.myTest7
          STARTED TEST displayName: myTest8, classDisplayName: MyTestNestedNested, className: my.company.product.MyTest.MyTestNestedNested, name: myTest8 // root.ios.MyTest.MyTestNested.MyTestNestedNested.my.company.product.MyTest.MyTestNestedNested.myTest8
            FAILURE testFailed // root.ios.MyTest.MyTestNested.MyTestNestedNested.my.company.product.MyTest.MyTestNestedNested.myTest8
          COMPLETED FAILURE // root.ios.MyTest.MyTestNested.MyTestNestedNested.my.company.product.MyTest.MyTestNestedNested.myTest8
          STARTED TEST displayName: myTest9, classDisplayName: MyTestNestedNested, className: my.company.product.MyTest.MyTestNestedNested, name: myTest9 // root.ios.MyTest.MyTestNested.MyTestNestedNested.my.company.product.MyTest.MyTestNestedNested.myTest9
          COMPLETED SKIPPED // root.ios.MyTest.MyTestNested.MyTestNestedNested.my.company.product.MyTest.MyTestNestedNested.myTest9
        COMPLETED FAILURE // root.ios.MyTest.MyTestNested.MyTestNestedNested
      COMPLETED FAILURE // root.ios.MyTest.MyTestNested
    COMPLETED FAILURE // root.ios.MyTest
    STARTED SUITE MyTest2 // root.ios.MyTest2
      STARTED TEST displayName: myTest10, classDisplayName: MyTest2, className: my.company.product.MyTest2, name: myTest10 // root.ios.MyTest2.my.company.product.MyTest2.myTest10
      COMPLETED SUCCESS // root.ios.MyTest2.my.company.product.MyTest2.myTest10
      STARTED TEST displayName: myTest11, classDisplayName: MyTest2, className: my.company.product.MyTest2, name: myTest11 // root.ios.MyTest2.my.company.product.MyTest2.myTest11
        FAILURE testFailed // root.ios.MyTest2.my.company.product.MyTest2.myTest11
      COMPLETED FAILURE // root.ios.MyTest2.my.company.product.MyTest2.myTest11
      STARTED TEST displayName: myTest12, classDisplayName: MyTest2, className: my.company.product.MyTest2, name: myTest12 // root.ios.MyTest2.my.company.product.MyTest2.myTest12
      COMPLETED SKIPPED // root.ios.MyTest2.my.company.product.MyTest2.myTest12
      STARTED SUITE MyTest2Nested // root.ios.MyTest2.MyTest2Nested
        STARTED TEST displayName: myTest13, classDisplayName: MyTestNested, className: my.company.product.MyTest2.MyTestNested, name: myTest13 // root.ios.MyTest2.MyTest2Nested.my.company.product.MyTest2.MyTestNested.myTest13
        COMPLETED SKIPPED // root.ios.MyTest2.MyTest2Nested.my.company.product.MyTest2.MyTestNested.myTest13
        STARTED TEST displayName: myTest14, classDisplayName: MyTestNested, className: my.company.product.MyTest2.MyTestNested, name: myTest14 // root.ios.MyTest2.MyTest2Nested.my.company.product.MyTest2.MyTestNested.myTest14
        COMPLETED SKIPPED // root.ios.MyTest2.MyTest2Nested.my.company.product.MyTest2.MyTestNested.myTest14
        STARTED TEST displayName: myTest15, classDisplayName: MyTestNested, className: my.company.product.MyTest2.MyTestNested, name: myTest15 // root.ios.MyTest2.MyTest2Nested.my.company.product.MyTest2.MyTestNested.myTest15
        COMPLETED SKIPPED // root.ios.MyTest2.MyTest2Nested.my.company.product.MyTest2.MyTestNested.myTest15
        STARTED SUITE MyTest2NestedNested // root.ios.MyTest2.MyTest2Nested.MyTest2NestedNested
          STARTED TEST displayName: myTest16, classDisplayName: MyTest2NestedNested, className: my.company.product.MyTest2.MyTest2NestedNested, name: myTest16 // root.ios.MyTest2.MyTest2Nested.MyTest2NestedNested.my.company.product.MyTest2.MyTest2NestedNested.myTest16
          COMPLETED SKIPPED // root.ios.MyTest2.MyTest2Nested.MyTest2NestedNested.my.company.product.MyTest2.MyTest2NestedNested.myTest16
          STARTED TEST displayName: myTest17, classDisplayName: MyTest2NestedNested, className: my.company.product.MyTest2.MyTest2NestedNested, name: myTest17 // root.ios.MyTest2.MyTest2Nested.MyTest2NestedNested.my.company.product.MyTest2.MyTest2NestedNested.myTest17
          COMPLETED SKIPPED // root.ios.MyTest2.MyTest2Nested.MyTest2NestedNested.my.company.product.MyTest2.MyTest2NestedNested.myTest17
          STARTED TEST displayName: myTest18, classDisplayName: MyTest2NestedNested, className: my.company.product.MyTest2.MyTest2NestedNested, name: myTest18 // root.ios.MyTest2.MyTest2Nested.MyTest2NestedNested.my.company.product.MyTest2.MyTest2NestedNested.myTest18
          COMPLETED SKIPPED // root.ios.MyTest2.MyTest2Nested.MyTest2NestedNested.my.company.product.MyTest2.MyTest2NestedNested.myTest18
        COMPLETED SKIPPED // root.ios.MyTest2.MyTest2Nested.MyTest2NestedNested
      COMPLETED SKIPPED // root.ios.MyTest2.MyTest2Nested
    COMPLETED FAILURE // root.ios.MyTest2
  COMPLETED FAILURE // root.ios
COMPLETED FAILURE // root
        """) {
            complexFixture()
        }
    }

    private fun TCServiceMessagesClient.complexFixture() {
        serviceMessage(TestSuiteStarted("MyTest"))

        serviceMessage(TestStarted("my.company.product.MyTest.myTest1", false, null))
        serviceMessage(TestFinished("my.company.product.MyTest.myTest1", 1))
        serviceMessage(TestStarted("my.company.product.MyTest.myTest2", false, null))
        serviceMessage(TestFailed("my.company.product.MyTest.myTest2", null))
        serviceMessage(TestFinished("my.company.product.MyTest.myTest2", 1))
        serviceMessage(TestIgnored("my.company.product.MyTest.myTest3", ""))

        serviceMessage(TestSuiteStarted("MyTestNested"))
        serviceMessage(TestStarted("my.company.product.MyTest.MyTestNested.myTest4", false, null))
        serviceMessage(TestFinished("my.company.product.MyTest.MyTestNested.myTest4", 1))
        serviceMessage(TestStarted("my.company.product.MyTest.MyTestNested.myTest5", false, null))
        serviceMessage(TestFailed("my.company.product.MyTest.MyTestNested.myTest5", null))
        serviceMessage(TestFinished("my.company.product.MyTest.MyTestNested.myTest5", 1))
        serviceMessage(TestIgnored("my.company.product.MyTest.MyTestNested.myTest6", ""))

        serviceMessage(TestSuiteStarted("MyTestNestedNested"))
        serviceMessage(TestStarted("my.company.product.MyTest.MyTestNestedNested.myTest7", false, null))
        serviceMessage(TestFinished("my.company.product.MyTest.MyTestNestedNested.myTest7", 1))
        serviceMessage(TestStarted("my.company.product.MyTest.MyTestNestedNested.myTest8", false, null))
        serviceMessage(TestFailed("my.company.product.MyTest.MyTestNestedNested.myTest8", null))
        serviceMessage(TestFinished("my.company.product.MyTest.MyTestNestedNested.myTest8", 1))
        serviceMessage(TestIgnored("my.company.product.MyTest.MyTestNestedNested.myTest9", ""))
        serviceMessage(TestSuiteFinished("MyTestNestedNested"))

        serviceMessage(TestSuiteFinished("MyTestNested"))

        serviceMessage(TestSuiteFinished("MyTest"))

        serviceMessage(TestSuiteStarted("MyTest2"))

        serviceMessage(TestStarted("my.company.product.MyTest2.myTest10", false, null))
        serviceMessage(TestFinished("my.company.product.MyTest2.myTest10", 1))
        serviceMessage(TestStarted("my.company.product.MyTest2.myTest11", false, null))
        serviceMessage(TestFailed("my.company.product.MyTest2.myTest11", null))
        serviceMessage(TestFinished("my.company.product.MyTest2.myTest11", 1))
        serviceMessage(TestIgnored("my.company.product.MyTest2.myTest12", ""))

        serviceMessage(TestSuiteStarted("MyTest2Nested"))
        serviceMessage(TestIgnored("my.company.product.MyTest2.MyTestNested.myTest13", ""))
        serviceMessage(TestIgnored("my.company.product.MyTest2.MyTestNested.myTest14", ""))
        serviceMessage(TestIgnored("my.company.product.MyTest2.MyTestNested.myTest15", ""))

        serviceMessage(TestSuiteStarted("MyTest2NestedNested"))
        serviceMessage(TestIgnored("my.company.product.MyTest2.MyTest2NestedNested.myTest16", ""))
        serviceMessage(TestIgnored("my.company.product.MyTest2.MyTest2NestedNested.myTest17", ""))
        serviceMessage(TestIgnored("my.company.product.MyTest2.MyTest2NestedNested.myTest18", ""))
        serviceMessage(TestSuiteFinished("MyTest2NestedNested"))

        serviceMessage(TestSuiteFinished("MyTest2Nested"))

        serviceMessage(TestSuiteFinished("MyTest2"))
    }
}