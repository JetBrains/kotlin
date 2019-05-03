package org.jetbrains.kotlin.gradle.internal.testing.tcsmc

import jetbrains.buildServer.messages.serviceMessages.TestFinished
import jetbrains.buildServer.messages.serviceMessages.TestStarted
import jetbrains.buildServer.messages.serviceMessages.TestSuiteFinished
import jetbrains.buildServer.messages.serviceMessages.TestSuiteStarted
import org.junit.Test

class AppendLeaf : TCServiceMessagesClientTest() {
    @Test
    fun appendLeafWithFqn() {
        skipRoots = true
        nameOfLeafTestToAppend = "js"

        assertEvents("""
STARTED SUITE root // root
  STARTED SUITE myMethod // root.myMethod
    STARTED TEST displayName: myMethod.js, classDisplayName: MyTest, className: my.company.product.MyTest, name: myMethod // root.myMethod.my.company.product.MyTest.myMethod.js
    COMPLETED SUCCESS // root.myMethod.my.company.product.MyTest.myMethod.js
  COMPLETED SUCCESS // root.myMethod
COMPLETED SUCCESS // root
    """) {
            serviceMessage(TestSuiteStarted(""))
            serviceMessage(TestStarted("my.company.product.MyTest.myMethod", false, null))
            serviceMessage(TestFinished("my.company.product.MyTest.myMethod", 1))
            serviceMessage(TestSuiteFinished(""))
        }
    }

    @Test
    fun appendLeafWithClassName() {
        skipRoots = true
        nameOfLeafTestToAppend = "js"

        assertEvents("""
STARTED SUITE root // root
  STARTED SUITE myMethod // root.myMethod
    STARTED TEST displayName: myMethod.js, classDisplayName: MyTest, className: MyTest, name: myMethod // root.myMethod.MyTest.myMethod.js
    COMPLETED SUCCESS // root.myMethod.MyTest.myMethod.js
  COMPLETED SUCCESS // root.myMethod
COMPLETED SUCCESS // root
    """) {
            serviceMessage(TestSuiteStarted(""))
            serviceMessage(TestStarted("MyTest.myMethod", false, null))
            serviceMessage(TestFinished("MyTest.myMethod", 1))
            serviceMessage(TestSuiteFinished(""))
        }
    }

    @Test
    fun replaceRootWithSuite() {
        nameOfRootSuiteToReplace = "js"

        assertEvents("""
STARTED SUITE root // root
  STARTED SUITE js // root.js
    STARTED SUITE MyTest // root.js.MyTest
      STARTED TEST displayName: myMethod, classDisplayName: MyTest, className: MyTest, name: myMethod // root.js.MyTest.myMethod
      COMPLETED SUCCESS // root.js.MyTest.myMethod
    COMPLETED SUCCESS // root.js.MyTest
  COMPLETED SUCCESS // root.js
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

    @Test
    fun appendLeaf() {
        skipRoots = true
        nameOfLeafTestToAppend = "js"

        assertEvents("""
STARTED SUITE root // root
  STARTED SUITE myMethod // root.myMethod
    STARTED TEST displayName: myMethod.js, classDisplayName: , className: , name: myMethod // root.myMethod.myMethod.js
    COMPLETED SUCCESS // root.myMethod.myMethod.js
  COMPLETED SUCCESS // root.myMethod
COMPLETED SUCCESS // root
    """) {
            serviceMessage(TestSuiteStarted(""))
            serviceMessage(TestStarted("myMethod", false, null))
            serviceMessage(TestFinished("myMethod", 1))
            serviceMessage(TestSuiteFinished(""))
        }
    }

    @Test
    fun appendLeafWithSuite() {
        skipRoots = true
        nameOfLeafTestToAppend = "js"

        assertEvents("""
STARTED SUITE root // root
  STARTED SUITE MyTest // root.MyTest
    STARTED SUITE myMethod // root.MyTest.myMethod
      STARTED TEST displayName: myMethod.js, classDisplayName: MyTest, className: MyTest, name: myMethod // root.MyTest.myMethod.myMethod.js
      COMPLETED SUCCESS // root.MyTest.myMethod.myMethod.js
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