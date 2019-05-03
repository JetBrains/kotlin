package org.jetbrains.kotlin.gradle.internal.testing.tcsmc

import jetbrains.buildServer.messages.serviceMessages.TestFinished
import jetbrains.buildServer.messages.serviceMessages.TestStarted
import jetbrains.buildServer.messages.serviceMessages.TestSuiteFinished
import jetbrains.buildServer.messages.serviceMessages.TestSuiteStarted
import org.junit.Test

class TestNameParsing: TCServiceMessagesClientTest() {
    @Test
    fun testFqn() {
        assertEvents("""
STARTED SUITE root // root
  STARTED TEST displayName: myMethod, classDisplayName: MyTest, className: my.company.product.MyTest, name: myMethod // root.my.company.product.MyTest.myMethod
  COMPLETED SUCCESS // root.my.company.product.MyTest.myMethod
COMPLETED SUCCESS // root
    """) {
            serviceMessage(TestStarted("my.company.product.MyTest.myMethod", false, null))
            serviceMessage(TestFinished("my.company.product.MyTest.myMethod", 1))
        }
    }

    @Test
    fun testSimpleClassName() {
        assertEvents("""
STARTED SUITE root // root
  STARTED TEST displayName: myMethod, classDisplayName: MyTest, className: MyTest, name: myMethod // root.MyTest.myMethod
  COMPLETED SUCCESS // root.MyTest.myMethod
COMPLETED SUCCESS // root
    """) {
            serviceMessage(TestStarted("MyTest.myMethod", false, null))
            serviceMessage(TestFinished("MyTest.myMethod", 1))
        }
    }

    @Test
    fun testParentSuite() {
        assertEvents("""
STARTED SUITE root // root
  STARTED SUITE MyTest // root.MyTest
    STARTED TEST displayName: myMethod, classDisplayName: MyTest, className: MyTest, name: myMethod // root.MyTest.myMethod
    COMPLETED SUCCESS // root.MyTest.myMethod
  COMPLETED SUCCESS // root.MyTest
COMPLETED SUCCESS // root
    """) {
            serviceMessage(TestSuiteStarted("MyTest"))
            serviceMessage(TestStarted("myMethod", false, null))
            serviceMessage(TestFinished("myMethod", 1))
            serviceMessage(TestSuiteFinished("MyTest"))
        }
    }

    @Test
    fun testAlone() {
        assertEvents("""
STARTED SUITE root // root
  STARTED TEST displayName: myMethod, classDisplayName: root, className: root, name: myMethod // root.myMethod
  COMPLETED SUCCESS // root.myMethod
COMPLETED SUCCESS // root
    """) {
            serviceMessage(TestStarted("myMethod", false, null))
            serviceMessage(TestFinished("myMethod", 1))
        }
    }
}