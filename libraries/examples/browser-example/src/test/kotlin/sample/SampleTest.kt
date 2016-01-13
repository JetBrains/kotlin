package test.sample

import org.junit.Test as test
import org.openqa.selenium.WebDriver
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import org.openqa.selenium.By
import java.io.File
import kotlin.test.*

open class SampleTest {
    open val driver: WebDriver = HtmlUnitDriver(true)

    @test fun homePage(): Unit {
        driver.get("file://" + File("sample.html").canonicalPath)
        Thread.sleep(1000)

        val foo = driver.findElement(By.id("foo"))!!
        val text = foo.text ?: ""
        println("Found $foo with text '$text'")
        assertEquals("Some Dynamically Created Content!!!", text.trim())
    }
}