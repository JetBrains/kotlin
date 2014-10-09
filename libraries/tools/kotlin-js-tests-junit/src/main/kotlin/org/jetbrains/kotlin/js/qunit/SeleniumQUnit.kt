package org.jetbrains.kotlin.js.qunit

import kotlin.test.*
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement

/**
* Waits up to a *maxMills* time for a predicate to be true, sleeping for *sleepMillis*
* and retrying until the timeout fails
*/
public fun waitFor(maxMillis: Long, sleepMillis: Long = 100, predicate: () -> Boolean): Boolean {
    val end = System.currentTimeMillis() + maxMillis
    while (true) {
        if (predicate()) {
            return true
        }
        val now = System.currentTimeMillis()
        if (now >= end) break
        val delay = sleepMillis
        Thread.sleep(delay)
    }
    return false
}

val TIMEOUT: Long = 5000

/**
 * Helper class to find QUnit tests using Selenium
 */
public class SeleniumQUnit(val driver: WebDriver) {

    /**
     * Returns all the test cases found in the current driver's page
     */
    public fun findTests(): List<WebElement> {
        val qunitContainer = driver.findElement(By.id("qunit"))!!

        val success = waitFor(TIMEOUT) {
            qunitContainer.getAttribute("class") == "done"
        }
        assertTrue(success, "Tests timed out after $TIMEOUT milliseconds.")

        var resultsElement = driver.findElement(By.id("qunit-tests"))
        assertNotNull(resultsElement, "No qunit test elements could be found in ${driver.getCurrentUrl()}")

        return resultsElement!!.findElements(By.xpath("li"))?.filterNotNull() ?: arrayListOf<WebElement>()
    }

    public fun findTestName(element: WebElement): String {
        fun defaultName(name: String?) = name ?: "unknown test name for $element"
        try {
            val testNameElement = element.findElement(By.xpath("descendant::*[@class = 'test-name']"))
            return defaultName(testNameElement!!.getText())
        } catch (e: Exception) {
            return defaultName(element.getAttribute("id"))
        }
    }

    public fun runTest(element: WebElement): Unit {
        var result: String = ""
        waitFor(5000) {
            result = element.getAttribute("class") ?: "no result"
            !result.startsWith("run")
        }
        if ("pass" != result) {
            var message: String? = null
            try {
                val messageElement = element.findElement(By.xpath("descendant::*[@class = 'test-message']"))!!
                message = messageElement.getText()
            } catch (e: Exception) {
                // ignore
            }
            val testName = "${findTestName(element)} result: $result"
            val fullMessage = if (message != null) {
                "$testName. $message"
            } else {
                "test result for test case $testName"
            }
            println("FAILED: $fullMessage")
            fail(fullMessage)
        }
    }
}
