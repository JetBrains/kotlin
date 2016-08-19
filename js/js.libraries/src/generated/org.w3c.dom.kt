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
import org.w3c.dom.url.*
import org.w3c.fetch.*
import org.w3c.files.*
import org.w3c.notifications.*
import org.w3c.performance.*
import org.w3c.workers.*
import org.w3c.xhr.*

@native public open class Document : Node, GeometryNode {
    open val fullscreenEnabled: Boolean
        get() = noImpl
    open val fullscreenElement: Element?
        get() = noImpl
    var onfullscreenchange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onfullscreenerror: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    open val location: Location?
        get() = noImpl
    var domain: String
        get() = noImpl
        set(value) = noImpl
    open val referrer: String
        get() = noImpl
    var cookie: String
        get() = noImpl
        set(value) = noImpl
    open val lastModified: String
        get() = noImpl
    open val readyState: String
        get() = noImpl
    var title: String
        get() = noImpl
        set(value) = noImpl
    var dir: String
        get() = noImpl
        set(value) = noImpl
    var body: HTMLElement?
        get() = noImpl
        set(value) = noImpl
    open val head: HTMLHeadElement?
        get() = noImpl
    open val images: HTMLCollection
        get() = noImpl
    open val embeds: HTMLCollection
        get() = noImpl
    open val plugins: HTMLCollection
        get() = noImpl
    open val links: HTMLCollection
        get() = noImpl
    open val forms: HTMLCollection
        get() = noImpl
    open val scripts: HTMLCollection
        get() = noImpl
    open val cssElementMap: DOMElementMap
        get() = noImpl
    open val currentScript: HTMLScriptElement?
        get() = noImpl
    open val defaultView: Window?
        get() = noImpl
    open val activeElement: Element?
        get() = noImpl
    var designMode: String
        get() = noImpl
        set(value) = noImpl
    open val commands: HTMLCollection
        get() = noImpl
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
    open val anchors: HTMLCollection
        get() = noImpl
    open val applets: HTMLCollection
        get() = noImpl
    open val all: HTMLAllCollection
        get() = noImpl
    open val implementation: DOMImplementation
        get() = noImpl
    open val URL: String
        get() = noImpl
    open val documentURI: String
        get() = noImpl
    open val origin: String
        get() = noImpl
    open val compatMode: String
        get() = noImpl
    open val characterSet: String
        get() = noImpl
    open val inputEncoding: String
        get() = noImpl
    open val contentType: String
        get() = noImpl
    open val doctype: DocumentType?
        get() = noImpl
    open val documentElement: Element?
        get() = noImpl
    open val styleSheets: StyleSheetList
        get() = noImpl
    var selectedStyleSheetSet: String?
        get() = noImpl
        set(value) = noImpl
    open val lastStyleSheetSet: String?
        get() = noImpl
    open val preferredStyleSheetSet: String?
        get() = noImpl
    open val styleSheetSets: Array<String>
        get() = noImpl
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
    var onerror: ((dynamic, String, Int, Int, Any?) -> dynamic)?
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
    open val children: HTMLCollection
        get() = noImpl
    open val firstElementChild: Element?
        get() = noImpl
    open val lastElementChild: Element?
        get() = noImpl
    open val childElementCount: Int
        get() = noImpl
    fun exitFullscreen(): Unit = noImpl
    @nativeGetter
    operator fun get(name: String): dynamic = noImpl
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
    fun elementFromPoint(x: Double, y: Double): Element? = noImpl
    fun elementsFromPoint(x: Double, y: Double): Array<Element> = noImpl
    fun caretPositionFromPoint(x: Double, y: Double): CaretPosition? = noImpl
    fun enableStyleSheetsForSet(name: String?): Unit = noImpl
    fun getElementById(elementId: String): Element? = noImpl
    fun prepend(vararg nodes: dynamic): Unit = noImpl
    fun append(vararg nodes: dynamic): Unit = noImpl
    fun query(relativeSelectors: String): Element? = noImpl
    fun queryAll(relativeSelectors: String): dynamic = noImpl
    fun querySelector(selectors: String): Element? = noImpl
    fun querySelectorAll(selectors: String): NodeList = noImpl
    fun getBoxQuads(options: BoxQuadOptions = noImpl): Array<DOMQuad> = noImpl
    fun convertQuadFromNode(quad: DOMQuad, from: GeometryNode, options: ConvertCoordinateOptions = noImpl): DOMQuad = noImpl
    fun convertRectFromNode(rect: DOMRectReadOnly, from: GeometryNode, options: ConvertCoordinateOptions = noImpl): DOMQuad = noImpl
    fun convertPointFromNode(point: DOMPointInit, from: GeometryNode, options: ConvertCoordinateOptions = noImpl): DOMPoint = noImpl
}

@native public interface Window : EventTarget, UnionMessagePortOrWindow {
    val caches: CacheStorage
        get() = noImpl
    val performance: Performance
        get() = noImpl
    val window: Window
        get() = noImpl
    val self: Window
        get() = noImpl
    val document: Document
        get() = noImpl
    var name: String
        get() = noImpl
        set(value) = noImpl
    val location: Location
        get() = noImpl
    val history: History
        get() = noImpl
    val locationbar: BarProp
        get() = noImpl
    val menubar: BarProp
        get() = noImpl
    val personalbar: BarProp
        get() = noImpl
    val scrollbars: BarProp
        get() = noImpl
    val statusbar: BarProp
        get() = noImpl
    val toolbar: BarProp
        get() = noImpl
    var status: String
        get() = noImpl
        set(value) = noImpl
    val closed: Boolean
        get() = noImpl
    val frames: Window
        get() = noImpl
    val length: Int
        get() = noImpl
    val top: Window
        get() = noImpl
    var opener: Any?
        get() = noImpl
        set(value) = noImpl
    val parent: Window
        get() = noImpl
    val frameElement: Element?
        get() = noImpl
    val navigator: Navigator
        get() = noImpl
    val external: External
        get() = noImpl
    val applicationCache: ApplicationCache
        get() = noImpl
    val screen: Screen
        get() = noImpl
    val innerWidth: Double
        get() = noImpl
    val innerHeight: Double
        get() = noImpl
    val scrollX: Double
        get() = noImpl
    val pageXOffset: Double
        get() = noImpl
    val scrollY: Double
        get() = noImpl
    val pageYOffset: Double
        get() = noImpl
    val screenX: Double
        get() = noImpl
    val screenY: Double
        get() = noImpl
    val outerWidth: Double
        get() = noImpl
    val outerHeight: Double
        get() = noImpl
    val devicePixelRatio: Double
        get() = noImpl
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
    var onerror: ((dynamic, String, Int, Int, Any?) -> dynamic)?
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
    val sessionStorage: Storage
        get() = noImpl
    val localStorage: Storage
        get() = noImpl
    fun close(): Unit = noImpl
    fun stop(): Unit = noImpl
    fun focus(): Unit = noImpl
    fun blur(): Unit = noImpl
    fun open(url: String = "about:blank", target: String = "_blank", features: String = "", replace: Boolean = false): Window = noImpl
    @nativeGetter
    operator fun get(index: Int): Window? = noImpl
    @nativeGetter
    operator fun get(name: String): dynamic = noImpl
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
    fun matchMedia(query: String): MediaQueryList = noImpl
    fun moveTo(x: Double, y: Double): Unit = noImpl
    fun moveBy(x: Double, y: Double): Unit = noImpl
    fun resizeTo(x: Double, y: Double): Unit = noImpl
    fun resizeBy(x: Double, y: Double): Unit = noImpl
    fun scroll(x: Double, y: Double, options: ScrollOptions = noImpl): Unit = noImpl
    fun scrollTo(x: Double, y: Double, options: ScrollOptions = noImpl): Unit = noImpl
    fun scrollBy(x: Double, y: Double, options: ScrollOptions = noImpl): Unit = noImpl
    fun getComputedStyle(elt: Element, pseudoElt: String? = noImpl): CSSStyleDeclaration = noImpl
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

@native public interface Element : Node, UnionElementOrProcessingInstruction, UnionElementOrHTMLCollection, UnionElementOrRadioNodeList, UnionElementOrMouseEvent, GeometryNode {
    var innerHTML: String
        get() = noImpl
        set(value) = noImpl
    var outerHTML: String
        get() = noImpl
        set(value) = noImpl
    val namespaceURI: String?
        get() = noImpl
    val prefix: String?
        get() = noImpl
    val localName: String
        get() = noImpl
    val tagName: String
        get() = noImpl
    var id: String
        get() = noImpl
        set(value) = noImpl
    var className: String
        get() = noImpl
        set(value) = noImpl
    val classList: DOMTokenList
        get() = noImpl
    val attributes: NamedNodeMap
        get() = noImpl
    var scrollTop: dynamic
        get() = noImpl
        set(value) = noImpl
    var scrollLeft: dynamic
        get() = noImpl
        set(value) = noImpl
    val scrollWidth: Double
        get() = noImpl
    val scrollHeight: Double
        get() = noImpl
    val clientTop: Double
        get() = noImpl
    val clientLeft: Double
        get() = noImpl
    val clientWidth: Double
        get() = noImpl
    val clientHeight: Double
        get() = noImpl
    val cascadedStyle: CSSStyleDeclaration
        get() = noImpl
    val defaultStyle: CSSStyleDeclaration
        get() = noImpl
    val rawComputedStyle: CSSStyleDeclaration
        get() = noImpl
    val usedStyle: CSSStyleDeclaration
        get() = noImpl
    val children: HTMLCollection
        get() = noImpl
    val firstElementChild: Element?
        get() = noImpl
    val lastElementChild: Element?
        get() = noImpl
    val childElementCount: Int
        get() = noImpl
    val previousElementSibling: Element?
        get() = noImpl
    val nextElementSibling: Element?
        get() = noImpl
    fun requestFullscreen(): Unit = noImpl
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
    fun getClientRects(): dynamic = noImpl
    fun getBoundingClientRect(): DOMRect = noImpl
    fun scrollIntoView(): Unit = noImpl
    fun scrollIntoView(top: Boolean, options: ScrollOptions = noImpl): Unit = noImpl
    fun pseudo(pseudoElt: String): PseudoElement? = noImpl
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
    fun getBoxQuads(options: BoxQuadOptions = noImpl): Array<DOMQuad> = noImpl
    fun convertQuadFromNode(quad: DOMQuad, from: GeometryNode, options: ConvertCoordinateOptions = noImpl): DOMQuad = noImpl
    fun convertRectFromNode(rect: DOMRectReadOnly, from: GeometryNode, options: ConvertCoordinateOptions = noImpl): DOMQuad = noImpl
    fun convertPointFromNode(point: DOMPointInit, from: GeometryNode, options: ConvertCoordinateOptions = noImpl): DOMPoint = noImpl
}

@native public open class CustomEvent(type: String, eventInitDict: CustomEventInit = noImpl) : Event(type, eventInitDict) {
    open val detail: Any?
        get() = noImpl
    fun initCustomEvent(type: String, bubbles: Boolean, cancelable: Boolean, detail: Any?): Unit = noImpl
}

@native public interface HTMLAllCollection : HTMLCollection {
    fun item(name: String): UnionElementOrHTMLCollection? = noImpl
//    override fun namedItem(name: String): UnionElementOrHTMLCollection? = noImpl
//    @nativeGetter
//    operator override fun get(name: String): UnionElementOrHTMLCollection? = noImpl
}

@native public interface HTMLFormControlsCollection : HTMLCollection {
//    override fun namedItem(name: String): UnionElementOrRadioNodeList? = noImpl
//    @nativeGetter
//    operator override fun get(name: String): UnionElementOrRadioNodeList? = noImpl
}

@native public interface RadioNodeList : NodeList, UnionElementOrRadioNodeList {
    var value: String
        get() = noImpl
        set(value) = noImpl
}

@native public interface HTMLOptionsCollection : HTMLCollection {
    override var length: Int
        get() = noImpl
        set(value) = noImpl
    var selectedIndex: Int
        get() = noImpl
        set(value) = noImpl
    @nativeSetter
    operator fun set(index: Int, option: HTMLOptionElement?): Unit = noImpl
    fun add(element: UnionHTMLOptGroupElementOrHTMLOptionElement, before: dynamic = null): Unit = noImpl
    fun remove(index: Int): Unit = noImpl
}

@native public interface HTMLPropertiesCollection : HTMLCollection {
    val names: Array<String>
        get() = noImpl
//    override fun namedItem(name: String): PropertyNodeList? = noImpl
//    @nativeGetter
//    operator override fun get(name: String): PropertyNodeList? = noImpl
}

@native public interface PropertyNodeList : NodeList {
    fun getValues(): Array<Any?> = noImpl
}

@native public interface DOMStringMap {
    @nativeGetter
    operator fun get(name: String): String? = noImpl
    @nativeSetter
    operator fun set(name: String, value: String): Unit = noImpl
}

@native public interface DOMElementMap {
    @nativeGetter
    operator fun get(name: String): Element? = noImpl
    @nativeSetter
    operator fun set(name: String, value: Element): Unit = noImpl
}

@native public open class XMLDocument : Document() {
    fun load(url: String): Boolean = noImpl
}

@native public interface HTMLElement : Element {
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
    val dataset: DOMStringMap
        get() = noImpl
    var itemScope: Boolean
        get() = noImpl
        set(value) = noImpl
    val itemType: DOMSettableTokenList
        get() = noImpl
    var itemId: String
        get() = noImpl
        set(value) = noImpl
    val itemRef: DOMSettableTokenList
        get() = noImpl
    val itemProp: DOMSettableTokenList
        get() = noImpl
    val properties: HTMLPropertiesCollection
        get() = noImpl
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
    val accessKeyLabel: String
        get() = noImpl
    var draggable: Boolean
        get() = noImpl
        set(value) = noImpl
    val dropzone: DOMSettableTokenList
        get() = noImpl
    var contextMenu: HTMLMenuElement?
        get() = noImpl
        set(value) = noImpl
    var spellcheck: Boolean
        get() = noImpl
        set(value) = noImpl
    val commandType: String?
        get() = noImpl
    val commandLabel: String?
        get() = noImpl
    val commandIcon: String?
        get() = noImpl
    val commandHidden: Boolean?
        get() = noImpl
    val commandDisabled: Boolean?
        get() = noImpl
    val commandChecked: Boolean?
        get() = noImpl
    val offsetParent: Element?
        get() = noImpl
    val offsetTop: Double
        get() = noImpl
    val offsetLeft: Double
        get() = noImpl
    val offsetWidth: Double
        get() = noImpl
    val offsetHeight: Double
        get() = noImpl
    val style: CSSStyleDeclaration
        get() = noImpl
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
    var onerror: ((dynamic, String, Int, Int, Any?) -> dynamic)?
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
    val isContentEditable: Boolean
        get() = noImpl
    fun click(): Unit = noImpl
    fun focus(): Unit = noImpl
    fun blur(): Unit = noImpl
    fun forceSpellCheck(): Unit = noImpl
}

@native public interface HTMLUnknownElement : HTMLElement {
}

@native public interface HTMLHtmlElement : HTMLElement {
    var version: String
        get() = noImpl
        set(value) = noImpl
}

@native public interface HTMLHeadElement : HTMLElement {
}

@native public interface HTMLTitleElement : HTMLElement {
    var text: String
        get() = noImpl
        set(value) = noImpl
}

@native public interface HTMLBaseElement : HTMLElement {
    var href: String
        get() = noImpl
        set(value) = noImpl
    var target: String
        get() = noImpl
        set(value) = noImpl
}

@native public interface HTMLLinkElement : HTMLElement {
    var href: String
        get() = noImpl
        set(value) = noImpl
    var crossOrigin: String?
        get() = noImpl
        set(value) = noImpl
    var rel: String
        get() = noImpl
        set(value) = noImpl
    val relList: DOMTokenList
        get() = noImpl
    var media: String
        get() = noImpl
        set(value) = noImpl
    var hreflang: String
        get() = noImpl
        set(value) = noImpl
    var type: String
        get() = noImpl
        set(value) = noImpl
    val sizes: DOMSettableTokenList
        get() = noImpl
    var charset: String
        get() = noImpl
        set(value) = noImpl
    var rev: String
        get() = noImpl
        set(value) = noImpl
    var target: String
        get() = noImpl
        set(value) = noImpl
    val sheet: StyleSheet?
        get() = noImpl
}

@native public interface HTMLMetaElement : HTMLElement {
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

@native public interface HTMLStyleElement : HTMLElement {
    var media: String
        get() = noImpl
        set(value) = noImpl
    var type: String
        get() = noImpl
        set(value) = noImpl
    var scoped: Boolean
        get() = noImpl
        set(value) = noImpl
    val sheet: StyleSheet?
        get() = noImpl
}

@native public interface HTMLBodyElement : HTMLElement {
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

@native public interface HTMLHeadingElement : HTMLElement {
    var align: String
        get() = noImpl
        set(value) = noImpl
}

@native public interface HTMLParagraphElement : HTMLElement {
    var align: String
        get() = noImpl
        set(value) = noImpl
}

@native public interface HTMLHRElement : HTMLElement {
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

@native public interface HTMLPreElement : HTMLElement {
    var width: Int
        get() = noImpl
        set(value) = noImpl
}

@native public interface HTMLQuoteElement : HTMLElement {
    var cite: String
        get() = noImpl
        set(value) = noImpl
}

@native public interface HTMLOListElement : HTMLElement {
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

@native public interface HTMLUListElement : HTMLElement {
    var compact: Boolean
        get() = noImpl
        set(value) = noImpl
    var type: String
        get() = noImpl
        set(value) = noImpl
}

@native public interface HTMLLIElement : HTMLElement {
    var value: Int
        get() = noImpl
        set(value) = noImpl
    var type: String
        get() = noImpl
        set(value) = noImpl
}

@native public interface HTMLDListElement : HTMLElement {
    var compact: Boolean
        get() = noImpl
        set(value) = noImpl
}

@native public interface HTMLDivElement : HTMLElement {
    var align: String
        get() = noImpl
        set(value) = noImpl
}

@native public interface HTMLAnchorElement : HTMLElement {
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
    val relList: DOMTokenList
        get() = noImpl
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
    var href: String
        get() = noImpl
        set(value) = noImpl
    val origin: String
        get() = noImpl
    var protocol: String
        get() = noImpl
        set(value) = noImpl
    var username: String
        get() = noImpl
        set(value) = noImpl
    var password: String
        get() = noImpl
        set(value) = noImpl
    var host: String
        get() = noImpl
        set(value) = noImpl
    var hostname: String
        get() = noImpl
        set(value) = noImpl
    var port: String
        get() = noImpl
        set(value) = noImpl
    var pathname: String
        get() = noImpl
        set(value) = noImpl
    var search: String
        get() = noImpl
        set(value) = noImpl
    var searchParams: URLSearchParams
        get() = noImpl
        set(value) = noImpl
    var hash: String
        get() = noImpl
        set(value) = noImpl
}

@native public interface HTMLDataElement : HTMLElement {
    var value: String
        get() = noImpl
        set(value) = noImpl
}

@native public interface HTMLTimeElement : HTMLElement {
    var dateTime: String
        get() = noImpl
        set(value) = noImpl
}

@native public interface HTMLSpanElement : HTMLElement {
}

@native public interface HTMLBRElement : HTMLElement {
    var clear: String
        get() = noImpl
        set(value) = noImpl
}

@native public interface HTMLModElement : HTMLElement {
    var cite: String
        get() = noImpl
        set(value) = noImpl
    var dateTime: String
        get() = noImpl
        set(value) = noImpl
}

@native public interface HTMLIFrameElement : HTMLElement {
    var src: String
        get() = noImpl
        set(value) = noImpl
    var srcdoc: String
        get() = noImpl
        set(value) = noImpl
    var name: String
        get() = noImpl
        set(value) = noImpl
    val sandbox: DOMSettableTokenList
        get() = noImpl
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
    val contentDocument: Document?
        get() = noImpl
    val contentWindow: Window?
        get() = noImpl
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

@native public interface HTMLEmbedElement : HTMLElement {
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

@native public interface HTMLObjectElement : HTMLElement {
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
    val form: HTMLFormElement?
        get() = noImpl
    var width: String
        get() = noImpl
        set(value) = noImpl
    var height: String
        get() = noImpl
        set(value) = noImpl
    val contentDocument: Document?
        get() = noImpl
    val contentWindow: Window?
        get() = noImpl
    val willValidate: Boolean
        get() = noImpl
    val validity: ValidityState
        get() = noImpl
    val validationMessage: String
        get() = noImpl
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

@native public interface HTMLParamElement : HTMLElement {
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

@native public interface HTMLVideoElement : HTMLMediaElement, CanvasImageSource, ImageBitmapSource {
    var width: Int
        get() = noImpl
        set(value) = noImpl
    var height: Int
        get() = noImpl
        set(value) = noImpl
    val videoWidth: Int
        get() = noImpl
    val videoHeight: Int
        get() = noImpl
    var poster: String
        get() = noImpl
        set(value) = noImpl
}

@native public interface HTMLAudioElement : HTMLMediaElement {
}

@native public interface HTMLSourceElement : HTMLElement {
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

@native public interface HTMLTrackElement : HTMLElement {
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
    val readyState: Short
        get() = noImpl
    val track: TextTrack
        get() = noImpl

    companion object {
        val NONE: Short = 0
        val LOADING: Short = 1
        val LOADED: Short = 2
        val ERROR: Short = 3
    }
}

@native public interface HTMLMediaElement : HTMLElement {
    val error: MediaError?
        get() = noImpl
    var src: String
        get() = noImpl
        set(value) = noImpl
    var srcObject: dynamic
        get() = noImpl
        set(value) = noImpl
    val currentSrc: String
        get() = noImpl
    var crossOrigin: String?
        get() = noImpl
        set(value) = noImpl
    val networkState: Short
        get() = noImpl
    var preload: String
        get() = noImpl
        set(value) = noImpl
    val buffered: TimeRanges
        get() = noImpl
    val readyState: Short
        get() = noImpl
    val seeking: Boolean
        get() = noImpl
    var currentTime: Double
        get() = noImpl
        set(value) = noImpl
    val duration: Double
        get() = noImpl
    val paused: Boolean
        get() = noImpl
    var defaultPlaybackRate: Double
        get() = noImpl
        set(value) = noImpl
    var playbackRate: Double
        get() = noImpl
        set(value) = noImpl
    val played: TimeRanges
        get() = noImpl
    val seekable: TimeRanges
        get() = noImpl
    val ended: Boolean
        get() = noImpl
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
    val audioTracks: AudioTrackList
        get() = noImpl
    val videoTracks: VideoTrackList
        get() = noImpl
    val textTracks: TextTrackList
        get() = noImpl
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

@native public interface MediaError {
    val code: Short
        get() = noImpl

    companion object {
        val MEDIA_ERR_ABORTED: Short = 1
        val MEDIA_ERR_NETWORK: Short = 2
        val MEDIA_ERR_DECODE: Short = 3
        val MEDIA_ERR_SRC_NOT_SUPPORTED: Short = 4
    }
}

@native public interface AudioTrackList : EventTarget {
    val length: Int
        get() = noImpl
    var onchange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onaddtrack: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onremovetrack: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    @nativeGetter
    operator fun get(index: Int): AudioTrack? = noImpl
    fun getTrackById(id: String): AudioTrack? = noImpl
}

@native public interface AudioTrack : UnionAudioTrackOrTextTrackOrVideoTrack {
    val id: String
        get() = noImpl
    val kind: String
        get() = noImpl
    val label: String
        get() = noImpl
    val language: String
        get() = noImpl
    var enabled: Boolean
        get() = noImpl
        set(value) = noImpl
}

@native public interface VideoTrackList : EventTarget {
    val length: Int
        get() = noImpl
    val selectedIndex: Int
        get() = noImpl
    var onchange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onaddtrack: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onremovetrack: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    @nativeGetter
    operator fun get(index: Int): VideoTrack? = noImpl
    fun getTrackById(id: String): VideoTrack? = noImpl
}

@native public interface VideoTrack : UnionAudioTrackOrTextTrackOrVideoTrack {
    val id: String
        get() = noImpl
    val kind: String
        get() = noImpl
    val label: String
        get() = noImpl
    val language: String
        get() = noImpl
    var selected: Boolean
        get() = noImpl
        set(value) = noImpl
}

@native public open class MediaController : EventTarget {
    open val readyState: Short
        get() = noImpl
    open val buffered: TimeRanges
        get() = noImpl
    open val seekable: TimeRanges
        get() = noImpl
    open val duration: Double
        get() = noImpl
    var currentTime: Double
        get() = noImpl
        set(value) = noImpl
    open val paused: Boolean
        get() = noImpl
    open val playbackState: String
        get() = noImpl
    open val played: TimeRanges
        get() = noImpl
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

@native public interface TextTrackList : EventTarget {
    val length: Int
        get() = noImpl
    var onchange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onaddtrack: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onremovetrack: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    @nativeGetter
    operator fun get(index: Int): TextTrack? = noImpl
    fun getTrackById(id: String): TextTrack? = noImpl
}

@native public interface TextTrack : EventTarget, UnionAudioTrackOrTextTrackOrVideoTrack {
    val kind: String
        get() = noImpl
    val label: String
        get() = noImpl
    val language: String
        get() = noImpl
    val id: String
        get() = noImpl
    val inBandMetadataTrackDispatchType: String
        get() = noImpl
    var mode: String
        get() = noImpl
        set(value) = noImpl
    val cues: TextTrackCueList?
        get() = noImpl
    val activeCues: TextTrackCueList?
        get() = noImpl
    var oncuechange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    fun addCue(cue: TextTrackCue): Unit = noImpl
    fun removeCue(cue: TextTrackCue): Unit = noImpl
}

@native public interface TextTrackCueList {
    val length: Int
        get() = noImpl
    @nativeGetter
    operator fun get(index: Int): TextTrackCue? = noImpl
    fun getCueById(id: String): TextTrackCue? = noImpl
}

@native public interface TextTrackCue : EventTarget {
    val track: TextTrack?
        get() = noImpl
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

@native public interface TimeRanges {
    val length: Int
        get() = noImpl
    fun start(index: Int): Double = noImpl
    fun end(index: Int): Double = noImpl
}

@native public open class TrackEvent(type: String, eventInitDict: TrackEventInit = noImpl) : Event(type, eventInitDict) {
    open val track: UnionAudioTrackOrTextTrackOrVideoTrack?
        get() = noImpl
}

@native public interface TrackEventInit : EventInit {
    var track: UnionAudioTrackOrTextTrackOrVideoTrack?
}

@Suppress("NOTHING_TO_INLINE")
public inline fun TrackEventInit(track: UnionAudioTrackOrTextTrackOrVideoTrack?, bubbles: Boolean = false, cancelable: Boolean = false): TrackEventInit {
    val o = js("({})")

    o["track"] = track
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable

    return o
}

@native public interface HTMLMapElement : HTMLElement {
    var name: String
        get() = noImpl
        set(value) = noImpl
    val areas: HTMLCollection
        get() = noImpl
    val images: HTMLCollection
        get() = noImpl
}

@native public interface HTMLAreaElement : HTMLElement {
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
    val relList: DOMTokenList
        get() = noImpl
    var hreflang: String
        get() = noImpl
        set(value) = noImpl
    var type: String
        get() = noImpl
        set(value) = noImpl
    var noHref: Boolean
        get() = noImpl
        set(value) = noImpl
    var href: String
        get() = noImpl
        set(value) = noImpl
    val origin: String
        get() = noImpl
    var protocol: String
        get() = noImpl
        set(value) = noImpl
    var username: String
        get() = noImpl
        set(value) = noImpl
    var password: String
        get() = noImpl
        set(value) = noImpl
    var host: String
        get() = noImpl
        set(value) = noImpl
    var hostname: String
        get() = noImpl
        set(value) = noImpl
    var port: String
        get() = noImpl
        set(value) = noImpl
    var pathname: String
        get() = noImpl
        set(value) = noImpl
    var search: String
        get() = noImpl
        set(value) = noImpl
    var searchParams: URLSearchParams
        get() = noImpl
        set(value) = noImpl
    var hash: String
        get() = noImpl
        set(value) = noImpl
}

@native public interface HTMLTableElement : HTMLElement {
    var caption: HTMLTableCaptionElement?
        get() = noImpl
        set(value) = noImpl
    var tHead: HTMLTableSectionElement?
        get() = noImpl
        set(value) = noImpl
    var tFoot: HTMLTableSectionElement?
        get() = noImpl
        set(value) = noImpl
    val tBodies: HTMLCollection
        get() = noImpl
    val rows: HTMLCollection
        get() = noImpl
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

@native public interface HTMLTableCaptionElement : HTMLElement {
    var align: String
        get() = noImpl
        set(value) = noImpl
}

@native public interface HTMLTableColElement : HTMLElement {
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

@native public interface HTMLTableSectionElement : HTMLElement {
    val rows: HTMLCollection
        get() = noImpl
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

@native public interface HTMLTableRowElement : HTMLElement {
    val rowIndex: Int
        get() = noImpl
    val sectionRowIndex: Int
        get() = noImpl
    val cells: HTMLCollection
        get() = noImpl
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

@native public interface HTMLTableDataCellElement : HTMLTableCellElement {
    var abbr: String
        get() = noImpl
        set(value) = noImpl
}

@native public interface HTMLTableHeaderCellElement : HTMLTableCellElement {
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

@native public interface HTMLTableCellElement : HTMLElement {
    var colSpan: Int
        get() = noImpl
        set(value) = noImpl
    var rowSpan: Int
        get() = noImpl
        set(value) = noImpl
    val headers: DOMSettableTokenList
        get() = noImpl
    val cellIndex: Int
        get() = noImpl
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

@native public interface HTMLFormElement : HTMLElement {
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
    val elements: HTMLFormControlsCollection
        get() = noImpl
    val length: Int
        get() = noImpl
    @nativeGetter
    operator fun get(index: Int): Element? = noImpl
    @nativeGetter
    operator fun get(name: String): UnionElementOrRadioNodeList? = noImpl
    fun submit(): Unit = noImpl
    fun reset(): Unit = noImpl
    fun checkValidity(): Boolean = noImpl
    fun reportValidity(): Boolean = noImpl
    fun requestAutocomplete(): Unit = noImpl
}

@native public interface HTMLLabelElement : HTMLElement {
    val form: HTMLFormElement?
        get() = noImpl
    var htmlFor: String
        get() = noImpl
        set(value) = noImpl
    val control: HTMLElement?
        get() = noImpl
}

@native public interface HTMLInputElement : HTMLElement {
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
    val form: HTMLFormElement?
        get() = noImpl
    val files: FileList?
        get() = noImpl
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
    val list: HTMLElement?
        get() = noImpl
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
    val willValidate: Boolean
        get() = noImpl
    val validity: ValidityState
        get() = noImpl
    val validationMessage: String
        get() = noImpl
    val labels: NodeList
        get() = noImpl
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

@native public interface HTMLButtonElement : HTMLElement {
    var autofocus: Boolean
        get() = noImpl
        set(value) = noImpl
    var disabled: Boolean
        get() = noImpl
        set(value) = noImpl
    val form: HTMLFormElement?
        get() = noImpl
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
    val willValidate: Boolean
        get() = noImpl
    val validity: ValidityState
        get() = noImpl
    val validationMessage: String
        get() = noImpl
    val labels: NodeList
        get() = noImpl
    fun checkValidity(): Boolean = noImpl
    fun reportValidity(): Boolean = noImpl
    fun setCustomValidity(error: String): Unit = noImpl
}

@native public interface HTMLSelectElement : HTMLElement {
    var autocomplete: String
        get() = noImpl
        set(value) = noImpl
    var autofocus: Boolean
        get() = noImpl
        set(value) = noImpl
    var disabled: Boolean
        get() = noImpl
        set(value) = noImpl
    val form: HTMLFormElement?
        get() = noImpl
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
    val type: String
        get() = noImpl
    val options: HTMLOptionsCollection
        get() = noImpl
    var length: Int
        get() = noImpl
        set(value) = noImpl
    val selectedOptions: HTMLCollection
        get() = noImpl
    var selectedIndex: Int
        get() = noImpl
        set(value) = noImpl
    var value: String
        get() = noImpl
        set(value) = noImpl
    val willValidate: Boolean
        get() = noImpl
    val validity: ValidityState
        get() = noImpl
    val validationMessage: String
        get() = noImpl
    val labels: NodeList
        get() = noImpl
    fun item(index: Int): Element? = noImpl
    @nativeGetter
    operator fun get(index: Int): Element? = noImpl
    fun namedItem(name: String): HTMLOptionElement? = noImpl
    fun add(element: UnionHTMLOptGroupElementOrHTMLOptionElement, before: dynamic = null): Unit = noImpl
    fun remove(index: Int): Unit = noImpl
    @nativeSetter
    operator fun set(index: Int, option: HTMLOptionElement?): Unit = noImpl
    fun checkValidity(): Boolean = noImpl
    fun reportValidity(): Boolean = noImpl
    fun setCustomValidity(error: String): Unit = noImpl
}

@native public interface HTMLDataListElement : HTMLElement {
    val options: HTMLCollection
        get() = noImpl
}

@native public interface HTMLOptGroupElement : HTMLElement, UnionHTMLOptGroupElementOrHTMLOptionElement {
    var disabled: Boolean
        get() = noImpl
        set(value) = noImpl
    var label: String
        get() = noImpl
        set(value) = noImpl
}

@native public interface HTMLOptionElement : HTMLElement, UnionHTMLOptGroupElementOrHTMLOptionElement {
    var disabled: Boolean
        get() = noImpl
        set(value) = noImpl
    val form: HTMLFormElement?
        get() = noImpl
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
    val index: Int
        get() = noImpl
}

@native public interface HTMLTextAreaElement : HTMLElement {
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
    val form: HTMLFormElement?
        get() = noImpl
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
    val type: String
        get() = noImpl
    var defaultValue: String
        get() = noImpl
        set(value) = noImpl
    var value: String
        get() = noImpl
        set(value) = noImpl
    val textLength: Int
        get() = noImpl
    val willValidate: Boolean
        get() = noImpl
    val validity: ValidityState
        get() = noImpl
    val validationMessage: String
        get() = noImpl
    val labels: NodeList
        get() = noImpl
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

@native public interface HTMLKeygenElement : HTMLElement {
    var autofocus: Boolean
        get() = noImpl
        set(value) = noImpl
    var challenge: String
        get() = noImpl
        set(value) = noImpl
    var disabled: Boolean
        get() = noImpl
        set(value) = noImpl
    val form: HTMLFormElement?
        get() = noImpl
    var keytype: String
        get() = noImpl
        set(value) = noImpl
    var name: String
        get() = noImpl
        set(value) = noImpl
    val type: String
        get() = noImpl
    val willValidate: Boolean
        get() = noImpl
    val validity: ValidityState
        get() = noImpl
    val validationMessage: String
        get() = noImpl
    val labels: NodeList
        get() = noImpl
    fun checkValidity(): Boolean = noImpl
    fun reportValidity(): Boolean = noImpl
    fun setCustomValidity(error: String): Unit = noImpl
}

@native public interface HTMLOutputElement : HTMLElement {
    val htmlFor: DOMSettableTokenList
        get() = noImpl
    val form: HTMLFormElement?
        get() = noImpl
    var name: String
        get() = noImpl
        set(value) = noImpl
    val type: String
        get() = noImpl
    var defaultValue: String
        get() = noImpl
        set(value) = noImpl
    var value: String
        get() = noImpl
        set(value) = noImpl
    val willValidate: Boolean
        get() = noImpl
    val validity: ValidityState
        get() = noImpl
    val validationMessage: String
        get() = noImpl
    val labels: NodeList
        get() = noImpl
    fun checkValidity(): Boolean = noImpl
    fun reportValidity(): Boolean = noImpl
    fun setCustomValidity(error: String): Unit = noImpl
}

@native public interface HTMLProgressElement : HTMLElement {
    var value: Double
        get() = noImpl
        set(value) = noImpl
    var max: Double
        get() = noImpl
        set(value) = noImpl
    val position: Double
        get() = noImpl
    val labels: NodeList
        get() = noImpl
}

@native public interface HTMLMeterElement : HTMLElement {
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
    val labels: NodeList
        get() = noImpl
}

@native public interface HTMLFieldSetElement : HTMLElement {
    var disabled: Boolean
        get() = noImpl
        set(value) = noImpl
    val form: HTMLFormElement?
        get() = noImpl
    var name: String
        get() = noImpl
        set(value) = noImpl
    val type: String
        get() = noImpl
    val elements: HTMLFormControlsCollection
        get() = noImpl
    val willValidate: Boolean
        get() = noImpl
    val validity: ValidityState
        get() = noImpl
    val validationMessage: String
        get() = noImpl
    fun checkValidity(): Boolean = noImpl
    fun reportValidity(): Boolean = noImpl
    fun setCustomValidity(error: String): Unit = noImpl
}

@native public interface HTMLLegendElement : HTMLElement {
    val form: HTMLFormElement?
        get() = noImpl
    var align: String
        get() = noImpl
        set(value) = noImpl
}

@native public open class AutocompleteErrorEvent(type: String, eventInitDict: AutocompleteErrorEventInit = noImpl) : Event(type, eventInitDict) {
    open val reason: String
        get() = noImpl
}

@native public interface AutocompleteErrorEventInit : EventInit {
    var reason: String
}

@Suppress("NOTHING_TO_INLINE")
public inline fun AutocompleteErrorEventInit(reason: String, bubbles: Boolean = false, cancelable: Boolean = false): AutocompleteErrorEventInit {
    val o = js("({})")

    o["reason"] = reason
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable

    return o
}

@native public interface ValidityState {
    val valueMissing: Boolean
        get() = noImpl
    val typeMismatch: Boolean
        get() = noImpl
    val patternMismatch: Boolean
        get() = noImpl
    val tooLong: Boolean
        get() = noImpl
    val tooShort: Boolean
        get() = noImpl
    val rangeUnderflow: Boolean
        get() = noImpl
    val rangeOverflow: Boolean
        get() = noImpl
    val stepMismatch: Boolean
        get() = noImpl
    val badInput: Boolean
        get() = noImpl
    val customError: Boolean
        get() = noImpl
    val valid: Boolean
        get() = noImpl
}

@native public interface HTMLDetailsElement : HTMLElement {
    var open: Boolean
        get() = noImpl
        set(value) = noImpl
}

@native public interface HTMLMenuElement : HTMLElement {
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

@native public interface HTMLMenuItemElement : HTMLElement {
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
    val command: HTMLElement?
        get() = noImpl
}

@native public open class RelatedEvent(type: String, eventInitDict: RelatedEventInit = noImpl) : Event(type, eventInitDict) {
    open val relatedTarget: EventTarget?
        get() = noImpl
}

@native public interface RelatedEventInit : EventInit {
    var relatedTarget: EventTarget?
}

@Suppress("NOTHING_TO_INLINE")
public inline fun RelatedEventInit(relatedTarget: EventTarget?, bubbles: Boolean = false, cancelable: Boolean = false): RelatedEventInit {
    val o = js("({})")

    o["relatedTarget"] = relatedTarget
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable

    return o
}

@native public interface HTMLDialogElement : HTMLElement {
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

@native public interface HTMLScriptElement : HTMLElement {
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

@native public interface HTMLTemplateElement : HTMLElement {
    val content: DocumentFragment
        get() = noImpl
}

@native public interface HTMLCanvasElement : HTMLElement, CanvasImageSource, ImageBitmapSource {
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

@native public interface CanvasProxy : Transferable {
    fun setContext(context: RenderingContext): Unit = noImpl
}

@native public interface CanvasRenderingContext2DSettings {
    var alpha: Boolean
}

@Suppress("NOTHING_TO_INLINE")
public inline fun CanvasRenderingContext2DSettings(alpha: Boolean = true): CanvasRenderingContext2DSettings {
    val o = js("({})")

    o["alpha"] = alpha

    return o
}

@native public open class CanvasRenderingContext2D() : RenderingContext, CanvasImageSource, ImageBitmapSource {
    constructor(width: Int, height: Int) : this()
    open val canvas: HTMLCanvasElement
        get() = noImpl
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

@native public interface CanvasGradient {
    fun addColorStop(offset: Double, color: String): Unit = noImpl
}

@native public interface CanvasPattern {
    fun setTransform(transform: SVGMatrix): Unit = noImpl
}

@native public interface TextMetrics {
    val width: Double
        get() = noImpl
    val actualBoundingBoxLeft: Double
        get() = noImpl
    val actualBoundingBoxRight: Double
        get() = noImpl
    val fontBoundingBoxAscent: Double
        get() = noImpl
    val fontBoundingBoxDescent: Double
        get() = noImpl
    val actualBoundingBoxAscent: Double
        get() = noImpl
    val actualBoundingBoxDescent: Double
        get() = noImpl
    val emHeightAscent: Double
        get() = noImpl
    val emHeightDescent: Double
        get() = noImpl
    val hangingBaseline: Double
        get() = noImpl
    val alphabeticBaseline: Double
        get() = noImpl
    val ideographicBaseline: Double
        get() = noImpl
}

@native public interface HitRegionOptions {
    var path: Path2D?
    var fillRule: String
    var id: String
    var parentID: String?
    var cursor: String
    var control: Element?
    var label: String?
    var role: String?
}

@Suppress("NOTHING_TO_INLINE")
public inline fun HitRegionOptions(path: Path2D? = null, fillRule: String = "nonzero", id: String = "", parentID: String? = null, cursor: String = "inherit", control: Element? = null, label: String? = null, role: String? = null): HitRegionOptions {
    val o = js("({})")

    o["path"] = path
    o["fillRule"] = fillRule
    o["id"] = id
    o["parentID"] = parentID
    o["cursor"] = cursor
    o["control"] = control
    o["label"] = label
    o["role"] = role

    return o
}

@native public open class ImageData : ImageBitmapSource {
    constructor(sw: Int, sh: Int)
    constructor(data: Uint8ClampedArray, sw: Int, sh: Int = noImpl)
    open val width: Int
        get() = noImpl
    open val height: Int
        get() = noImpl
    open val data: Uint8ClampedArray
        get() = noImpl
}

@native public open class DrawingStyle(scope: Element = noImpl) {
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

@native public open class Path2D() {
    constructor(path: Path2D) : this()
    constructor(paths: Array<Path2D>, fillRule: String = "nonzero") : this()
    constructor(d: String) : this()
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

@native public interface Touch {
    val region: String?
        get() = noImpl
}

@native public interface DataTransfer {
    var dropEffect: String
        get() = noImpl
        set(value) = noImpl
    var effectAllowed: String
        get() = noImpl
        set(value) = noImpl
    val items: DataTransferItemList
        get() = noImpl
    val types: Array<String>
        get() = noImpl
    val files: FileList
        get() = noImpl
    fun setDragImage(image: Element, x: Int, y: Int): Unit = noImpl
    fun getData(format: String): String = noImpl
    fun setData(format: String, data: String): Unit = noImpl
    fun clearData(format: String = noImpl): Unit = noImpl
}

@native public interface DataTransferItemList {
    val length: Int
        get() = noImpl
    @nativeGetter
    operator fun get(index: Int): DataTransferItem? = noImpl
    fun add(data: String, type: String): DataTransferItem? = noImpl
    fun add(data: File): DataTransferItem? = noImpl
    fun remove(index: Int): Unit = noImpl
    fun clear(): Unit = noImpl
}

@native public interface DataTransferItem {
    val kind: String
        get() = noImpl
    val type: String
        get() = noImpl
    fun getAsString(_callback: ((String) -> Unit)?): Unit = noImpl
    fun getAsFile(): File? = noImpl
}

@native public open class DragEvent(type: String, eventInitDict: DragEventInit = noImpl) : MouseEvent(noImpl, noImpl) {
    open val dataTransfer: DataTransfer?
        get() = noImpl
}

@native public interface DragEventInit : MouseEventInit {
    var dataTransfer: DataTransfer?
}

@Suppress("NOTHING_TO_INLINE")
public inline fun DragEventInit(dataTransfer: DataTransfer?, screenX: Int = 0, screenY: Int = 0, clientX: Int = 0, clientY: Int = 0, button: Short = 0, buttons: Short = 0, relatedTarget: EventTarget? = null, ctrlKey: Boolean = false, shiftKey: Boolean = false, altKey: Boolean = false, metaKey: Boolean = false, modifierAltGraph: Boolean = false, modifierCapsLock: Boolean = false, modifierFn: Boolean = false, modifierFnLock: Boolean = false, modifierHyper: Boolean = false, modifierNumLock: Boolean = false, modifierOS: Boolean = false, modifierScrollLock: Boolean = false, modifierSuper: Boolean = false, modifierSymbol: Boolean = false, modifierSymbolLock: Boolean = false, view: Window? = null, detail: Int = 0, bubbles: Boolean = false, cancelable: Boolean = false): DragEventInit {
    val o = js("({})")

    o["dataTransfer"] = dataTransfer
    o["screenX"] = screenX
    o["screenY"] = screenY
    o["clientX"] = clientX
    o["clientY"] = clientY
    o["button"] = button
    o["buttons"] = buttons
    o["relatedTarget"] = relatedTarget
    o["ctrlKey"] = ctrlKey
    o["shiftKey"] = shiftKey
    o["altKey"] = altKey
    o["metaKey"] = metaKey
    o["modifierAltGraph"] = modifierAltGraph
    o["modifierCapsLock"] = modifierCapsLock
    o["modifierFn"] = modifierFn
    o["modifierFnLock"] = modifierFnLock
    o["modifierHyper"] = modifierHyper
    o["modifierNumLock"] = modifierNumLock
    o["modifierOS"] = modifierOS
    o["modifierScrollLock"] = modifierScrollLock
    o["modifierSuper"] = modifierSuper
    o["modifierSymbol"] = modifierSymbol
    o["modifierSymbolLock"] = modifierSymbolLock
    o["view"] = view
    o["detail"] = detail
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable

    return o
}

@native public interface BarProp {
    var visible: Boolean
        get() = noImpl
        set(value) = noImpl
}

@native public interface History {
    val length: Int
        get() = noImpl
    val state: Any?
        get() = noImpl
    fun go(delta: Int = noImpl): Unit = noImpl
    fun back(): Unit = noImpl
    fun forward(): Unit = noImpl
    fun pushState(data: Any?, title: String, url: String? = null): Unit = noImpl
    fun replaceState(data: Any?, title: String, url: String? = null): Unit = noImpl
}

@native public interface Location {
    val ancestorOrigins: Array<String>
        get() = noImpl
    var href: String
        get() = noImpl
        set(value) = noImpl
    val origin: String
        get() = noImpl
    var protocol: String
        get() = noImpl
        set(value) = noImpl
    var username: String
        get() = noImpl
        set(value) = noImpl
    var password: String
        get() = noImpl
        set(value) = noImpl
    var host: String
        get() = noImpl
        set(value) = noImpl
    var hostname: String
        get() = noImpl
        set(value) = noImpl
    var port: String
        get() = noImpl
        set(value) = noImpl
    var pathname: String
        get() = noImpl
        set(value) = noImpl
    var search: String
        get() = noImpl
        set(value) = noImpl
    var searchParams: URLSearchParams
        get() = noImpl
        set(value) = noImpl
    var hash: String
        get() = noImpl
        set(value) = noImpl
    fun assign(url: String): Unit = noImpl
    fun replace(url: String): Unit = noImpl
    fun reload(): Unit = noImpl
}

@native public open class PopStateEvent(type: String, eventInitDict: PopStateEventInit = noImpl) : Event(type, eventInitDict) {
    open val state: Any?
        get() = noImpl
}

@native public interface PopStateEventInit : EventInit {
    var state: Any?
}

@Suppress("NOTHING_TO_INLINE")
public inline fun PopStateEventInit(state: Any?, bubbles: Boolean = false, cancelable: Boolean = false): PopStateEventInit {
    val o = js("({})")

    o["state"] = state
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable

    return o
}

@native public open class HashChangeEvent(type: String, eventInitDict: HashChangeEventInit = noImpl) : Event(type, eventInitDict) {
    open val oldURL: String
        get() = noImpl
    open val newURL: String
        get() = noImpl
}

@native public interface HashChangeEventInit : EventInit {
    var oldURL: String
    var newURL: String
}

@Suppress("NOTHING_TO_INLINE")
public inline fun HashChangeEventInit(oldURL: String, newURL: String, bubbles: Boolean = false, cancelable: Boolean = false): HashChangeEventInit {
    val o = js("({})")

    o["oldURL"] = oldURL
    o["newURL"] = newURL
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable

    return o
}

@native public open class PageTransitionEvent(type: String, eventInitDict: PageTransitionEventInit = noImpl) : Event(type, eventInitDict) {
    open val persisted: Boolean
        get() = noImpl
}

@native public interface PageTransitionEventInit : EventInit {
    var persisted: Boolean
}

@Suppress("NOTHING_TO_INLINE")
public inline fun PageTransitionEventInit(persisted: Boolean, bubbles: Boolean = false, cancelable: Boolean = false): PageTransitionEventInit {
    val o = js("({})")

    o["persisted"] = persisted
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable

    return o
}

@native public open class BeforeUnloadEvent : Event(noImpl, noImpl) {
    var returnValue: String
        get() = noImpl
        set(value) = noImpl
}

@native public interface ApplicationCache : EventTarget {
    val status: Short
        get() = noImpl
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

@native public open class ErrorEvent(type: String, eventInitDict: ErrorEventInit = noImpl) : Event(type, eventInitDict) {
    open val message: String
        get() = noImpl
    open val filename: String
        get() = noImpl
    open val lineno: Int
        get() = noImpl
    open val colno: Int
        get() = noImpl
    open val error: Any?
        get() = noImpl
}

@native public interface ErrorEventInit : EventInit {
    var message: String
    var filename: String
    var lineno: Int
    var colno: Int
    var error: Any?
}

@Suppress("NOTHING_TO_INLINE")
public inline fun ErrorEventInit(message: String, filename: String, lineno: Int, colno: Int, error: Any?, bubbles: Boolean = false, cancelable: Boolean = false): ErrorEventInit {
    val o = js("({})")

    o["message"] = message
    o["filename"] = filename
    o["lineno"] = lineno
    o["colno"] = colno
    o["error"] = error
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable

    return o
}

@native public interface Navigator {
    val serviceWorker: ServiceWorkerContainer
        get() = noImpl
    val appCodeName: String
        get() = noImpl
    val appName: String
        get() = noImpl
    val appVersion: String
        get() = noImpl
    val platform: String
        get() = noImpl
    val product: String
        get() = noImpl
    val userAgent: String
        get() = noImpl
    val vendorSub: String
        get() = noImpl
    val language: String?
        get() = noImpl
    val languages: Array<String>
        get() = noImpl
    val onLine: Boolean
        get() = noImpl
    val cookieEnabled: Boolean
        get() = noImpl
    val plugins: PluginArray
        get() = noImpl
    val mimeTypes: MimeTypeArray
        get() = noImpl
    val javaEnabled: Boolean
        get() = noImpl
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

@native public interface PluginArray {
    val length: Int
        get() = noImpl
    fun refresh(reload: Boolean = false): Unit = noImpl
    fun item(index: Int): Plugin? = noImpl
    @nativeGetter
    operator fun get(index: Int): Plugin? = noImpl
    fun namedItem(name: String): Plugin? = noImpl
    @nativeGetter
    operator fun get(name: String): Plugin? = noImpl
}

@native public interface MimeTypeArray {
    val length: Int
        get() = noImpl
    fun item(index: Int): MimeType? = noImpl
    @nativeGetter
    operator fun get(index: Int): MimeType? = noImpl
    fun namedItem(name: String): MimeType? = noImpl
    @nativeGetter
    operator fun get(name: String): MimeType? = noImpl
}

@native public interface Plugin {
    val name: String
        get() = noImpl
    val description: String
        get() = noImpl
    val filename: String
        get() = noImpl
    val length: Int
        get() = noImpl
    fun item(index: Int): MimeType? = noImpl
    @nativeGetter
    operator fun get(index: Int): MimeType? = noImpl
    fun namedItem(name: String): MimeType? = noImpl
    @nativeGetter
    operator fun get(name: String): MimeType? = noImpl
}

@native public interface MimeType {
    val type: String
        get() = noImpl
    val description: String
        get() = noImpl
    val suffixes: String
        get() = noImpl
    val enabledPlugin: Plugin
        get() = noImpl
}

@native public interface External {
    fun AddSearchProvider(engineURL: String): Unit = noImpl
    fun IsSearchProviderInstalled(engineURL: String): Int = noImpl
}

@native public interface ImageBitmap : CanvasImageSource, ImageBitmapSource {
    val width: Int
        get() = noImpl
    val height: Int
        get() = noImpl
}

@native public open class MessageEvent(type: String, eventInitDict: MessageEventInit = noImpl) : Event(type, eventInitDict) {
    open val data: Any?
        get() = noImpl
    open val origin: String
        get() = noImpl
    open val lastEventId: String
        get() = noImpl
    open val source: UnionMessagePortOrWindow?
        get() = noImpl
    open val ports: Array<MessagePort>?
        get() = noImpl
    fun initMessageEvent(typeArg: String, canBubbleArg: Boolean, cancelableArg: Boolean, dataArg: Any?, originArg: String, lastEventIdArg: String, sourceArg: UnionMessagePortOrWindow, portsArg: Array<MessagePort>?): Unit = noImpl
}

@native public interface MessageEventInit : EventInit {
    var data: Any?
    var origin: String
    var lastEventId: String
    var source: UnionMessagePortOrWindow?
    var ports: Array<MessagePort>
}

@Suppress("NOTHING_TO_INLINE")
public inline fun MessageEventInit(data: Any?, origin: String, lastEventId: String, source: UnionMessagePortOrWindow?, ports: Array<MessagePort>, bubbles: Boolean = false, cancelable: Boolean = false): MessageEventInit {
    val o = js("({})")

    o["data"] = data
    o["origin"] = origin
    o["lastEventId"] = lastEventId
    o["source"] = source
    o["ports"] = ports
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable

    return o
}

@native public open class EventSource(url: String, eventSourceInitDict: EventSourceInit = noImpl) : EventTarget {
    open val url: String
        get() = noImpl
    open val withCredentials: Boolean
        get() = noImpl
    open val readyState: Short
        get() = noImpl
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

@native public interface EventSourceInit {
    var withCredentials: Boolean
}

@Suppress("NOTHING_TO_INLINE")
public inline fun EventSourceInit(withCredentials: Boolean = false): EventSourceInit {
    val o = js("({})")

    o["withCredentials"] = withCredentials

    return o
}

@native public open class WebSocket(url: String, protocols: dynamic = noImpl) : EventTarget {
    open val url: String
        get() = noImpl
    open val readyState: Short
        get() = noImpl
    open val bufferedAmount: Int
        get() = noImpl
    var onopen: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onerror: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onclose: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    open val extensions: String
        get() = noImpl
    open val protocol: String
        get() = noImpl
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

@native public open class CloseEvent(type: String, eventInitDict: CloseEventInit = noImpl) : Event(type, eventInitDict) {
    open val wasClean: Boolean
        get() = noImpl
    open val code: Short
        get() = noImpl
    open val reason: String
        get() = noImpl
}

@native public interface CloseEventInit : EventInit {
    var wasClean: Boolean
    var code: Short
    var reason: String
}

@Suppress("NOTHING_TO_INLINE")
public inline fun CloseEventInit(wasClean: Boolean, code: Short, reason: String, bubbles: Boolean = false, cancelable: Boolean = false): CloseEventInit {
    val o = js("({})")

    o["wasClean"] = wasClean
    o["code"] = code
    o["reason"] = reason
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable

    return o
}

@native public open class MessageChannel {
    open val port1: MessagePort
        get() = noImpl
    open val port2: MessagePort
        get() = noImpl
}

@native public interface MessagePort : EventTarget, UnionMessagePortOrWindow, UnionMessagePortOrServiceWorker, UnionClientOrMessagePortOrServiceWorker, Transferable {
    var onmessage: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    fun postMessage(message: Any?, transfer: Array<Transferable> = noImpl): Unit = noImpl
    fun start(): Unit = noImpl
    fun close(): Unit = noImpl
}

@native public open class PortCollection {
    fun add(port: MessagePort): Unit = noImpl
    fun remove(port: MessagePort): Unit = noImpl
    fun clear(): Unit = noImpl
    fun iterate(callback: (MessagePort) -> Unit): Unit = noImpl
}

@native public open class BroadcastChannel(channel: String) : EventTarget {
    open val name: String
        get() = noImpl
    var onmessage: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    fun postMessage(message: Any?): Unit = noImpl
    fun close(): Unit = noImpl
}

@native public interface WorkerGlobalScope : EventTarget {
    val caches: CacheStorage
        get() = noImpl
    val self: WorkerGlobalScope
        get() = noImpl
    val location: WorkerLocation
        get() = noImpl
    var onerror: ((dynamic, String, Int, Int, Any?) -> dynamic)?
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
    val navigator: WorkerNavigator
        get() = noImpl
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

@native public interface DedicatedWorkerGlobalScope : WorkerGlobalScope {
    var onmessage: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    fun postMessage(message: Any?, transfer: Array<Transferable> = noImpl): Unit = noImpl
}

@native public interface SharedWorkerGlobalScope : WorkerGlobalScope {
    val name: String
        get() = noImpl
    val applicationCache: ApplicationCache
        get() = noImpl
    var onconnect: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
}

@native public open class Worker(scriptURL: String) : EventTarget {
    var onmessage: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onerror: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    fun terminate(): Unit = noImpl
    fun postMessage(message: Any?, transfer: Array<Transferable> = noImpl): Unit = noImpl
}

@native public open class SharedWorker(scriptURL: String, name: String = noImpl) : EventTarget {
    open val port: MessagePort
        get() = noImpl
    var onerror: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
}

@native public interface WorkerNavigator {
    val serviceWorker: ServiceWorkerContainer
        get() = noImpl
    val appCodeName: String
        get() = noImpl
    val appName: String
        get() = noImpl
    val appVersion: String
        get() = noImpl
    val platform: String
        get() = noImpl
    val product: String
        get() = noImpl
    val userAgent: String
        get() = noImpl
    val vendorSub: String
        get() = noImpl
    val language: String?
        get() = noImpl
    val languages: Array<String>
        get() = noImpl
    val onLine: Boolean
        get() = noImpl
    fun taintEnabled(): Boolean = noImpl
}

@native public interface WorkerLocation {
    var href: String
        get() = noImpl
        set(value) = noImpl
    val origin: String
        get() = noImpl
    val protocol: String
        get() = noImpl
    val host: String
        get() = noImpl
    val hostname: String
        get() = noImpl
    val port: String
        get() = noImpl
    val pathname: String
        get() = noImpl
    val search: String
        get() = noImpl
    val hash: String
        get() = noImpl
}

@native public interface Storage {
    val length: Int
        get() = noImpl
    fun key(index: Int): String? = noImpl
    fun getItem(key: String): String? = noImpl
    @nativeGetter
    operator fun get(key: String): String? = noImpl
    fun setItem(key: String, value: String): Unit = noImpl
    @nativeSetter
    operator fun set(key: String, value: String): Unit = noImpl
    fun removeItem(key: String): Unit = noImpl
    fun clear(): Unit = noImpl
}

@native public open class StorageEvent(type: String, eventInitDict: StorageEventInit = noImpl) : Event(type, eventInitDict) {
    open val key: String?
        get() = noImpl
    open val oldValue: String?
        get() = noImpl
    open val newValue: String?
        get() = noImpl
    open val url: String
        get() = noImpl
    open val storageArea: Storage?
        get() = noImpl
}

@native public interface StorageEventInit : EventInit {
    var key: String?
    var oldValue: String?
    var newValue: String?
    var url: String
    var storageArea: Storage?
}

@Suppress("NOTHING_TO_INLINE")
public inline fun StorageEventInit(key: String?, oldValue: String?, newValue: String?, url: String, storageArea: Storage?, bubbles: Boolean = false, cancelable: Boolean = false): StorageEventInit {
    val o = js("({})")

    o["key"] = key
    o["oldValue"] = oldValue
    o["newValue"] = newValue
    o["url"] = url
    o["storageArea"] = storageArea
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable

    return o
}

@native public interface HTMLAppletElement : HTMLElement {
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

@native public interface HTMLMarqueeElement : HTMLElement {
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

@native public interface HTMLFrameSetElement : HTMLElement {
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

@native public interface HTMLFrameElement : HTMLElement {
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
    val contentDocument: Document?
        get() = noImpl
    val contentWindow: Window?
        get() = noImpl
    var marginHeight: String
        get() = noImpl
        set(value) = noImpl
    var marginWidth: String
        get() = noImpl
        set(value) = noImpl
}

@native public interface HTMLDirectoryElement : HTMLElement {
    var compact: Boolean
        get() = noImpl
        set(value) = noImpl
}

@native public interface HTMLFontElement : HTMLElement {
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

@native public interface HTMLImageElement : HTMLElement, CanvasImageSource, ImageBitmapSource {
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
    val naturalWidth: Int
        get() = noImpl
    val naturalHeight: Int
        get() = noImpl
    val complete: Boolean
        get() = noImpl
    val currentSrc: String
        get() = noImpl
    val x: Double
        get() = noImpl
    val y: Double
        get() = noImpl
}

@native public interface HTMLPictureElement : HTMLElement {
}

@native public interface EventInit {
    var bubbles: Boolean
    var cancelable: Boolean
}

@Suppress("NOTHING_TO_INLINE")
public inline fun EventInit(bubbles: Boolean = false, cancelable: Boolean = false): EventInit {
    val o = js("({})")

    o["bubbles"] = bubbles
    o["cancelable"] = cancelable

    return o
}

@native public interface CustomEventInit : EventInit {
    var detail: Any?
}

@Suppress("NOTHING_TO_INLINE")
public inline fun CustomEventInit(detail: Any? = null, bubbles: Boolean = false, cancelable: Boolean = false): CustomEventInit {
    val o = js("({})")

    o["detail"] = detail
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable

    return o
}

@native public interface NodeList {
    val length: Int
        get() = noImpl
    fun item(index: Int): Node? = noImpl
    @nativeGetter
    operator fun get(index: Int): Node? = noImpl
}

@native public interface HTMLCollection : UnionElementOrHTMLCollection {
    val length: Int
        get() = noImpl
    fun item(index: Int): Element? = noImpl
    @nativeGetter
    operator fun get(index: Int): Element? = noImpl
    fun namedItem(name: String): Element? = noImpl
    @nativeGetter
    operator fun get(name: String): Element? = noImpl
}

@native public open class MutationObserver(callback: (Array<MutationRecord>, MutationObserver) -> Unit) {
    fun observe(target: Node, options: MutationObserverInit): Unit = noImpl
    fun disconnect(): Unit = noImpl
    fun takeRecords(): Array<MutationRecord> = noImpl
}

@native public interface MutationObserverInit {
    var childList: Boolean
    var attributes: Boolean
    var characterData: Boolean
    var subtree: Boolean
    var attributeOldValue: Boolean
    var characterDataOldValue: Boolean
    var attributeFilter: Array<String>
}

@Suppress("NOTHING_TO_INLINE")
public inline fun MutationObserverInit(childList: Boolean = false, attributes: Boolean, characterData: Boolean, subtree: Boolean = false, attributeOldValue: Boolean, characterDataOldValue: Boolean, attributeFilter: Array<String>): MutationObserverInit {
    val o = js("({})")

    o["childList"] = childList
    o["attributes"] = attributes
    o["characterData"] = characterData
    o["subtree"] = subtree
    o["attributeOldValue"] = attributeOldValue
    o["characterDataOldValue"] = characterDataOldValue
    o["attributeFilter"] = attributeFilter

    return o
}

@native public interface MutationRecord {
    val type: String
        get() = noImpl
    val target: Node
        get() = noImpl
    val addedNodes: NodeList
        get() = noImpl
    val removedNodes: NodeList
        get() = noImpl
    val previousSibling: Node?
        get() = noImpl
    val nextSibling: Node?
        get() = noImpl
    val attributeName: String?
        get() = noImpl
    val attributeNamespace: String?
        get() = noImpl
    val oldValue: String?
        get() = noImpl
}

@native public interface Node : EventTarget {
    val nodeType: Short
        get() = noImpl
    val nodeName: String
        get() = noImpl
    val baseURI: String?
        get() = noImpl
    val ownerDocument: Document?
        get() = noImpl
    val parentNode: Node?
        get() = noImpl
    val parentElement: Element?
        get() = noImpl
    val childNodes: NodeList
        get() = noImpl
    val firstChild: Node?
        get() = noImpl
    val lastChild: Node?
        get() = noImpl
    val previousSibling: Node?
        get() = noImpl
    val nextSibling: Node?
        get() = noImpl
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

@native public interface DOMImplementation {
    fun createDocumentType(qualifiedName: String, publicId: String, systemId: String): DocumentType = noImpl
    fun createDocument(namespace: String?, qualifiedName: String, doctype: DocumentType? = null): XMLDocument = noImpl
    fun createHTMLDocument(title: String = noImpl): Document = noImpl
    fun hasFeature(): Boolean = noImpl
}

@native public open class DocumentFragment : Node {
    open val children: HTMLCollection
        get() = noImpl
    open val firstElementChild: Element?
        get() = noImpl
    open val lastElementChild: Element?
        get() = noImpl
    open val childElementCount: Int
        get() = noImpl
    fun getElementById(elementId: String): Element? = noImpl
    fun prepend(vararg nodes: dynamic): Unit = noImpl
    fun append(vararg nodes: dynamic): Unit = noImpl
    fun query(relativeSelectors: String): Element? = noImpl
    fun queryAll(relativeSelectors: String): dynamic = noImpl
    fun querySelector(selectors: String): Element? = noImpl
    fun querySelectorAll(selectors: String): NodeList = noImpl
}

@native public interface DocumentType : Node {
    val name: String
        get() = noImpl
    val publicId: String
        get() = noImpl
    val systemId: String
        get() = noImpl
    fun before(vararg nodes: dynamic): Unit = noImpl
    fun after(vararg nodes: dynamic): Unit = noImpl
    fun replaceWith(vararg nodes: dynamic): Unit = noImpl
    fun remove(): Unit = noImpl
}

@native public interface NamedNodeMap {
    val length: Int
        get() = noImpl
    fun item(index: Int): Attr? = noImpl
    @nativeGetter
    operator fun get(index: Int): Attr? = noImpl
    fun getNamedItem(name: String): Attr? = noImpl
    @nativeGetter
    operator fun get(name: String): Attr? = noImpl
    fun getNamedItemNS(namespace: String?, localName: String): Attr? = noImpl
    fun setNamedItem(attr: Attr): Attr? = noImpl
    fun setNamedItemNS(attr: Attr): Attr? = noImpl
    fun removeNamedItem(name: String): Attr = noImpl
    fun removeNamedItemNS(namespace: String?, localName: String): Attr = noImpl
}

@native public interface Attr {
    val namespaceURI: String?
        get() = noImpl
    val prefix: String?
        get() = noImpl
    val localName: String
        get() = noImpl
    val name: String
        get() = noImpl
    var value: String
        get() = noImpl
        set(value) = noImpl
    var nodeValue: String
        get() = noImpl
        set(value) = noImpl
    var textContent: String
        get() = noImpl
        set(value) = noImpl
    val ownerElement: Element?
        get() = noImpl
    val specified: Boolean
        get() = noImpl
}

@native public interface CharacterData : Node {
    var data: String
        get() = noImpl
        set(value) = noImpl
    val length: Int
        get() = noImpl
    val previousElementSibling: Element?
        get() = noImpl
    val nextElementSibling: Element?
        get() = noImpl
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

@native public open class Text(data: String = "") : CharacterData, GeometryNode {
    open val wholeText: String
        get() = noImpl
    fun splitText(offset: Int): Text = noImpl
    fun getBoxQuads(options: BoxQuadOptions = noImpl): Array<DOMQuad> = noImpl
    fun convertQuadFromNode(quad: DOMQuad, from: GeometryNode, options: ConvertCoordinateOptions = noImpl): DOMQuad = noImpl
    fun convertRectFromNode(rect: DOMRectReadOnly, from: GeometryNode, options: ConvertCoordinateOptions = noImpl): DOMQuad = noImpl
    fun convertPointFromNode(point: DOMPointInit, from: GeometryNode, options: ConvertCoordinateOptions = noImpl): DOMPoint = noImpl
}

@native public interface ProcessingInstruction : CharacterData, UnionElementOrProcessingInstruction {
    val target: String
        get() = noImpl
    val sheet: StyleSheet?
        get() = noImpl
}

@native public open class Comment(data: String = "") : CharacterData {
}

@native public open class Range {
    open val startContainer: Node
        get() = noImpl
    open val startOffset: Int
        get() = noImpl
    open val endContainer: Node
        get() = noImpl
    open val endOffset: Int
        get() = noImpl
    open val collapsed: Boolean
        get() = noImpl
    open val commonAncestorContainer: Node
        get() = noImpl
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
    fun getClientRects(): dynamic = noImpl
    fun getBoundingClientRect(): DOMRect = noImpl

    companion object {
        val START_TO_START: Short = 0
        val START_TO_END: Short = 1
        val END_TO_END: Short = 2
        val END_TO_START: Short = 3
    }
}

@native public interface NodeIterator {
    val root: Node
        get() = noImpl
    val referenceNode: Node
        get() = noImpl
    val pointerBeforeReferenceNode: Boolean
        get() = noImpl
    val whatToShow: Int
        get() = noImpl
    val filter: NodeFilter?
        get() = noImpl
    fun nextNode(): Node? = noImpl
    fun previousNode(): Node? = noImpl
    fun detach(): Unit = noImpl
}

@native public interface TreeWalker {
    val root: Node
        get() = noImpl
    val whatToShow: Int
        get() = noImpl
    val filter: NodeFilter?
        get() = noImpl
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

@native public interface NodeFilter {
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

@native public interface DOMTokenList {
    val length: Int
        get() = noImpl
    fun item(index: Int): String? = noImpl
    @nativeGetter
    operator fun get(index: Int): String? = noImpl
    fun contains(token: String): Boolean = noImpl
    fun add(vararg tokens: String): Unit = noImpl
    fun remove(vararg tokens: String): Unit = noImpl
    fun toggle(token: String, force: Boolean = noImpl): Boolean = noImpl
}

@native public interface DOMSettableTokenList : DOMTokenList {
    var value: String
        get() = noImpl
        set(value) = noImpl
}

@native public interface Selection {
    val anchorNode: Node?
        get() = noImpl
    val anchorOffset: Int
        get() = noImpl
    val focusNode: Node?
        get() = noImpl
    val focusOffset: Int
        get() = noImpl
    val isCollapsed: Boolean
        get() = noImpl
    val rangeCount: Int
        get() = noImpl
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

@native public open class EditingBeforeInputEvent(type: String, eventInitDict: EditingBeforeInputEventInit = noImpl) : Event(type, eventInitDict) {
    open val command: String
        get() = noImpl
    open val value: String
        get() = noImpl
}

@native public interface EditingBeforeInputEventInit : EventInit {
    var command: String
    var value: String
}

@Suppress("NOTHING_TO_INLINE")
public inline fun EditingBeforeInputEventInit(command: String, value: String, bubbles: Boolean = false, cancelable: Boolean = false): EditingBeforeInputEventInit {
    val o = js("({})")

    o["command"] = command
    o["value"] = value
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable

    return o
}

@native public open class EditingInputEvent(type: String, eventInitDict: EditingInputEventInit = noImpl) : Event(type, eventInitDict) {
    open val command: String
        get() = noImpl
    open val value: String
        get() = noImpl
}

@native public interface EditingInputEventInit : EventInit {
    var command: String
    var value: String
}

@Suppress("NOTHING_TO_INLINE")
public inline fun EditingInputEventInit(command: String, value: String, bubbles: Boolean = false, cancelable: Boolean = false): EditingInputEventInit {
    val o = js("({})")

    o["command"] = command
    o["value"] = value
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable

    return o
}

@native public open class DOMPointReadOnly(x: Double, y: Double, z: Double, w: Double) {
    open val x: Double
        get() = noImpl
    open val y: Double
        get() = noImpl
    open val z: Double
        get() = noImpl
    open val w: Double
        get() = noImpl
    fun matrixTransform(matrix: DOMMatrixReadOnly): DOMPoint = noImpl
}

@native public open class DOMPoint : DOMPointReadOnly {
    constructor(point: DOMPointInit) : super(noImpl, noImpl, noImpl, noImpl)
    constructor(x: Double = 0.0, y: Double = 0.0, z: Double = 0.0, w: Double = 1.0) : super(x, y, z, w)
    override var x: Double
        get() = noImpl
        set(value) = noImpl
    override var y: Double
        get() = noImpl
        set(value) = noImpl
    override var z: Double
        get() = noImpl
        set(value) = noImpl
    override var w: Double
        get() = noImpl
        set(value) = noImpl
}

@native public interface DOMPointInit {
    var x: Double
    var y: Double
    var z: Double
    var w: Double
}

@Suppress("NOTHING_TO_INLINE")
public inline fun DOMPointInit(x: Double = 0.0, y: Double = 0.0, z: Double = 0.0, w: Double = 1.0): DOMPointInit {
    val o = js("({})")

    o["x"] = x
    o["y"] = y
    o["z"] = z
    o["w"] = w

    return o
}

@native public open class DOMRect(x: Double = 0.0, y: Double = 0.0, width: Double = 0.0, height: Double = 0.0) : DOMRectReadOnly(x, y, width, height) {
    override var x: Double
        get() = noImpl
        set(value) = noImpl
    override var y: Double
        get() = noImpl
        set(value) = noImpl
    override var width: Double
        get() = noImpl
        set(value) = noImpl
    override var height: Double
        get() = noImpl
        set(value) = noImpl
}

@native public open class DOMRectReadOnly(x: Double, y: Double, width: Double, height: Double) {
    open val x: Double
        get() = noImpl
    open val y: Double
        get() = noImpl
    open val width: Double
        get() = noImpl
    open val height: Double
        get() = noImpl
    open val top: Double
        get() = noImpl
    open val right: Double
        get() = noImpl
    open val bottom: Double
        get() = noImpl
    open val left: Double
        get() = noImpl
}

@native public interface DOMRectInit {
    var x: Double
    var y: Double
    var width: Double
    var height: Double
}

@Suppress("NOTHING_TO_INLINE")
public inline fun DOMRectInit(x: Double = 0.0, y: Double = 0.0, width: Double = 0.0, height: Double = 0.0): DOMRectInit {
    val o = js("({})")

    o["x"] = x
    o["y"] = y
    o["width"] = width
    o["height"] = height

    return o
}

@native public open class DOMQuad {
    constructor(p1: DOMPointInit = noImpl, p2: DOMPointInit = noImpl, p3: DOMPointInit = noImpl, p4: DOMPointInit = noImpl)
    constructor(rect: DOMRectInit)
    open val p1: DOMPoint
        get() = noImpl
    open val p2: DOMPoint
        get() = noImpl
    open val p3: DOMPoint
        get() = noImpl
    open val p4: DOMPoint
        get() = noImpl
    open val bounds: DOMRectReadOnly
        get() = noImpl
}

@native public open class DOMMatrixReadOnly(numberSequence: Array<Double>) {
    open val a: Double
        get() = noImpl
    open val b: Double
        get() = noImpl
    open val c: Double
        get() = noImpl
    open val d: Double
        get() = noImpl
    open val e: Double
        get() = noImpl
    open val f: Double
        get() = noImpl
    open val m11: Double
        get() = noImpl
    open val m12: Double
        get() = noImpl
    open val m13: Double
        get() = noImpl
    open val m14: Double
        get() = noImpl
    open val m21: Double
        get() = noImpl
    open val m22: Double
        get() = noImpl
    open val m23: Double
        get() = noImpl
    open val m24: Double
        get() = noImpl
    open val m31: Double
        get() = noImpl
    open val m32: Double
        get() = noImpl
    open val m33: Double
        get() = noImpl
    open val m34: Double
        get() = noImpl
    open val m41: Double
        get() = noImpl
    open val m42: Double
        get() = noImpl
    open val m43: Double
        get() = noImpl
    open val m44: Double
        get() = noImpl
    open val is2D: Boolean
        get() = noImpl
    open val isIdentity: Boolean
        get() = noImpl
    fun translate(tx: Double, ty: Double, tz: Double = 0.0): DOMMatrix = noImpl
    fun scale(scale: Double, originX: Double = 0.0, originY: Double = 0.0): DOMMatrix = noImpl
    fun scale3d(scale: Double, originX: Double = 0.0, originY: Double = 0.0, originZ: Double = 0.0): DOMMatrix = noImpl
    fun scaleNonUniform(scaleX: Double, scaleY: Double = 1.0, scaleZ: Double = 1.0, originX: Double = 0.0, originY: Double = 0.0, originZ: Double = 0.0): DOMMatrix = noImpl
    fun rotate(angle: Double, originX: Double = 0.0, originY: Double = 0.0): DOMMatrix = noImpl
    fun rotateFromVector(x: Double, y: Double): DOMMatrix = noImpl
    fun rotateAxisAngle(x: Double, y: Double, z: Double, angle: Double): DOMMatrix = noImpl
    fun skewX(sx: Double): DOMMatrix = noImpl
    fun skewY(sy: Double): DOMMatrix = noImpl
    fun multiply(other: DOMMatrix): DOMMatrix = noImpl
    fun flipX(): DOMMatrix = noImpl
    fun flipY(): DOMMatrix = noImpl
    fun inverse(): DOMMatrix = noImpl
    fun transformPoint(point: DOMPointInit = noImpl): DOMPoint = noImpl
    fun toFloat32Array(): Float32Array = noImpl
    fun toFloat64Array(): Float64Array = noImpl
}

@native public open class DOMMatrix() : DOMMatrixReadOnly(noImpl) {
    constructor(transformList: String) : this()
    constructor(other: DOMMatrixReadOnly) : this()
    constructor(array32: Float32Array) : this()
    constructor(array64: Float64Array) : this()
    constructor(numberSequence: Array<Double>) : this()
    override var a: Double
        get() = noImpl
        set(value) = noImpl
    override var b: Double
        get() = noImpl
        set(value) = noImpl
    override var c: Double
        get() = noImpl
        set(value) = noImpl
    override var d: Double
        get() = noImpl
        set(value) = noImpl
    override var e: Double
        get() = noImpl
        set(value) = noImpl
    override var f: Double
        get() = noImpl
        set(value) = noImpl
    override var m11: Double
        get() = noImpl
        set(value) = noImpl
    override var m12: Double
        get() = noImpl
        set(value) = noImpl
    override var m13: Double
        get() = noImpl
        set(value) = noImpl
    override var m14: Double
        get() = noImpl
        set(value) = noImpl
    override var m21: Double
        get() = noImpl
        set(value) = noImpl
    override var m22: Double
        get() = noImpl
        set(value) = noImpl
    override var m23: Double
        get() = noImpl
        set(value) = noImpl
    override var m24: Double
        get() = noImpl
        set(value) = noImpl
    override var m31: Double
        get() = noImpl
        set(value) = noImpl
    override var m32: Double
        get() = noImpl
        set(value) = noImpl
    override var m33: Double
        get() = noImpl
        set(value) = noImpl
    override var m34: Double
        get() = noImpl
        set(value) = noImpl
    override var m41: Double
        get() = noImpl
        set(value) = noImpl
    override var m42: Double
        get() = noImpl
        set(value) = noImpl
    override var m43: Double
        get() = noImpl
        set(value) = noImpl
    override var m44: Double
        get() = noImpl
        set(value) = noImpl
    fun multiplySelf(other: DOMMatrix): DOMMatrix = noImpl
    fun preMultiplySelf(other: DOMMatrix): DOMMatrix = noImpl
    fun translateSelf(tx: Double, ty: Double, tz: Double = 0.0): DOMMatrix = noImpl
    fun scaleSelf(scale: Double, originX: Double = 0.0, originY: Double = 0.0): DOMMatrix = noImpl
    fun scale3dSelf(scale: Double, originX: Double = 0.0, originY: Double = 0.0, originZ: Double = 0.0): DOMMatrix = noImpl
    fun scaleNonUniformSelf(scaleX: Double, scaleY: Double = 1.0, scaleZ: Double = 1.0, originX: Double = 0.0, originY: Double = 0.0, originZ: Double = 0.0): DOMMatrix = noImpl
    fun rotateSelf(angle: Double, originX: Double = 0.0, originY: Double = 0.0): DOMMatrix = noImpl
    fun rotateFromVectorSelf(x: Double, y: Double): DOMMatrix = noImpl
    fun rotateAxisAngleSelf(x: Double, y: Double, z: Double, angle: Double): DOMMatrix = noImpl
    fun skewXSelf(sx: Double): DOMMatrix = noImpl
    fun skewYSelf(sy: Double): DOMMatrix = noImpl
    fun invertSelf(): DOMMatrix = noImpl
    fun setMatrixValue(transformList: String): DOMMatrix = noImpl
}

@native public interface ScrollOptions {
    var behavior: String
}

@Suppress("NOTHING_TO_INLINE")
public inline fun ScrollOptions(behavior: String = "auto"): ScrollOptions {
    val o = js("({})")

    o["behavior"] = behavior

    return o
}

@native public interface MediaQueryList {
    val media: String
        get() = noImpl
    val matches: Boolean
        get() = noImpl
    fun addListener(listener: (MediaQueryList) -> Unit): Unit = noImpl
    fun removeListener(listener: (MediaQueryList) -> Unit): Unit = noImpl
}

@native public interface Screen {
    val availWidth: Double
        get() = noImpl
    val availHeight: Double
        get() = noImpl
    val width: Double
        get() = noImpl
    val height: Double
        get() = noImpl
    val colorDepth: Int
        get() = noImpl
    val pixelDepth: Int
        get() = noImpl
}

@native public interface CaretPosition {
    val offsetNode: Node
        get() = noImpl
    val offset: Int
        get() = noImpl
    fun getClientRect(): DOMRect? = noImpl
}

@native public interface ScrollOptionsHorizontal : ScrollOptions {
    var x: Double
}

@Suppress("NOTHING_TO_INLINE")
public inline fun ScrollOptionsHorizontal(x: Double, behavior: String = "auto"): ScrollOptionsHorizontal {
    val o = js("({})")

    o["x"] = x
    o["behavior"] = behavior

    return o
}

@native public interface ScrollOptionsVertical : ScrollOptions {
    var y: Double
}

@Suppress("NOTHING_TO_INLINE")
public inline fun ScrollOptionsVertical(y: Double, behavior: String = "auto"): ScrollOptionsVertical {
    val o = js("({})")

    o["y"] = y
    o["behavior"] = behavior

    return o
}

@native public interface BoxQuadOptions {
    var box: String
    var relativeTo: GeometryNode
}

@Suppress("NOTHING_TO_INLINE")
public inline fun BoxQuadOptions(box: String = "border", relativeTo: GeometryNode): BoxQuadOptions {
    val o = js("({})")

    o["box"] = box
    o["relativeTo"] = relativeTo

    return o
}

@native public interface ConvertCoordinateOptions {
    var fromBox: String
    var toBox: String
}

@Suppress("NOTHING_TO_INLINE")
public inline fun ConvertCoordinateOptions(fromBox: String = "border", toBox: String = "border"): ConvertCoordinateOptions {
    val o = js("({})")

    o["fromBox"] = fromBox
    o["toBox"] = toBox

    return o
}

@native public @marker interface UnionElementOrProcessingInstruction {
}

@native public @marker interface UnionElementOrHTMLCollection {
}

@native public @marker interface UnionElementOrRadioNodeList {
}

@native public @marker interface UnionHTMLOptGroupElementOrHTMLOptionElement {
}

@native public @marker interface UnionAudioTrackOrTextTrackOrVideoTrack {
}

@native public @marker interface UnionElementOrMouseEvent {
}

@native public @marker interface UnionMessagePortOrWindow {
}

@native public @marker interface UnionMessagePortOrServiceWorker {
}

@native public @marker interface ArrayBufferView {
}

@native public @marker interface Transferable {
}

@native public @marker interface RenderingContext {
}

@native public @marker interface CanvasImageSource {
}

@native public @marker interface ImageBitmapSource {
}

@native public @marker interface GeometryNode {
}

