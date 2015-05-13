/*
 * Generated file
 * DO NOT EDIT
 * 
 * See libraries/tools/idl2k for details
 */

package org.w3c.dom

import org.khronos.webgl.*
import org.w3c.dom.css.*
import org.w3c.dom.events.*
import org.w3c.dom.parsing.*
import org.w3c.dom.svg.*
import org.w3c.fetch.*
import org.w3c.files.*
import org.w3c.notifications.*
import org.w3c.performance.*
import org.w3c.workers.*
import org.w3c.xhr.*

native public trait HTMLAllCollection : HTMLCollection {
    fun item(name: String): UnionElementOrHTMLCollection? = noImpl
//    override fun namedItem(name: String): UnionElementOrHTMLCollection? = noImpl
//    nativeGetter override fun get(name: String): UnionElementOrHTMLCollection? = noImpl
}

native public trait HTMLFormControlsCollection : HTMLCollection {
//    override fun namedItem(name: String): UnionElementOrRadioNodeList? = noImpl
//    nativeGetter override fun get(name: String): UnionElementOrRadioNodeList? = noImpl
}

native public trait RadioNodeList : NodeList, UnionElementOrRadioNodeList {
    var value: String
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLOptionsCollection : HTMLCollection {
    var selectedIndex: Int
        get() = noImpl
        set(value) = noImpl
    nativeSetter fun set(index: Int, option: HTMLOptionElement?): Unit = noImpl
    fun add(element: UnionHTMLOptGroupElementOrHTMLOptionElement, before: dynamic = null): Unit = noImpl
    fun remove(index: Int): Unit = noImpl
}

native public trait HTMLPropertiesCollection : HTMLCollection {
    var names: Array<String>
        get() = noImpl
        set(value) = noImpl
//    override fun namedItem(name: String): PropertyNodeList? = noImpl
//    nativeGetter override fun get(name: String): PropertyNodeList? = noImpl
}

native public trait PropertyNodeList : NodeList {
    fun getValues(): Array<Any?> = noImpl
}

native public trait DOMStringMap {
    nativeGetter fun get(name: String): String? = noImpl
    nativeSetter fun set(name: String, value: String): Unit = noImpl
}

native public trait DOMElementMap {
    nativeGetter fun get(name: String): Element? = noImpl
    nativeSetter fun set(name: String, value: Element): Unit = noImpl
}

native public open class Document : Node {
    var fullscreenEnabled: Boolean
        get() = noImpl
        set(value) = noImpl
    var fullscreenElement: Element?
        get() = noImpl
        set(value) = noImpl
    var onfullscreenchange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onfullscreenerror: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var styleSheets: StyleSheetList
        get() = noImpl
        set(value) = noImpl
    var selectedStyleSheetSet: String?
        get() = noImpl
        set(value) = noImpl
    var lastStyleSheetSet: String?
        get() = noImpl
        set(value) = noImpl
    var preferredStyleSheetSet: String?
        get() = noImpl
        set(value) = noImpl
    var styleSheetSets: Array<String>
        get() = noImpl
        set(value) = noImpl
    var location: Location?
        get() = noImpl
        set(value) = noImpl
    var domain: String
        get() = noImpl
        set(value) = noImpl
    var referrer: String
        get() = noImpl
        set(value) = noImpl
    var cookie: String
        get() = noImpl
        set(value) = noImpl
    var lastModified: String
        get() = noImpl
        set(value) = noImpl
    var readyState: String
        get() = noImpl
        set(value) = noImpl
    var title: String
        get() = noImpl
        set(value) = noImpl
    var dir: String
        get() = noImpl
        set(value) = noImpl
    var body: HTMLElement?
        get() = noImpl
        set(value) = noImpl
    var head: HTMLHeadElement?
        get() = noImpl
        set(value) = noImpl
    var images: HTMLCollection
        get() = noImpl
        set(value) = noImpl
    var embeds: HTMLCollection
        get() = noImpl
        set(value) = noImpl
    var plugins: HTMLCollection
        get() = noImpl
        set(value) = noImpl
    var links: HTMLCollection
        get() = noImpl
        set(value) = noImpl
    var forms: HTMLCollection
        get() = noImpl
        set(value) = noImpl
    var scripts: HTMLCollection
        get() = noImpl
        set(value) = noImpl
    var cssElementMap: DOMElementMap
        get() = noImpl
        set(value) = noImpl
    var currentScript: HTMLScriptElement?
        get() = noImpl
        set(value) = noImpl
    var defaultView: Window?
        get() = noImpl
        set(value) = noImpl
    var activeElement: Element?
        get() = noImpl
        set(value) = noImpl
    var designMode: String
        get() = noImpl
        set(value) = noImpl
    var commands: HTMLCollection
        get() = noImpl
        set(value) = noImpl
    var onreadystatechange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var fgColor: String
        get() = noImpl
        set(value) = noImpl
    var linkColor: String
        get() = noImpl
        set(value) = noImpl
    var vlinkColor: String
        get() = noImpl
        set(value) = noImpl
    var alinkColor: String
        get() = noImpl
        set(value) = noImpl
    var bgColor: String
        get() = noImpl
        set(value) = noImpl
    var anchors: HTMLCollection
        get() = noImpl
        set(value) = noImpl
    var applets: HTMLCollection
        get() = noImpl
        set(value) = noImpl
    var all: HTMLAllCollection
        get() = noImpl
        set(value) = noImpl
    var implementation: DOMImplementation
        get() = noImpl
        set(value) = noImpl
    var URL: String
        get() = noImpl
        set(value) = noImpl
    var documentURI: String
        get() = noImpl
        set(value) = noImpl
    var origin: String
        get() = noImpl
        set(value) = noImpl
    var compatMode: String
        get() = noImpl
        set(value) = noImpl
    var characterSet: String
        get() = noImpl
        set(value) = noImpl
    var inputEncoding: String
        get() = noImpl
        set(value) = noImpl
    var contentType: String
        get() = noImpl
        set(value) = noImpl
    var doctype: DocumentType?
        get() = noImpl
        set(value) = noImpl
    var documentElement: Element?
        get() = noImpl
        set(value) = noImpl
    var onabort: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onautocomplete: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onautocompleteerror: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onblur: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var oncancel: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var oncanplay: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var oncanplaythrough: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onchange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onclick: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onclose: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var oncontextmenu: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var oncuechange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var ondblclick: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var ondrag: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var ondragend: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var ondragenter: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var ondragexit: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var ondragleave: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var ondragover: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var ondragstart: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var ondrop: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var ondurationchange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onemptied: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onended: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onerror: ((dynamic, dynamic, String, Int, Int, Any?) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onfocus: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var oninput: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var oninvalid: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onkeydown: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onkeypress: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onkeyup: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onload: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onloadeddata: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onloadedmetadata: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onloadstart: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onmousedown: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onmouseenter: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onmouseleave: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onmousemove: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onmouseout: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onmouseover: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onmouseup: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onmousewheel: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onpause: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onplay: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onplaying: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onprogress: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onratechange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onreset: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onresize: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onscroll: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onseeked: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onseeking: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onselect: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onshow: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onsort: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onstalled: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onsubmit: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onsuspend: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var ontimeupdate: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var ontoggle: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onvolumechange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onwaiting: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var children: HTMLCollection
        get() = noImpl
        set(value) = noImpl
    var firstElementChild: Element?
        get() = noImpl
        set(value) = noImpl
    var lastElementChild: Element?
        get() = noImpl
        set(value) = noImpl
    var childElementCount: Int
        get() = noImpl
        set(value) = noImpl
    fun exitFullscreen(): Unit = noImpl
    fun enableStyleSheetsForSet(name: String?): Unit = noImpl
    nativeGetter fun get(name: String): dynamic = noImpl
    fun getElementsByName(elementName: String): NodeList = noImpl
    fun getItems(typeNames: String = ""): NodeList = noImpl
    fun open(type: String = "text/html", replace: String = ""): Document = noImpl
    fun open(url: String, name: String, features: String, replace: Boolean = false): Window = noImpl
    fun close(): Unit = noImpl
    fun write(vararg text: String): Unit = noImpl
    fun writeln(vararg text: String): Unit = noImpl
    fun hasFocus(): Boolean = noImpl
    fun execCommand(commandId: String, showUI: Boolean = false, value: String = ""): Boolean = noImpl
    fun queryCommandEnabled(commandId: String): Boolean = noImpl
    fun queryCommandIndeterm(commandId: String): Boolean = noImpl
    fun queryCommandState(commandId: String): Boolean = noImpl
    fun queryCommandSupported(commandId: String): Boolean = noImpl
    fun queryCommandValue(commandId: String): String = noImpl
    fun clear(): Unit = noImpl
    fun captureEvents(): Unit = noImpl
    fun releaseEvents(): Unit = noImpl
    fun getElementsByTagName(localName: String): HTMLCollection = noImpl
    fun getElementsByTagNameNS(namespace: String?, localName: String): HTMLCollection = noImpl
    fun getElementsByClassName(classNames: String): HTMLCollection = noImpl
    fun createElement(localName: String): Element = noImpl
    fun createElementNS(namespace: String?, qualifiedName: String): Element = noImpl
    fun createDocumentFragment(): DocumentFragment = noImpl
    fun createTextNode(data: String): Text = noImpl
    fun createComment(data: String): Comment = noImpl
    fun createProcessingInstruction(target: String, data: String): ProcessingInstruction = noImpl
    fun importNode(node: Node, deep: Boolean = false): Node = noImpl
    fun adoptNode(node: Node): Node = noImpl
    fun createAttribute(localName: String): Attr = noImpl
    fun createAttributeNS(namespace: String?, name: String): Attr = noImpl
    fun createEvent(interface_: String): Event = noImpl
    fun createRange(): Range = noImpl
    fun createNodeIterator(root: Node, whatToShow: Int = noImpl, filter: NodeFilter? = null): NodeIterator = noImpl
    fun createNodeIterator(root: Node, whatToShow: Int = noImpl, filter: ((Node) -> Short)? = null): NodeIterator = noImpl
    fun createTreeWalker(root: Node, whatToShow: Int = noImpl, filter: NodeFilter? = null): TreeWalker = noImpl
    fun createTreeWalker(root: Node, whatToShow: Int = noImpl, filter: ((Node) -> Short)? = null): TreeWalker = noImpl
    fun getSelection(): Selection = noImpl
    fun getElementById(elementId: String): Element? = noImpl
    fun prepend(vararg nodes: dynamic): Unit = noImpl
    fun append(vararg nodes: dynamic): Unit = noImpl
    fun query(relativeSelectors: String): Element? = noImpl
    fun queryAll(relativeSelectors: String): dynamic = noImpl
    fun querySelector(selectors: String): Element? = noImpl
    fun querySelectorAll(selectors: String): NodeList = noImpl
}

native public open class XMLDocument : Document() {
    fun load(url: String): Boolean = noImpl
}

native public trait HTMLElement : Element {
    var title: String
        get() = noImpl
        set(value) = noImpl
    var lang: String
        get() = noImpl
        set(value) = noImpl
    var translate: Boolean
        get() = noImpl
        set(value) = noImpl
    var dir: String
        get() = noImpl
        set(value) = noImpl
    var dataset: DOMStringMap
        get() = noImpl
        set(value) = noImpl
    var itemScope: Boolean
        get() = noImpl
        set(value) = noImpl
    var itemType: DOMSettableTokenList
        get() = noImpl
        set(value) = noImpl
    var itemId: String
        get() = noImpl
        set(value) = noImpl
    var itemRef: DOMSettableTokenList
        get() = noImpl
        set(value) = noImpl
    var itemProp: DOMSettableTokenList
        get() = noImpl
        set(value) = noImpl
    var properties: HTMLPropertiesCollection
        get() = noImpl
        set(value) = noImpl
    var itemValue: Any?
        get() = noImpl
        set(value) = noImpl
    var hidden: Boolean
        get() = noImpl
        set(value) = noImpl
    var tabIndex: Int
        get() = noImpl
        set(value) = noImpl
    var accessKey: String
        get() = noImpl
        set(value) = noImpl
    var accessKeyLabel: String
        get() = noImpl
        set(value) = noImpl
    var draggable: Boolean
        get() = noImpl
        set(value) = noImpl
    var dropzone: DOMSettableTokenList
        get() = noImpl
        set(value) = noImpl
    var contextMenu: HTMLMenuElement?
        get() = noImpl
        set(value) = noImpl
    var spellcheck: Boolean
        get() = noImpl
        set(value) = noImpl
    var commandType: String?
        get() = noImpl
        set(value) = noImpl
    var commandLabel: String?
        get() = noImpl
        set(value) = noImpl
    var commandIcon: String?
        get() = noImpl
        set(value) = noImpl
    var commandHidden: Boolean?
        get() = noImpl
        set(value) = noImpl
    var commandDisabled: Boolean?
        get() = noImpl
        set(value) = noImpl
    var commandChecked: Boolean?
        get() = noImpl
        set(value) = noImpl
    var onabort: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onautocomplete: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onautocompleteerror: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onblur: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var oncancel: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var oncanplay: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var oncanplaythrough: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onchange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onclick: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onclose: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var oncontextmenu: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var oncuechange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var ondblclick: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var ondrag: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var ondragend: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var ondragenter: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var ondragexit: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var ondragleave: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var ondragover: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var ondragstart: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var ondrop: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var ondurationchange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onemptied: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onended: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onerror: ((dynamic, dynamic, String, Int, Int, Any?) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onfocus: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var oninput: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var oninvalid: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onkeydown: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onkeypress: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onkeyup: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onload: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onloadeddata: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onloadedmetadata: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onloadstart: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onmousedown: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onmouseenter: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onmouseleave: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onmousemove: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onmouseout: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onmouseover: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onmouseup: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onmousewheel: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onpause: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onplay: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onplaying: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onprogress: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onratechange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onreset: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onresize: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onscroll: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onseeked: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onseeking: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onselect: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onshow: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onsort: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onstalled: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onsubmit: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onsuspend: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var ontimeupdate: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var ontoggle: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onvolumechange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onwaiting: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var contentEditable: String
        get() = noImpl
        set(value) = noImpl
    var isContentEditable: Boolean
        get() = noImpl
        set(value) = noImpl
    var style: CSSStyleDeclaration
        get() = noImpl
        set(value) = noImpl
    fun click(): Unit = noImpl
    fun focus(): Unit = noImpl
    fun blur(): Unit = noImpl
    fun forceSpellCheck(): Unit = noImpl
}

native public trait HTMLUnknownElement : HTMLElement {
}

native public trait HTMLHtmlElement : HTMLElement {
    var version: String
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLHeadElement : HTMLElement {
}

native public trait HTMLTitleElement : HTMLElement {
    var text: String
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLBaseElement : HTMLElement {
    var href: String
        get() = noImpl
        set(value) = noImpl
    var target: String
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLLinkElement : HTMLElement {
    var href: String
        get() = noImpl
        set(value) = noImpl
    var crossOrigin: String?
        get() = noImpl
        set(value) = noImpl
    var rel: String
        get() = noImpl
        set(value) = noImpl
    var relList: DOMTokenList
        get() = noImpl
        set(value) = noImpl
    var media: String
        get() = noImpl
        set(value) = noImpl
    var hreflang: String
        get() = noImpl
        set(value) = noImpl
    var type: String
        get() = noImpl
        set(value) = noImpl
    var sizes: DOMSettableTokenList
        get() = noImpl
        set(value) = noImpl
    var charset: String
        get() = noImpl
        set(value) = noImpl
    var rev: String
        get() = noImpl
        set(value) = noImpl
    var target: String
        get() = noImpl
        set(value) = noImpl
    var sheet: StyleSheet?
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLMetaElement : HTMLElement {
    var name: String
        get() = noImpl
        set(value) = noImpl
    var httpEquiv: String
        get() = noImpl
        set(value) = noImpl
    var content: String
        get() = noImpl
        set(value) = noImpl
    var scheme: String
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLStyleElement : HTMLElement {
    var media: String
        get() = noImpl
        set(value) = noImpl
    var type: String
        get() = noImpl
        set(value) = noImpl
    var scoped: Boolean
        get() = noImpl
        set(value) = noImpl
    var sheet: StyleSheet?
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLBodyElement : HTMLElement {
    var text: String
        get() = noImpl
        set(value) = noImpl
    var link: String
        get() = noImpl
        set(value) = noImpl
    var vLink: String
        get() = noImpl
        set(value) = noImpl
    var aLink: String
        get() = noImpl
        set(value) = noImpl
    var bgColor: String
        get() = noImpl
        set(value) = noImpl
    var background: String
        get() = noImpl
        set(value) = noImpl
    var onafterprint: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onbeforeprint: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onbeforeunload: ((Event) -> String?)?
        get() = noImpl
        set(value) = noImpl
    var onhashchange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onlanguagechange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onmessage: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onoffline: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var ononline: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onpagehide: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onpageshow: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onpopstate: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onstorage: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onunload: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLHeadingElement : HTMLElement {
    var align: String
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLParagraphElement : HTMLElement {
    var align: String
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLHRElement : HTMLElement {
    var align: String
        get() = noImpl
        set(value) = noImpl
    var color: String
        get() = noImpl
        set(value) = noImpl
    var noShade: Boolean
        get() = noImpl
        set(value) = noImpl
    var size: String
        get() = noImpl
        set(value) = noImpl
    var width: String
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLPreElement : HTMLElement {
    var width: Int
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLQuoteElement : HTMLElement {
    var cite: String
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLOListElement : HTMLElement {
    var reversed: Boolean
        get() = noImpl
        set(value) = noImpl
    var start: Int
        get() = noImpl
        set(value) = noImpl
    var type: String
        get() = noImpl
        set(value) = noImpl
    var compact: Boolean
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLUListElement : HTMLElement {
    var compact: Boolean
        get() = noImpl
        set(value) = noImpl
    var type: String
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLLIElement : HTMLElement {
    var value: Int
        get() = noImpl
        set(value) = noImpl
    var type: String
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLDListElement : HTMLElement {
    var compact: Boolean
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLDivElement : HTMLElement {
    var align: String
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLAnchorElement : HTMLElement {
    var target: String
        get() = noImpl
        set(value) = noImpl
    var download: String
        get() = noImpl
        set(value) = noImpl
    var ping: DOMSettableTokenList
        get() = noImpl
        set(value) = noImpl
    var rel: String
        get() = noImpl
        set(value) = noImpl
    var relList: DOMTokenList
        get() = noImpl
        set(value) = noImpl
    var hreflang: String
        get() = noImpl
        set(value) = noImpl
    var type: String
        get() = noImpl
        set(value) = noImpl
    var text: String
        get() = noImpl
        set(value) = noImpl
    var coords: String
        get() = noImpl
        set(value) = noImpl
    var charset: String
        get() = noImpl
        set(value) = noImpl
    var name: String
        get() = noImpl
        set(value) = noImpl
    var rev: String
        get() = noImpl
        set(value) = noImpl
    var shape: String
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLDataElement : HTMLElement {
    var value: String
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLTimeElement : HTMLElement {
    var dateTime: String
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLSpanElement : HTMLElement {
}

native public trait HTMLBRElement : HTMLElement {
    var clear: String
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLModElement : HTMLElement {
    var cite: String
        get() = noImpl
        set(value) = noImpl
    var dateTime: String
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLIFrameElement : HTMLElement {
    var src: String
        get() = noImpl
        set(value) = noImpl
    var srcdoc: String
        get() = noImpl
        set(value) = noImpl
    var name: String
        get() = noImpl
        set(value) = noImpl
    var sandbox: DOMSettableTokenList
        get() = noImpl
        set(value) = noImpl
    var seamless: Boolean
        get() = noImpl
        set(value) = noImpl
    var allowFullscreen: Boolean
        get() = noImpl
        set(value) = noImpl
    var width: String
        get() = noImpl
        set(value) = noImpl
    var height: String
        get() = noImpl
        set(value) = noImpl
    var contentDocument: Document?
        get() = noImpl
        set(value) = noImpl
    var contentWindow: Window?
        get() = noImpl
        set(value) = noImpl
    var align: String
        get() = noImpl
        set(value) = noImpl
    var scrolling: String
        get() = noImpl
        set(value) = noImpl
    var frameBorder: String
        get() = noImpl
        set(value) = noImpl
    var longDesc: String
        get() = noImpl
        set(value) = noImpl
    var marginHeight: String
        get() = noImpl
        set(value) = noImpl
    var marginWidth: String
        get() = noImpl
        set(value) = noImpl
    fun getSVGDocument(): Document? = noImpl
}

native public trait HTMLEmbedElement : HTMLElement {
    var src: String
        get() = noImpl
        set(value) = noImpl
    var type: String
        get() = noImpl
        set(value) = noImpl
    var width: String
        get() = noImpl
        set(value) = noImpl
    var height: String
        get() = noImpl
        set(value) = noImpl
    var align: String
        get() = noImpl
        set(value) = noImpl
    var name: String
        get() = noImpl
        set(value) = noImpl
    fun getSVGDocument(): Document? = noImpl
}

native public trait HTMLObjectElement : HTMLElement {
    var data: String
        get() = noImpl
        set(value) = noImpl
    var type: String
        get() = noImpl
        set(value) = noImpl
    var typeMustMatch: Boolean
        get() = noImpl
        set(value) = noImpl
    var name: String
        get() = noImpl
        set(value) = noImpl
    var useMap: String
        get() = noImpl
        set(value) = noImpl
    var form: HTMLFormElement?
        get() = noImpl
        set(value) = noImpl
    var width: String
        get() = noImpl
        set(value) = noImpl
    var height: String
        get() = noImpl
        set(value) = noImpl
    var contentDocument: Document?
        get() = noImpl
        set(value) = noImpl
    var contentWindow: Window?
        get() = noImpl
        set(value) = noImpl
    var willValidate: Boolean
        get() = noImpl
        set(value) = noImpl
    var validity: ValidityState
        get() = noImpl
        set(value) = noImpl
    var validationMessage: String
        get() = noImpl
        set(value) = noImpl
    var align: String
        get() = noImpl
        set(value) = noImpl
    var archive: String
        get() = noImpl
        set(value) = noImpl
    var code: String
        get() = noImpl
        set(value) = noImpl
    var declare: Boolean
        get() = noImpl
        set(value) = noImpl
    var hspace: Int
        get() = noImpl
        set(value) = noImpl
    var standby: String
        get() = noImpl
        set(value) = noImpl
    var vspace: Int
        get() = noImpl
        set(value) = noImpl
    var codeBase: String
        get() = noImpl
        set(value) = noImpl
    var codeType: String
        get() = noImpl
        set(value) = noImpl
    var border: String
        get() = noImpl
        set(value) = noImpl
    fun getSVGDocument(): Document? = noImpl
    fun checkValidity(): Boolean = noImpl
    fun reportValidity(): Boolean = noImpl
    fun setCustomValidity(error: String): Unit = noImpl
}

native public trait HTMLParamElement : HTMLElement {
    var name: String
        get() = noImpl
        set(value) = noImpl
    var value: String
        get() = noImpl
        set(value) = noImpl
    var type: String
        get() = noImpl
        set(value) = noImpl
    var valueType: String
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLVideoElement : HTMLMediaElement, CanvasImageSource, ImageBitmapSource {
    var width: Int
        get() = noImpl
        set(value) = noImpl
    var height: Int
        get() = noImpl
        set(value) = noImpl
    var videoWidth: Int
        get() = noImpl
        set(value) = noImpl
    var videoHeight: Int
        get() = noImpl
        set(value) = noImpl
    var poster: String
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLAudioElement : HTMLMediaElement {
}

native public trait HTMLSourceElement : HTMLElement {
    var src: String
        get() = noImpl
        set(value) = noImpl
    var type: String
        get() = noImpl
        set(value) = noImpl
    var srcset: String
        get() = noImpl
        set(value) = noImpl
    var sizes: String
        get() = noImpl
        set(value) = noImpl
    var media: String
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLTrackElement : HTMLElement {
    var kind: String
        get() = noImpl
        set(value) = noImpl
    var src: String
        get() = noImpl
        set(value) = noImpl
    var srclang: String
        get() = noImpl
        set(value) = noImpl
    var label: String
        get() = noImpl
        set(value) = noImpl
    var default: Boolean
        get() = noImpl
        set(value) = noImpl
    var readyState: Short
        get() = noImpl
        set(value) = noImpl
    var track: TextTrack
        get() = noImpl
        set(value) = noImpl

    companion object {
        val NONE: Short = 0
        val LOADING: Short = 1
        val LOADED: Short = 2
        val ERROR: Short = 3
    }
}

native public trait HTMLMediaElement : HTMLElement {
    var error: MediaError?
        get() = noImpl
        set(value) = noImpl
    var src: String
        get() = noImpl
        set(value) = noImpl
    var srcObject: dynamic
        get() = noImpl
        set(value) = noImpl
    var currentSrc: String
        get() = noImpl
        set(value) = noImpl
    var crossOrigin: String?
        get() = noImpl
        set(value) = noImpl
    var networkState: Short
        get() = noImpl
        set(value) = noImpl
    var preload: String
        get() = noImpl
        set(value) = noImpl
    var buffered: TimeRanges
        get() = noImpl
        set(value) = noImpl
    var readyState: Short
        get() = noImpl
        set(value) = noImpl
    var seeking: Boolean
        get() = noImpl
        set(value) = noImpl
    var currentTime: Double
        get() = noImpl
        set(value) = noImpl
    var duration: Double
        get() = noImpl
        set(value) = noImpl
    var paused: Boolean
        get() = noImpl
        set(value) = noImpl
    var defaultPlaybackRate: Double
        get() = noImpl
        set(value) = noImpl
    var playbackRate: Double
        get() = noImpl
        set(value) = noImpl
    var played: TimeRanges
        get() = noImpl
        set(value) = noImpl
    var seekable: TimeRanges
        get() = noImpl
        set(value) = noImpl
    var ended: Boolean
        get() = noImpl
        set(value) = noImpl
    var autoplay: Boolean
        get() = noImpl
        set(value) = noImpl
    var loop: Boolean
        get() = noImpl
        set(value) = noImpl
    var mediaGroup: String
        get() = noImpl
        set(value) = noImpl
    var controller: MediaController?
        get() = noImpl
        set(value) = noImpl
    var controls: Boolean
        get() = noImpl
        set(value) = noImpl
    var volume: Double
        get() = noImpl
        set(value) = noImpl
    var muted: Boolean
        get() = noImpl
        set(value) = noImpl
    var defaultMuted: Boolean
        get() = noImpl
        set(value) = noImpl
    var audioTracks: AudioTrackList
        get() = noImpl
        set(value) = noImpl
    var videoTracks: VideoTrackList
        get() = noImpl
        set(value) = noImpl
    var textTracks: TextTrackList
        get() = noImpl
        set(value) = noImpl
    fun load(): Unit = noImpl
    fun canPlayType(type: String): String = noImpl
    fun fastSeek(time: Double): Unit = noImpl
    fun getStartDate(): Date = noImpl
    fun play(): Unit = noImpl
    fun pause(): Unit = noImpl
    fun addTextTrack(kind: String, label: String = "", language: String = ""): TextTrack = noImpl

    companion object {
        val NETWORK_EMPTY: Short = 0
        val NETWORK_IDLE: Short = 1
        val NETWORK_LOADING: Short = 2
        val NETWORK_NO_SOURCE: Short = 3
        val HAVE_NOTHING: Short = 0
        val HAVE_METADATA: Short = 1
        val HAVE_CURRENT_DATA: Short = 2
        val HAVE_FUTURE_DATA: Short = 3
        val HAVE_ENOUGH_DATA: Short = 4
    }
}

native public trait MediaError {
    var code: Short
        get() = noImpl
        set(value) = noImpl

    companion object {
        val MEDIA_ERR_ABORTED: Short = 1
        val MEDIA_ERR_NETWORK: Short = 2
        val MEDIA_ERR_DECODE: Short = 3
        val MEDIA_ERR_SRC_NOT_SUPPORTED: Short = 4
    }
}

native public trait AudioTrackList : EventTarget {
    var length: Int
        get() = noImpl
        set(value) = noImpl
    var onchange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onaddtrack: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onremovetrack: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    nativeGetter fun get(index: Int): AudioTrack? = noImpl
    fun getTrackById(id: String): AudioTrack? = noImpl
}

native public trait AudioTrack : UnionAudioTrackOrTextTrackOrVideoTrack {
    var id: String
        get() = noImpl
        set(value) = noImpl
    var kind: String
        get() = noImpl
        set(value) = noImpl
    var label: String
        get() = noImpl
        set(value) = noImpl
    var language: String
        get() = noImpl
        set(value) = noImpl
    var enabled: Boolean
        get() = noImpl
        set(value) = noImpl
}

native public trait VideoTrackList : EventTarget {
    var length: Int
        get() = noImpl
        set(value) = noImpl
    var selectedIndex: Int
        get() = noImpl
        set(value) = noImpl
    var onchange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onaddtrack: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onremovetrack: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    nativeGetter fun get(index: Int): VideoTrack? = noImpl
    fun getTrackById(id: String): VideoTrack? = noImpl
}

native public trait VideoTrack : UnionAudioTrackOrTextTrackOrVideoTrack {
    var id: String
        get() = noImpl
        set(value) = noImpl
    var kind: String
        get() = noImpl
        set(value) = noImpl
    var label: String
        get() = noImpl
        set(value) = noImpl
    var language: String
        get() = noImpl
        set(value) = noImpl
    var selected: Boolean
        get() = noImpl
        set(value) = noImpl
}

native public open class MediaController : EventTarget {
    var readyState: Short
        get() = noImpl
        set(value) = noImpl
    var buffered: TimeRanges
        get() = noImpl
        set(value) = noImpl
    var seekable: TimeRanges
        get() = noImpl
        set(value) = noImpl
    var duration: Double
        get() = noImpl
        set(value) = noImpl
    var currentTime: Double
        get() = noImpl
        set(value) = noImpl
    var paused: Boolean
        get() = noImpl
        set(value) = noImpl
    var playbackState: String
        get() = noImpl
        set(value) = noImpl
    var played: TimeRanges
        get() = noImpl
        set(value) = noImpl
    var defaultPlaybackRate: Double
        get() = noImpl
        set(value) = noImpl
    var playbackRate: Double
        get() = noImpl
        set(value) = noImpl
    var volume: Double
        get() = noImpl
        set(value) = noImpl
    var muted: Boolean
        get() = noImpl
        set(value) = noImpl
    var onemptied: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onloadedmetadata: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onloadeddata: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var oncanplay: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var oncanplaythrough: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onplaying: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onended: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onwaiting: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var ondurationchange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var ontimeupdate: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onplay: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onpause: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onratechange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onvolumechange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    fun pause(): Unit = noImpl
    fun unpause(): Unit = noImpl
    fun play(): Unit = noImpl
}

native public trait TextTrackList : EventTarget {
    var length: Int
        get() = noImpl
        set(value) = noImpl
    var onchange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onaddtrack: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onremovetrack: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    nativeGetter fun get(index: Int): TextTrack? = noImpl
    fun getTrackById(id: String): TextTrack? = noImpl
}

native public trait TextTrack : EventTarget, UnionAudioTrackOrTextTrackOrVideoTrack {
    var kind: String
        get() = noImpl
        set(value) = noImpl
    var label: String
        get() = noImpl
        set(value) = noImpl
    var language: String
        get() = noImpl
        set(value) = noImpl
    var id: String
        get() = noImpl
        set(value) = noImpl
    var inBandMetadataTrackDispatchType: String
        get() = noImpl
        set(value) = noImpl
    var mode: String
        get() = noImpl
        set(value) = noImpl
    var cues: TextTrackCueList?
        get() = noImpl
        set(value) = noImpl
    var activeCues: TextTrackCueList?
        get() = noImpl
        set(value) = noImpl
    var oncuechange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    fun addCue(cue: TextTrackCue): Unit = noImpl
    fun removeCue(cue: TextTrackCue): Unit = noImpl
}

native public trait TextTrackCueList {
    var length: Int
        get() = noImpl
        set(value) = noImpl
    nativeGetter fun get(index: Int): TextTrackCue? = noImpl
    fun getCueById(id: String): TextTrackCue? = noImpl
}

native public trait TextTrackCue : EventTarget {
    var track: TextTrack?
        get() = noImpl
        set(value) = noImpl
    var id: String
        get() = noImpl
        set(value) = noImpl
    var startTime: Double
        get() = noImpl
        set(value) = noImpl
    var endTime: Double
        get() = noImpl
        set(value) = noImpl
    var pauseOnExit: Boolean
        get() = noImpl
        set(value) = noImpl
    var onenter: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onexit: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
}

native public trait TimeRanges {
    var length: Int
        get() = noImpl
        set(value) = noImpl
    fun start(index: Int): Double = noImpl
    fun end(index: Int): Double = noImpl
}

native public open class TrackEvent(type: String, eventInitDict: TrackEventInit = noImpl) : Event(type, eventInitDict) {
    var track: UnionAudioTrackOrTextTrackOrVideoTrack?
        get() = noImpl
        set(value) = noImpl
}

native public open class TrackEventInit : EventInit() {
    var track: UnionAudioTrackOrTextTrackOrVideoTrack?
}

native public trait HTMLMapElement : HTMLElement {
    var name: String
        get() = noImpl
        set(value) = noImpl
    var areas: HTMLCollection
        get() = noImpl
        set(value) = noImpl
    var images: HTMLCollection
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLAreaElement : HTMLElement {
    var alt: String
        get() = noImpl
        set(value) = noImpl
    var coords: String
        get() = noImpl
        set(value) = noImpl
    var shape: String
        get() = noImpl
        set(value) = noImpl
    var target: String
        get() = noImpl
        set(value) = noImpl
    var download: String
        get() = noImpl
        set(value) = noImpl
    var ping: DOMSettableTokenList
        get() = noImpl
        set(value) = noImpl
    var rel: String
        get() = noImpl
        set(value) = noImpl
    var relList: DOMTokenList
        get() = noImpl
        set(value) = noImpl
    var hreflang: String
        get() = noImpl
        set(value) = noImpl
    var type: String
        get() = noImpl
        set(value) = noImpl
    var noHref: Boolean
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLTableElement : HTMLElement {
    var caption: HTMLTableCaptionElement?
        get() = noImpl
        set(value) = noImpl
    var tHead: HTMLTableSectionElement?
        get() = noImpl
        set(value) = noImpl
    var tFoot: HTMLTableSectionElement?
        get() = noImpl
        set(value) = noImpl
    var tBodies: HTMLCollection
        get() = noImpl
        set(value) = noImpl
    var rows: HTMLCollection
        get() = noImpl
        set(value) = noImpl
    var sortable: Boolean
        get() = noImpl
        set(value) = noImpl
    var align: String
        get() = noImpl
        set(value) = noImpl
    var border: String
        get() = noImpl
        set(value) = noImpl
    var frame: String
        get() = noImpl
        set(value) = noImpl
    var rules: String
        get() = noImpl
        set(value) = noImpl
    var summary: String
        get() = noImpl
        set(value) = noImpl
    var width: String
        get() = noImpl
        set(value) = noImpl
    var bgColor: String
        get() = noImpl
        set(value) = noImpl
    var cellPadding: String
        get() = noImpl
        set(value) = noImpl
    var cellSpacing: String
        get() = noImpl
        set(value) = noImpl
    fun createCaption(): HTMLElement = noImpl
    fun deleteCaption(): Unit = noImpl
    fun createTHead(): HTMLElement = noImpl
    fun deleteTHead(): Unit = noImpl
    fun createTFoot(): HTMLElement = noImpl
    fun deleteTFoot(): Unit = noImpl
    fun createTBody(): HTMLElement = noImpl
    fun insertRow(index: Int = -1): HTMLElement = noImpl
    fun deleteRow(index: Int): Unit = noImpl
    fun stopSorting(): Unit = noImpl
}

native public trait HTMLTableCaptionElement : HTMLElement {
    var align: String
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLTableColElement : HTMLElement {
    var span: Int
        get() = noImpl
        set(value) = noImpl
    var align: String
        get() = noImpl
        set(value) = noImpl
    var ch: String
        get() = noImpl
        set(value) = noImpl
    var chOff: String
        get() = noImpl
        set(value) = noImpl
    var vAlign: String
        get() = noImpl
        set(value) = noImpl
    var width: String
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLTableSectionElement : HTMLElement {
    var rows: HTMLCollection
        get() = noImpl
        set(value) = noImpl
    var align: String
        get() = noImpl
        set(value) = noImpl
    var ch: String
        get() = noImpl
        set(value) = noImpl
    var chOff: String
        get() = noImpl
        set(value) = noImpl
    var vAlign: String
        get() = noImpl
        set(value) = noImpl
    fun insertRow(index: Int = -1): HTMLElement = noImpl
    fun deleteRow(index: Int): Unit = noImpl
}

native public trait HTMLTableRowElement : HTMLElement {
    var rowIndex: Int
        get() = noImpl
        set(value) = noImpl
    var sectionRowIndex: Int
        get() = noImpl
        set(value) = noImpl
    var cells: HTMLCollection
        get() = noImpl
        set(value) = noImpl
    var align: String
        get() = noImpl
        set(value) = noImpl
    var ch: String
        get() = noImpl
        set(value) = noImpl
    var chOff: String
        get() = noImpl
        set(value) = noImpl
    var vAlign: String
        get() = noImpl
        set(value) = noImpl
    var bgColor: String
        get() = noImpl
        set(value) = noImpl
    fun insertCell(index: Int = -1): HTMLElement = noImpl
    fun deleteCell(index: Int): Unit = noImpl
}

native public trait HTMLTableDataCellElement : HTMLTableCellElement {
    var abbr: String
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLTableHeaderCellElement : HTMLTableCellElement {
    var scope: String
        get() = noImpl
        set(value) = noImpl
    var abbr: String
        get() = noImpl
        set(value) = noImpl
    var sorted: String
        get() = noImpl
        set(value) = noImpl
    fun sort(): Unit = noImpl
}

native public trait HTMLTableCellElement : HTMLElement {
    var colSpan: Int
        get() = noImpl
        set(value) = noImpl
    var rowSpan: Int
        get() = noImpl
        set(value) = noImpl
    var headers: DOMSettableTokenList
        get() = noImpl
        set(value) = noImpl
    var cellIndex: Int
        get() = noImpl
        set(value) = noImpl
    var align: String
        get() = noImpl
        set(value) = noImpl
    var axis: String
        get() = noImpl
        set(value) = noImpl
    var height: String
        get() = noImpl
        set(value) = noImpl
    var width: String
        get() = noImpl
        set(value) = noImpl
    var ch: String
        get() = noImpl
        set(value) = noImpl
    var chOff: String
        get() = noImpl
        set(value) = noImpl
    var noWrap: Boolean
        get() = noImpl
        set(value) = noImpl
    var vAlign: String
        get() = noImpl
        set(value) = noImpl
    var bgColor: String
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLFormElement : HTMLElement {
    var acceptCharset: String
        get() = noImpl
        set(value) = noImpl
    var action: String
        get() = noImpl
        set(value) = noImpl
    var autocomplete: String
        get() = noImpl
        set(value) = noImpl
    var enctype: String
        get() = noImpl
        set(value) = noImpl
    var encoding: String
        get() = noImpl
        set(value) = noImpl
    var method: String
        get() = noImpl
        set(value) = noImpl
    var name: String
        get() = noImpl
        set(value) = noImpl
    var noValidate: Boolean
        get() = noImpl
        set(value) = noImpl
    var target: String
        get() = noImpl
        set(value) = noImpl
    var elements: HTMLFormControlsCollection
        get() = noImpl
        set(value) = noImpl
    var length: Int
        get() = noImpl
        set(value) = noImpl
    nativeGetter fun get(index: Int): Element? = noImpl
    nativeGetter fun get(name: String): UnionElementOrRadioNodeList? = noImpl
    fun submit(): Unit = noImpl
    fun reset(): Unit = noImpl
    fun checkValidity(): Boolean = noImpl
    fun reportValidity(): Boolean = noImpl
    fun requestAutocomplete(): Unit = noImpl
}

native public trait HTMLLabelElement : HTMLElement {
    var form: HTMLFormElement?
        get() = noImpl
        set(value) = noImpl
    var htmlFor: String
        get() = noImpl
        set(value) = noImpl
    var control: HTMLElement?
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLInputElement : HTMLElement {
    var accept: String
        get() = noImpl
        set(value) = noImpl
    var alt: String
        get() = noImpl
        set(value) = noImpl
    var autocomplete: String
        get() = noImpl
        set(value) = noImpl
    var autofocus: Boolean
        get() = noImpl
        set(value) = noImpl
    var defaultChecked: Boolean
        get() = noImpl
        set(value) = noImpl
    var checked: Boolean
        get() = noImpl
        set(value) = noImpl
    var dirName: String
        get() = noImpl
        set(value) = noImpl
    var disabled: Boolean
        get() = noImpl
        set(value) = noImpl
    var form: HTMLFormElement?
        get() = noImpl
        set(value) = noImpl
    var files: FileList?
        get() = noImpl
        set(value) = noImpl
    var formAction: String
        get() = noImpl
        set(value) = noImpl
    var formEnctype: String
        get() = noImpl
        set(value) = noImpl
    var formMethod: String
        get() = noImpl
        set(value) = noImpl
    var formNoValidate: Boolean
        get() = noImpl
        set(value) = noImpl
    var formTarget: String
        get() = noImpl
        set(value) = noImpl
    var height: Int
        get() = noImpl
        set(value) = noImpl
    var indeterminate: Boolean
        get() = noImpl
        set(value) = noImpl
    var inputMode: String
        get() = noImpl
        set(value) = noImpl
    var list: HTMLElement?
        get() = noImpl
        set(value) = noImpl
    var max: String
        get() = noImpl
        set(value) = noImpl
    var maxLength: Int
        get() = noImpl
        set(value) = noImpl
    var min: String
        get() = noImpl
        set(value) = noImpl
    var minLength: Int
        get() = noImpl
        set(value) = noImpl
    var multiple: Boolean
        get() = noImpl
        set(value) = noImpl
    var name: String
        get() = noImpl
        set(value) = noImpl
    var pattern: String
        get() = noImpl
        set(value) = noImpl
    var placeholder: String
        get() = noImpl
        set(value) = noImpl
    var readOnly: Boolean
        get() = noImpl
        set(value) = noImpl
    var required: Boolean
        get() = noImpl
        set(value) = noImpl
    var size: Int
        get() = noImpl
        set(value) = noImpl
    var src: String
        get() = noImpl
        set(value) = noImpl
    var step: String
        get() = noImpl
        set(value) = noImpl
    var type: String
        get() = noImpl
        set(value) = noImpl
    var defaultValue: String
        get() = noImpl
        set(value) = noImpl
    var value: String
        get() = noImpl
        set(value) = noImpl
    var valueAsDate: Date?
        get() = noImpl
        set(value) = noImpl
    var valueAsNumber: Double
        get() = noImpl
        set(value) = noImpl
    var valueLow: Double
        get() = noImpl
        set(value) = noImpl
    var valueHigh: Double
        get() = noImpl
        set(value) = noImpl
    var width: Int
        get() = noImpl
        set(value) = noImpl
    var willValidate: Boolean
        get() = noImpl
        set(value) = noImpl
    var validity: ValidityState
        get() = noImpl
        set(value) = noImpl
    var validationMessage: String
        get() = noImpl
        set(value) = noImpl
    var labels: NodeList
        get() = noImpl
        set(value) = noImpl
    var selectionStart: Int
        get() = noImpl
        set(value) = noImpl
    var selectionEnd: Int
        get() = noImpl
        set(value) = noImpl
    var selectionDirection: String
        get() = noImpl
        set(value) = noImpl
    var align: String
        get() = noImpl
        set(value) = noImpl
    var useMap: String
        get() = noImpl
        set(value) = noImpl
    fun stepUp(n: Int = 1): Unit = noImpl
    fun stepDown(n: Int = 1): Unit = noImpl
    fun checkValidity(): Boolean = noImpl
    fun reportValidity(): Boolean = noImpl
    fun setCustomValidity(error: String): Unit = noImpl
    fun select(): Unit = noImpl
    fun setRangeText(replacement: String): Unit = noImpl
    fun setRangeText(replacement: String, start: Int, end: Int, selectionMode: String = "preserve"): Unit = noImpl
    fun setSelectionRange(start: Int, end: Int, direction: String = noImpl): Unit = noImpl
}

native public trait HTMLButtonElement : HTMLElement {
    var autofocus: Boolean
        get() = noImpl
        set(value) = noImpl
    var disabled: Boolean
        get() = noImpl
        set(value) = noImpl
    var form: HTMLFormElement?
        get() = noImpl
        set(value) = noImpl
    var formAction: String
        get() = noImpl
        set(value) = noImpl
    var formEnctype: String
        get() = noImpl
        set(value) = noImpl
    var formMethod: String
        get() = noImpl
        set(value) = noImpl
    var formNoValidate: Boolean
        get() = noImpl
        set(value) = noImpl
    var formTarget: String
        get() = noImpl
        set(value) = noImpl
    var name: String
        get() = noImpl
        set(value) = noImpl
    var type: String
        get() = noImpl
        set(value) = noImpl
    var value: String
        get() = noImpl
        set(value) = noImpl
    var menu: HTMLMenuElement?
        get() = noImpl
        set(value) = noImpl
    var willValidate: Boolean
        get() = noImpl
        set(value) = noImpl
    var validity: ValidityState
        get() = noImpl
        set(value) = noImpl
    var validationMessage: String
        get() = noImpl
        set(value) = noImpl
    var labels: NodeList
        get() = noImpl
        set(value) = noImpl
    fun checkValidity(): Boolean = noImpl
    fun reportValidity(): Boolean = noImpl
    fun setCustomValidity(error: String): Unit = noImpl
}

native public trait HTMLSelectElement : HTMLElement {
    var autocomplete: String
        get() = noImpl
        set(value) = noImpl
    var autofocus: Boolean
        get() = noImpl
        set(value) = noImpl
    var disabled: Boolean
        get() = noImpl
        set(value) = noImpl
    var form: HTMLFormElement?
        get() = noImpl
        set(value) = noImpl
    var multiple: Boolean
        get() = noImpl
        set(value) = noImpl
    var name: String
        get() = noImpl
        set(value) = noImpl
    var required: Boolean
        get() = noImpl
        set(value) = noImpl
    var size: Int
        get() = noImpl
        set(value) = noImpl
    var type: String
        get() = noImpl
        set(value) = noImpl
    var options: HTMLOptionsCollection
        get() = noImpl
        set(value) = noImpl
    var length: Int
        get() = noImpl
        set(value) = noImpl
    var selectedOptions: HTMLCollection
        get() = noImpl
        set(value) = noImpl
    var selectedIndex: Int
        get() = noImpl
        set(value) = noImpl
    var value: String
        get() = noImpl
        set(value) = noImpl
    var willValidate: Boolean
        get() = noImpl
        set(value) = noImpl
    var validity: ValidityState
        get() = noImpl
        set(value) = noImpl
    var validationMessage: String
        get() = noImpl
        set(value) = noImpl
    var labels: NodeList
        get() = noImpl
        set(value) = noImpl
    fun item(index: Int): Element? = noImpl
    nativeGetter fun get(index: Int): Element? = noImpl
    fun namedItem(name: String): HTMLOptionElement? = noImpl
    fun add(element: UnionHTMLOptGroupElementOrHTMLOptionElement, before: dynamic = null): Unit = noImpl
    fun remove(index: Int): Unit = noImpl
    nativeSetter fun set(index: Int, option: HTMLOptionElement?): Unit = noImpl
    fun checkValidity(): Boolean = noImpl
    fun reportValidity(): Boolean = noImpl
    fun setCustomValidity(error: String): Unit = noImpl
}

native public trait HTMLDataListElement : HTMLElement {
    var options: HTMLCollection
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLOptGroupElement : HTMLElement, UnionHTMLOptGroupElementOrHTMLOptionElement {
    var disabled: Boolean
        get() = noImpl
        set(value) = noImpl
    var label: String
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLOptionElement : HTMLElement, UnionHTMLOptGroupElementOrHTMLOptionElement {
    var disabled: Boolean
        get() = noImpl
        set(value) = noImpl
    var form: HTMLFormElement?
        get() = noImpl
        set(value) = noImpl
    var label: String
        get() = noImpl
        set(value) = noImpl
    var defaultSelected: Boolean
        get() = noImpl
        set(value) = noImpl
    var selected: Boolean
        get() = noImpl
        set(value) = noImpl
    var value: String
        get() = noImpl
        set(value) = noImpl
    var text: String
        get() = noImpl
        set(value) = noImpl
    var index: Int
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLTextAreaElement : HTMLElement {
    var autocomplete: String
        get() = noImpl
        set(value) = noImpl
    var autofocus: Boolean
        get() = noImpl
        set(value) = noImpl
    var cols: Int
        get() = noImpl
        set(value) = noImpl
    var dirName: String
        get() = noImpl
        set(value) = noImpl
    var disabled: Boolean
        get() = noImpl
        set(value) = noImpl
    var form: HTMLFormElement?
        get() = noImpl
        set(value) = noImpl
    var inputMode: String
        get() = noImpl
        set(value) = noImpl
    var maxLength: Int
        get() = noImpl
        set(value) = noImpl
    var minLength: Int
        get() = noImpl
        set(value) = noImpl
    var name: String
        get() = noImpl
        set(value) = noImpl
    var placeholder: String
        get() = noImpl
        set(value) = noImpl
    var readOnly: Boolean
        get() = noImpl
        set(value) = noImpl
    var required: Boolean
        get() = noImpl
        set(value) = noImpl
    var rows: Int
        get() = noImpl
        set(value) = noImpl
    var wrap: String
        get() = noImpl
        set(value) = noImpl
    var type: String
        get() = noImpl
        set(value) = noImpl
    var defaultValue: String
        get() = noImpl
        set(value) = noImpl
    var value: String
        get() = noImpl
        set(value) = noImpl
    var textLength: Int
        get() = noImpl
        set(value) = noImpl
    var willValidate: Boolean
        get() = noImpl
        set(value) = noImpl
    var validity: ValidityState
        get() = noImpl
        set(value) = noImpl
    var validationMessage: String
        get() = noImpl
        set(value) = noImpl
    var labels: NodeList
        get() = noImpl
        set(value) = noImpl
    var selectionStart: Int
        get() = noImpl
        set(value) = noImpl
    var selectionEnd: Int
        get() = noImpl
        set(value) = noImpl
    var selectionDirection: String
        get() = noImpl
        set(value) = noImpl
    fun checkValidity(): Boolean = noImpl
    fun reportValidity(): Boolean = noImpl
    fun setCustomValidity(error: String): Unit = noImpl
    fun select(): Unit = noImpl
    fun setRangeText(replacement: String): Unit = noImpl
    fun setRangeText(replacement: String, start: Int, end: Int, selectionMode: String = "preserve"): Unit = noImpl
    fun setSelectionRange(start: Int, end: Int, direction: String = noImpl): Unit = noImpl
}

native public trait HTMLKeygenElement : HTMLElement {
    var autofocus: Boolean
        get() = noImpl
        set(value) = noImpl
    var challenge: String
        get() = noImpl
        set(value) = noImpl
    var disabled: Boolean
        get() = noImpl
        set(value) = noImpl
    var form: HTMLFormElement?
        get() = noImpl
        set(value) = noImpl
    var keytype: String
        get() = noImpl
        set(value) = noImpl
    var name: String
        get() = noImpl
        set(value) = noImpl
    var type: String
        get() = noImpl
        set(value) = noImpl
    var willValidate: Boolean
        get() = noImpl
        set(value) = noImpl
    var validity: ValidityState
        get() = noImpl
        set(value) = noImpl
    var validationMessage: String
        get() = noImpl
        set(value) = noImpl
    var labels: NodeList
        get() = noImpl
        set(value) = noImpl
    fun checkValidity(): Boolean = noImpl
    fun reportValidity(): Boolean = noImpl
    fun setCustomValidity(error: String): Unit = noImpl
}

native public trait HTMLOutputElement : HTMLElement {
    var htmlFor: DOMSettableTokenList
        get() = noImpl
        set(value) = noImpl
    var form: HTMLFormElement?
        get() = noImpl
        set(value) = noImpl
    var name: String
        get() = noImpl
        set(value) = noImpl
    var type: String
        get() = noImpl
        set(value) = noImpl
    var defaultValue: String
        get() = noImpl
        set(value) = noImpl
    var value: String
        get() = noImpl
        set(value) = noImpl
    var willValidate: Boolean
        get() = noImpl
        set(value) = noImpl
    var validity: ValidityState
        get() = noImpl
        set(value) = noImpl
    var validationMessage: String
        get() = noImpl
        set(value) = noImpl
    var labels: NodeList
        get() = noImpl
        set(value) = noImpl
    fun checkValidity(): Boolean = noImpl
    fun reportValidity(): Boolean = noImpl
    fun setCustomValidity(error: String): Unit = noImpl
}

native public trait HTMLProgressElement : HTMLElement {
    var value: Double
        get() = noImpl
        set(value) = noImpl
    var max: Double
        get() = noImpl
        set(value) = noImpl
    var position: Double
        get() = noImpl
        set(value) = noImpl
    var labels: NodeList
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLMeterElement : HTMLElement {
    var value: Double
        get() = noImpl
        set(value) = noImpl
    var min: Double
        get() = noImpl
        set(value) = noImpl
    var max: Double
        get() = noImpl
        set(value) = noImpl
    var low: Double
        get() = noImpl
        set(value) = noImpl
    var high: Double
        get() = noImpl
        set(value) = noImpl
    var optimum: Double
        get() = noImpl
        set(value) = noImpl
    var labels: NodeList
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLFieldSetElement : HTMLElement {
    var disabled: Boolean
        get() = noImpl
        set(value) = noImpl
    var form: HTMLFormElement?
        get() = noImpl
        set(value) = noImpl
    var name: String
        get() = noImpl
        set(value) = noImpl
    var type: String
        get() = noImpl
        set(value) = noImpl
    var elements: HTMLFormControlsCollection
        get() = noImpl
        set(value) = noImpl
    var willValidate: Boolean
        get() = noImpl
        set(value) = noImpl
    var validity: ValidityState
        get() = noImpl
        set(value) = noImpl
    var validationMessage: String
        get() = noImpl
        set(value) = noImpl
    fun checkValidity(): Boolean = noImpl
    fun reportValidity(): Boolean = noImpl
    fun setCustomValidity(error: String): Unit = noImpl
}

native public trait HTMLLegendElement : HTMLElement {
    var form: HTMLFormElement?
        get() = noImpl
        set(value) = noImpl
    var align: String
        get() = noImpl
        set(value) = noImpl
}

native public open class AutocompleteErrorEvent(type: String, eventInitDict: AutocompleteErrorEventInit = noImpl) : Event(type, eventInitDict) {
    var reason: String
        get() = noImpl
        set(value) = noImpl
}

native public open class AutocompleteErrorEventInit : EventInit() {
    var reason: String
}

native public trait ValidityState {
    var valueMissing: Boolean
        get() = noImpl
        set(value) = noImpl
    var typeMismatch: Boolean
        get() = noImpl
        set(value) = noImpl
    var patternMismatch: Boolean
        get() = noImpl
        set(value) = noImpl
    var tooLong: Boolean
        get() = noImpl
        set(value) = noImpl
    var tooShort: Boolean
        get() = noImpl
        set(value) = noImpl
    var rangeUnderflow: Boolean
        get() = noImpl
        set(value) = noImpl
    var rangeOverflow: Boolean
        get() = noImpl
        set(value) = noImpl
    var stepMismatch: Boolean
        get() = noImpl
        set(value) = noImpl
    var badInput: Boolean
        get() = noImpl
        set(value) = noImpl
    var customError: Boolean
        get() = noImpl
        set(value) = noImpl
    var valid: Boolean
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLDetailsElement : HTMLElement {
    var open: Boolean
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLMenuElement : HTMLElement {
    var type: String
        get() = noImpl
        set(value) = noImpl
    var label: String
        get() = noImpl
        set(value) = noImpl
    var compact: Boolean
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLMenuItemElement : HTMLElement {
    var type: String
        get() = noImpl
        set(value) = noImpl
    var label: String
        get() = noImpl
        set(value) = noImpl
    var icon: String
        get() = noImpl
        set(value) = noImpl
    var disabled: Boolean
        get() = noImpl
        set(value) = noImpl
    var checked: Boolean
        get() = noImpl
        set(value) = noImpl
    var radiogroup: String
        get() = noImpl
        set(value) = noImpl
    var default: Boolean
        get() = noImpl
        set(value) = noImpl
    var command: HTMLElement?
        get() = noImpl
        set(value) = noImpl
}

native public open class RelatedEvent(type: String, eventInitDict: RelatedEventInit = noImpl) : Event(type, eventInitDict) {
    var relatedTarget: EventTarget?
        get() = noImpl
        set(value) = noImpl
}

native public open class RelatedEventInit : EventInit() {
    var relatedTarget: EventTarget?
}

native public trait HTMLDialogElement : HTMLElement {
    var open: Boolean
        get() = noImpl
        set(value) = noImpl
    var returnValue: String
        get() = noImpl
        set(value) = noImpl
    fun show(anchor: UnionElementOrMouseEvent = noImpl): Unit = noImpl
    fun showModal(anchor: UnionElementOrMouseEvent = noImpl): Unit = noImpl
    fun close(returnValue: String = noImpl): Unit = noImpl
}

native public trait HTMLScriptElement : HTMLElement {
    var src: String
        get() = noImpl
        set(value) = noImpl
    var type: String
        get() = noImpl
        set(value) = noImpl
    var charset: String
        get() = noImpl
        set(value) = noImpl
    var async: Boolean
        get() = noImpl
        set(value) = noImpl
    var defer: Boolean
        get() = noImpl
        set(value) = noImpl
    var crossOrigin: String?
        get() = noImpl
        set(value) = noImpl
    var text: String
        get() = noImpl
        set(value) = noImpl
    var event: String
        get() = noImpl
        set(value) = noImpl
    var htmlFor: String
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLTemplateElement : HTMLElement {
    var content: DocumentFragment
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLCanvasElement : HTMLElement, CanvasImageSource, ImageBitmapSource {
    var width: Int
        get() = noImpl
        set(value) = noImpl
    var height: Int
        get() = noImpl
        set(value) = noImpl
    fun getContext(contextId: String, vararg arguments: Any?): RenderingContext? = noImpl
    fun probablySupportsContext(contextId: String, vararg arguments: Any?): Boolean = noImpl
    fun setContext(context: RenderingContext): Unit = noImpl
    fun transferControlToProxy(): CanvasProxy = noImpl
    fun toDataURL(type: String = noImpl, vararg arguments: Any?): String = noImpl
    fun toBlob(_callback: ((File) -> Unit)?, type: String = noImpl, vararg arguments: Any?): Unit = noImpl
}

native public trait CanvasProxy : Transferable {
    fun setContext(context: RenderingContext): Unit = noImpl
}

native public open class CanvasRenderingContext2DSettings {
    var alpha: Boolean = true
}

native public open class CanvasRenderingContext2D : RenderingContext, CanvasImageSource, ImageBitmapSource {
    var canvas: HTMLCanvasElement
        get() = noImpl
        set(value) = noImpl
    var width: Int
        get() = noImpl
        set(value) = noImpl
    var height: Int
        get() = noImpl
        set(value) = noImpl
    var currentTransform: SVGMatrix
        get() = noImpl
        set(value) = noImpl
    var globalAlpha: Double
        get() = noImpl
        set(value) = noImpl
    var globalCompositeOperation: String
        get() = noImpl
        set(value) = noImpl
    var imageSmoothingEnabled: Boolean
        get() = noImpl
        set(value) = noImpl
    var strokeStyle: dynamic
        get() = noImpl
        set(value) = noImpl
    var fillStyle: dynamic
        get() = noImpl
        set(value) = noImpl
    var shadowOffsetX: Double
        get() = noImpl
        set(value) = noImpl
    var shadowOffsetY: Double
        get() = noImpl
        set(value) = noImpl
    var shadowBlur: Double
        get() = noImpl
        set(value) = noImpl
    var shadowColor: String
        get() = noImpl
        set(value) = noImpl
    var lineWidth: Double
        get() = noImpl
        set(value) = noImpl
    var lineCap: String
        get() = noImpl
        set(value) = noImpl
    var lineJoin: String
        get() = noImpl
        set(value) = noImpl
    var miterLimit: Double
        get() = noImpl
        set(value) = noImpl
    var lineDashOffset: Double
        get() = noImpl
        set(value) = noImpl
    var font: String
        get() = noImpl
        set(value) = noImpl
    var textAlign: String
        get() = noImpl
        set(value) = noImpl
    var textBaseline: String
        get() = noImpl
        set(value) = noImpl
    var direction: String
        get() = noImpl
        set(value) = noImpl
    fun commit(): Unit = noImpl
    fun save(): Unit = noImpl
    fun restore(): Unit = noImpl
    fun scale(x: Double, y: Double): Unit = noImpl
    fun rotate(angle: Double): Unit = noImpl
    fun translate(x: Double, y: Double): Unit = noImpl
    fun transform(a: Double, b: Double, c: Double, d: Double, e: Double, f: Double): Unit = noImpl
    fun setTransform(a: Double, b: Double, c: Double, d: Double, e: Double, f: Double): Unit = noImpl
    fun resetTransform(): Unit = noImpl
    fun createLinearGradient(x0: Double, y0: Double, x1: Double, y1: Double): CanvasGradient = noImpl
    fun createRadialGradient(x0: Double, y0: Double, r0: Double, x1: Double, y1: Double, r1: Double): CanvasGradient = noImpl
    fun createPattern(image: CanvasImageSource, repetition: String): CanvasPattern = noImpl
    fun clearRect(x: Double, y: Double, w: Double, h: Double): Unit = noImpl
    fun fillRect(x: Double, y: Double, w: Double, h: Double): Unit = noImpl
    fun strokeRect(x: Double, y: Double, w: Double, h: Double): Unit = noImpl
    fun beginPath(): Unit = noImpl
    fun fill(fillRule: String = "nonzero"): Unit = noImpl
    fun fill(path: Path2D, fillRule: String = "nonzero"): Unit = noImpl
    fun stroke(): Unit = noImpl
    fun stroke(path: Path2D): Unit = noImpl
    fun drawFocusIfNeeded(element: Element): Unit = noImpl
    fun drawFocusIfNeeded(path: Path2D, element: Element): Unit = noImpl
    fun scrollPathIntoView(): Unit = noImpl
    fun scrollPathIntoView(path: Path2D): Unit = noImpl
    fun clip(fillRule: String = "nonzero"): Unit = noImpl
    fun clip(path: Path2D, fillRule: String = "nonzero"): Unit = noImpl
    fun resetClip(): Unit = noImpl
    fun isPointInPath(x: Double, y: Double, fillRule: String = "nonzero"): Boolean = noImpl
    fun isPointInPath(path: Path2D, x: Double, y: Double, fillRule: String = "nonzero"): Boolean = noImpl
    fun isPointInStroke(x: Double, y: Double): Boolean = noImpl
    fun isPointInStroke(path: Path2D, x: Double, y: Double): Boolean = noImpl
    fun fillText(text: String, x: Double, y: Double, maxWidth: Double = noImpl): Unit = noImpl
    fun strokeText(text: String, x: Double, y: Double, maxWidth: Double = noImpl): Unit = noImpl
    fun measureText(text: String): TextMetrics = noImpl
    fun drawImage(image: CanvasImageSource, dx: Double, dy: Double): Unit = noImpl
    fun drawImage(image: CanvasImageSource, dx: Double, dy: Double, dw: Double, dh: Double): Unit = noImpl
    fun drawImage(image: CanvasImageSource, sx: Double, sy: Double, sw: Double, sh: Double, dx: Double, dy: Double, dw: Double, dh: Double): Unit = noImpl
    fun addHitRegion(options: HitRegionOptions = noImpl): Unit = noImpl
    fun removeHitRegion(id: String): Unit = noImpl
    fun clearHitRegions(): Unit = noImpl
    fun createImageData(sw: Double, sh: Double): ImageData = noImpl
    fun createImageData(imagedata: ImageData): ImageData = noImpl
    fun getImageData(sx: Double, sy: Double, sw: Double, sh: Double): ImageData = noImpl
    fun putImageData(imagedata: ImageData, dx: Double, dy: Double): Unit = noImpl
    fun putImageData(imagedata: ImageData, dx: Double, dy: Double, dirtyX: Double, dirtyY: Double, dirtyWidth: Double, dirtyHeight: Double): Unit = noImpl
    fun setLineDash(segments: Array<Double>): Unit = noImpl
    fun getLineDash(): Array<Double> = noImpl
    fun closePath(): Unit = noImpl
    fun moveTo(x: Double, y: Double): Unit = noImpl
    fun lineTo(x: Double, y: Double): Unit = noImpl
    fun quadraticCurveTo(cpx: Double, cpy: Double, x: Double, y: Double): Unit = noImpl
    fun bezierCurveTo(cp1x: Double, cp1y: Double, cp2x: Double, cp2y: Double, x: Double, y: Double): Unit = noImpl
    fun arcTo(x1: Double, y1: Double, x2: Double, y2: Double, radius: Double): Unit = noImpl
    fun arcTo(x1: Double, y1: Double, x2: Double, y2: Double, radiusX: Double, radiusY: Double, rotation: Double): Unit = noImpl
    fun rect(x: Double, y: Double, w: Double, h: Double): Unit = noImpl
    fun arc(x: Double, y: Double, radius: Double, startAngle: Double, endAngle: Double, anticlockwise: Boolean = false): Unit = noImpl
    fun ellipse(x: Double, y: Double, radiusX: Double, radiusY: Double, rotation: Double, startAngle: Double, endAngle: Double, anticlockwise: Boolean = false): Unit = noImpl
}

native public trait CanvasGradient {
    fun addColorStop(offset: Double, color: String): Unit = noImpl
}

native public trait CanvasPattern {
    fun setTransform(transform: SVGMatrix): Unit = noImpl
}

native public trait TextMetrics {
    var width: Double
        get() = noImpl
        set(value) = noImpl
    var actualBoundingBoxLeft: Double
        get() = noImpl
        set(value) = noImpl
    var actualBoundingBoxRight: Double
        get() = noImpl
        set(value) = noImpl
    var fontBoundingBoxAscent: Double
        get() = noImpl
        set(value) = noImpl
    var fontBoundingBoxDescent: Double
        get() = noImpl
        set(value) = noImpl
    var actualBoundingBoxAscent: Double
        get() = noImpl
        set(value) = noImpl
    var actualBoundingBoxDescent: Double
        get() = noImpl
        set(value) = noImpl
    var emHeightAscent: Double
        get() = noImpl
        set(value) = noImpl
    var emHeightDescent: Double
        get() = noImpl
        set(value) = noImpl
    var hangingBaseline: Double
        get() = noImpl
        set(value) = noImpl
    var alphabeticBaseline: Double
        get() = noImpl
        set(value) = noImpl
    var ideographicBaseline: Double
        get() = noImpl
        set(value) = noImpl
}

native public open class HitRegionOptions {
    var path: Path2D? = null
    var fillRule: String = "nonzero"
    var id: String = ""
    var parentID: String? = null
    var cursor: String = "inherit"
    var control: Element? = null
    var label: String? = null
    var role: String? = null
}

native public open class ImageData(sw: Int, sh: Int) : ImageBitmapSource {
    var width: Int
        get() = noImpl
        set(value) = noImpl
    var height: Int
        get() = noImpl
        set(value) = noImpl
    var data: Uint8ClampedArray
        get() = noImpl
        set(value) = noImpl
}

native public open class DrawingStyle(scope: Element = noImpl) {
    var lineWidth: Double
        get() = noImpl
        set(value) = noImpl
    var lineCap: String
        get() = noImpl
        set(value) = noImpl
    var lineJoin: String
        get() = noImpl
        set(value) = noImpl
    var miterLimit: Double
        get() = noImpl
        set(value) = noImpl
    var lineDashOffset: Double
        get() = noImpl
        set(value) = noImpl
    var font: String
        get() = noImpl
        set(value) = noImpl
    var textAlign: String
        get() = noImpl
        set(value) = noImpl
    var textBaseline: String
        get() = noImpl
        set(value) = noImpl
    var direction: String
        get() = noImpl
        set(value) = noImpl
    fun setLineDash(segments: Array<Double>): Unit = noImpl
    fun getLineDash(): Array<Double> = noImpl
}

native public open class Path2D {
    fun addPath(path: Path2D, transformation: SVGMatrix? = null): Unit = noImpl
    fun addPathByStrokingPath(path: Path2D, styles: dynamic, transformation: SVGMatrix? = null): Unit = noImpl
    fun addText(text: String, styles: dynamic, transformation: SVGMatrix?, x: Double, y: Double, maxWidth: Double = noImpl): Unit = noImpl
    fun addPathByStrokingText(text: String, styles: dynamic, transformation: SVGMatrix?, x: Double, y: Double, maxWidth: Double = noImpl): Unit = noImpl
    fun addText(text: String, styles: dynamic, transformation: SVGMatrix?, path: Path2D, maxWidth: Double = noImpl): Unit = noImpl
    fun addPathByStrokingText(text: String, styles: dynamic, transformation: SVGMatrix?, path: Path2D, maxWidth: Double = noImpl): Unit = noImpl
    fun closePath(): Unit = noImpl
    fun moveTo(x: Double, y: Double): Unit = noImpl
    fun lineTo(x: Double, y: Double): Unit = noImpl
    fun quadraticCurveTo(cpx: Double, cpy: Double, x: Double, y: Double): Unit = noImpl
    fun bezierCurveTo(cp1x: Double, cp1y: Double, cp2x: Double, cp2y: Double, x: Double, y: Double): Unit = noImpl
    fun arcTo(x1: Double, y1: Double, x2: Double, y2: Double, radius: Double): Unit = noImpl
    fun arcTo(x1: Double, y1: Double, x2: Double, y2: Double, radiusX: Double, radiusY: Double, rotation: Double): Unit = noImpl
    fun rect(x: Double, y: Double, w: Double, h: Double): Unit = noImpl
    fun arc(x: Double, y: Double, radius: Double, startAngle: Double, endAngle: Double, anticlockwise: Boolean = false): Unit = noImpl
    fun ellipse(x: Double, y: Double, radiusX: Double, radiusY: Double, rotation: Double, startAngle: Double, endAngle: Double, anticlockwise: Boolean = false): Unit = noImpl
}

native public trait Touch {
    var region: String?
        get() = noImpl
        set(value) = noImpl
}

native public trait DataTransfer {
    var dropEffect: String
        get() = noImpl
        set(value) = noImpl
    var effectAllowed: String
        get() = noImpl
        set(value) = noImpl
    var items: DataTransferItemList
        get() = noImpl
        set(value) = noImpl
    var types: Array<String>
        get() = noImpl
        set(value) = noImpl
    var files: FileList
        get() = noImpl
        set(value) = noImpl
    fun setDragImage(image: Element, x: Int, y: Int): Unit = noImpl
    fun getData(format: String): String = noImpl
    fun setData(format: String, data: String): Unit = noImpl
    fun clearData(format: String = noImpl): Unit = noImpl
}

native public trait DataTransferItemList {
    var length: Int
        get() = noImpl
        set(value) = noImpl
    nativeGetter fun get(index: Int): DataTransferItem? = noImpl
    fun add(data: String, type: String): DataTransferItem? = noImpl
    fun add(data: File): DataTransferItem? = noImpl
    fun remove(index: Int): Unit = noImpl
    fun clear(): Unit = noImpl
}

native public trait DataTransferItem {
    var kind: String
        get() = noImpl
        set(value) = noImpl
    var type: String
        get() = noImpl
        set(value) = noImpl
    fun getAsString(_callback: ((String) -> Unit)?): Unit = noImpl
    fun getAsFile(): File? = noImpl
}

native public open class DragEvent(type: String, eventInitDict: DragEventInit = noImpl) : MouseEvent(noImpl, noImpl) {
    var dataTransfer: DataTransfer?
        get() = noImpl
        set(value) = noImpl
}

native public open class DragEventInit : MouseEventInit() {
    var dataTransfer: DataTransfer?
}

native public trait Window : EventTarget, UnionMessagePortOrWindow {
    var caches: CacheStorage
        get() = noImpl
        set(value) = noImpl
    var performance: Performance
        get() = noImpl
        set(value) = noImpl
    var window: Window
        get() = noImpl
        set(value) = noImpl
    var self: Window
        get() = noImpl
        set(value) = noImpl
    var document: Document
        get() = noImpl
        set(value) = noImpl
    var name: String
        get() = noImpl
        set(value) = noImpl
    var location: Location
        get() = noImpl
        set(value) = noImpl
    var history: History
        get() = noImpl
        set(value) = noImpl
    var locationbar: BarProp
        get() = noImpl
        set(value) = noImpl
    var menubar: BarProp
        get() = noImpl
        set(value) = noImpl
    var personalbar: BarProp
        get() = noImpl
        set(value) = noImpl
    var scrollbars: BarProp
        get() = noImpl
        set(value) = noImpl
    var statusbar: BarProp
        get() = noImpl
        set(value) = noImpl
    var toolbar: BarProp
        get() = noImpl
        set(value) = noImpl
    var status: String
        get() = noImpl
        set(value) = noImpl
    var closed: Boolean
        get() = noImpl
        set(value) = noImpl
    var frames: Window
        get() = noImpl
        set(value) = noImpl
    var length: Int
        get() = noImpl
        set(value) = noImpl
    var top: Window
        get() = noImpl
        set(value) = noImpl
    var opener: Any?
        get() = noImpl
        set(value) = noImpl
    var parent: Window
        get() = noImpl
        set(value) = noImpl
    var frameElement: Element?
        get() = noImpl
        set(value) = noImpl
    var navigator: Navigator
        get() = noImpl
        set(value) = noImpl
    var external: External
        get() = noImpl
        set(value) = noImpl
    var applicationCache: ApplicationCache
        get() = noImpl
        set(value) = noImpl
    var onabort: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onautocomplete: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onautocompleteerror: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onblur: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var oncancel: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var oncanplay: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var oncanplaythrough: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onchange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onclick: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onclose: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var oncontextmenu: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var oncuechange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var ondblclick: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var ondrag: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var ondragend: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var ondragenter: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var ondragexit: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var ondragleave: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var ondragover: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var ondragstart: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var ondrop: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var ondurationchange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onemptied: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onended: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onerror: ((dynamic, dynamic, String, Int, Int, Any?) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onfocus: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var oninput: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var oninvalid: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onkeydown: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onkeypress: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onkeyup: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onload: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onloadeddata: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onloadedmetadata: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onloadstart: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onmousedown: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onmouseenter: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onmouseleave: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onmousemove: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onmouseout: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onmouseover: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onmouseup: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onmousewheel: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onpause: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onplay: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onplaying: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onprogress: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onratechange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onreset: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onresize: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onscroll: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onseeked: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onseeking: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onselect: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onshow: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onsort: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onstalled: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onsubmit: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onsuspend: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var ontimeupdate: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var ontoggle: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onvolumechange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onwaiting: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onafterprint: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onbeforeprint: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onbeforeunload: ((Event) -> String?)?
        get() = noImpl
        set(value) = noImpl
    var onhashchange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onlanguagechange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onmessage: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onoffline: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var ononline: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onpagehide: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onpageshow: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onpopstate: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onstorage: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onunload: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var sessionStorage: Storage
        get() = noImpl
        set(value) = noImpl
    var localStorage: Storage
        get() = noImpl
        set(value) = noImpl
    fun getComputedStyle(elt: Element, pseudoElt: String? = noImpl): CSSStyleDeclaration = noImpl
    fun close(): Unit = noImpl
    fun stop(): Unit = noImpl
    fun focus(): Unit = noImpl
    fun blur(): Unit = noImpl
    fun open(url: String = "about:blank", target: String = "_blank", features: String = "", replace: Boolean = false): Window = noImpl
    nativeGetter fun get(index: Int): Window? = noImpl
    nativeGetter fun get(name: String): dynamic = noImpl
    fun alert(): Unit = noImpl
    fun alert(message: String): Unit = noImpl
    fun confirm(message: String = ""): Boolean = noImpl
    fun prompt(message: String = "", default: String = ""): String? = noImpl
    fun print(): Unit = noImpl
    fun showModalDialog(url: String, argument: Any? = noImpl): Any? = noImpl
    fun requestAnimationFrame(callback: (Double) -> Unit): Int = noImpl
    fun cancelAnimationFrame(handle: Int): Unit = noImpl
    fun postMessage(message: Any?, targetOrigin: String, transfer: Array<Transferable> = noImpl): Unit = noImpl
    fun captureEvents(): Unit = noImpl
    fun releaseEvents(): Unit = noImpl
    fun getSelection(): Selection = noImpl
    fun btoa(btoa: String): String = noImpl
    fun atob(atob: String): String = noImpl
    fun setTimeout(handler: () -> dynamic, timeout: Int = 0, vararg arguments: Any?): Int = noImpl
    fun setTimeout(handler: String, timeout: Int = 0, vararg arguments: Any?): Int = noImpl
    fun clearTimeout(handle: Int = 0): Unit = noImpl
    fun setInterval(handler: () -> dynamic, timeout: Int = 0, vararg arguments: Any?): Int = noImpl
    fun setInterval(handler: String, timeout: Int = 0, vararg arguments: Any?): Int = noImpl
    fun clearInterval(handle: Int = 0): Unit = noImpl
    fun createImageBitmap(image: ImageBitmapSource): dynamic = noImpl
    fun createImageBitmap(image: ImageBitmapSource, sx: Int, sy: Int, sw: Int, sh: Int): dynamic = noImpl
    fun fetch(input: dynamic, init: RequestInit = noImpl): dynamic = noImpl
}

native public trait BarProp {
    var visible: Boolean
        get() = noImpl
        set(value) = noImpl
}

native public trait History {
    var length: Int
        get() = noImpl
        set(value) = noImpl
    var state: Any?
        get() = noImpl
        set(value) = noImpl
    fun go(delta: Int = noImpl): Unit = noImpl
    fun back(): Unit = noImpl
    fun forward(): Unit = noImpl
    fun pushState(data: Any?, title: String, url: String? = null): Unit = noImpl
    fun replaceState(data: Any?, title: String, url: String? = null): Unit = noImpl
}

native public trait Location {
    var ancestorOrigins: Array<String>
        get() = noImpl
        set(value) = noImpl
    fun assign(url: String): Unit = noImpl
    fun replace(url: String): Unit = noImpl
    fun reload(): Unit = noImpl
}

native public open class PopStateEvent(type: String, eventInitDict: PopStateEventInit = noImpl) : Event(type, eventInitDict) {
    var state: Any?
        get() = noImpl
        set(value) = noImpl
}

native public open class PopStateEventInit : EventInit() {
    var state: Any?
}

native public open class HashChangeEvent(type: String, eventInitDict: HashChangeEventInit = noImpl) : Event(type, eventInitDict) {
    var oldURL: String
        get() = noImpl
        set(value) = noImpl
    var newURL: String
        get() = noImpl
        set(value) = noImpl
}

native public open class HashChangeEventInit : EventInit() {
    var oldURL: String
    var newURL: String
}

native public open class PageTransitionEvent(type: String, eventInitDict: PageTransitionEventInit = noImpl) : Event(type, eventInitDict) {
    var persisted: Boolean
        get() = noImpl
        set(value) = noImpl
}

native public open class PageTransitionEventInit : EventInit() {
    var persisted: Boolean
}

native public open class BeforeUnloadEvent : Event(noImpl, noImpl) {
    var returnValue: String
        get() = noImpl
        set(value) = noImpl
}

native public trait ApplicationCache : EventTarget {
    var status: Short
        get() = noImpl
        set(value) = noImpl
    var onchecking: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onerror: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onnoupdate: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var ondownloading: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onprogress: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onupdateready: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var oncached: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onobsolete: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    fun update(): Unit = noImpl
    fun abort(): Unit = noImpl
    fun swapCache(): Unit = noImpl

    companion object {
        val UNCACHED: Short = 0
        val IDLE: Short = 1
        val CHECKING: Short = 2
        val DOWNLOADING: Short = 3
        val UPDATEREADY: Short = 4
        val OBSOLETE: Short = 5
    }
}

native public open class ErrorEvent(type: String, eventInitDict: ErrorEventInit = noImpl) : Event(type, eventInitDict) {
    var message: String
        get() = noImpl
        set(value) = noImpl
    var filename: String
        get() = noImpl
        set(value) = noImpl
    var lineno: Int
        get() = noImpl
        set(value) = noImpl
    var colno: Int
        get() = noImpl
        set(value) = noImpl
    var error: Any?
        get() = noImpl
        set(value) = noImpl
}

native public open class ErrorEventInit : EventInit() {
    var message: String
    var filename: String
    var lineno: Int
    var colno: Int
    var error: Any?
}

native public trait Navigator {
    var serviceWorker: ServiceWorkerContainer
        get() = noImpl
        set(value) = noImpl
    var appCodeName: String
        get() = noImpl
        set(value) = noImpl
    var appName: String
        get() = noImpl
        set(value) = noImpl
    var appVersion: String
        get() = noImpl
        set(value) = noImpl
    var platform: String
        get() = noImpl
        set(value) = noImpl
    var product: String
        get() = noImpl
        set(value) = noImpl
    var userAgent: String
        get() = noImpl
        set(value) = noImpl
    var vendorSub: String
        get() = noImpl
        set(value) = noImpl
    var language: String?
        get() = noImpl
        set(value) = noImpl
    var languages: Array<String>
        get() = noImpl
        set(value) = noImpl
    var onLine: Boolean
        get() = noImpl
        set(value) = noImpl
    var cookieEnabled: Boolean
        get() = noImpl
        set(value) = noImpl
    var plugins: PluginArray
        get() = noImpl
        set(value) = noImpl
    var mimeTypes: MimeTypeArray
        get() = noImpl
        set(value) = noImpl
    var javaEnabled: Boolean
        get() = noImpl
        set(value) = noImpl
    fun vibrate(pattern: dynamic): Boolean = noImpl
    fun taintEnabled(): Boolean = noImpl
    fun registerProtocolHandler(scheme: String, url: String, title: String): Unit = noImpl
    fun registerContentHandler(mimeType: String, url: String, title: String): Unit = noImpl
    fun isProtocolHandlerRegistered(scheme: String, url: String): String = noImpl
    fun isContentHandlerRegistered(mimeType: String, url: String): String = noImpl
    fun unregisterProtocolHandler(scheme: String, url: String): Unit = noImpl
    fun unregisterContentHandler(mimeType: String, url: String): Unit = noImpl
    fun yieldForStorageUpdates(): Unit = noImpl
}

native public trait PluginArray {
    var length: Int
        get() = noImpl
        set(value) = noImpl
    fun refresh(reload: Boolean = false): Unit = noImpl
    fun item(index: Int): Plugin? = noImpl
    nativeGetter fun get(index: Int): Plugin? = noImpl
    fun namedItem(name: String): Plugin? = noImpl
    nativeGetter fun get(name: String): Plugin? = noImpl
}

native public trait MimeTypeArray {
    var length: Int
        get() = noImpl
        set(value) = noImpl
    fun item(index: Int): MimeType? = noImpl
    nativeGetter fun get(index: Int): MimeType? = noImpl
    fun namedItem(name: String): MimeType? = noImpl
    nativeGetter fun get(name: String): MimeType? = noImpl
}

native public trait Plugin {
    var name: String
        get() = noImpl
        set(value) = noImpl
    var description: String
        get() = noImpl
        set(value) = noImpl
    var filename: String
        get() = noImpl
        set(value) = noImpl
    var length: Int
        get() = noImpl
        set(value) = noImpl
    fun item(index: Int): MimeType? = noImpl
    nativeGetter fun get(index: Int): MimeType? = noImpl
    fun namedItem(name: String): MimeType? = noImpl
    nativeGetter fun get(name: String): MimeType? = noImpl
}

native public trait MimeType {
    var type: String
        get() = noImpl
        set(value) = noImpl
    var description: String
        get() = noImpl
        set(value) = noImpl
    var suffixes: String
        get() = noImpl
        set(value) = noImpl
    var enabledPlugin: Plugin
        get() = noImpl
        set(value) = noImpl
}

native public trait External {
    fun AddSearchProvider(engineURL: String): Unit = noImpl
    fun IsSearchProviderInstalled(engineURL: String): Int = noImpl
}

native public trait ImageBitmap : CanvasImageSource, ImageBitmapSource {
    var width: Int
        get() = noImpl
        set(value) = noImpl
    var height: Int
        get() = noImpl
        set(value) = noImpl
}

native public open class MessageEvent(type: String, eventInitDict: MessageEventInit = noImpl) : Event(type, eventInitDict) {
    var data: Any?
        get() = noImpl
        set(value) = noImpl
    var origin: String
        get() = noImpl
        set(value) = noImpl
    var lastEventId: String
        get() = noImpl
        set(value) = noImpl
    var source: UnionMessagePortOrWindow?
        get() = noImpl
        set(value) = noImpl
    var ports: Array<dynamic>
        get() = noImpl
        set(value) = noImpl
    fun initMessageEvent(typeArg: String, canBubbleArg: Boolean, cancelableArg: Boolean, dataArg: Any?, originArg: String, lastEventIdArg: String, sourceArg: UnionMessagePortOrWindow, portsArg: Array<dynamic>): Unit = noImpl
}

native public open class MessageEventInit : EventInit() {
    var data: Any?
    var origin: String
    var lastEventId: String
    var source: UnionMessagePortOrWindow?
    var ports: Array<MessagePort>
}

native public open class EventSource(url: String, eventSourceInitDict: EventSourceInit = noImpl) : EventTarget {
    var url: String
        get() = noImpl
        set(value) = noImpl
    var withCredentials: Boolean
        get() = noImpl
        set(value) = noImpl
    var readyState: Short
        get() = noImpl
        set(value) = noImpl
    var onopen: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onmessage: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onerror: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    fun close(): Unit = noImpl

    companion object {
        val CONNECTING: Short = 0
        val OPEN: Short = 1
        val CLOSED: Short = 2
    }
}

native public open class EventSourceInit {
    var withCredentials: Boolean = false
}

native public open class WebSocket(url: String, protocols: dynamic = noImpl) : EventTarget {
    var url: String
        get() = noImpl
        set(value) = noImpl
    var readyState: Short
        get() = noImpl
        set(value) = noImpl
    var bufferedAmount: Int
        get() = noImpl
        set(value) = noImpl
    var onopen: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onerror: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onclose: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var extensions: String
        get() = noImpl
        set(value) = noImpl
    var protocol: String
        get() = noImpl
        set(value) = noImpl
    var onmessage: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var binaryType: String
        get() = noImpl
        set(value) = noImpl
    fun close(code: Short = noImpl, reason: String = noImpl): Unit = noImpl
    fun send(data: String): Unit = noImpl
    fun send(data: Blob): Unit = noImpl
    fun send(data: ArrayBuffer): Unit = noImpl
    fun send(data: ArrayBufferView): Unit = noImpl

    companion object {
        val CONNECTING: Short = 0
        val OPEN: Short = 1
        val CLOSING: Short = 2
        val CLOSED: Short = 3
    }
}

native public open class CloseEvent(type: String, eventInitDict: CloseEventInit = noImpl) : Event(type, eventInitDict) {
    var wasClean: Boolean
        get() = noImpl
        set(value) = noImpl
    var code: Short
        get() = noImpl
        set(value) = noImpl
    var reason: String
        get() = noImpl
        set(value) = noImpl
}

native public open class CloseEventInit : EventInit() {
    var wasClean: Boolean
    var code: Short
    var reason: String
}

native public open class MessageChannel {
    var port1: MessagePort
        get() = noImpl
        set(value) = noImpl
    var port2: MessagePort
        get() = noImpl
        set(value) = noImpl
}

native public trait MessagePort : EventTarget, UnionMessagePortOrWindow, UnionMessagePortOrServiceWorker, UnionClientOrMessagePortOrServiceWorker, Transferable {
    var onmessage: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    fun postMessage(message: Any?, transfer: Array<Transferable> = noImpl): Unit = noImpl
    fun start(): Unit = noImpl
    fun close(): Unit = noImpl
}

native public open class PortCollection {
    fun add(port: MessagePort): Unit = noImpl
    fun remove(port: MessagePort): Unit = noImpl
    fun clear(): Unit = noImpl
    fun iterate(callback: (MessagePort) -> Unit): Unit = noImpl
}

native public open class BroadcastChannel(channel: String) : EventTarget {
    var name: String
        get() = noImpl
        set(value) = noImpl
    var onmessage: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    fun postMessage(message: Any?): Unit = noImpl
    fun close(): Unit = noImpl
}

native public trait WorkerGlobalScope : EventTarget {
    var caches: CacheStorage
        get() = noImpl
        set(value) = noImpl
    var self: WorkerGlobalScope
        get() = noImpl
        set(value) = noImpl
    var location: WorkerLocation
        get() = noImpl
        set(value) = noImpl
    var onerror: ((dynamic, dynamic, String, Int, Int, Any?) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onlanguagechange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onoffline: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var ononline: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var navigator: WorkerNavigator
        get() = noImpl
        set(value) = noImpl
    fun close(): Unit = noImpl
    fun importScripts(vararg urls: String): Unit = noImpl
    fun createImageBitmap(image: ImageBitmapSource): dynamic = noImpl
    fun createImageBitmap(image: ImageBitmapSource, sx: Int, sy: Int, sw: Int, sh: Int): dynamic = noImpl
    fun setTimeout(handler: () -> dynamic, timeout: Int = 0, vararg arguments: Any?): Int = noImpl
    fun setTimeout(handler: String, timeout: Int = 0, vararg arguments: Any?): Int = noImpl
    fun clearTimeout(handle: Int = 0): Unit = noImpl
    fun setInterval(handler: () -> dynamic, timeout: Int = 0, vararg arguments: Any?): Int = noImpl
    fun setInterval(handler: String, timeout: Int = 0, vararg arguments: Any?): Int = noImpl
    fun clearInterval(handle: Int = 0): Unit = noImpl
    fun btoa(btoa: String): String = noImpl
    fun atob(atob: String): String = noImpl
    fun fetch(input: dynamic, init: RequestInit = noImpl): dynamic = noImpl
}

native public trait DedicatedWorkerGlobalScope : WorkerGlobalScope {
    var onmessage: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    fun postMessage(message: Any?, transfer: Array<Transferable> = noImpl): Unit = noImpl
}

native public trait SharedWorkerGlobalScope : WorkerGlobalScope {
    var name: String
        get() = noImpl
        set(value) = noImpl
    var applicationCache: ApplicationCache
        get() = noImpl
        set(value) = noImpl
    var onconnect: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
}

native public open class Worker(scriptURL: String) : EventTarget {
    var onmessage: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onerror: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    fun terminate(): Unit = noImpl
    fun postMessage(message: Any?, transfer: Array<Transferable> = noImpl): Unit = noImpl
}

native public open class SharedWorker(scriptURL: String, name: String = noImpl) : EventTarget {
    var port: MessagePort
        get() = noImpl
        set(value) = noImpl
    var onerror: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
}

native public trait WorkerNavigator {
    var serviceWorker: ServiceWorkerContainer
        get() = noImpl
        set(value) = noImpl
    var appCodeName: String
        get() = noImpl
        set(value) = noImpl
    var appName: String
        get() = noImpl
        set(value) = noImpl
    var appVersion: String
        get() = noImpl
        set(value) = noImpl
    var platform: String
        get() = noImpl
        set(value) = noImpl
    var product: String
        get() = noImpl
        set(value) = noImpl
    var userAgent: String
        get() = noImpl
        set(value) = noImpl
    var vendorSub: String
        get() = noImpl
        set(value) = noImpl
    var language: String?
        get() = noImpl
        set(value) = noImpl
    var languages: Array<String>
        get() = noImpl
        set(value) = noImpl
    var onLine: Boolean
        get() = noImpl
        set(value) = noImpl
    fun taintEnabled(): Boolean = noImpl
}

native public trait WorkerLocation {
}

native public trait Storage {
    var length: Int
        get() = noImpl
        set(value) = noImpl
    fun key(index: Int): String? = noImpl
    fun getItem(key: String): String? = noImpl
    nativeGetter fun get(key: String): String? = noImpl
    fun setItem(key: String, value: String): Unit = noImpl
    nativeSetter fun set(key: String, value: String): Unit = noImpl
    fun removeItem(key: String): Unit = noImpl
    fun clear(): Unit = noImpl
}

native public open class StorageEvent(type: String, eventInitDict: StorageEventInit = noImpl) : Event(type, eventInitDict) {
    var key: String?
        get() = noImpl
        set(value) = noImpl
    var oldValue: String?
        get() = noImpl
        set(value) = noImpl
    var newValue: String?
        get() = noImpl
        set(value) = noImpl
    var url: String
        get() = noImpl
        set(value) = noImpl
    var storageArea: Storage?
        get() = noImpl
        set(value) = noImpl
}

native public open class StorageEventInit : EventInit() {
    var key: String?
    var oldValue: String?
    var newValue: String?
    var url: String
    var storageArea: Storage?
}

native public trait HTMLAppletElement : HTMLElement {
    var align: String
        get() = noImpl
        set(value) = noImpl
    var alt: String
        get() = noImpl
        set(value) = noImpl
    var archive: String
        get() = noImpl
        set(value) = noImpl
    var code: String
        get() = noImpl
        set(value) = noImpl
    var codeBase: String
        get() = noImpl
        set(value) = noImpl
    var height: String
        get() = noImpl
        set(value) = noImpl
    var hspace: Int
        get() = noImpl
        set(value) = noImpl
    var name: String
        get() = noImpl
        set(value) = noImpl
    var _object: String
        get() = noImpl
        set(value) = noImpl
    var vspace: Int
        get() = noImpl
        set(value) = noImpl
    var width: String
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLMarqueeElement : HTMLElement {
    var behavior: String
        get() = noImpl
        set(value) = noImpl
    var bgColor: String
        get() = noImpl
        set(value) = noImpl
    var direction: String
        get() = noImpl
        set(value) = noImpl
    var height: String
        get() = noImpl
        set(value) = noImpl
    var hspace: Int
        get() = noImpl
        set(value) = noImpl
    var loop: Int
        get() = noImpl
        set(value) = noImpl
    var scrollAmount: Int
        get() = noImpl
        set(value) = noImpl
    var scrollDelay: Int
        get() = noImpl
        set(value) = noImpl
    var trueSpeed: Boolean
        get() = noImpl
        set(value) = noImpl
    var vspace: Int
        get() = noImpl
        set(value) = noImpl
    var width: String
        get() = noImpl
        set(value) = noImpl
    var onbounce: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onfinish: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onstart: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    fun start(): Unit = noImpl
    fun stop(): Unit = noImpl
}

native public trait HTMLFrameSetElement : HTMLElement {
    var cols: String
        get() = noImpl
        set(value) = noImpl
    var rows: String
        get() = noImpl
        set(value) = noImpl
    var onafterprint: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onbeforeprint: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onbeforeunload: ((Event) -> String?)?
        get() = noImpl
        set(value) = noImpl
    var onhashchange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onlanguagechange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onmessage: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onoffline: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var ononline: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onpagehide: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onpageshow: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onpopstate: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onstorage: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onunload: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLFrameElement : HTMLElement {
    var name: String
        get() = noImpl
        set(value) = noImpl
    var scrolling: String
        get() = noImpl
        set(value) = noImpl
    var src: String
        get() = noImpl
        set(value) = noImpl
    var frameBorder: String
        get() = noImpl
        set(value) = noImpl
    var longDesc: String
        get() = noImpl
        set(value) = noImpl
    var noResize: Boolean
        get() = noImpl
        set(value) = noImpl
    var contentDocument: Document?
        get() = noImpl
        set(value) = noImpl
    var contentWindow: Window?
        get() = noImpl
        set(value) = noImpl
    var marginHeight: String
        get() = noImpl
        set(value) = noImpl
    var marginWidth: String
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLDirectoryElement : HTMLElement {
    var compact: Boolean
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLFontElement : HTMLElement {
    var color: String
        get() = noImpl
        set(value) = noImpl
    var face: String
        get() = noImpl
        set(value) = noImpl
    var size: String
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLImageElement : HTMLElement, CanvasImageSource, ImageBitmapSource {
    var name: String
        get() = noImpl
        set(value) = noImpl
    var lowsrc: String
        get() = noImpl
        set(value) = noImpl
    var align: String
        get() = noImpl
        set(value) = noImpl
    var hspace: Int
        get() = noImpl
        set(value) = noImpl
    var vspace: Int
        get() = noImpl
        set(value) = noImpl
    var longDesc: String
        get() = noImpl
        set(value) = noImpl
    var border: String
        get() = noImpl
        set(value) = noImpl
    var alt: String
        get() = noImpl
        set(value) = noImpl
    var src: String
        get() = noImpl
        set(value) = noImpl
    var srcset: String
        get() = noImpl
        set(value) = noImpl
    var sizes: String
        get() = noImpl
        set(value) = noImpl
    var crossOrigin: String?
        get() = noImpl
        set(value) = noImpl
    var useMap: String
        get() = noImpl
        set(value) = noImpl
    var isMap: Boolean
        get() = noImpl
        set(value) = noImpl
    var width: Int
        get() = noImpl
        set(value) = noImpl
    var height: Int
        get() = noImpl
        set(value) = noImpl
    var naturalWidth: Int
        get() = noImpl
        set(value) = noImpl
    var naturalHeight: Int
        get() = noImpl
        set(value) = noImpl
    var complete: Boolean
        get() = noImpl
        set(value) = noImpl
    var currentSrc: String
        get() = noImpl
        set(value) = noImpl
}

native public trait HTMLPictureElement : HTMLElement {
}

native public open class Event(type: String, eventInitDict: EventInit = noImpl) {
    var type: String
        get() = noImpl
        set(value) = noImpl
    var target: EventTarget?
        get() = noImpl
        set(value) = noImpl
    var currentTarget: EventTarget?
        get() = noImpl
        set(value) = noImpl
    var eventPhase: Short
        get() = noImpl
        set(value) = noImpl
    var bubbles: Boolean
        get() = noImpl
        set(value) = noImpl
    var cancelable: Boolean
        get() = noImpl
        set(value) = noImpl
    var defaultPrevented: Boolean
        get() = noImpl
        set(value) = noImpl
    var isTrusted: Boolean
        get() = noImpl
        set(value) = noImpl
    var timeStamp: Number
        get() = noImpl
        set(value) = noImpl
    fun stopPropagation(): Unit = noImpl
    fun stopImmediatePropagation(): Unit = noImpl
    fun preventDefault(): Unit = noImpl
    fun initEvent(type: String, bubbles: Boolean, cancelable: Boolean): Unit = noImpl

    companion object {
        val NONE: Short = 0
        val CAPTURING_PHASE: Short = 1
        val AT_TARGET: Short = 2
        val BUBBLING_PHASE: Short = 3
    }
}

native public open class EventInit {
    var bubbles: Boolean = false
    var cancelable: Boolean = false
}

native public open class CustomEvent(type: String, eventInitDict: CustomEventInit = noImpl) : Event(type, eventInitDict) {
    var detail: Any?
        get() = noImpl
        set(value) = noImpl
    fun initCustomEvent(type: String, bubbles: Boolean, cancelable: Boolean, detail: Any?): Unit = noImpl
}

native public open class CustomEventInit : EventInit() {
    var detail: Any? = null
}

native public trait EventTarget {
    fun addEventListener(type: String, callback: EventListener?, capture: Boolean = false): Unit = noImpl
    fun addEventListener(type: String, callback: ((Event) -> Unit)?, capture: Boolean = false): Unit = noImpl
    fun removeEventListener(type: String, callback: EventListener?, capture: Boolean = false): Unit = noImpl
    fun removeEventListener(type: String, callback: ((Event) -> Unit)?, capture: Boolean = false): Unit = noImpl
    fun dispatchEvent(event: Event): Boolean = noImpl
}

native public trait EventListener {
    fun handleEvent(event: Event): Unit = noImpl
}

native public trait NodeList {
    var length: Int
        get() = noImpl
        set(value) = noImpl
    fun item(index: Int): Node? = noImpl
    nativeGetter fun get(index: Int): Node? = noImpl
}

native public trait HTMLCollection : UnionElementOrHTMLCollection {
    var length: Int
        get() = noImpl
        set(value) = noImpl
    fun item(index: Int): Element? = noImpl
    nativeGetter fun get(index: Int): Element? = noImpl
    fun namedItem(name: String): Element? = noImpl
    nativeGetter fun get(name: String): Element? = noImpl
}

native public open class MutationObserver(callback: (Array<MutationRecord>, MutationObserver) -> Unit) {
    fun observe(target: Node, options: MutationObserverInit): Unit = noImpl
    fun disconnect(): Unit = noImpl
    fun takeRecords(): Array<MutationRecord> = noImpl
}

native public open class MutationObserverInit {
    var childList: Boolean = false
    var attributes: Boolean
    var characterData: Boolean
    var subtree: Boolean = false
    var attributeOldValue: Boolean
    var characterDataOldValue: Boolean
    var attributeFilter: Array<String>
}

native public trait MutationRecord {
    var type: String
        get() = noImpl
        set(value) = noImpl
    var target: Node
        get() = noImpl
        set(value) = noImpl
    var addedNodes: NodeList
        get() = noImpl
        set(value) = noImpl
    var removedNodes: NodeList
        get() = noImpl
        set(value) = noImpl
    var previousSibling: Node?
        get() = noImpl
        set(value) = noImpl
    var nextSibling: Node?
        get() = noImpl
        set(value) = noImpl
    var attributeName: String?
        get() = noImpl
        set(value) = noImpl
    var attributeNamespace: String?
        get() = noImpl
        set(value) = noImpl
    var oldValue: String?
        get() = noImpl
        set(value) = noImpl
}

native public trait Node : EventTarget {
    var nodeType: Short
        get() = noImpl
        set(value) = noImpl
    var nodeName: String
        get() = noImpl
        set(value) = noImpl
    var baseURI: String?
        get() = noImpl
        set(value) = noImpl
    var ownerDocument: Document?
        get() = noImpl
        set(value) = noImpl
    var parentNode: Node?
        get() = noImpl
        set(value) = noImpl
    var parentElement: Element?
        get() = noImpl
        set(value) = noImpl
    var childNodes: NodeList
        get() = noImpl
        set(value) = noImpl
    var firstChild: Node?
        get() = noImpl
        set(value) = noImpl
    var lastChild: Node?
        get() = noImpl
        set(value) = noImpl
    var previousSibling: Node?
        get() = noImpl
        set(value) = noImpl
    var nextSibling: Node?
        get() = noImpl
        set(value) = noImpl
    var nodeValue: String?
        get() = noImpl
        set(value) = noImpl
    var textContent: String?
        get() = noImpl
        set(value) = noImpl
    fun hasChildNodes(): Boolean = noImpl
    fun normalize(): Unit = noImpl
    fun cloneNode(deep: Boolean = false): Node = noImpl
    fun isEqualNode(otherNode: Node?): Boolean = noImpl
    fun compareDocumentPosition(other: Node): Short = noImpl
    fun contains(other: Node?): Boolean = noImpl
    fun lookupPrefix(namespace: String?): String? = noImpl
    fun lookupNamespaceURI(prefix: String?): String? = noImpl
    fun isDefaultNamespace(namespace: String?): Boolean = noImpl
    fun insertBefore(node: Node, child: Node?): Node = noImpl
    fun appendChild(node: Node): Node = noImpl
    fun replaceChild(node: Node, child: Node): Node = noImpl
    fun removeChild(child: Node): Node = noImpl

    companion object {
        val ELEMENT_NODE: Short = 1
        val ATTRIBUTE_NODE: Short = 2
        val TEXT_NODE: Short = 3
        val CDATA_SECTION_NODE: Short = 4
        val ENTITY_REFERENCE_NODE: Short = 5
        val ENTITY_NODE: Short = 6
        val PROCESSING_INSTRUCTION_NODE: Short = 7
        val COMMENT_NODE: Short = 8
        val DOCUMENT_NODE: Short = 9
        val DOCUMENT_TYPE_NODE: Short = 10
        val DOCUMENT_FRAGMENT_NODE: Short = 11
        val NOTATION_NODE: Short = 12
        val DOCUMENT_POSITION_DISCONNECTED: Short = 0x01
        val DOCUMENT_POSITION_PRECEDING: Short = 0x02
        val DOCUMENT_POSITION_FOLLOWING: Short = 0x04
        val DOCUMENT_POSITION_CONTAINS: Short = 0x08
        val DOCUMENT_POSITION_CONTAINED_BY: Short = 0x10
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short = 0x20
    }
}

native public trait DOMImplementation {
    fun createDocumentType(qualifiedName: String, publicId: String, systemId: String): DocumentType = noImpl
    fun createDocument(namespace: String?, qualifiedName: String, doctype: DocumentType? = null): XMLDocument = noImpl
    fun createHTMLDocument(title: String = noImpl): Document = noImpl
    fun hasFeature(): Boolean = noImpl
}

native public open class DocumentFragment : Node {
    var children: HTMLCollection
        get() = noImpl
        set(value) = noImpl
    var firstElementChild: Element?
        get() = noImpl
        set(value) = noImpl
    var lastElementChild: Element?
        get() = noImpl
        set(value) = noImpl
    var childElementCount: Int
        get() = noImpl
        set(value) = noImpl
    fun getElementById(elementId: String): Element? = noImpl
    fun prepend(vararg nodes: dynamic): Unit = noImpl
    fun append(vararg nodes: dynamic): Unit = noImpl
    fun query(relativeSelectors: String): Element? = noImpl
    fun queryAll(relativeSelectors: String): dynamic = noImpl
    fun querySelector(selectors: String): Element? = noImpl
    fun querySelectorAll(selectors: String): NodeList = noImpl
}

native public trait DocumentType : Node {
    var name: String
        get() = noImpl
        set(value) = noImpl
    var publicId: String
        get() = noImpl
        set(value) = noImpl
    var systemId: String
        get() = noImpl
        set(value) = noImpl
    fun before(vararg nodes: dynamic): Unit = noImpl
    fun after(vararg nodes: dynamic): Unit = noImpl
    fun replaceWith(vararg nodes: dynamic): Unit = noImpl
    fun remove(): Unit = noImpl
}

native public trait Element : Node, UnionElementOrHTMLCollection, UnionElementOrRadioNodeList, UnionElementOrMouseEvent, UnionElementOrProcessingInstruction {
    var innerHTML: String
        get() = noImpl
        set(value) = noImpl
    var outerHTML: String
        get() = noImpl
        set(value) = noImpl
    var namespaceURI: String?
        get() = noImpl
        set(value) = noImpl
    var prefix: String?
        get() = noImpl
        set(value) = noImpl
    var localName: String
        get() = noImpl
        set(value) = noImpl
    var tagName: String
        get() = noImpl
        set(value) = noImpl
    var id: String
        get() = noImpl
        set(value) = noImpl
    var className: String
        get() = noImpl
        set(value) = noImpl
    var classList: DOMTokenList
        get() = noImpl
        set(value) = noImpl
    var attributes: NamedNodeMap
        get() = noImpl
        set(value) = noImpl
    var children: HTMLCollection
        get() = noImpl
        set(value) = noImpl
    var firstElementChild: Element?
        get() = noImpl
        set(value) = noImpl
    var lastElementChild: Element?
        get() = noImpl
        set(value) = noImpl
    var childElementCount: Int
        get() = noImpl
        set(value) = noImpl
    var previousElementSibling: Element?
        get() = noImpl
        set(value) = noImpl
    var nextElementSibling: Element?
        get() = noImpl
        set(value) = noImpl
    var cascadedStyle: CSSStyleDeclaration
        get() = noImpl
        set(value) = noImpl
    var defaultStyle: CSSStyleDeclaration
        get() = noImpl
        set(value) = noImpl
    var rawComputedStyle: CSSStyleDeclaration
        get() = noImpl
        set(value) = noImpl
    var usedStyle: CSSStyleDeclaration
        get() = noImpl
        set(value) = noImpl
    fun requestFullscreen(): Unit = noImpl
    fun pseudo(pseudoElt: String): PseudoElement? = noImpl
    fun insertAdjacentHTML(position: String, text: String): Unit = noImpl
    fun hasAttributes(): Boolean = noImpl
    fun getAttribute(name: String): String? = noImpl
    fun getAttributeNS(namespace: String?, localName: String): String? = noImpl
    fun setAttribute(name: String, value: String): Unit = noImpl
    fun setAttributeNS(namespace: String?, name: String, value: String): Unit = noImpl
    fun removeAttribute(name: String): Unit = noImpl
    fun removeAttributeNS(namespace: String?, localName: String): Unit = noImpl
    fun hasAttribute(name: String): Boolean = noImpl
    fun hasAttributeNS(namespace: String?, localName: String): Boolean = noImpl
    fun getAttributeNode(name: String): Attr? = noImpl
    fun getAttributeNodeNS(namespace: String?, localName: String): Attr? = noImpl
    fun setAttributeNode(attr: Attr): Attr? = noImpl
    fun setAttributeNodeNS(attr: Attr): Attr? = noImpl
    fun removeAttributeNode(attr: Attr): Attr = noImpl
    fun closest(selectors: String): Element? = noImpl
    fun matches(selectors: String): Boolean = noImpl
    fun getElementsByTagName(localName: String): HTMLCollection = noImpl
    fun getElementsByTagNameNS(namespace: String?, localName: String): HTMLCollection = noImpl
    fun getElementsByClassName(classNames: String): HTMLCollection = noImpl
    fun prepend(vararg nodes: dynamic): Unit = noImpl
    fun append(vararg nodes: dynamic): Unit = noImpl
    fun query(relativeSelectors: String): Element? = noImpl
    fun queryAll(relativeSelectors: String): dynamic = noImpl
    fun querySelector(selectors: String): Element? = noImpl
    fun querySelectorAll(selectors: String): NodeList = noImpl
    fun before(vararg nodes: dynamic): Unit = noImpl
    fun after(vararg nodes: dynamic): Unit = noImpl
    fun replaceWith(vararg nodes: dynamic): Unit = noImpl
    fun remove(): Unit = noImpl
}

native public trait NamedNodeMap {
    var length: Int
        get() = noImpl
        set(value) = noImpl
    fun item(index: Int): Attr? = noImpl
    nativeGetter fun get(index: Int): Attr? = noImpl
    fun getNamedItem(name: String): Attr? = noImpl
    nativeGetter fun get(name: String): Attr? = noImpl
    fun getNamedItemNS(namespace: String?, localName: String): Attr? = noImpl
    fun setNamedItem(attr: Attr): Attr? = noImpl
    fun setNamedItemNS(attr: Attr): Attr? = noImpl
    fun removeNamedItem(name: String): Attr = noImpl
    fun removeNamedItemNS(namespace: String?, localName: String): Attr = noImpl
}

native public trait Attr {
    var namespaceURI: String?
        get() = noImpl
        set(value) = noImpl
    var prefix: String?
        get() = noImpl
        set(value) = noImpl
    var localName: String
        get() = noImpl
        set(value) = noImpl
    var name: String
        get() = noImpl
        set(value) = noImpl
    var value: String
        get() = noImpl
        set(value) = noImpl
    var nodeValue: String
        get() = noImpl
        set(value) = noImpl
    var textContent: String
        get() = noImpl
        set(value) = noImpl
    var ownerElement: Element?
        get() = noImpl
        set(value) = noImpl
    var specified: Boolean
        get() = noImpl
        set(value) = noImpl
}

native public trait CharacterData : Node {
    var data: String
        get() = noImpl
        set(value) = noImpl
    var length: Int
        get() = noImpl
        set(value) = noImpl
    var previousElementSibling: Element?
        get() = noImpl
        set(value) = noImpl
    var nextElementSibling: Element?
        get() = noImpl
        set(value) = noImpl
    fun substringData(offset: Int, count: Int): String = noImpl
    fun appendData(data: String): Unit = noImpl
    fun insertData(offset: Int, data: String): Unit = noImpl
    fun deleteData(offset: Int, count: Int): Unit = noImpl
    fun replaceData(offset: Int, count: Int, data: String): Unit = noImpl
    fun before(vararg nodes: dynamic): Unit = noImpl
    fun after(vararg nodes: dynamic): Unit = noImpl
    fun replaceWith(vararg nodes: dynamic): Unit = noImpl
    fun remove(): Unit = noImpl
}

native public open class Text(data: String = "") : CharacterData {
    var wholeText: String
        get() = noImpl
        set(value) = noImpl
    fun splitText(offset: Int): Text = noImpl
}

native public trait ProcessingInstruction : CharacterData, UnionElementOrProcessingInstruction {
    var target: String
        get() = noImpl
        set(value) = noImpl
    var sheet: StyleSheet?
        get() = noImpl
        set(value) = noImpl
}

native public open class Comment(data: String = "") : CharacterData {
}

native public open class Range {
    var startContainer: Node
        get() = noImpl
        set(value) = noImpl
    var startOffset: Int
        get() = noImpl
        set(value) = noImpl
    var endContainer: Node
        get() = noImpl
        set(value) = noImpl
    var endOffset: Int
        get() = noImpl
        set(value) = noImpl
    var collapsed: Boolean
        get() = noImpl
        set(value) = noImpl
    var commonAncestorContainer: Node
        get() = noImpl
        set(value) = noImpl
    fun createContextualFragment(fragment: String): DocumentFragment = noImpl
    fun setStart(node: Node, offset: Int): Unit = noImpl
    fun setEnd(node: Node, offset: Int): Unit = noImpl
    fun setStartBefore(node: Node): Unit = noImpl
    fun setStartAfter(node: Node): Unit = noImpl
    fun setEndBefore(node: Node): Unit = noImpl
    fun setEndAfter(node: Node): Unit = noImpl
    fun collapse(toStart: Boolean = false): Unit = noImpl
    fun selectNode(node: Node): Unit = noImpl
    fun selectNodeContents(node: Node): Unit = noImpl
    fun compareBoundaryPoints(how: Short, sourceRange: Range): Short = noImpl
    fun deleteContents(): Unit = noImpl
    fun extractContents(): DocumentFragment = noImpl
    fun cloneContents(): DocumentFragment = noImpl
    fun insertNode(node: Node): Unit = noImpl
    fun surroundContents(newParent: Node): Unit = noImpl
    fun cloneRange(): Range = noImpl
    fun detach(): Unit = noImpl
    fun isPointInRange(node: Node, offset: Int): Boolean = noImpl
    fun comparePoint(node: Node, offset: Int): Short = noImpl
    fun intersectsNode(node: Node): Boolean = noImpl

    companion object {
        val START_TO_START: Short = 0
        val START_TO_END: Short = 1
        val END_TO_END: Short = 2
        val END_TO_START: Short = 3
    }
}

native public trait NodeIterator {
    var root: Node
        get() = noImpl
        set(value) = noImpl
    var referenceNode: Node
        get() = noImpl
        set(value) = noImpl
    var pointerBeforeReferenceNode: Boolean
        get() = noImpl
        set(value) = noImpl
    var whatToShow: Int
        get() = noImpl
        set(value) = noImpl
    var filter: NodeFilter?
        get() = noImpl
        set(value) = noImpl
    fun nextNode(): Node? = noImpl
    fun previousNode(): Node? = noImpl
    fun detach(): Unit = noImpl
}

native public trait TreeWalker {
    var root: Node
        get() = noImpl
        set(value) = noImpl
    var whatToShow: Int
        get() = noImpl
        set(value) = noImpl
    var filter: NodeFilter?
        get() = noImpl
        set(value) = noImpl
    var currentNode: Node
        get() = noImpl
        set(value) = noImpl
    fun parentNode(): Node? = noImpl
    fun firstChild(): Node? = noImpl
    fun lastChild(): Node? = noImpl
    fun previousSibling(): Node? = noImpl
    fun nextSibling(): Node? = noImpl
    fun previousNode(): Node? = noImpl
    fun nextNode(): Node? = noImpl
}

native public trait NodeFilter {
    fun acceptNode(node: Node): Short = noImpl

    companion object {
        val FILTER_ACCEPT: Short = 1
        val FILTER_REJECT: Short = 2
        val FILTER_SKIP: Short = 3
        val SHOW_ALL: Int = noImpl
        val SHOW_ELEMENT: Int = 0x1
        val SHOW_ATTRIBUTE: Int = 0x2
        val SHOW_TEXT: Int = 0x4
        val SHOW_CDATA_SECTION: Int = 0x8
        val SHOW_ENTITY_REFERENCE: Int = 0x10
        val SHOW_ENTITY: Int = 0x20
        val SHOW_PROCESSING_INSTRUCTION: Int = 0x40
        val SHOW_COMMENT: Int = 0x80
        val SHOW_DOCUMENT: Int = 0x100
        val SHOW_DOCUMENT_TYPE: Int = 0x200
        val SHOW_DOCUMENT_FRAGMENT: Int = 0x400
        val SHOW_NOTATION: Int = 0x800
    }
}

native public trait DOMTokenList {
    var length: Int
        get() = noImpl
        set(value) = noImpl
    fun item(index: Int): String? = noImpl
    nativeGetter fun get(index: Int): String? = noImpl
    fun contains(token: String): Boolean = noImpl
    fun add(vararg tokens: String): Unit = noImpl
    fun remove(vararg tokens: String): Unit = noImpl
    fun toggle(token: String, force: Boolean = noImpl): Boolean = noImpl
}

native public trait DOMSettableTokenList : DOMTokenList {
    var value: String
        get() = noImpl
        set(value) = noImpl
}

native public trait Selection {
    var anchorNode: Node?
        get() = noImpl
        set(value) = noImpl
    var anchorOffset: Int
        get() = noImpl
        set(value) = noImpl
    var focusNode: Node?
        get() = noImpl
        set(value) = noImpl
    var focusOffset: Int
        get() = noImpl
        set(value) = noImpl
    var isCollapsed: Boolean
        get() = noImpl
        set(value) = noImpl
    var rangeCount: Int
        get() = noImpl
        set(value) = noImpl
    fun collapse(node: Node, offset: Int): Unit = noImpl
    fun collapseToStart(): Unit = noImpl
    fun collapseToEnd(): Unit = noImpl
    fun extend(node: Node, offset: Int): Unit = noImpl
    fun selectAllChildren(node: Node): Unit = noImpl
    fun deleteFromDocument(): Unit = noImpl
    fun getRangeAt(index: Int): Range = noImpl
    fun addRange(range: Range): Unit = noImpl
    fun removeRange(range: Range): Unit = noImpl
    fun removeAllRanges(): Unit = noImpl
}

native public open class EditingBeforeInputEvent(type: String, eventInitDict: EditingBeforeInputEventInit = noImpl) : Event(type, eventInitDict) {
    var command: String
        get() = noImpl
        set(value) = noImpl
    var value: String
        get() = noImpl
        set(value) = noImpl
}

native public open class EditingBeforeInputEventInit : EventInit() {
    var command: String
    var value: String
}

native public open class EditingInputEvent(type: String, eventInitDict: EditingInputEventInit = noImpl) : Event(type, eventInitDict) {
    var command: String
        get() = noImpl
        set(value) = noImpl
    var value: String
        get() = noImpl
        set(value) = noImpl
}

native public open class EditingInputEventInit : EventInit() {
    var command: String
    var value: String
}

native public marker trait UnionElementOrHTMLCollection {
}

native public marker trait UnionElementOrRadioNodeList {
}

native public marker trait UnionHTMLOptGroupElementOrHTMLOptionElement {
}

native public marker trait UnionAudioTrackOrTextTrackOrVideoTrack {
}

native public marker trait UnionElementOrMouseEvent {
}

native public marker trait UnionMessagePortOrWindow {
}

native public marker trait UnionElementOrProcessingInstruction {
}

native public marker trait ArrayBufferView {
}

native public marker trait Transferable {
}

native public marker trait RenderingContext {
}

native public marker trait CanvasImageSource {
}

native public marker trait ImageBitmapSource {
}

