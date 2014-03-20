package test.dom

import kotlin.dom.*
import kotlin.test.*
import org.w3c.dom.*
import org.junit.Test as test

class NextSiblingTest {

    test fun nextSibling() {
        val doc = createDocument()

        doc.addElement("foo") {
            id = "id1"
            style = "bold"
            classes = "bar"
            addElement("child") {
                id = "id2"
                classes = "another"
                addElement("grandChild") {
                    id = "id3"
                    classes = " bar tiny"
                    addText("Hello World!")
                // TODO support neater syntax sugar for adding text?
                // += "Hello World!"
                }
                addElement("grandChild2") {
                    id = "id4"
                    classes = "tiny thing bar "
                    addText("Hello World!")
                // TODO support neater syntax sugar for adding text?
                // += "Hello World!"
                }
            }
        }

        println("builder document: ${doc.toXmlString()}")


        val elems = doc["#id3"]
        val element = elems.first()
        val elements = element.nextElements()
        val nodes = element.nextSiblings().toList()

        assertEquals(1, elements.size())
        assertEquals(1, nodes.size())
    }
}
