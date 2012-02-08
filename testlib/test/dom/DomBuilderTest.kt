package test.dom

import std.*
import std.dom.*
import stdhack.test.*
import org.w3c.dom.*

class DomBuilderTest() : TestSupport() {

    fun testBuildDocumnet() {
        var doc = createDocument()

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
    }
}
