package test.dom

import std.*
import std.dom.*
import std.util.*
import stdhack.test.*
import org.w3c.dom.*

class DomBuilderTest() : TestSupport() {

    fun testBuildDocument() {
        var doc = createDocument()

        assert {
            doc["grandchild"].isEmpty()
        }

        doc.addElement("foo") {
            id = "id1"
            style = "bold"
            cssClass = "bar"
            addElement("child") {
                cssClass = "another"
                addElement("grandChild") {
                    cssClass = "tiny"
                    addText("Hello World!")
                // TODO support neater syntax sugar for adding text?
                // += "Hello World!"
                }
            }
        }
        println("builder document: ${doc.toXmlString()}")

        val grandChild = doc["grandChild"].first
        if (grandChild != null) {
            println("got element ${grandChild.toXmlString()} with text '${grandChild.text}`")
            assertEquals("Hello World!", grandChild.text)
            assertEquals("tiny", grandChild.attribute("class") ?: "")
        } else {
            fail("Not an Element $grandChild")
        }
        val children = doc.rootElement.children()
        val xml = children.toXmlString()
        println("root element has children: ${xml}")
        assertEquals(1, children.size())
    }
}
