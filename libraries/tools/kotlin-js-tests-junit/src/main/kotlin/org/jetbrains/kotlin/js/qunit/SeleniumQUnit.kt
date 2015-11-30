package org.jetbrains.kotlin.js.qunit

import org.openqa.selenium.By
import org.openqa.selenium.TimeoutException
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.WebDriverWait
import kotlin.test.assertNotNull
import kotlin.test.fail

private val TIMEOUT_PER_INIT: Long = 10
private val TIMEOUT_PER_TEST: Long = 10

/**
 * Helper class to find QUnit tests using Selenium
 */
public class SeleniumQUnit(val driver: WebDriver) {

    /**
     * Returns all the test cases found in the current driver's page
     */
    public fun findTests(): List<WebElement> {
        val qunitContainer = driver.findElement(By.id("qunit"))!!

        waitWhileInit(qunitContainer)
        waitWhileTestsRunning(qunitContainer)

        var resultsElement = driver.findElement(By.id("qunit-tests"))
        assertNotNull(resultsElement, "No qunit test elements could be found in ${driver.currentUrl}")

        return resultsElement!!.findElements(By.xpath("li"))?.filterNotNull() ?: arrayListOf<WebElement>()
    }

    private fun waitWhileInit(qunitContainer: WebElement) {
        try {
            WebDriverWait(driver, TIMEOUT_PER_INIT).until { qunitContainer.classAttribute !in listOf(null, "running") }
        } catch (e: TimeoutException) {
            fail("Tests initialization timeout ($TIMEOUT_PER_INIT s)")
        }
    }

    private fun waitWhileTestsRunning(qunitContainer: WebElement) {
        val wait = WebDriverWait(driver, TIMEOUT_PER_TEST)

        var currentTest = qunitContainer.classAttribute
        try {
            while (currentTest != "done") {
                wait.until { qunitContainer.classAttribute != currentTest }
                currentTest = qunitContainer.classAttribute
            }
        } catch (e: TimeoutException) {
            fail("Test $currentTest timeout ($TIMEOUT_PER_TEST s)")
        }
    }

    public fun findTestName(element: WebElement): String {
        fun defaultName(name: String?) = name ?: "unknown test name for $element"
        try {
            val testNameElement = element.findElement(By.xpath("descendant::*[@class = 'test-name']"))
            return defaultName(testNameElement!!.text)
        } catch (e: Exception) {
            return defaultName(element["id"])
        }
    }

    public fun runTest(element: WebElement): Unit {
        var result: String
        result = element.classAttribute ?: "no result"
        if ("pass" != result) {
            val testName = "${findTestName(element)} result: $result"
            val failMessages =
                try {
                    element.findElements(By.xpath("descendant::li[@class!='pass']/*[@class = 'test-message']"))
                            .map { "$testName. ${it.text}" }
                } catch (e: Exception) {
                    listOf("test result for test case $testName")
                }

            for (message in failMessages)
                println("FAILED: $message")
            fail(failMessages.joinToString("\n"))
        }
    }

    private operator fun WebElement.get(id: String): String? = getAttribute(id)

    private val WebElement.classAttribute: String?
        get() = this["class"]
}
