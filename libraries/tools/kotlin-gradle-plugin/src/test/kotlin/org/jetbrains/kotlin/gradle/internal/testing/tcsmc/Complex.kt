package org.jetbrains.kotlin.gradle.internal.testing.tcsmc

import jetbrains.buildServer.messages.serviceMessages.*
import org.junit.Test

class Complex : TCServiceMessagesClientTest() {
    @Test
    fun testComplexJs() {
        rootNodeName = "jsTest"

        assertEvents(
            """
STARTED SUITE jsTest // jsTest
  STARTED SUITE my.company.product.MyTest // jsTest//my/company/product/MyTest
    STARTED TEST displayName: myTest1, classDisplayName: MyTest, className: my.company.product.MyTest, name: myTest1 // jsTest//my/company/product/MyTest/my.company.product.MyTest.myTest1
    COMPLETED SUCCESS // jsTest//my/company/product/MyTest/my.company.product.MyTest.myTest1
    STARTED TEST displayName: myTest2, classDisplayName: MyTest, className: my.company.product.MyTest, name: myTest2 // jsTest//my/company/product/MyTest/my.company.product.MyTest.myTest2
      FAILURE  // jsTest//my/company/product/MyTest/my.company.product.MyTest.myTest2
    COMPLETED FAILURE // jsTest//my/company/product/MyTest/my.company.product.MyTest.myTest2
    STARTED TEST displayName: myTest3, classDisplayName: MyTest, className: my.company.product.MyTest, name: myTest3 // jsTest//my/company/product/MyTest/my.company.product.MyTest.myTest3
    COMPLETED SKIPPED // jsTest//my/company/product/MyTest/my.company.product.MyTest.myTest3
    STARTED SUITE my.company.product.MyTest.MyTestNested // jsTest//my/company/product/MyTest/MyTestNested
      STARTED TEST displayName: myTest4, classDisplayName: MyTestNested, className: my.company.product.MyTest.MyTestNested, name: myTest4 // jsTest//my/company/product/MyTest/MyTestNested/my.company.product.MyTest.MyTestNested.myTest4
      COMPLETED SUCCESS // jsTest//my/company/product/MyTest/MyTestNested/my.company.product.MyTest.MyTestNested.myTest4
      STARTED TEST displayName: myTest5, classDisplayName: MyTestNested, className: my.company.product.MyTest.MyTestNested, name: myTest5 // jsTest//my/company/product/MyTest/MyTestNested/my.company.product.MyTest.MyTestNested.myTest5
        FAILURE  // jsTest//my/company/product/MyTest/MyTestNested/my.company.product.MyTest.MyTestNested.myTest5
      COMPLETED FAILURE // jsTest//my/company/product/MyTest/MyTestNested/my.company.product.MyTest.MyTestNested.myTest5
      STARTED TEST displayName: myTest6, classDisplayName: MyTestNested, className: my.company.product.MyTest.MyTestNested, name: myTest6 // jsTest//my/company/product/MyTest/MyTestNested/my.company.product.MyTest.MyTestNested.myTest6
      COMPLETED SKIPPED // jsTest//my/company/product/MyTest/MyTestNested/my.company.product.MyTest.MyTestNested.myTest6
      STARTED SUITE my.company.product.MyTest.MyTestNested.MyTestNestedNested // jsTest//my/company/product/MyTest/MyTestNested/MyTestNestedNested
        STARTED TEST displayName: myTest7, classDisplayName: MyTestNestedNested, className: my.company.product.MyTest.MyTestNestedNested, name: myTest7 // jsTest//my/company/product/MyTest/MyTestNested/MyTestNestedNested/my.company.product.MyTest.MyTestNestedNested.myTest7
        COMPLETED SUCCESS // jsTest//my/company/product/MyTest/MyTestNested/MyTestNestedNested/my.company.product.MyTest.MyTestNestedNested.myTest7
        STARTED TEST displayName: myTest8, classDisplayName: MyTestNestedNested, className: my.company.product.MyTest.MyTestNestedNested, name: myTest8 // jsTest//my/company/product/MyTest/MyTestNested/MyTestNestedNested/my.company.product.MyTest.MyTestNestedNested.myTest8
          FAILURE  // jsTest//my/company/product/MyTest/MyTestNested/MyTestNestedNested/my.company.product.MyTest.MyTestNestedNested.myTest8
        COMPLETED FAILURE // jsTest//my/company/product/MyTest/MyTestNested/MyTestNestedNested/my.company.product.MyTest.MyTestNestedNested.myTest8
        STARTED TEST displayName: myTest9, classDisplayName: MyTestNestedNested, className: my.company.product.MyTest.MyTestNestedNested, name: myTest9 // jsTest//my/company/product/MyTest/MyTestNested/MyTestNestedNested/my.company.product.MyTest.MyTestNestedNested.myTest9
        COMPLETED SKIPPED // jsTest//my/company/product/MyTest/MyTestNested/MyTestNestedNested/my.company.product.MyTest.MyTestNestedNested.myTest9
      COMPLETED FAILURE // jsTest//my/company/product/MyTest/MyTestNested/MyTestNestedNested
    COMPLETED FAILURE // jsTest//my/company/product/MyTest/MyTestNested
  COMPLETED FAILURE // jsTest//my/company/product/MyTest
  STARTED SUITE my.company.product.MyTest2 // jsTest//my/company/product/MyTest2
    STARTED TEST displayName: myTest10, classDisplayName: MyTest2, className: my.company.product.MyTest2, name: myTest10 // jsTest//my/company/product/MyTest2/my.company.product.MyTest2.myTest10
    COMPLETED SUCCESS // jsTest//my/company/product/MyTest2/my.company.product.MyTest2.myTest10
    STARTED TEST displayName: myTest11, classDisplayName: MyTest2, className: my.company.product.MyTest2, name: myTest11 // jsTest//my/company/product/MyTest2/my.company.product.MyTest2.myTest11
      FAILURE  // jsTest//my/company/product/MyTest2/my.company.product.MyTest2.myTest11
    COMPLETED FAILURE // jsTest//my/company/product/MyTest2/my.company.product.MyTest2.myTest11
    STARTED TEST displayName: myTest12, classDisplayName: MyTest2, className: my.company.product.MyTest2, name: myTest12 // jsTest//my/company/product/MyTest2/my.company.product.MyTest2.myTest12
    COMPLETED SKIPPED // jsTest//my/company/product/MyTest2/my.company.product.MyTest2.myTest12
    STARTED SUITE my.company.product.MyTest2.MyTest2Nested // jsTest//my/company/product/MyTest2/MyTest2Nested
      STARTED TEST displayName: myTest13, classDisplayName: MyTestNested, className: my.company.product.MyTest2.MyTestNested, name: myTest13 // jsTest//my/company/product/MyTest2/MyTest2Nested/my.company.product.MyTest2.MyTestNested.myTest13
      COMPLETED SKIPPED // jsTest//my/company/product/MyTest2/MyTest2Nested/my.company.product.MyTest2.MyTestNested.myTest13
      STARTED TEST displayName: myTest14, classDisplayName: MyTestNested, className: my.company.product.MyTest2.MyTestNested, name: myTest14 // jsTest//my/company/product/MyTest2/MyTest2Nested/my.company.product.MyTest2.MyTestNested.myTest14
      COMPLETED SKIPPED // jsTest//my/company/product/MyTest2/MyTest2Nested/my.company.product.MyTest2.MyTestNested.myTest14
      STARTED TEST displayName: myTest15, classDisplayName: MyTestNested, className: my.company.product.MyTest2.MyTestNested, name: myTest15 // jsTest//my/company/product/MyTest2/MyTest2Nested/my.company.product.MyTest2.MyTestNested.myTest15
      COMPLETED SKIPPED // jsTest//my/company/product/MyTest2/MyTest2Nested/my.company.product.MyTest2.MyTestNested.myTest15
      STARTED SUITE my.company.product.MyTest2.MyTest2Nested.MyTest2NestedNested // jsTest//my/company/product/MyTest2/MyTest2Nested/MyTest2NestedNested
        STARTED TEST displayName: myTest16, classDisplayName: MyTest2NestedNested, className: my.company.product.MyTest2.MyTest2NestedNested, name: myTest16 // jsTest//my/company/product/MyTest2/MyTest2Nested/MyTest2NestedNested/my.company.product.MyTest2.MyTest2NestedNested.myTest16
        COMPLETED SKIPPED // jsTest//my/company/product/MyTest2/MyTest2Nested/MyTest2NestedNested/my.company.product.MyTest2.MyTest2NestedNested.myTest16
        STARTED TEST displayName: myTest17, classDisplayName: MyTest2NestedNested, className: my.company.product.MyTest2.MyTest2NestedNested, name: myTest17 // jsTest//my/company/product/MyTest2/MyTest2Nested/MyTest2NestedNested/my.company.product.MyTest2.MyTest2NestedNested.myTest17
        COMPLETED SKIPPED // jsTest//my/company/product/MyTest2/MyTest2Nested/MyTest2NestedNested/my.company.product.MyTest2.MyTest2NestedNested.myTest17
        STARTED TEST displayName: myTest18, classDisplayName: MyTest2NestedNested, className: my.company.product.MyTest2.MyTest2NestedNested, name: myTest18 // jsTest//my/company/product/MyTest2/MyTest2Nested/MyTest2NestedNested/my.company.product.MyTest2.MyTest2NestedNested.myTest18
        COMPLETED SKIPPED // jsTest//my/company/product/MyTest2/MyTest2Nested/MyTest2NestedNested/my.company.product.MyTest2.MyTest2NestedNested.myTest18
      COMPLETED SKIPPED // jsTest//my/company/product/MyTest2/MyTest2Nested/MyTest2NestedNested
    COMPLETED SKIPPED // jsTest//my/company/product/MyTest2/MyTest2Nested
  COMPLETED FAILURE // jsTest//my/company/product/MyTest2
COMPLETED FAILURE // jsTest
            """.trimIndent()
        ) {
            serviceMessage(TestSuiteStarted(""))
            serviceMessage(TestSuiteStarted("my"))
            serviceMessage(TestSuiteStarted("company"))
            serviceMessage(TestSuiteStarted("product"))

            /**/serviceMessage(TestSuiteStarted("MyTest"))
            /**/serviceMessage(TestStarted("my.company.product.MyTest.myTest1", false, null))
            /**/serviceMessage(TestFinished("my.company.product.MyTest.myTest1", 1))
            /**/serviceMessage(TestStarted("my.company.product.MyTest.myTest2", false, null))
            /**/serviceMessage(TestFailed("my.company.product.MyTest.myTest2", null))
            /**/serviceMessage(TestFinished("my.company.product.MyTest.myTest2", 1))
            /**/serviceMessage(TestIgnored("my.company.product.MyTest.myTest3", ""))
            /**/serviceMessage(TestSuiteStarted("MyTestNested"))
            /**//**/serviceMessage(TestStarted("my.company.product.MyTest.MyTestNested.myTest4", false, null))
            /**//**/serviceMessage(TestFinished("my.company.product.MyTest.MyTestNested.myTest4", 1))
            /**//**/serviceMessage(TestStarted("my.company.product.MyTest.MyTestNested.myTest5", false, null))
            /**//**/serviceMessage(TestFailed("my.company.product.MyTest.MyTestNested.myTest5", null))
            /**//**/serviceMessage(TestFinished("my.company.product.MyTest.MyTestNested.myTest5", 1))
            /**//**/serviceMessage(TestIgnored("my.company.product.MyTest.MyTestNested.myTest6", ""))
            /**//**/serviceMessage(TestSuiteStarted("MyTestNestedNested"))
            /**//**//**/serviceMessage(TestStarted("my.company.product.MyTest.MyTestNestedNested.myTest7", false, null))
            /**//**//**/serviceMessage(TestFinished("my.company.product.MyTest.MyTestNestedNested.myTest7", 1))
            /**//**//**/serviceMessage(TestStarted("my.company.product.MyTest.MyTestNestedNested.myTest8", false, null))
            /**//**//**/serviceMessage(TestFailed("my.company.product.MyTest.MyTestNestedNested.myTest8", null))
            /**//**//**/serviceMessage(TestFinished("my.company.product.MyTest.MyTestNestedNested.myTest8", 1))
            /**//**//**/serviceMessage(TestIgnored("my.company.product.MyTest.MyTestNestedNested.myTest9", ""))
            /**//**/serviceMessage(TestSuiteFinished("MyTestNestedNested"))
            /**/serviceMessage(TestSuiteFinished("MyTestNested"))
            serviceMessage(TestSuiteFinished("MyTest"))

            serviceMessage(TestSuiteStarted("MyTest2"))
            /**/serviceMessage(TestStarted("my.company.product.MyTest2.myTest10", false, null))
            /**/serviceMessage(TestFinished("my.company.product.MyTest2.myTest10", 1))
            /**/serviceMessage(TestStarted("my.company.product.MyTest2.myTest11", false, null))
            /**/serviceMessage(TestFailed("my.company.product.MyTest2.myTest11", null))
            /**/serviceMessage(TestFinished("my.company.product.MyTest2.myTest11", 1))
            /**/serviceMessage(TestIgnored("my.company.product.MyTest2.myTest12", ""))
            /**/serviceMessage(TestSuiteStarted("MyTest2Nested"))
            /**//**/serviceMessage(TestIgnored("my.company.product.MyTest2.MyTestNested.myTest13", ""))
            /**//**/serviceMessage(TestIgnored("my.company.product.MyTest2.MyTestNested.myTest14", ""))
            /**//**/serviceMessage(TestIgnored("my.company.product.MyTest2.MyTestNested.myTest15", ""))
            /**//**/serviceMessage(TestSuiteStarted("MyTest2NestedNested"))
            /**//**//**/serviceMessage(TestIgnored("my.company.product.MyTest2.MyTest2NestedNested.myTest16", ""))
            /**//**//**/serviceMessage(TestIgnored("my.company.product.MyTest2.MyTest2NestedNested.myTest17", ""))
            /**//**//**/serviceMessage(TestIgnored("my.company.product.MyTest2.MyTest2NestedNested.myTest18", ""))
            /**//**/serviceMessage(TestSuiteFinished("MyTest2NestedNested"))
            /**/serviceMessage(TestSuiteFinished("MyTest2Nested"))
            serviceMessage(TestSuiteFinished("MyTest2"))

            serviceMessage(TestSuiteFinished("product"))
            serviceMessage(TestSuiteFinished("company"))
            serviceMessage(TestSuiteFinished("my"))
            serviceMessage(TestSuiteFinished(""))
        }
    }

    @Test
    fun testComplexIos() {
        rootNodeName = "iosTest"

        assertEvents(
            """
STARTED SUITE iosTest // iosTest
  STARTED SUITE my.company.product.MyTest // iosTest/my.company.product.MyTest
    STARTED TEST displayName: myTest1, classDisplayName: MyTest, className: my.company.product.MyTest, name: myTest1 // iosTest/my.company.product.MyTest/my.company.product.MyTest.myTest1
    COMPLETED SUCCESS // iosTest/my.company.product.MyTest/my.company.product.MyTest.myTest1
    STARTED TEST displayName: myTest2, classDisplayName: MyTest, className: my.company.product.MyTest, name: myTest2 // iosTest/my.company.product.MyTest/my.company.product.MyTest.myTest2
      FAILURE  // iosTest/my.company.product.MyTest/my.company.product.MyTest.myTest2
    COMPLETED FAILURE // iosTest/my.company.product.MyTest/my.company.product.MyTest.myTest2
    STARTED TEST displayName: myTest3, classDisplayName: MyTest, className: my.company.product.MyTest, name: myTest3 // iosTest/my.company.product.MyTest/my.company.product.MyTest.myTest3
    COMPLETED SKIPPED // iosTest/my.company.product.MyTest/my.company.product.MyTest.myTest3
    STARTED SUITE my.company.product.MyTest.my.company.product.MyTest.MyTestNested // iosTest/my.company.product.MyTest/my.company.product.MyTest.MyTestNested
      STARTED TEST displayName: myTest4, classDisplayName: MyTestNested, className: my.company.product.MyTest.MyTestNested, name: myTest4 // iosTest/my.company.product.MyTest/my.company.product.MyTest.MyTestNested/my.company.product.MyTest.MyTestNested.myTest4
      COMPLETED SUCCESS // iosTest/my.company.product.MyTest/my.company.product.MyTest.MyTestNested/my.company.product.MyTest.MyTestNested.myTest4
      STARTED TEST displayName: myTest5, classDisplayName: MyTestNested, className: my.company.product.MyTest.MyTestNested, name: myTest5 // iosTest/my.company.product.MyTest/my.company.product.MyTest.MyTestNested/my.company.product.MyTest.MyTestNested.myTest5
        FAILURE  // iosTest/my.company.product.MyTest/my.company.product.MyTest.MyTestNested/my.company.product.MyTest.MyTestNested.myTest5
      COMPLETED FAILURE // iosTest/my.company.product.MyTest/my.company.product.MyTest.MyTestNested/my.company.product.MyTest.MyTestNested.myTest5
      STARTED TEST displayName: myTest6, classDisplayName: MyTestNested, className: my.company.product.MyTest.MyTestNested, name: myTest6 // iosTest/my.company.product.MyTest/my.company.product.MyTest.MyTestNested/my.company.product.MyTest.MyTestNested.myTest6
      COMPLETED SKIPPED // iosTest/my.company.product.MyTest/my.company.product.MyTest.MyTestNested/my.company.product.MyTest.MyTestNested.myTest6
      STARTED SUITE my.company.product.MyTest.my.company.product.MyTest.MyTestNested.my.company.product.MyTest.MyTestNested.MyTestNestedNested // iosTest/my.company.product.MyTest/my.company.product.MyTest.MyTestNested/my.company.product.MyTest.MyTestNested.MyTestNestedNested
        STARTED TEST displayName: myTest7, classDisplayName: MyTestNestedNested, className: my.company.product.MyTest.MyTestNestedNested, name: myTest7 // iosTest/my.company.product.MyTest/my.company.product.MyTest.MyTestNested/my.company.product.MyTest.MyTestNested.MyTestNestedNested/my.company.product.MyTest.MyTestNestedNested.myTest7
        COMPLETED SUCCESS // iosTest/my.company.product.MyTest/my.company.product.MyTest.MyTestNested/my.company.product.MyTest.MyTestNested.MyTestNestedNested/my.company.product.MyTest.MyTestNestedNested.myTest7
        STARTED TEST displayName: myTest8, classDisplayName: MyTestNestedNested, className: my.company.product.MyTest.MyTestNestedNested, name: myTest8 // iosTest/my.company.product.MyTest/my.company.product.MyTest.MyTestNested/my.company.product.MyTest.MyTestNested.MyTestNestedNested/my.company.product.MyTest.MyTestNestedNested.myTest8
          FAILURE  // iosTest/my.company.product.MyTest/my.company.product.MyTest.MyTestNested/my.company.product.MyTest.MyTestNested.MyTestNestedNested/my.company.product.MyTest.MyTestNestedNested.myTest8
        COMPLETED FAILURE // iosTest/my.company.product.MyTest/my.company.product.MyTest.MyTestNested/my.company.product.MyTest.MyTestNested.MyTestNestedNested/my.company.product.MyTest.MyTestNestedNested.myTest8
        STARTED TEST displayName: myTest9, classDisplayName: MyTestNestedNested, className: my.company.product.MyTest.MyTestNestedNested, name: myTest9 // iosTest/my.company.product.MyTest/my.company.product.MyTest.MyTestNested/my.company.product.MyTest.MyTestNested.MyTestNestedNested/my.company.product.MyTest.MyTestNestedNested.myTest9
        COMPLETED SKIPPED // iosTest/my.company.product.MyTest/my.company.product.MyTest.MyTestNested/my.company.product.MyTest.MyTestNested.MyTestNestedNested/my.company.product.MyTest.MyTestNestedNested.myTest9
      COMPLETED FAILURE // iosTest/my.company.product.MyTest/my.company.product.MyTest.MyTestNested/my.company.product.MyTest.MyTestNested.MyTestNestedNested
    COMPLETED FAILURE // iosTest/my.company.product.MyTest/my.company.product.MyTest.MyTestNested
  COMPLETED FAILURE // iosTest/my.company.product.MyTest
COMPLETED FAILURE // iosTest
            """.trimIndent()
        ) {
            serviceMessage(TestSuiteStarted("my.company.product.MyTest"))
            /**/serviceMessage(TestStarted("my.company.product.MyTest.myTest1", false, null))
            /**/serviceMessage(TestFinished("my.company.product.MyTest.myTest1", 1))
            /**/serviceMessage(TestStarted("my.company.product.MyTest.myTest2", false, null))
            /**/serviceMessage(TestFailed("my.company.product.MyTest.myTest2", null))
            /**/serviceMessage(TestFinished("my.company.product.MyTest.myTest2", 1))
            /**/serviceMessage(TestIgnored("my.company.product.MyTest.myTest3", ""))
            /**/serviceMessage(TestSuiteStarted("my.company.product.MyTest.MyTestNested"))
            /**//**/serviceMessage(TestStarted("my.company.product.MyTest.MyTestNested.myTest4", false, null))
            /**//**/serviceMessage(TestFinished("my.company.product.MyTest.MyTestNested.myTest4", 1))
            /**//**/serviceMessage(TestStarted("my.company.product.MyTest.MyTestNested.myTest5", false, null))
            /**//**/serviceMessage(TestFailed("my.company.product.MyTest.MyTestNested.myTest5", null))
            /**//**/serviceMessage(TestFinished("my.company.product.MyTest.MyTestNested.myTest5", 1))
            /**//**/serviceMessage(TestIgnored("my.company.product.MyTest.MyTestNested.myTest6", ""))
            /**//**/serviceMessage(TestSuiteStarted("my.company.product.MyTest.MyTestNested.MyTestNestedNested"))
            /**//**//**/serviceMessage(TestStarted("my.company.product.MyTest.MyTestNestedNested.myTest7", false, null))
            /**//**//**/serviceMessage(TestFinished("my.company.product.MyTest.MyTestNestedNested.myTest7", 1))
            /**//**//**/serviceMessage(TestStarted("my.company.product.MyTest.MyTestNestedNested.myTest8", false, null))
            /**//**//**/serviceMessage(TestFailed("my.company.product.MyTest.MyTestNestedNested.myTest8", null))
            /**//**//**/serviceMessage(TestFinished("my.company.product.MyTest.MyTestNestedNested.myTest8", 1))
            /**//**//**/serviceMessage(TestIgnored("my.company.product.MyTest.MyTestNestedNested.myTest9", ""))
            /**//**/serviceMessage(TestSuiteFinished("my.company.product.MyTest.MyTestNested.MyTestNestedNested"))
            /**/serviceMessage(TestSuiteFinished("my.company.product.MyTest.MyTestNested"))
            serviceMessage(TestSuiteFinished("my.company.product.MyTest"))
        }
    }

}