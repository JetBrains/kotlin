package kotlin.dom

import org.w3c.dom.*
import org.w3c.dom.DOMTokenList
import org.w3c.dom.HTMLCollection
import org.w3c.dom.HTMLElement

/** Searches for elements using the element name, an element ID (if prefixed with dot) or element class (if prefixed with #) */
@Deprecated("Use querySelectorAll instead", level = DeprecationLevel.ERROR)
@Suppress("DEPRECATION_ERROR")
operator fun Document?.get(selector: String): List<Element> {
    return this?.querySelectorAll(selector)?.asList()?.filterElements() ?: emptyList()
}

/** Searches for elements using the element name, an element ID (if prefixed with dot) or element class (if prefixed with #) */
@Deprecated("Use querySelectorAll instead", level = DeprecationLevel.ERROR)
@Suppress("DEPRECATION_ERROR")
operator fun Element.get(selector: String): List<Element> {
    return querySelectorAll(selector).asList().filterElements()
}
