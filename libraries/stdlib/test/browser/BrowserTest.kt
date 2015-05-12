package test.browser

import kotlin.dom.*
import kotlin.browser.*

import kotlin.test.*
import org.junit.Test as test

class BrowserTest {

    test fun accessBrowserDOM() {
        registerBrowserPage()

        val h1 = document["h1"].first()
        assertEquals("Hello World!", h1.textContent)
    }

    protected fun registerBrowserPage() {
        // lets simulate being a browser registering its DOM
        val doc = createDocument()
        doc.addElement("html") {
            addElement("body") {
                addElement("h1") {
                    addText("Hello World!")
                }
                addElement("p") {
                    addText("This is some text content")
                }
            }
        }
        document = doc
    }
}