package org.jetbrains.kotlin.js.qunit

import java.util.List
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
        val delta = end - now
        val delay = sleepMillis
        Thread.sleep(delay)
    }
    return false
}

/**
 * Helper class to find QUnit tests using Selenium
 */
public class SeleniumQUnit(val driver: WebDriver) {

    /**
     * Returns all the test cases found in the current driver's page
     */
    public fun findTests(): List<WebElement> {
        var resultsElement: WebElement? = null
        waitFor(5000) {
            resultsElement = driver.findElement(By.id("qunit-tests"))
            resultsElement != null
        }
        assertNotNull(resultsElement, "No qunit test elements could be found in ${driver.getCurrentUrl()}")
        return resultsElement!!.findElements(By.tagName("li")).filterNotNull()
    }

    public fun findTestName(element: WebElement): String {
        return element.getAttribute("id") ?: "unknown test name for $element"
    }

    public fun runTest(element: WebElement): Unit {
        var result: String = ""
        waitFor(5000) {
            result = element.getAttribute("class") ?: "no result"
            !result.startsWith("run")
        }
        assertEquals("pass", result, "test result for test case ${findTestName(element)}")
    }
}