package test.dom

import std.*
import std.dom.*
import std.util.*
import kool.test.*
import org.w3c.dom.*

class DomBuilderTest() : TestSupport() {

    fun testBuildDocument() {
        var doc = createDocument()

        assertTrue {
            doc["grandchild"].isEmpty()
        }

        doc.addElement("foo") {
            id = "id1"
            style = "bold"
            cssClass = "bar"
            addElement("child") {
                id = "id2"
                cssClass = "another"
                addElement("grandChild") {
                    id = "id3"
                    cssClass = "tiny"
                    addText("Hello World!")
                // TODO support neater syntax sugar for adding text?
                // += "Hello World!"
                }
            }
        }
        println("builder document: ${doc.toXmlString()}")


        // test css selections on document
        assertEquals(0, doc[".doesNotExist"].size())
        assertEquals(1, doc[".another"].size())
        assertEquals(1, doc[".tiny"].size())
        assertEquals(1, doc[".bar"].size())

        // element tag selections
        assertEquals(0, doc["doesNotExist"].size())
        assertEquals(1, doc["foo"].size())
        assertEquals(1, doc["child"].size())
        assertEquals(1, doc["grandChild"].size())

        // id selections
        assertEquals(1, doc["#id1"].size())
        assertEquals(1, doc["#id2"].size())
        assertEquals(1, doc["#id3"].size())

        val root = doc.rootElement
        if (root != null) {
            assertTrue {
                root.hasClass("bar")
            }

            // test css selections on element
            assertEquals(0, root[".doesNotExist"].size())
            assertEquals(1, root[".another"].size())
            assertEquals(1, root[".tiny"].size())
            assertEquals(0, root[".bar"].size())

            // element tag selections
            assertEquals(0, root["doesNotExist"].size())
            assertEquals(0, root["foo"].size())
            assertEquals(1, root["child"].size())
            assertEquals(1, root["grandChild"].size())

            // id selections
            assertEquals(1, root["#id1"].size())
            assertEquals(1, root["#id2"].size())
            assertEquals(1, root["#id3"].size())
        } else {
            fail("No root!")
        }


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
