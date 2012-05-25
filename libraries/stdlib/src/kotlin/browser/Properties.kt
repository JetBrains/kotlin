package kotlin.browser

import org.w3c.dom.Document
import js.native

private var _document: Document? = null

/**
 * Provides access to the current active browsers DOM for the currently visible page.
 */
native var document: Document
 get() = _document!!
 set(value) {
     _document = value
 }