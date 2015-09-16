@file:JvmVersion
@file:JvmName("BrowserKt")
package kotlin.browser

import org.w3c.dom.Document

private var _document: Document? = null

/**
 * Provides access to the current active browsers DOM for the currently visible page.
 */
public var document: Document
    get() {
        // Note this code is only executed on the JVM
        val answer = _document
        return if (answer == null) {
            kotlin.dom.createDocument()
        } else answer
    }
    set(value) {
        _document = value
    }
