package org.jetbrains.kotlin.gradle.internal.testing.tcsmc

import jetbrains.buildServer.messages.serviceMessages.TestFinished
import jetbrains.buildServer.messages.serviceMessages.TestStarted
import jetbrains.buildServer.messages.serviceMessages.TestSuiteFinished
import jetbrains.buildServer.messages.serviceMessages.TestSuiteStarted
import org.junit.Test

class ReplaceRoot : TCServiceMessagesClientTest() {
    @Test
    fun replaceRootWithFqn() {
        nameOfRootSuiteToReplace = "js"

        assertEvents("""
STARTED SUITE root // root
  STARTED SUITE js // root.js
    STARTED TEST displayName: myMethod, classDisplayName: MyTest, className: my.company.product.MyTest, name: myMethod // root.js.my.company.product.MyTest.myMethod
    COMPLETED SUCCESS // root.js.my.company.product.MyTest.myMethod
  COMPLETED SUCCESS // root.js
COMPLETED SUCCESS // root
    """) {
            serviceMessage(TestSuiteStarted(""))
            serviceMessage(TestStarted("my.company.product.MyTest.myMethod", false, null))
            serviceMessage(TestFinished("my.company.product.MyTest.myMethod", 1))
            serviceMessage(TestSuiteFinished(""))
        }
    }

    @Test
    fun replaceRootWithClassName() {
        nameOfRootSuiteToReplace = "js"

        assertEvents("""
STARTED SUITE root // root
  STARTED SUITE js // root.js
    STARTED TEST displayName: myMethod, classDisplayName: MyTest, className: MyTest, name: myMethod // root.js.MyTest.myMethod
    COMPLETED SUCCESS // root.js.MyTest.myMethod
  COMPLETED SUCCESS // root.js
COMPLETED SUCCESS // root
    """) {
            serviceMessage(TestSuiteStarted(""))
            serviceMessage(TestStarted("MyTest.myMethod", false, null))
            serviceMessage(TestFinished("MyTest.myMethod", 1))
            serviceMessage(TestSuiteFinished(""))
        }
    }

    @Test
    fun replaceRoot() {
        nameOfRootSuiteToReplace = "js"

        assertEvents("""
STARTED SUITE root // root
  STARTED SUITE js // root.js
    STARTED TEST displayName: myMethod, classDisplayName: js, className: js, name: myMethod // root.js.myMethod
    COMPLETED SUCCESS // root.js.myMethod
  COMPLETED SUCCESS // root.js
COMPLETED SUCCESS // root
    """) {
            serviceMessage(TestSuiteStarted(""))
            serviceMessage(TestStarted("myMethod", false, null))
            serviceMessage(TestFinished("myMethod", 1))
            serviceMessage(TestSuiteFinished(""))
        }
    }
}