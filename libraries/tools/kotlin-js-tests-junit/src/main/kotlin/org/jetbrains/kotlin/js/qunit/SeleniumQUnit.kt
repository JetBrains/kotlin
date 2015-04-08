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

val TIMEOUT: Long = 10000

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
        waitFor(TIMEOUT) {
            result = element.getAttribute("class") ?: "no result"
            !result.startsWith("run")
        }
        if ("pass" != result) {
            val testName = "${findTestName(element)} result: $result"
            val failMessages =
                try {
                    element.findElements(By.xpath("descendant::li[@class!='pass']/*[@class = 'test-message']"))
                            .map { "$testName. ${it.getText()}" }
                } catch (e: Exception) {
                    listOf("test result for test case $testName")
                }

            for (message in failMessages)
                println("FAILED: $message")
            fail(failMessages.join("\n"))
        }
    }
}
