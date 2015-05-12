package test.dom

import kotlin.dom.*
import kotlin.test.*
import org.w3c.dom.*
import org.junit.Test as test

class DomBuilderTest() {

    test fun buildDocument() {
        var doc = createDocument()

        assertTrue {
            doc["grandchild"].isEmpty()
        }

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
                    id = "id3"
                    classes = "tiny thing bar "
                    addText("Hello World!")
                // TODO support neater syntax sugar for adding text?
                // += "Hello World!"
                }
            }
        }

        // test css selections on document
        assertEquals(0, doc[".doesNotExist"].size())
        assertEquals(1, doc[".another"].size())
        assertEquals(3, doc[".bar"].size())
        assertEquals(2, doc[".tiny"].size())

        // element tag selections
        assertEquals(0, doc["doesNotExist"].size())
        assertEquals(1, doc["foo"].size())
        assertEquals(1, doc["child"].size())
        assertEquals(1, doc["grandChild"].size())

        // id selections
        assertEquals(1, doc["#id1"].size())
        assertEquals(1, doc["#id2"].size())
        assertEquals(1, doc["#id3"].size())

        val root = doc.documentElement
        if (root == null) {
            fail("No root!")
        } else {
            assertTrue {
                root.hasClass("bar")
            }

            // test css selections on element
            assertEquals(0, root[".doesNotExist"].size())
            assertEquals(1, root[".another"].size())
            assertEquals(2, root[".bar"].size())
            assertEquals(2, root[".tiny"].size())

            // element tag selections
            assertEquals(0, root["doesNotExist"].size())
            assertEquals(0, root["foo"].size())
            assertEquals(1, root["child"].size())
            assertEquals(1, root["grandChild"].size())

            // id selections
            assertEquals(1, root["#id1"].size())
            assertEquals(1, root["#id2"].size())
            assertEquals(1, root["#id3"].size())

            // iterating through next element siblings
            for (e in root.nextElements()) {
                println("found element: $e")
            }

        }
        val grandChild = doc["grandChild"].firstOrNull()
        if (grandChild != null) {
            assertEquals("Hello World!", grandChild.textContent)
            assertEquals(" bar tiny", grandChild.attribute("class"))

            // test the classSet
            val classSet = grandChild.classSet

            assertTrue(classSet.contains("bar"))
            assertTrue(classSet.contains("tiny"))
            assertTrue(classSet.size() == 2 )
            assertFalse(classSet.contains("doesNotExist"))

            // lets add a new class and some existing classes
            grandChild.addClass("bar")
            grandChild.addClass("newThingy")
            assertEquals("bar tiny newThingy", grandChild.classes)

            // remove
            grandChild.removeClass("bar")
            assertEquals("tiny newThingy", grandChild.classes)

            grandChild.removeClass("tiny")
            assertEquals("newThingy", grandChild.classes)

        } else {
            fail("Not an Element $grandChild")
        }
        val child = doc["child"].firstOrNull()
        if (child != null) {
            val gc1 = child.childElements("grandChild")
            assertEquals(1, gc1.size(), "Expected a single child but found $gc1")
            val gc2 = child.childElements("grandChild2")
            assertEquals(1, gc2.size(), "Expected a single child but found $gc2")
        } else {
            fail("No child found!")
        }
        val children = doc.documentElement.children()
        assertEquals(1, children.size())

    }
}
