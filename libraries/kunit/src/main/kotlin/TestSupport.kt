package org.jetbrains.kotlin.junit

import junit.framework.TestCase

/**
 * Useful base class for test cases using the old JUnit 3 naming convention of functions
 * starting with "test*" as being a test case
 */
abstract class TestSupport() : TestCase() {
}
