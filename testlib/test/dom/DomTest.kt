package test.dom

import std.*
import std.dom.*
import stdhack.test.*
import org.w3c.dom.*

class DomTest() : TestSupport() {

    fun testCreateDocument() {
        var doc = createDocument()
        assertNotNull(doc, "Should have created a document")

        val e = doc.createElement("foo").sure()
        assertCssClass(e, "")

        // now lets update the cssClass property
        e.cssClass = "foo"
        assertCssClass(e, "foo")

        // now using the attribute directly
        e.setAttribute("class", "bar")
        assertCssClass(e, "bar")

        doc += e
        println("document ${doc.toXmlString()}")
    }


    fun assertCssClass(e: Element, value: String?): Unit {
        val cl = e.cssClass
        val cl2 = e.getAttribute("class")
        println("element ${e.toXmlString()} has cssClass `${cl}` class attr `${cl2}`")

        assertEquals(value, cl, "value of element.cssClass")
        assertEquals(value, cl2, "value of element.getAttribute(\"class\")")
    }
}
