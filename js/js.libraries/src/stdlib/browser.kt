package kotlin.browser

import org.w3c.dom.Document
import js.native
import js.library

/**
 * Provides access to the current active browsers DOM for the currently visible page.
 */
native("document") var document: Document

