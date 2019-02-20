package org.jetbrains.kotlin.gradle.internal.testing.tcsmc

import jetbrains.buildServer.messages.serviceMessages.TestFinished
import jetbrains.buildServer.messages.serviceMessages.TestStarted
import org.junit.Test

class AppendRoot : TCServiceMessagesClientTest() {
    @Test
    fun testWithFqn() {
        nameOfRootSuiteToAppend = "ios"

        assertEvents("""
STARTED SUITE root // root
  STARTED SUITE ios // root.ios
    STARTED TEST displayName: myMethod, classDisplayName: MyTest, className: my.company.product.MyTest, name: myMethod // root.ios.my.company.product.MyTest.myMethod
    COMPLETED SUCCESS // root.ios.my.company.product.MyTest.myMethod
  COMPLETED SUCCESS // root.ios
COMPLETED SUCCESS // root
    """) {
            serviceMessage(TestStarted("my.company.product.MyTest.myMethod", false, null))
            serviceMessage(TestFinished("my.company.product.MyTest.myMethod", 1))
        }
    }

    @Test
    fun testWithClassName() {
        nameOfRootSuiteToAppend = "ios"

        assertEvents("""
STARTED SUITE root // root
  STARTED SUITE ios // root.ios
    STARTED TEST displayName: myMethod, classDisplayName: MyTest, className: MyTest, name: myMethod // root.ios.MyTest.myMethod
    COMPLETED SUCCESS // root.ios.MyTest.myMethod
  COMPLETED SUCCESS // root.ios
COMPLETED SUCCESS // root
    """) {
            serviceMessage(TestStarted("MyTest.myMethod", false, null))
            serviceMessage(TestFinished("MyTest.myMethod", 1))
        }
    }

    @Test
    fun testRoot() {
        nameOfRootSuiteToAppend = "ios"

        assertEvents("""
STARTED SUITE root // root
  STARTED SUITE ios // root.ios
    STARTED TEST displayName: myMethod, classDisplayName: ios, className: ios, name: myMethod // root.ios.myMethod
    COMPLETED SUCCESS // root.ios.myMethod
  COMPLETED SUCCESS // root.ios
COMPLETED SUCCESS // root
    """) {
            serviceMessage(TestStarted("myMethod", false, null))
            serviceMessage(TestFinished("myMethod", 1))
        }
    }
}