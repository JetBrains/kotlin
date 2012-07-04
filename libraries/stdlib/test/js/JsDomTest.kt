package test.js

import kotlin.*
import kotlin.browser.*
import kotlin.dom.*
import kotlin.test.*
import org.w3c.dom.*
import org.junit.Test as test

class JsDomTest {

    test fun testCreateDocument() {
        var doc = document
        assertNotNull(doc, "Should have created a document")

        val e = doc.createElement("foo")!!
        assertCssClass(e, "")

        // now lets update the cssClass property
        e.classes = "foo"
        assertCssClass(e, "foo")

        // now using the attribute directly
        e.setAttribute("class", "bar")
        assertCssClass(e, "bar")
    }

    test fun addText() {
        var doc = document
        assertNotNull(doc, "Should have created a document")

        val e = doc.createElement("foo")!!
        e + "hello"

        val xml = e.toXmlString()
        println("element after text ${xml}")

        assertEquals("hello", e.text)

    }


    fun assertCssClass(e: Element, value: String?): Unit {
        val cl = e.classes
        val cl2 = e.getAttribute("class") ?: ""
        val xml = e.toXmlString()
        println("element ${xml} has cssClass `${cl}` class attr `${cl2}`")

        assertEquals(value, cl, "value of element.cssClass")
        assertEquals(value, cl2, "value of element.getAttribute(\"class\")")
    }
}
