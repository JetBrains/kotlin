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

private class HTMLCollectionListView(val collection: HTMLCollection) : AbstractList<HTMLElement>() {
    override val size: Int get() = collection.length

    override fun get(index: Int): HTMLElement =
            when {
                index in 0..size - 1 -> collection.item(index) as HTMLElement
                else -> throw IndexOutOfBoundsException("index $index is not in range [0 .. ${size - 1})")
            }
}

//@Deprecated(W)
public fun HTMLCollection.asList(): List<HTMLElement> = HTMLCollectionListView(this)

private class DOMTokenListView(val delegate: DOMTokenList) : AbstractList<String>() {
    override val size: Int get() = delegate.length

    override fun get(index: Int) =
            when {
                index in 0..size - 1 -> delegate.item(index)!!
                else -> throw IndexOutOfBoundsException("index $index is not in range [0 .. ${size - 1})")
            }
}

//@Deprecated(W)
public fun DOMTokenList.asList(): List<String> = DOMTokenListView(this)
