package org.jetbrains.kotlin.gradle.internal.testing.tcsmc

import jetbrains.buildServer.messages.serviceMessages.TestFinished
import jetbrains.buildServer.messages.serviceMessages.TestStarted
import jetbrains.buildServer.messages.serviceMessages.TestSuiteFinished
import jetbrains.buildServer.messages.serviceMessages.TestSuiteStarted
import org.junit.Test

class SkipRoot: TCServiceMessagesClientTest() {
    @Test
    fun testSkipRoot() {
        skipRoots = true

        assertEvents("""
STARTED SUITE root // root
  STARTED SUITE MyTest // root.MyTest
    STARTED TEST displayName: myMethod, classDisplayName: MyTest, className: MyTest, name: myMethod // root.MyTest.myMethod
    COMPLETED SUCCESS // root.MyTest.myMethod
  COMPLETED SUCCESS // root.MyTest
COMPLETED SUCCESS // root
    """) {
            serviceMessage(TestSuiteStarted(""))
            serviceMessage(TestSuiteStarted("MyTest"))
            serviceMessage(TestStarted("myMethod", false, null))
            serviceMessage(TestFinished("myMethod", 1))
            serviceMessage(TestSuiteFinished("MyTest"))
            serviceMessage(TestSuiteFinished(""))
        }
    }
}