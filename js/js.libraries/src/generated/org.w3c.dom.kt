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

@native public open class Document : Node(), GlobalEventHandlers, DocumentAndElementEventHandlers, NonElementParentNode, DocumentOrShadowRoot, ParentNode, GeometryUtils {
    open val fullscreenEnabled: Boolean
        get() = noImpl
    open val fullscreen: Boolean
        get() = noImpl
    var onfullscreenchange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onfullscreenerror: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    open val rootElement: SVGSVGElement?
        get() = noImpl
    var title: String
        get() = noImpl
        set(value) = noImpl
    open val referrer: String
        get() = noImpl
    var domain: String
        get() = noImpl
        set(value) = noImpl
    open val activeElement: Element?
        get() = noImpl
    open val location: Location?
        get() = noImpl
    var cookie: String
        get() = noImpl
        set(value) = noImpl
    open val lastModified: String
        get() = noImpl
    open val readyState: String
        get() = noImpl
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
    open val currentScript: HTMLOrSVGScriptElement?
        get() = noImpl
    open val defaultView: Window?
        get() = noImpl
    var designMode: String
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
    open val charset: String
        get() = noImpl
    open val inputEncoding: String
        get() = noImpl
    open val contentType: String
        get() = noImpl
    open val doctype: DocumentType?
        get() = noImpl
    open val documentElement: Element?
        get() = noImpl
    open val scrollingElement: Element?
        get() = noImpl
    open val styleSheets: StyleSheetList
        get() = noImpl
    fun exitFullscreen(): dynamic = noImpl
    @nativeGetter
    operator fun get(name: String): dynamic = noImpl
    fun getElementsByName(elementName: String): NodeList = noImpl
    fun open(type: String = "text/html", replace: String = ""): Document = noImpl
    fun open(url: String, name: String, features: String): Window = noImpl
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
    fun getElementsByTagName(qualifiedName: String): HTMLCollection = noImpl
    fun getElementsByTagNameNS(namespace: String?, localName: String): HTMLCollection = noImpl
    fun getElementsByClassName(classNames: String): HTMLCollection = noImpl
    fun createElement(localName: String, options: ElementCreationOptions = noImpl): Element = noImpl
    fun createElementNS(namespace: String?, qualifiedName: String, options: ElementCreationOptions = noImpl): Element = noImpl
    fun createDocumentFragment(): DocumentFragment = noImpl
    fun createTextNode(data: String): Text = noImpl
    fun createCDATASection(data: String): CDATASection = noImpl
    fun createComment(data: String): Comment = noImpl
    fun createProcessingInstruction(target: String, data: String): ProcessingInstruction = noImpl
    fun importNode(node: Node, deep: Boolean = false): Node = noImpl
    fun adoptNode(node: Node): Node = noImpl
    fun createAttribute(localName: String): Attr = noImpl
    fun createAttributeNS(namespace: String?, qualifiedName: String): Attr = noImpl
    fun createEvent(interface_: String): Event = noImpl
    fun createRange(): Range = noImpl
    fun createNodeIterator(root: Node, whatToShow: Int = noImpl, filter: NodeFilter? = null): NodeIterator = noImpl
    fun createNodeIterator(root: Node, whatToShow: Int = noImpl, filter: ((Node) -> Short)? = null): NodeIterator = noImpl
    fun createTreeWalker(root: Node, whatToShow: Int = noImpl, filter: NodeFilter? = null): TreeWalker = noImpl
    fun createTreeWalker(root: Node, whatToShow: Int = noImpl, filter: ((Node) -> Short)? = null): TreeWalker = noImpl
    fun elementFromPoint(x: Double, y: Double): Element? = noImpl
    fun elementsFromPoint(x: Double, y: Double): Array<Element> = noImpl
    fun caretPositionFromPoint(x: Double, y: Double): CaretPosition? = noImpl
}

@native public abstract class Window : EventTarget(), GlobalEventHandlers, WindowEventHandlers, WindowOrWorkerGlobalScope, WindowSessionStorage, WindowLocalStorage, GlobalPerformance, UnionMessagePortOrWindow {
    open val window: Window
        get() = noImpl
    open val self: Window
        get() = noImpl
    open val document: Document
        get() = noImpl
    open var name: String
        get() = noImpl
        set(value) = noImpl
    open val location: Location
        get() = noImpl
    open val history: History
        get() = noImpl
    open val customElements: CustomElementRegistry
        get() = noImpl
    open val locationbar: BarProp
        get() = noImpl
    open val menubar: BarProp
        get() = noImpl
    open val personalbar: BarProp
        get() = noImpl
    open val scrollbars: BarProp
        get() = noImpl
    open val statusbar: BarProp
        get() = noImpl
    open val toolbar: BarProp
        get() = noImpl
    open var status: String
        get() = noImpl
        set(value) = noImpl
    open val closed: Boolean
        get() = noImpl
    open val frames: Window
        get() = noImpl
    open val length: Int
        get() = noImpl
    open val top: Window
        get() = noImpl
    open var opener: Any?
        get() = noImpl
        set(value) = noImpl
    open val parent: Window
        get() = noImpl
    open val frameElement: Element?
        get() = noImpl
    open val navigator: Navigator
        get() = noImpl
    open val applicationCache: ApplicationCache
        get() = noImpl
    open val external: External
        get() = noImpl
    open val screen: Screen
        get() = noImpl
    open val innerWidth: Int
        get() = noImpl
    open val innerHeight: Int
        get() = noImpl
    open val scrollX: Double
        get() = noImpl
    open val pageXOffset: Double
        get() = noImpl
    open val scrollY: Double
        get() = noImpl
    open val pageYOffset: Double
        get() = noImpl
    open val screenX: Int
        get() = noImpl
    open val screenY: Int
        get() = noImpl
    open val outerWidth: Int
        get() = noImpl
    open val outerHeight: Int
        get() = noImpl
    open val devicePixelRatio: Double
        get() = noImpl
    fun close(): Unit = noImpl
    fun stop(): Unit = noImpl
    fun focus(): Unit = noImpl
    fun blur(): Unit = noImpl
    fun open(url: String = "about:blank", target: String = "_blank", features: String = ""): Window? = noImpl
    @nativeGetter
    operator fun get(name: String): dynamic = noImpl
    fun alert(): Unit = noImpl
    fun alert(message: String): Unit = noImpl
    fun confirm(message: String = ""): Boolean = noImpl
    fun prompt(message: String = "", default: String = ""): String? = noImpl
    fun print(): Unit = noImpl
    fun requestAnimationFrame(callback: (Double) -> Unit): Int = noImpl
    fun cancelAnimationFrame(handle: Int): Unit = noImpl
    fun postMessage(message: Any?, targetOrigin: String, transfer: Array<dynamic> = arrayOf()): Unit = noImpl
    fun captureEvents(): Unit = noImpl
    fun releaseEvents(): Unit = noImpl
    fun matchMedia(query: String): MediaQueryList = noImpl
    fun moveTo(x: Int, y: Int): Unit = noImpl
    fun moveBy(x: Int, y: Int): Unit = noImpl
    fun resizeTo(x: Int, y: Int): Unit = noImpl
    fun resizeBy(x: Int, y: Int): Unit = noImpl
    fun scroll(options: ScrollToOptions = noImpl): Unit = noImpl
    fun scroll(x: Double, y: Double): Unit = noImpl
    fun scrollTo(options: ScrollToOptions = noImpl): Unit = noImpl
    fun scrollTo(x: Double, y: Double): Unit = noImpl
    fun scrollBy(options: ScrollToOptions = noImpl): Unit = noImpl
    fun scrollBy(x: Double, y: Double): Unit = noImpl
    fun getComputedStyle(elt: Element, pseudoElt: String? = noImpl): CSSStyleDeclaration = noImpl
}

@native public abstract class HTMLAllCollection {
    open val length: Int
        get() = noImpl
//    @nativeGetter
//    operator fun get(index: Int): Element? = noImpl
//    fun namedItem(name: String): UnionElementOrHTMLCollection? = noImpl
//    @nativeGetter
//    operator fun get(name: String): UnionElementOrHTMLCollection? = noImpl
    fun item(nameOrIndex: String = noImpl): UnionElementOrHTMLCollection? = noImpl
}

@native public abstract class HTMLFormControlsCollection : HTMLCollection() {
//    override fun namedItem(name: String): UnionElementOrRadioNodeList? = noImpl
//    @nativeGetter
//    operator override fun get(name: String): UnionElementOrRadioNodeList? = noImpl
}

@native public abstract class RadioNodeList : NodeList(), UnionElementOrRadioNodeList {
    open var value: String
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class HTMLOptionsCollection : HTMLCollection() {
    override var length: Int
        get() = noImpl
        set(value) = noImpl
    open var selectedIndex: Int
        get() = noImpl
        set(value) = noImpl
    @nativeSetter
    operator fun set(index: Int, option: HTMLOptionElement?): Unit = noImpl
    fun add(element: UnionHTMLOptGroupElementOrHTMLOptionElement, before: dynamic = null): Unit = noImpl
    fun remove(index: Int): Unit = noImpl
}

@native public abstract class HTMLElement : Element(), ElementCSSInlineStyle, GlobalEventHandlers, DocumentAndElementEventHandlers, ElementContentEditable {
    open var title: String
        get() = noImpl
        set(value) = noImpl
    open var lang: String
        get() = noImpl
        set(value) = noImpl
    open var translate: Boolean
        get() = noImpl
        set(value) = noImpl
    open var dir: String
        get() = noImpl
        set(value) = noImpl
    open val dataset: DOMStringMap
        get() = noImpl
    open var hidden: Boolean
        get() = noImpl
        set(value) = noImpl
    open var tabIndex: Int
        get() = noImpl
        set(value) = noImpl
    open var accessKey: String
        get() = noImpl
        set(value) = noImpl
    open val accessKeyLabel: String
        get() = noImpl
    open var draggable: Boolean
        get() = noImpl
        set(value) = noImpl
    open val dropzone: DOMTokenList
        get() = noImpl
    open var contextMenu: HTMLMenuElement?
        get() = noImpl
        set(value) = noImpl
    open var spellcheck: Boolean
        get() = noImpl
        set(value) = noImpl
    open var innerText: String
        get() = noImpl
        set(value) = noImpl
    open val offsetParent: Element?
        get() = noImpl
    open val offsetTop: Int
        get() = noImpl
    open val offsetLeft: Int
        get() = noImpl
    open val offsetWidth: Int
        get() = noImpl
    open val offsetHeight: Int
        get() = noImpl
    fun click(): Unit = noImpl
    fun focus(): Unit = noImpl
    fun blur(): Unit = noImpl
    fun forceSpellCheck(): Unit = noImpl
}

@native public abstract class HTMLUnknownElement : HTMLElement() {
}

@native public abstract class DOMStringMap {
    @nativeGetter
    operator fun get(name: String): String? = noImpl
    @nativeSetter
    operator fun set(name: String, value: String): Unit = noImpl
}

@native public abstract class HTMLHtmlElement : HTMLElement() {
    open var version: String
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class HTMLHeadElement : HTMLElement() {
}

@native public abstract class HTMLTitleElement : HTMLElement() {
    open var text: String
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class HTMLBaseElement : HTMLElement() {
    open var href: String
        get() = noImpl
        set(value) = noImpl
    open var target: String
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class HTMLLinkElement : HTMLElement(), LinkStyle {
    open var scope: String
        get() = noImpl
        set(value) = noImpl
    open var workerType: String
        get() = noImpl
        set(value) = noImpl
    open var href: String
        get() = noImpl
        set(value) = noImpl
    open var crossOrigin: String?
        get() = noImpl
        set(value) = noImpl
    open var rel: String
        get() = noImpl
        set(value) = noImpl
    @native("as") open var as_: String
        get() = noImpl
        set(value) = noImpl
    open val relList: DOMTokenList
        get() = noImpl
    open var media: String
        get() = noImpl
        set(value) = noImpl
    open var nonce: String
        get() = noImpl
        set(value) = noImpl
    open var hreflang: String
        get() = noImpl
        set(value) = noImpl
    open var type: String
        get() = noImpl
        set(value) = noImpl
    open val sizes: DOMTokenList
        get() = noImpl
    open var referrerPolicy: String
        get() = noImpl
        set(value) = noImpl
    open var charset: String
        get() = noImpl
        set(value) = noImpl
    open var rev: String
        get() = noImpl
        set(value) = noImpl
    open var target: String
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class HTMLMetaElement : HTMLElement() {
    open var name: String
        get() = noImpl
        set(value) = noImpl
    open var httpEquiv: String
        get() = noImpl
        set(value) = noImpl
    open var content: String
        get() = noImpl
        set(value) = noImpl
    open var scheme: String
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class HTMLStyleElement : HTMLElement(), LinkStyle {
    open var media: String
        get() = noImpl
        set(value) = noImpl
    open var nonce: String
        get() = noImpl
        set(value) = noImpl
    open var type: String
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class HTMLBodyElement : HTMLElement(), WindowEventHandlers {
    open var text: String
        get() = noImpl
        set(value) = noImpl
    open var link: String
        get() = noImpl
        set(value) = noImpl
    open var vLink: String
        get() = noImpl
        set(value) = noImpl
    open var aLink: String
        get() = noImpl
        set(value) = noImpl
    open var bgColor: String
        get() = noImpl
        set(value) = noImpl
    open var background: String
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class HTMLHeadingElement : HTMLElement() {
    open var align: String
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class HTMLParagraphElement : HTMLElement() {
    open var align: String
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class HTMLHRElement : HTMLElement() {
    open var align: String
        get() = noImpl
        set(value) = noImpl
    open var color: String
        get() = noImpl
        set(value) = noImpl
    open var noShade: Boolean
        get() = noImpl
        set(value) = noImpl
    open var size: String
        get() = noImpl
        set(value) = noImpl
    open var width: String
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class HTMLPreElement : HTMLElement() {
    open var width: Int
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class HTMLQuoteElement : HTMLElement() {
    open var cite: String
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class HTMLOListElement : HTMLElement() {
    open var reversed: Boolean
        get() = noImpl
        set(value) = noImpl
    open var start: Int
        get() = noImpl
        set(value) = noImpl
    open var type: String
        get() = noImpl
        set(value) = noImpl
    open var compact: Boolean
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class HTMLUListElement : HTMLElement() {
    open var compact: Boolean
        get() = noImpl
        set(value) = noImpl
    open var type: String
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class HTMLLIElement : HTMLElement() {
    open var value: Int
        get() = noImpl
        set(value) = noImpl
    open var type: String
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class HTMLDListElement : HTMLElement() {
    open var compact: Boolean
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class HTMLDivElement : HTMLElement() {
    open var align: String
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class HTMLAnchorElement : HTMLElement(), HTMLHyperlinkElementUtils {
    open var target: String
        get() = noImpl
        set(value) = noImpl
    open var download: String
        get() = noImpl
        set(value) = noImpl
    open var ping: String
        get() = noImpl
        set(value) = noImpl
    open var rel: String
        get() = noImpl
        set(value) = noImpl
    open val relList: DOMTokenList
        get() = noImpl
    open var hreflang: String
        get() = noImpl
        set(value) = noImpl
    open var type: String
        get() = noImpl
        set(value) = noImpl
    open var text: String
        get() = noImpl
        set(value) = noImpl
    open var referrerPolicy: String
        get() = noImpl
        set(value) = noImpl
    open var coords: String
        get() = noImpl
        set(value) = noImpl
    open var charset: String
        get() = noImpl
        set(value) = noImpl
    open var name: String
        get() = noImpl
        set(value) = noImpl
    open var rev: String
        get() = noImpl
        set(value) = noImpl
    open var shape: String
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class HTMLDataElement : HTMLElement() {
    open var value: String
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class HTMLTimeElement : HTMLElement() {
    open var dateTime: String
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class HTMLSpanElement : HTMLElement() {
}

@native public abstract class HTMLBRElement : HTMLElement() {
    open var clear: String
        get() = noImpl
        set(value) = noImpl
}

@native public interface HTMLHyperlinkElementUtils {
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
    var hash: String
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class HTMLModElement : HTMLElement() {
    open var cite: String
        get() = noImpl
        set(value) = noImpl
    open var dateTime: String
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class HTMLPictureElement : HTMLElement() {
}

@native public abstract class HTMLSourceElement : HTMLElement() {
    open var src: String
        get() = noImpl
        set(value) = noImpl
    open var type: String
        get() = noImpl
        set(value) = noImpl
    open var srcset: String
        get() = noImpl
        set(value) = noImpl
    open var sizes: String
        get() = noImpl
        set(value) = noImpl
    open var media: String
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class HTMLImageElement : HTMLElement(), TexImageSource, HTMLOrSVGImageElement {
    open var alt: String
        get() = noImpl
        set(value) = noImpl
    open var src: String
        get() = noImpl
        set(value) = noImpl
    open var srcset: String
        get() = noImpl
        set(value) = noImpl
    open var sizes: String
        get() = noImpl
        set(value) = noImpl
    open var crossOrigin: String?
        get() = noImpl
        set(value) = noImpl
    open var useMap: String
        get() = noImpl
        set(value) = noImpl
    open var isMap: Boolean
        get() = noImpl
        set(value) = noImpl
    open var width: Int
        get() = noImpl
        set(value) = noImpl
    open var height: Int
        get() = noImpl
        set(value) = noImpl
    open val naturalWidth: Int
        get() = noImpl
    open val naturalHeight: Int
        get() = noImpl
    open val complete: Boolean
        get() = noImpl
    open val currentSrc: String
        get() = noImpl
    open var referrerPolicy: String
        get() = noImpl
        set(value) = noImpl
    open var name: String
        get() = noImpl
        set(value) = noImpl
    open var lowsrc: String
        get() = noImpl
        set(value) = noImpl
    open var align: String
        get() = noImpl
        set(value) = noImpl
    open var hspace: Int
        get() = noImpl
        set(value) = noImpl
    open var vspace: Int
        get() = noImpl
        set(value) = noImpl
    open var longDesc: String
        get() = noImpl
        set(value) = noImpl
    open var border: String
        get() = noImpl
        set(value) = noImpl
    open val x: Int
        get() = noImpl
    open val y: Int
        get() = noImpl
}

@native public abstract class HTMLIFrameElement : HTMLElement() {
    open var src: String
        get() = noImpl
        set(value) = noImpl
    open var srcdoc: String
        get() = noImpl
        set(value) = noImpl
    open var name: String
        get() = noImpl
        set(value) = noImpl
    open val sandbox: DOMTokenList
        get() = noImpl
    open var allowFullscreen: Boolean
        get() = noImpl
        set(value) = noImpl
    open var allowUserMedia: Boolean
        get() = noImpl
        set(value) = noImpl
    open var width: String
        get() = noImpl
        set(value) = noImpl
    open var height: String
        get() = noImpl
        set(value) = noImpl
    open var referrerPolicy: String
        get() = noImpl
        set(value) = noImpl
    open val contentDocument: Document?
        get() = noImpl
    open val contentWindow: Window?
        get() = noImpl
    open var align: String
        get() = noImpl
        set(value) = noImpl
    open var scrolling: String
        get() = noImpl
        set(value) = noImpl
    open var frameBorder: String
        get() = noImpl
        set(value) = noImpl
    open var longDesc: String
        get() = noImpl
        set(value) = noImpl
    open var marginHeight: String
        get() = noImpl
        set(value) = noImpl
    open var marginWidth: String
        get() = noImpl
        set(value) = noImpl
    fun getSVGDocument(): Document? = noImpl
}

@native public abstract class HTMLEmbedElement : HTMLElement() {
    open var src: String
        get() = noImpl
        set(value) = noImpl
    open var type: String
        get() = noImpl
        set(value) = noImpl
    open var width: String
        get() = noImpl
        set(value) = noImpl
    open var height: String
        get() = noImpl
        set(value) = noImpl
    open var align: String
        get() = noImpl
        set(value) = noImpl
    open var name: String
        get() = noImpl
        set(value) = noImpl
    fun getSVGDocument(): Document? = noImpl
}

@native public abstract class HTMLObjectElement : HTMLElement() {
    open var data: String
        get() = noImpl
        set(value) = noImpl
    open var type: String
        get() = noImpl
        set(value) = noImpl
    open var typeMustMatch: Boolean
        get() = noImpl
        set(value) = noImpl
    open var name: String
        get() = noImpl
        set(value) = noImpl
    open var useMap: String
        get() = noImpl
        set(value) = noImpl
    open val form: HTMLFormElement?
        get() = noImpl
    open var width: String
        get() = noImpl
        set(value) = noImpl
    open var height: String
        get() = noImpl
        set(value) = noImpl
    open val contentDocument: Document?
        get() = noImpl
    open val contentWindow: Window?
        get() = noImpl
    open val willValidate: Boolean
        get() = noImpl
    open val validity: ValidityState
        get() = noImpl
    open val validationMessage: String
        get() = noImpl
    open var align: String
        get() = noImpl
        set(value) = noImpl
    open var archive: String
        get() = noImpl
        set(value) = noImpl
    open var code: String
        get() = noImpl
        set(value) = noImpl
    open var declare: Boolean
        get() = noImpl
        set(value) = noImpl
    open var hspace: Int
        get() = noImpl
        set(value) = noImpl
    open var standby: String
        get() = noImpl
        set(value) = noImpl
    open var vspace: Int
        get() = noImpl
        set(value) = noImpl
    open var codeBase: String
        get() = noImpl
        set(value) = noImpl
    open var codeType: String
        get() = noImpl
        set(value) = noImpl
    open var border: String
        get() = noImpl
        set(value) = noImpl
    fun getSVGDocument(): Document? = noImpl
    fun checkValidity(): Boolean = noImpl
    fun reportValidity(): Boolean = noImpl
    fun setCustomValidity(error: String): Unit = noImpl
}

@native public abstract class HTMLParamElement : HTMLElement() {
    open var name: String
        get() = noImpl
        set(value) = noImpl
    open var value: String
        get() = noImpl
        set(value) = noImpl
    open var type: String
        get() = noImpl
        set(value) = noImpl
    open var valueType: String
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class HTMLVideoElement : HTMLMediaElement(), TexImageSource {
    open var width: Int
        get() = noImpl
        set(value) = noImpl
    open var height: Int
        get() = noImpl
        set(value) = noImpl
    open val videoWidth: Int
        get() = noImpl
    open val videoHeight: Int
        get() = noImpl
    open var poster: String
        get() = noImpl
        set(value) = noImpl
    open var playsInline: Boolean
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class HTMLAudioElement : HTMLMediaElement() {
}

@native public abstract class HTMLTrackElement : HTMLElement() {
    open var kind: String
        get() = noImpl
        set(value) = noImpl
    open var src: String
        get() = noImpl
        set(value) = noImpl
    open var srclang: String
        get() = noImpl
        set(value) = noImpl
    open var label: String
        get() = noImpl
        set(value) = noImpl
    open var default: Boolean
        get() = noImpl
        set(value) = noImpl
    open val readyState: Short
        get() = noImpl
    open val track: TextTrack
        get() = noImpl

    companion object {
        val NONE: Short = 0
        val LOADING: Short = 1
        val LOADED: Short = 2
        val ERROR: Short = 3
    }
}

@native public abstract class HTMLMediaElement : HTMLElement() {
    open val error: MediaError?
        get() = noImpl
    open var src: String
        get() = noImpl
        set(value) = noImpl
    open var srcObject: dynamic
        get() = noImpl
        set(value) = noImpl
    open val currentSrc: String
        get() = noImpl
    open var crossOrigin: String?
        get() = noImpl
        set(value) = noImpl
    open val networkState: Short
        get() = noImpl
    open var preload: String
        get() = noImpl
        set(value) = noImpl
    open val buffered: TimeRanges
        get() = noImpl
    open val readyState: Short
        get() = noImpl
    open val seeking: Boolean
        get() = noImpl
    open var currentTime: Double
        get() = noImpl
        set(value) = noImpl
    open val duration: Double
        get() = noImpl
    open val paused: Boolean
        get() = noImpl
    open var defaultPlaybackRate: Double
        get() = noImpl
        set(value) = noImpl
    open var playbackRate: Double
        get() = noImpl
        set(value) = noImpl
    open val played: TimeRanges
        get() = noImpl
    open val seekable: TimeRanges
        get() = noImpl
    open val ended: Boolean
        get() = noImpl
    open var autoplay: Boolean
        get() = noImpl
        set(value) = noImpl
    open var loop: Boolean
        get() = noImpl
        set(value) = noImpl
    open var controls: Boolean
        get() = noImpl
        set(value) = noImpl
    open var volume: Double
        get() = noImpl
        set(value) = noImpl
    open var muted: Boolean
        get() = noImpl
        set(value) = noImpl
    open var defaultMuted: Boolean
        get() = noImpl
        set(value) = noImpl
    open val audioTracks: AudioTrackList
        get() = noImpl
    open val videoTracks: VideoTrackList
        get() = noImpl
    open val textTracks: TextTrackList
        get() = noImpl
    fun load(): Unit = noImpl
    fun canPlayType(type: String): String = noImpl
    fun fastSeek(time: Double): Unit = noImpl
    fun getStartDate(): dynamic = noImpl
    fun play(): dynamic = noImpl
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

@native public abstract class MediaError {
    open val code: Short
        get() = noImpl

    companion object {
        val MEDIA_ERR_ABORTED: Short = 1
        val MEDIA_ERR_NETWORK: Short = 2
        val MEDIA_ERR_DECODE: Short = 3
        val MEDIA_ERR_SRC_NOT_SUPPORTED: Short = 4
    }
}

@native public abstract class AudioTrackList : EventTarget() {
    open val length: Int
        get() = noImpl
    open var onchange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    open var onaddtrack: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    open var onremovetrack: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    @nativeGetter
    operator fun get(index: Int): AudioTrack? = noImpl
    fun getTrackById(id: String): AudioTrack? = noImpl
}

@native public abstract class AudioTrack : UnionAudioTrackOrTextTrackOrVideoTrack {
    open val id: String
        get() = noImpl
    open val kind: String
        get() = noImpl
    open val label: String
        get() = noImpl
    open val language: String
        get() = noImpl
    open var enabled: Boolean
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class VideoTrackList : EventTarget() {
    open val length: Int
        get() = noImpl
    open val selectedIndex: Int
        get() = noImpl
    open var onchange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    open var onaddtrack: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    open var onremovetrack: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    @nativeGetter
    operator fun get(index: Int): VideoTrack? = noImpl
    fun getTrackById(id: String): VideoTrack? = noImpl
}

@native public abstract class VideoTrack : UnionAudioTrackOrTextTrackOrVideoTrack {
    open val id: String
        get() = noImpl
    open val kind: String
        get() = noImpl
    open val label: String
        get() = noImpl
    open val language: String
        get() = noImpl
    open var selected: Boolean
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class TextTrackList : EventTarget() {
    open val length: Int
        get() = noImpl
    open var onchange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    open var onaddtrack: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    open var onremovetrack: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    @nativeGetter
    operator fun get(index: Int): TextTrack? = noImpl
    fun getTrackById(id: String): TextTrack? = noImpl
}

@native public abstract class TextTrack : EventTarget(), UnionAudioTrackOrTextTrackOrVideoTrack {
    open val kind: String
        get() = noImpl
    open val label: String
        get() = noImpl
    open val language: String
        get() = noImpl
    open val id: String
        get() = noImpl
    open val inBandMetadataTrackDispatchType: String
        get() = noImpl
    open var mode: String
        get() = noImpl
        set(value) = noImpl
    open val cues: TextTrackCueList?
        get() = noImpl
    open val activeCues: TextTrackCueList?
        get() = noImpl
    open var oncuechange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    fun addCue(cue: TextTrackCue): Unit = noImpl
    fun removeCue(cue: TextTrackCue): Unit = noImpl
}

@native public abstract class TextTrackCueList {
    open val length: Int
        get() = noImpl
    @nativeGetter
    operator fun get(index: Int): TextTrackCue? = noImpl
    fun getCueById(id: String): TextTrackCue? = noImpl
}

@native public abstract class TextTrackCue : EventTarget() {
    open val track: TextTrack?
        get() = noImpl
    open var id: String
        get() = noImpl
        set(value) = noImpl
    open var startTime: Double
        get() = noImpl
        set(value) = noImpl
    open var endTime: Double
        get() = noImpl
        set(value) = noImpl
    open var pauseOnExit: Boolean
        get() = noImpl
        set(value) = noImpl
    open var onenter: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    open var onexit: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class TimeRanges {
    open val length: Int
        get() = noImpl
    fun start(index: Int): Double = noImpl
    fun end(index: Int): Double = noImpl
}

@native public open class TrackEvent(type: String, eventInitDict: TrackEventInit = noImpl) : Event(type, eventInitDict) {
    open val track: UnionAudioTrackOrTextTrackOrVideoTrack?
        get() = noImpl
}

@native public interface TrackEventInit : EventInit {
    var track: UnionAudioTrackOrTextTrackOrVideoTrack? /* = null */
}

@Suppress("NOTHING_TO_INLINE")
public inline fun TrackEventInit(track: UnionAudioTrackOrTextTrackOrVideoTrack? = null, bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): TrackEventInit {
    val o = js("({})")

    o["track"] = track
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable
    o["composed"] = composed

    return o
}

@native public abstract class HTMLMapElement : HTMLElement() {
    open var name: String
        get() = noImpl
        set(value) = noImpl
    open val areas: HTMLCollection
        get() = noImpl
}

@native public abstract class HTMLAreaElement : HTMLElement(), HTMLHyperlinkElementUtils {
    open var alt: String
        get() = noImpl
        set(value) = noImpl
    open var coords: String
        get() = noImpl
        set(value) = noImpl
    open var shape: String
        get() = noImpl
        set(value) = noImpl
    open var target: String
        get() = noImpl
        set(value) = noImpl
    open var download: String
        get() = noImpl
        set(value) = noImpl
    open var ping: String
        get() = noImpl
        set(value) = noImpl
    open var rel: String
        get() = noImpl
        set(value) = noImpl
    open val relList: DOMTokenList
        get() = noImpl
    open var referrerPolicy: String
        get() = noImpl
        set(value) = noImpl
    open var noHref: Boolean
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class HTMLTableElement : HTMLElement() {
    open var caption: HTMLTableCaptionElement?
        get() = noImpl
        set(value) = noImpl
    open var tHead: HTMLTableSectionElement?
        get() = noImpl
        set(value) = noImpl
    open var tFoot: HTMLTableSectionElement?
        get() = noImpl
        set(value) = noImpl
    open val tBodies: HTMLCollection
        get() = noImpl
    open val rows: HTMLCollection
        get() = noImpl
    open var align: String
        get() = noImpl
        set(value) = noImpl
    open var border: String
        get() = noImpl
        set(value) = noImpl
    open var frame: String
        get() = noImpl
        set(value) = noImpl
    open var rules: String
        get() = noImpl
        set(value) = noImpl
    open var summary: String
        get() = noImpl
        set(value) = noImpl
    open var width: String
        get() = noImpl
        set(value) = noImpl
    open var bgColor: String
        get() = noImpl
        set(value) = noImpl
    open var cellPadding: String
        get() = noImpl
        set(value) = noImpl
    open var cellSpacing: String
        get() = noImpl
        set(value) = noImpl
    fun createCaption(): HTMLTableCaptionElement = noImpl
    fun deleteCaption(): Unit = noImpl
    fun createTHead(): HTMLTableSectionElement = noImpl
    fun deleteTHead(): Unit = noImpl
    fun createTFoot(): HTMLTableSectionElement = noImpl
    fun deleteTFoot(): Unit = noImpl
    fun createTBody(): HTMLTableSectionElement = noImpl
    fun insertRow(index: Int = -1): HTMLTableRowElement = noImpl
    fun deleteRow(index: Int): Unit = noImpl
}

@native public abstract class HTMLTableCaptionElement : HTMLElement() {
    open var align: String
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class HTMLTableColElement : HTMLElement() {
    open var span: Int
        get() = noImpl
        set(value) = noImpl
    open var align: String
        get() = noImpl
        set(value) = noImpl
    open var ch: String
        get() = noImpl
        set(value) = noImpl
    open var chOff: String
        get() = noImpl
        set(value) = noImpl
    open var vAlign: String
        get() = noImpl
        set(value) = noImpl
    open var width: String
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class HTMLTableSectionElement : HTMLElement() {
    open val rows: HTMLCollection
        get() = noImpl
    open var align: String
        get() = noImpl
        set(value) = noImpl
    open var ch: String
        get() = noImpl
        set(value) = noImpl
    open var chOff: String
        get() = noImpl
        set(value) = noImpl
    open var vAlign: String
        get() = noImpl
        set(value) = noImpl
    fun insertRow(index: Int = -1): HTMLElement = noImpl
    fun deleteRow(index: Int): Unit = noImpl
}

@native public abstract class HTMLTableRowElement : HTMLElement() {
    open val rowIndex: Int
        get() = noImpl
    open val sectionRowIndex: Int
        get() = noImpl
    open val cells: HTMLCollection
        get() = noImpl
    open var align: String
        get() = noImpl
        set(value) = noImpl
    open var ch: String
        get() = noImpl
        set(value) = noImpl
    open var chOff: String
        get() = noImpl
        set(value) = noImpl
    open var vAlign: String
        get() = noImpl
        set(value) = noImpl
    open var bgColor: String
        get() = noImpl
        set(value) = noImpl
    fun insertCell(index: Int = -1): HTMLElement = noImpl
    fun deleteCell(index: Int): Unit = noImpl
}

@native public abstract class HTMLTableCellElement : HTMLElement() {
    open var colSpan: Int
        get() = noImpl
        set(value) = noImpl
    open var rowSpan: Int
        get() = noImpl
        set(value) = noImpl
    open var headers: String
        get() = noImpl
        set(value) = noImpl
    open val cellIndex: Int
        get() = noImpl
    open var scope: String
        get() = noImpl
        set(value) = noImpl
    open var abbr: String
        get() = noImpl
        set(value) = noImpl
    open var align: String
        get() = noImpl
        set(value) = noImpl
    open var axis: String
        get() = noImpl
        set(value) = noImpl
    open var height: String
        get() = noImpl
        set(value) = noImpl
    open var width: String
        get() = noImpl
        set(value) = noImpl
    open var ch: String
        get() = noImpl
        set(value) = noImpl
    open var chOff: String
        get() = noImpl
        set(value) = noImpl
    open var noWrap: Boolean
        get() = noImpl
        set(value) = noImpl
    open var vAlign: String
        get() = noImpl
        set(value) = noImpl
    open var bgColor: String
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class HTMLFormElement : HTMLElement() {
    open var acceptCharset: String
        get() = noImpl
        set(value) = noImpl
    open var action: String
        get() = noImpl
        set(value) = noImpl
    open var autocomplete: String
        get() = noImpl
        set(value) = noImpl
    open var enctype: String
        get() = noImpl
        set(value) = noImpl
    open var encoding: String
        get() = noImpl
        set(value) = noImpl
    open var method: String
        get() = noImpl
        set(value) = noImpl
    open var name: String
        get() = noImpl
        set(value) = noImpl
    open var noValidate: Boolean
        get() = noImpl
        set(value) = noImpl
    open var target: String
        get() = noImpl
        set(value) = noImpl
    open val elements: HTMLFormControlsCollection
        get() = noImpl
    open val length: Int
        get() = noImpl
    @nativeGetter
    operator fun get(index: Int): Element? = noImpl
    @nativeGetter
    operator fun get(name: String): UnionElementOrRadioNodeList? = noImpl
    fun submit(): Unit = noImpl
    fun reset(): Unit = noImpl
    fun checkValidity(): Boolean = noImpl
    fun reportValidity(): Boolean = noImpl
}

@native public abstract class HTMLLabelElement : HTMLElement() {
    open val form: HTMLFormElement?
        get() = noImpl
    open var htmlFor: String
        get() = noImpl
        set(value) = noImpl
    open val control: HTMLElement?
        get() = noImpl
}

@native public abstract class HTMLInputElement : HTMLElement() {
    open var accept: String
        get() = noImpl
        set(value) = noImpl
    open var alt: String
        get() = noImpl
        set(value) = noImpl
    open var autocomplete: String
        get() = noImpl
        set(value) = noImpl
    open var autofocus: Boolean
        get() = noImpl
        set(value) = noImpl
    open var defaultChecked: Boolean
        get() = noImpl
        set(value) = noImpl
    open var checked: Boolean
        get() = noImpl
        set(value) = noImpl
    open var dirName: String
        get() = noImpl
        set(value) = noImpl
    open var disabled: Boolean
        get() = noImpl
        set(value) = noImpl
    open val form: HTMLFormElement?
        get() = noImpl
    open val files: FileList?
        get() = noImpl
    open var formAction: String
        get() = noImpl
        set(value) = noImpl
    open var formEnctype: String
        get() = noImpl
        set(value) = noImpl
    open var formMethod: String
        get() = noImpl
        set(value) = noImpl
    open var formNoValidate: Boolean
        get() = noImpl
        set(value) = noImpl
    open var formTarget: String
        get() = noImpl
        set(value) = noImpl
    open var height: Int
        get() = noImpl
        set(value) = noImpl
    open var indeterminate: Boolean
        get() = noImpl
        set(value) = noImpl
    open var inputMode: String
        get() = noImpl
        set(value) = noImpl
    open val list: HTMLElement?
        get() = noImpl
    open var max: String
        get() = noImpl
        set(value) = noImpl
    open var maxLength: Int
        get() = noImpl
        set(value) = noImpl
    open var min: String
        get() = noImpl
        set(value) = noImpl
    open var minLength: Int
        get() = noImpl
        set(value) = noImpl
    open var multiple: Boolean
        get() = noImpl
        set(value) = noImpl
    open var name: String
        get() = noImpl
        set(value) = noImpl
    open var pattern: String
        get() = noImpl
        set(value) = noImpl
    open var placeholder: String
        get() = noImpl
        set(value) = noImpl
    open var readOnly: Boolean
        get() = noImpl
        set(value) = noImpl
    open var required: Boolean
        get() = noImpl
        set(value) = noImpl
    open var size: Int
        get() = noImpl
        set(value) = noImpl
    open var src: String
        get() = noImpl
        set(value) = noImpl
    open var step: String
        get() = noImpl
        set(value) = noImpl
    open var type: String
        get() = noImpl
        set(value) = noImpl
    open var defaultValue: String
        get() = noImpl
        set(value) = noImpl
    open var value: String
        get() = noImpl
        set(value) = noImpl
    open var valueAsDate: dynamic
        get() = noImpl
        set(value) = noImpl
    open var valueAsNumber: Double
        get() = noImpl
        set(value) = noImpl
    open var width: Int
        get() = noImpl
        set(value) = noImpl
    open val willValidate: Boolean
        get() = noImpl
    open val validity: ValidityState
        get() = noImpl
    open val validationMessage: String
        get() = noImpl
    open val labels: NodeList
        get() = noImpl
    open var selectionStart: Int?
        get() = noImpl
        set(value) = noImpl
    open var selectionEnd: Int?
        get() = noImpl
        set(value) = noImpl
    open var selectionDirection: String?
        get() = noImpl
        set(value) = noImpl
    open var align: String
        get() = noImpl
        set(value) = noImpl
    open var useMap: String
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

@native public abstract class HTMLButtonElement : HTMLElement() {
    open var autofocus: Boolean
        get() = noImpl
        set(value) = noImpl
    open var disabled: Boolean
        get() = noImpl
        set(value) = noImpl
    open val form: HTMLFormElement?
        get() = noImpl
    open var formAction: String
        get() = noImpl
        set(value) = noImpl
    open var formEnctype: String
        get() = noImpl
        set(value) = noImpl
    open var formMethod: String
        get() = noImpl
        set(value) = noImpl
    open var formNoValidate: Boolean
        get() = noImpl
        set(value) = noImpl
    open var formTarget: String
        get() = noImpl
        set(value) = noImpl
    open var name: String
        get() = noImpl
        set(value) = noImpl
    open var type: String
        get() = noImpl
        set(value) = noImpl
    open var value: String
        get() = noImpl
        set(value) = noImpl
    open var menu: HTMLMenuElement?
        get() = noImpl
        set(value) = noImpl
    open val willValidate: Boolean
        get() = noImpl
    open val validity: ValidityState
        get() = noImpl
    open val validationMessage: String
        get() = noImpl
    open val labels: NodeList
        get() = noImpl
    fun checkValidity(): Boolean = noImpl
    fun reportValidity(): Boolean = noImpl
    fun setCustomValidity(error: String): Unit = noImpl
}

@native public abstract class HTMLSelectElement : HTMLElement() {
    open var autocomplete: String
        get() = noImpl
        set(value) = noImpl
    open var autofocus: Boolean
        get() = noImpl
        set(value) = noImpl
    open var disabled: Boolean
        get() = noImpl
        set(value) = noImpl
    open val form: HTMLFormElement?
        get() = noImpl
    open var multiple: Boolean
        get() = noImpl
        set(value) = noImpl
    open var name: String
        get() = noImpl
        set(value) = noImpl
    open var required: Boolean
        get() = noImpl
        set(value) = noImpl
    open var size: Int
        get() = noImpl
        set(value) = noImpl
    open val type: String
        get() = noImpl
    open val options: HTMLOptionsCollection
        get() = noImpl
    open var length: Int
        get() = noImpl
        set(value) = noImpl
    open val selectedOptions: HTMLCollection
        get() = noImpl
    open var selectedIndex: Int
        get() = noImpl
        set(value) = noImpl
    open var value: String
        get() = noImpl
        set(value) = noImpl
    open val willValidate: Boolean
        get() = noImpl
    open val validity: ValidityState
        get() = noImpl
    open val validationMessage: String
        get() = noImpl
    open val labels: NodeList
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

@native public abstract class HTMLDataListElement : HTMLElement() {
    open val options: HTMLCollection
        get() = noImpl
}

@native public abstract class HTMLOptGroupElement : HTMLElement(), UnionHTMLOptGroupElementOrHTMLOptionElement {
    open var disabled: Boolean
        get() = noImpl
        set(value) = noImpl
    open var label: String
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class HTMLOptionElement : HTMLElement(), UnionHTMLOptGroupElementOrHTMLOptionElement {
    open var disabled: Boolean
        get() = noImpl
        set(value) = noImpl
    open val form: HTMLFormElement?
        get() = noImpl
    open var label: String
        get() = noImpl
        set(value) = noImpl
    open var defaultSelected: Boolean
        get() = noImpl
        set(value) = noImpl
    open var selected: Boolean
        get() = noImpl
        set(value) = noImpl
    open var value: String
        get() = noImpl
        set(value) = noImpl
    open var text: String
        get() = noImpl
        set(value) = noImpl
    open val index: Int
        get() = noImpl
}

@native public abstract class HTMLTextAreaElement : HTMLElement() {
    open var autocomplete: String
        get() = noImpl
        set(value) = noImpl
    open var autofocus: Boolean
        get() = noImpl
        set(value) = noImpl
    open var cols: Int
        get() = noImpl
        set(value) = noImpl
    open var dirName: String
        get() = noImpl
        set(value) = noImpl
    open var disabled: Boolean
        get() = noImpl
        set(value) = noImpl
    open val form: HTMLFormElement?
        get() = noImpl
    open var inputMode: String
        get() = noImpl
        set(value) = noImpl
    open var maxLength: Int
        get() = noImpl
        set(value) = noImpl
    open var minLength: Int
        get() = noImpl
        set(value) = noImpl
    open var name: String
        get() = noImpl
        set(value) = noImpl
    open var placeholder: String
        get() = noImpl
        set(value) = noImpl
    open var readOnly: Boolean
        get() = noImpl
        set(value) = noImpl
    open var required: Boolean
        get() = noImpl
        set(value) = noImpl
    open var rows: Int
        get() = noImpl
        set(value) = noImpl
    open var wrap: String
        get() = noImpl
        set(value) = noImpl
    open val type: String
        get() = noImpl
    open var defaultValue: String
        get() = noImpl
        set(value) = noImpl
    open var value: String
        get() = noImpl
        set(value) = noImpl
    open val textLength: Int
        get() = noImpl
    open val willValidate: Boolean
        get() = noImpl
    open val validity: ValidityState
        get() = noImpl
    open val validationMessage: String
        get() = noImpl
    open val labels: NodeList
        get() = noImpl
    open var selectionStart: Int?
        get() = noImpl
        set(value) = noImpl
    open var selectionEnd: Int?
        get() = noImpl
        set(value) = noImpl
    open var selectionDirection: String?
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

@native public abstract class HTMLKeygenElement : HTMLElement() {
    open var autofocus: Boolean
        get() = noImpl
        set(value) = noImpl
    open var challenge: String
        get() = noImpl
        set(value) = noImpl
    open var disabled: Boolean
        get() = noImpl
        set(value) = noImpl
    open val form: HTMLFormElement?
        get() = noImpl
    open var keytype: String
        get() = noImpl
        set(value) = noImpl
    open var name: String
        get() = noImpl
        set(value) = noImpl
    open val type: String
        get() = noImpl
    open val willValidate: Boolean
        get() = noImpl
    open val validity: ValidityState
        get() = noImpl
    open val validationMessage: String
        get() = noImpl
    open val labels: NodeList
        get() = noImpl
    fun checkValidity(): Boolean = noImpl
    fun reportValidity(): Boolean = noImpl
    fun setCustomValidity(error: String): Unit = noImpl
}

@native public abstract class HTMLOutputElement : HTMLElement() {
    open val htmlFor: DOMTokenList
        get() = noImpl
    open val form: HTMLFormElement?
        get() = noImpl
    open var name: String
        get() = noImpl
        set(value) = noImpl
    open val type: String
        get() = noImpl
    open var defaultValue: String
        get() = noImpl
        set(value) = noImpl
    open var value: String
        get() = noImpl
        set(value) = noImpl
    open val willValidate: Boolean
        get() = noImpl
    open val validity: ValidityState
        get() = noImpl
    open val validationMessage: String
        get() = noImpl
    open val labels: NodeList
        get() = noImpl
    fun checkValidity(): Boolean = noImpl
    fun reportValidity(): Boolean = noImpl
    fun setCustomValidity(error: String): Unit = noImpl
}

@native public abstract class HTMLProgressElement : HTMLElement() {
    open var value: Double
        get() = noImpl
        set(value) = noImpl
    open var max: Double
        get() = noImpl
        set(value) = noImpl
    open val position: Double
        get() = noImpl
    open val labels: NodeList
        get() = noImpl
}

@native public abstract class HTMLMeterElement : HTMLElement() {
    open var value: Double
        get() = noImpl
        set(value) = noImpl
    open var min: Double
        get() = noImpl
        set(value) = noImpl
    open var max: Double
        get() = noImpl
        set(value) = noImpl
    open var low: Double
        get() = noImpl
        set(value) = noImpl
    open var high: Double
        get() = noImpl
        set(value) = noImpl
    open var optimum: Double
        get() = noImpl
        set(value) = noImpl
    open val labels: NodeList
        get() = noImpl
}

@native public abstract class HTMLFieldSetElement : HTMLElement() {
    open var disabled: Boolean
        get() = noImpl
        set(value) = noImpl
    open val form: HTMLFormElement?
        get() = noImpl
    open var name: String
        get() = noImpl
        set(value) = noImpl
    open val type: String
        get() = noImpl
    open val elements: HTMLCollection
        get() = noImpl
    open val willValidate: Boolean
        get() = noImpl
    open val validity: ValidityState
        get() = noImpl
    open val validationMessage: String
        get() = noImpl
    fun checkValidity(): Boolean = noImpl
    fun reportValidity(): Boolean = noImpl
    fun setCustomValidity(error: String): Unit = noImpl
}

@native public abstract class HTMLLegendElement : HTMLElement() {
    open val form: HTMLFormElement?
        get() = noImpl
    open var align: String
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class ValidityState {
    open val valueMissing: Boolean
        get() = noImpl
    open val typeMismatch: Boolean
        get() = noImpl
    open val patternMismatch: Boolean
        get() = noImpl
    open val tooLong: Boolean
        get() = noImpl
    open val tooShort: Boolean
        get() = noImpl
    open val rangeUnderflow: Boolean
        get() = noImpl
    open val rangeOverflow: Boolean
        get() = noImpl
    open val stepMismatch: Boolean
        get() = noImpl
    open val badInput: Boolean
        get() = noImpl
    open val customError: Boolean
        get() = noImpl
    open val valid: Boolean
        get() = noImpl
}

@native public abstract class HTMLDetailsElement : HTMLElement() {
    open var open: Boolean
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class HTMLMenuElement : HTMLElement() {
    open var type: String
        get() = noImpl
        set(value) = noImpl
    open var label: String
        get() = noImpl
        set(value) = noImpl
    open var compact: Boolean
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class HTMLMenuItemElement : HTMLElement() {
    open var type: String
        get() = noImpl
        set(value) = noImpl
    open var label: String
        get() = noImpl
        set(value) = noImpl
    open var icon: String
        get() = noImpl
        set(value) = noImpl
    open var disabled: Boolean
        get() = noImpl
        set(value) = noImpl
    open var checked: Boolean
        get() = noImpl
        set(value) = noImpl
    open var radiogroup: String
        get() = noImpl
        set(value) = noImpl
    open var default: Boolean
        get() = noImpl
        set(value) = noImpl
}

@native public open class RelatedEvent(type: String, eventInitDict: RelatedEventInit = noImpl) : Event(type, eventInitDict) {
    open val relatedTarget: EventTarget?
        get() = noImpl
}

@native public interface RelatedEventInit : EventInit {
    var relatedTarget: EventTarget? /* = null */
}

@Suppress("NOTHING_TO_INLINE")
public inline fun RelatedEventInit(relatedTarget: EventTarget? = null, bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): RelatedEventInit {
    val o = js("({})")

    o["relatedTarget"] = relatedTarget
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable
    o["composed"] = composed

    return o
}

@native public abstract class HTMLDialogElement : HTMLElement() {
    open var open: Boolean
        get() = noImpl
        set(value) = noImpl
    open var returnValue: String
        get() = noImpl
        set(value) = noImpl
    fun show(anchor: UnionElementOrMouseEvent = noImpl): Unit = noImpl
    fun showModal(anchor: UnionElementOrMouseEvent = noImpl): Unit = noImpl
    fun close(returnValue: String = noImpl): Unit = noImpl
}

@native public abstract class HTMLScriptElement : HTMLElement(), HTMLOrSVGScriptElement {
    open var src: String
        get() = noImpl
        set(value) = noImpl
    open var type: String
        get() = noImpl
        set(value) = noImpl
    open var charset: String
        get() = noImpl
        set(value) = noImpl
    open var async: Boolean
        get() = noImpl
        set(value) = noImpl
    open var defer: Boolean
        get() = noImpl
        set(value) = noImpl
    open var crossOrigin: String?
        get() = noImpl
        set(value) = noImpl
    open var text: String
        get() = noImpl
        set(value) = noImpl
    open var nonce: String
        get() = noImpl
        set(value) = noImpl
    open var event: String
        get() = noImpl
        set(value) = noImpl
    open var htmlFor: String
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class HTMLTemplateElement : HTMLElement() {
    open val content: DocumentFragment
        get() = noImpl
}

@native public abstract class HTMLSlotElement : HTMLElement() {
    open var name: String
        get() = noImpl
        set(value) = noImpl
    fun assignedNodes(options: AssignedNodesOptions = noImpl): Array<Node> = noImpl
}

@native public interface AssignedNodesOptions {
    var flatten: Boolean? /* = false */
}

@Suppress("NOTHING_TO_INLINE")
public inline fun AssignedNodesOptions(flatten: Boolean? = false): AssignedNodesOptions {
    val o = js("({})")

    o["flatten"] = flatten

    return o
}

@native public abstract class HTMLCanvasElement : HTMLElement(), TexImageSource {
    open var width: Int
        get() = noImpl
        set(value) = noImpl
    open var height: Int
        get() = noImpl
        set(value) = noImpl
    fun getContext(contextId: String, vararg arguments: Any?): RenderingContext? = noImpl
    fun toDataURL(type: String = noImpl, quality: Any? = noImpl): String = noImpl
    fun toBlob(_callback: (Blob?) -> Unit, type: String = noImpl, quality: Any? = noImpl): Unit = noImpl
}

@native public interface CanvasRenderingContext2DSettings {
    var alpha: Boolean? /* = true */
}

@Suppress("NOTHING_TO_INLINE")
public inline fun CanvasRenderingContext2DSettings(alpha: Boolean? = true): CanvasRenderingContext2DSettings {
    val o = js("({})")

    o["alpha"] = alpha

    return o
}

@native public abstract class CanvasRenderingContext2D : CanvasState, CanvasTransform, CanvasCompositing, CanvasImageSmoothing, CanvasFillStrokeStyles, CanvasShadowStyles, CanvasFilters, CanvasRect, CanvasDrawPath, CanvasUserInterface, CanvasText, CanvasDrawImage, CanvasHitRegion, CanvasImageData, CanvasPathDrawingStyles, CanvasTextDrawingStyles, CanvasPath, RenderingContext {
    open val canvas: HTMLCanvasElement
        get() = noImpl
}

@native public interface CanvasState {
    fun save(): Unit = noImpl
    fun restore(): Unit = noImpl
}

@native public interface CanvasTransform {
    fun scale(x: Double, y: Double): Unit = noImpl
    fun rotate(angle: Double): Unit = noImpl
    fun translate(x: Double, y: Double): Unit = noImpl
    fun transform(a: Double, b: Double, c: Double, d: Double, e: Double, f: Double): Unit = noImpl
    fun getTransform(): DOMMatrix = noImpl
    fun setTransform(a: Double, b: Double, c: Double, d: Double, e: Double, f: Double): Unit = noImpl
    fun setTransform(transform: dynamic = noImpl): Unit = noImpl
    fun resetTransform(): Unit = noImpl
}

@native public interface CanvasCompositing {
    var globalAlpha: Double
        get() = noImpl
        set(value) = noImpl
    var globalCompositeOperation: String
        get() = noImpl
        set(value) = noImpl
}

@native public interface CanvasImageSmoothing {
    var imageSmoothingEnabled: Boolean
        get() = noImpl
        set(value) = noImpl
    var imageSmoothingQuality: String
        get() = noImpl
        set(value) = noImpl
}

@native public interface CanvasFillStrokeStyles {
    var strokeStyle: dynamic
        get() = noImpl
        set(value) = noImpl
    var fillStyle: dynamic
        get() = noImpl
        set(value) = noImpl
    fun createLinearGradient(x0: Double, y0: Double, x1: Double, y1: Double): CanvasGradient = noImpl
    fun createRadialGradient(x0: Double, y0: Double, r0: Double, x1: Double, y1: Double, r1: Double): CanvasGradient = noImpl
    fun createPattern(image: dynamic, repetition: String): CanvasPattern? = noImpl
}

@native public interface CanvasShadowStyles {
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
}

@native public interface CanvasFilters {
    var filter: String
        get() = noImpl
        set(value) = noImpl
}

@native public interface CanvasRect {
    fun clearRect(x: Double, y: Double, w: Double, h: Double): Unit = noImpl
    fun fillRect(x: Double, y: Double, w: Double, h: Double): Unit = noImpl
    fun strokeRect(x: Double, y: Double, w: Double, h: Double): Unit = noImpl
}

@native public interface CanvasDrawPath {
    fun beginPath(): Unit = noImpl
    fun fill(fillRule: String = "nonzero"): Unit = noImpl
    fun fill(path: Path2D, fillRule: String = "nonzero"): Unit = noImpl
    fun stroke(): Unit = noImpl
    fun stroke(path: Path2D): Unit = noImpl
    fun clip(fillRule: String = "nonzero"): Unit = noImpl
    fun clip(path: Path2D, fillRule: String = "nonzero"): Unit = noImpl
    fun resetClip(): Unit = noImpl
    fun isPointInPath(x: Double, y: Double, fillRule: String = "nonzero"): Boolean = noImpl
    fun isPointInPath(path: Path2D, x: Double, y: Double, fillRule: String = "nonzero"): Boolean = noImpl
    fun isPointInStroke(x: Double, y: Double): Boolean = noImpl
    fun isPointInStroke(path: Path2D, x: Double, y: Double): Boolean = noImpl
}

@native public interface CanvasUserInterface {
    fun drawFocusIfNeeded(element: Element): Unit = noImpl
    fun drawFocusIfNeeded(path: Path2D, element: Element): Unit = noImpl
    fun scrollPathIntoView(): Unit = noImpl
    fun scrollPathIntoView(path: Path2D): Unit = noImpl
}

@native public interface CanvasText {
    fun fillText(text: String, x: Double, y: Double, maxWidth: Double = noImpl): Unit = noImpl
    fun strokeText(text: String, x: Double, y: Double, maxWidth: Double = noImpl): Unit = noImpl
    fun measureText(text: String): TextMetrics = noImpl
}

@native public interface CanvasDrawImage {
    fun drawImage(image: dynamic, dx: Double, dy: Double): Unit = noImpl
    fun drawImage(image: dynamic, dx: Double, dy: Double, dw: Double, dh: Double): Unit = noImpl
    fun drawImage(image: dynamic, sx: Double, sy: Double, sw: Double, sh: Double, dx: Double, dy: Double, dw: Double, dh: Double): Unit = noImpl
}

@native public interface CanvasHitRegion {
    fun addHitRegion(options: HitRegionOptions = noImpl): Unit = noImpl
    fun removeHitRegion(id: String): Unit = noImpl
    fun clearHitRegions(): Unit = noImpl
}

@native public interface CanvasImageData {
    fun createImageData(sw: Double, sh: Double): ImageData = noImpl
    fun createImageData(imagedata: ImageData): ImageData = noImpl
    fun getImageData(sx: Double, sy: Double, sw: Double, sh: Double): ImageData = noImpl
    fun putImageData(imagedata: ImageData, dx: Double, dy: Double): Unit = noImpl
    fun putImageData(imagedata: ImageData, dx: Double, dy: Double, dirtyX: Double, dirtyY: Double, dirtyWidth: Double, dirtyHeight: Double): Unit = noImpl
}

@native public interface CanvasPathDrawingStyles {
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
    fun setLineDash(segments: Array<Double>): Unit = noImpl
    fun getLineDash(): Array<Double> = noImpl
}

@native public interface CanvasTextDrawingStyles {
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
}

@native public interface CanvasPath {
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

@native public abstract class CanvasGradient {
    fun addColorStop(offset: Double, color: String): Unit = noImpl
}

@native public abstract class CanvasPattern {
    fun setTransform(transform: dynamic = noImpl): Unit = noImpl
}

@native public abstract class TextMetrics {
    open val width: Double
        get() = noImpl
    open val actualBoundingBoxLeft: Double
        get() = noImpl
    open val actualBoundingBoxRight: Double
        get() = noImpl
    open val fontBoundingBoxAscent: Double
        get() = noImpl
    open val fontBoundingBoxDescent: Double
        get() = noImpl
    open val actualBoundingBoxAscent: Double
        get() = noImpl
    open val actualBoundingBoxDescent: Double
        get() = noImpl
    open val emHeightAscent: Double
        get() = noImpl
    open val emHeightDescent: Double
        get() = noImpl
    open val hangingBaseline: Double
        get() = noImpl
    open val alphabeticBaseline: Double
        get() = noImpl
    open val ideographicBaseline: Double
        get() = noImpl
}

@native public interface HitRegionOptions {
    var path: Path2D? /* = null */
    var fillRule: String? /* = "nonzero" */
    var id: String? /* = "" */
    var parentID: String? /* = null */
    var cursor: String? /* = "inherit" */
    var control: Element? /* = null */
    var label: String? /* = null */
    var role: String? /* = null */
}

@Suppress("NOTHING_TO_INLINE")
public inline fun HitRegionOptions(path: Path2D? = null, fillRule: String? = "nonzero", id: String? = "", parentID: String? = null, cursor: String? = "inherit", control: Element? = null, label: String? = null, role: String? = null): HitRegionOptions {
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

@native public open class ImageData : TexImageSource {
    constructor(sw: Int, sh: Int)
    constructor(data: Uint8ClampedArray, sw: Int, sh: Int = noImpl)
    open val width: Int
        get() = noImpl
    open val height: Int
        get() = noImpl
    open val data: Uint8ClampedArray
        get() = noImpl
}

@native public open class Path2D() : CanvasPath {
    constructor(path: Path2D) : this()
    constructor(paths: Array<Path2D>, fillRule: String = "nonzero") : this()
    constructor(d: String) : this()
    fun addPath(path: Path2D, transform: dynamic = noImpl): Unit = noImpl
}

@native public abstract class Touch {
    open val region: String?
        get() = noImpl
}

@native public abstract class ImageBitmapRenderingContext {
    open val canvas: HTMLCanvasElement
        get() = noImpl
    fun transferFromImageBitmap(bitmap: ImageBitmap?): Unit = noImpl
}

@native public interface ImageBitmapRenderingContextSettings {
    var alpha: Boolean? /* = true */
}

@Suppress("NOTHING_TO_INLINE")
public inline fun ImageBitmapRenderingContextSettings(alpha: Boolean? = true): ImageBitmapRenderingContextSettings {
    val o = js("({})")

    o["alpha"] = alpha

    return o
}

@native public abstract class CustomElementRegistry {
    fun define(name: String, constructor: () -> dynamic, options: ElementDefinitionOptions = noImpl): Unit = noImpl
    fun get(name: String): Any? = noImpl
    fun whenDefined(name: String): dynamic = noImpl
}

@native public interface ElementDefinitionOptions {
    var extends: String?
}

@Suppress("NOTHING_TO_INLINE")
public inline fun ElementDefinitionOptions(extends: String?): ElementDefinitionOptions {
    val o = js("({})")

    o["extends"] = extends

    return o
}

@native public interface ElementContentEditable {
    var contentEditable: String
        get() = noImpl
        set(value) = noImpl
    val isContentEditable: Boolean
        get() = noImpl
}

@native public abstract class DataTransfer {
    open var dropEffect: String
        get() = noImpl
        set(value) = noImpl
    open var effectAllowed: String
        get() = noImpl
        set(value) = noImpl
    open val items: DataTransferItemList
        get() = noImpl
    open val types: dynamic
        get() = noImpl
    open val files: FileList
        get() = noImpl
    fun setDragImage(image: Element, x: Int, y: Int): Unit = noImpl
    fun getData(format: String): String = noImpl
    fun setData(format: String, data: String): Unit = noImpl
    fun clearData(format: String = noImpl): Unit = noImpl
}

@native public abstract class DataTransferItemList {
    open val length: Int
        get() = noImpl
    @nativeGetter
    operator fun get(index: Int): DataTransferItem? = noImpl
    fun add(data: String, type: String): DataTransferItem? = noImpl
    fun add(data: File): DataTransferItem? = noImpl
    fun remove(index: Int): Unit = noImpl
    fun clear(): Unit = noImpl
}

@native public abstract class DataTransferItem {
    open val kind: String
        get() = noImpl
    open val type: String
        get() = noImpl
    fun getAsString(_callback: ((String) -> Unit)?): Unit = noImpl
    fun getAsFile(): File? = noImpl
}

@native public open class DragEvent(type: String, eventInitDict: DragEventInit = noImpl) : MouseEvent(type, eventInitDict) {
    open val dataTransfer: DataTransfer?
        get() = noImpl
}

@native public interface DragEventInit : MouseEventInit {
    var dataTransfer: DataTransfer? /* = null */
}

@Suppress("NOTHING_TO_INLINE")
public inline fun DragEventInit(dataTransfer: DataTransfer? = null, screenX: Int? = 0, screenY: Int? = 0, clientX: Int? = 0, clientY: Int? = 0, button: Short? = 0, buttons: Short? = 0, relatedTarget: EventTarget? = null, ctrlKey: Boolean? = false, shiftKey: Boolean? = false, altKey: Boolean? = false, metaKey: Boolean? = false, modifierAltGraph: Boolean? = false, modifierCapsLock: Boolean? = false, modifierFn: Boolean? = false, modifierFnLock: Boolean? = false, modifierHyper: Boolean? = false, modifierNumLock: Boolean? = false, modifierScrollLock: Boolean? = false, modifierSuper: Boolean? = false, modifierSymbol: Boolean? = false, modifierSymbolLock: Boolean? = false, view: Window? = null, detail: Int? = 0, bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): DragEventInit {
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
    o["modifierScrollLock"] = modifierScrollLock
    o["modifierSuper"] = modifierSuper
    o["modifierSymbol"] = modifierSymbol
    o["modifierSymbolLock"] = modifierSymbolLock
    o["view"] = view
    o["detail"] = detail
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable
    o["composed"] = composed

    return o
}

@native public abstract class BarProp {
    open val visible: Boolean
        get() = noImpl
}

@native public abstract class History {
    open val length: Int
        get() = noImpl
    open var scrollRestoration: String
        get() = noImpl
        set(value) = noImpl
    open val state: Any?
        get() = noImpl
    fun go(delta: Int = 0): Unit = noImpl
    fun back(): Unit = noImpl
    fun forward(): Unit = noImpl
    fun pushState(data: Any?, title: String, url: String? = null): Unit = noImpl
    fun replaceState(data: Any?, title: String, url: String? = null): Unit = noImpl
}

@native public abstract class Location {
    open var href: String
        get() = noImpl
        set(value) = noImpl
    open val origin: String
        get() = noImpl
    open var protocol: String
        get() = noImpl
        set(value) = noImpl
    open var host: String
        get() = noImpl
        set(value) = noImpl
    open var hostname: String
        get() = noImpl
        set(value) = noImpl
    open var port: String
        get() = noImpl
        set(value) = noImpl
    open var pathname: String
        get() = noImpl
        set(value) = noImpl
    open var search: String
        get() = noImpl
        set(value) = noImpl
    open var hash: String
        get() = noImpl
        set(value) = noImpl
    open val ancestorOrigins: dynamic
        get() = noImpl
    fun assign(url: String): Unit = noImpl
    fun replace(url: String): Unit = noImpl
    fun reload(): Unit = noImpl
}

@native public open class PopStateEvent(type: String, eventInitDict: PopStateEventInit = noImpl) : Event(type, eventInitDict) {
    open val state: Any?
        get() = noImpl
}

@native public interface PopStateEventInit : EventInit {
    var state: Any? /* = null */
}

@Suppress("NOTHING_TO_INLINE")
public inline fun PopStateEventInit(state: Any? = null, bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): PopStateEventInit {
    val o = js("({})")

    o["state"] = state
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable
    o["composed"] = composed

    return o
}

@native public open class HashChangeEvent(type: String, eventInitDict: HashChangeEventInit = noImpl) : Event(type, eventInitDict) {
    open val oldURL: String
        get() = noImpl
    open val newURL: String
        get() = noImpl
}

@native public interface HashChangeEventInit : EventInit {
    var oldURL: String? /* = "" */
    var newURL: String? /* = "" */
}

@Suppress("NOTHING_TO_INLINE")
public inline fun HashChangeEventInit(oldURL: String? = "", newURL: String? = "", bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): HashChangeEventInit {
    val o = js("({})")

    o["oldURL"] = oldURL
    o["newURL"] = newURL
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable
    o["composed"] = composed

    return o
}

@native public open class PageTransitionEvent(type: String, eventInitDict: PageTransitionEventInit = noImpl) : Event(type, eventInitDict) {
    open val persisted: Boolean
        get() = noImpl
}

@native public interface PageTransitionEventInit : EventInit {
    var persisted: Boolean? /* = false */
}

@Suppress("NOTHING_TO_INLINE")
public inline fun PageTransitionEventInit(persisted: Boolean? = false, bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): PageTransitionEventInit {
    val o = js("({})")

    o["persisted"] = persisted
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable
    o["composed"] = composed

    return o
}

@native public open class BeforeUnloadEvent : Event(noImpl, noImpl) {
    var returnValue: String
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class ApplicationCache : EventTarget() {
    open val status: Short
        get() = noImpl
    open var onchecking: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    open var onerror: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    open var onnoupdate: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    open var ondownloading: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    open var onprogress: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    open var onupdateready: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    open var oncached: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    open var onobsolete: ((Event) -> dynamic)?
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

@native public interface NavigatorOnLine {
    val onLine: Boolean
        get() = noImpl
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
    var message: String? /* = "" */
    var filename: String? /* = "" */
    var lineno: Int? /* = 0 */
    var colno: Int? /* = 0 */
    var error: Any? /* = null */
}

@Suppress("NOTHING_TO_INLINE")
public inline fun ErrorEventInit(message: String? = "", filename: String? = "", lineno: Int? = 0, colno: Int? = 0, error: Any? = null, bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): ErrorEventInit {
    val o = js("({})")

    o["message"] = message
    o["filename"] = filename
    o["lineno"] = lineno
    o["colno"] = colno
    o["error"] = error
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable
    o["composed"] = composed

    return o
}

@native public open class PromiseRejectionEvent(type: String, eventInitDict: PromiseRejectionEventInit) : Event(type, eventInitDict) {
    open val promise: dynamic
        get() = noImpl
    open val reason: Any?
        get() = noImpl
}

@native public interface PromiseRejectionEventInit : EventInit {
    var promise: dynamic
    var reason: Any?
}

@Suppress("NOTHING_TO_INLINE")
public inline fun PromiseRejectionEventInit(promise: dynamic, reason: Any?, bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): PromiseRejectionEventInit {
    val o = js("({})")

    o["promise"] = promise
    o["reason"] = reason
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable
    o["composed"] = composed

    return o
}

@native public interface GlobalEventHandlers {
    var onabort: ((Event) -> dynamic)?
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
    var onloadend: ((Event) -> dynamic)?
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
    var onwheel: ((Event) -> dynamic)?
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
}

@native public interface WindowEventHandlers {
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
    var onrejectionhandled: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onstorage: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onunhandledrejection: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onunload: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
}

@native public interface DocumentAndElementEventHandlers {
    var oncopy: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var oncut: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onpaste: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
}

@native public interface WindowOrWorkerGlobalScope {
    val caches: CacheStorage
        get() = noImpl
    val origin: String
        get() = noImpl
    fun fetch(input: dynamic, init: RequestInit = noImpl): dynamic = noImpl
    fun btoa(data: String): String = noImpl
    fun atob(data: String): String = noImpl
    fun setTimeout(handler: dynamic, timeout: Int = 0, vararg arguments: Any?): Int = noImpl
    fun clearTimeout(handle: Int = 0): Unit = noImpl
    fun setInterval(handler: dynamic, timeout: Int = 0, vararg arguments: Any?): Int = noImpl
    fun clearInterval(handle: Int = 0): Unit = noImpl
    fun createImageBitmap(image: dynamic, options: ImageBitmapOptions = noImpl): dynamic = noImpl
    fun createImageBitmap(image: dynamic, sx: Int, sy: Int, sw: Int, sh: Int, options: ImageBitmapOptions = noImpl): dynamic = noImpl
}

@native public abstract class Navigator : NavigatorID, NavigatorLanguage, NavigatorOnLine, NavigatorContentUtils, NavigatorCookies, NavigatorPlugins, NavigatorConcurrentHardware {
    open val serviceWorker: ServiceWorkerContainer
        get() = noImpl
    fun vibrate(pattern: dynamic): Boolean = noImpl
}

@native public interface NavigatorID {
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
    val productSub: String
        get() = noImpl
    val userAgent: String
        get() = noImpl
    val vendor: String
        get() = noImpl
    val vendorSub: String
        get() = noImpl
    val oscpu: String
        get() = noImpl
    fun taintEnabled(): Boolean = noImpl
}

@native public interface NavigatorLanguage {
    val language: String
        get() = noImpl
    val languages: dynamic
        get() = noImpl
}

@native public interface NavigatorContentUtils {
    fun registerProtocolHandler(scheme: String, url: String, title: String): Unit = noImpl
    fun registerContentHandler(mimeType: String, url: String, title: String): Unit = noImpl
    fun isProtocolHandlerRegistered(scheme: String, url: String): String = noImpl
    fun isContentHandlerRegistered(mimeType: String, url: String): String = noImpl
    fun unregisterProtocolHandler(scheme: String, url: String): Unit = noImpl
    fun unregisterContentHandler(mimeType: String, url: String): Unit = noImpl
}

@native public interface NavigatorCookies {
    val cookieEnabled: Boolean
        get() = noImpl
}

@native public interface NavigatorPlugins {
    val plugins: PluginArray
        get() = noImpl
    val mimeTypes: MimeTypeArray
        get() = noImpl
    fun javaEnabled(): Boolean = noImpl
}

@native public abstract class PluginArray {
    open val length: Int
        get() = noImpl
    fun refresh(reload: Boolean = false): Unit = noImpl
    fun item(index: Int): Plugin? = noImpl
    @nativeGetter
    operator fun get(index: Int): Plugin? = noImpl
    fun namedItem(name: String): Plugin? = noImpl
    @nativeGetter
    operator fun get(name: String): Plugin? = noImpl
}

@native public abstract class MimeTypeArray {
    open val length: Int
        get() = noImpl
    fun item(index: Int): MimeType? = noImpl
    @nativeGetter
    operator fun get(index: Int): MimeType? = noImpl
    fun namedItem(name: String): MimeType? = noImpl
    @nativeGetter
    operator fun get(name: String): MimeType? = noImpl
}

@native public abstract class Plugin {
    open val name: String
        get() = noImpl
    open val description: String
        get() = noImpl
    open val filename: String
        get() = noImpl
    open val length: Int
        get() = noImpl
    fun item(index: Int): MimeType? = noImpl
    @nativeGetter
    operator fun get(index: Int): MimeType? = noImpl
    fun namedItem(name: String): MimeType? = noImpl
    @nativeGetter
    operator fun get(name: String): MimeType? = noImpl
}

@native public abstract class MimeType {
    open val type: String
        get() = noImpl
    open val description: String
        get() = noImpl
    open val suffixes: String
        get() = noImpl
    open val enabledPlugin: Plugin
        get() = noImpl
}

@native public abstract class ImageBitmap : TexImageSource {
    open val width: Int
        get() = noImpl
    open val height: Int
        get() = noImpl
    fun close(): Unit = noImpl
}

@native public interface ImageBitmapOptions {
    var imageOrientation: String? /* = "none" */
    var premultiplyAlpha: String? /* = "default" */
    var colorSpaceConversion: String? /* = "default" */
    var resizeWidth: Int?
    var resizeHeight: Int?
    var resizeQuality: String? /* = "low" */
}

@Suppress("NOTHING_TO_INLINE")
public inline fun ImageBitmapOptions(imageOrientation: String? = "none", premultiplyAlpha: String? = "default", colorSpaceConversion: String? = "default", resizeWidth: Int?, resizeHeight: Int?, resizeQuality: String? = "low"): ImageBitmapOptions {
    val o = js("({})")

    o["imageOrientation"] = imageOrientation
    o["premultiplyAlpha"] = premultiplyAlpha
    o["colorSpaceConversion"] = colorSpaceConversion
    o["resizeWidth"] = resizeWidth
    o["resizeHeight"] = resizeHeight
    o["resizeQuality"] = resizeQuality

    return o
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
    open val ports: dynamic
        get() = noImpl
    fun initMessageEvent(type: String, bubbles: Boolean, cancelable: Boolean, data: Any?, origin: String, lastEventId: String, source: UnionMessagePortOrWindow?, ports: Array<MessagePort>): Unit = noImpl
}

@native public interface MessageEventInit : EventInit {
    var data: Any? /* = null */
    var origin: String? /* = "" */
    var lastEventId: String? /* = "" */
    var source: UnionMessagePortOrWindow? /* = null */
    var ports: Array<MessagePort>? /* = arrayOf() */
}

@Suppress("NOTHING_TO_INLINE")
public inline fun MessageEventInit(data: Any? = null, origin: String? = "", lastEventId: String? = "", source: UnionMessagePortOrWindow? = null, ports: Array<MessagePort>? = arrayOf(), bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): MessageEventInit {
    val o = js("({})")

    o["data"] = data
    o["origin"] = origin
    o["lastEventId"] = lastEventId
    o["source"] = source
    o["ports"] = ports
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable
    o["composed"] = composed

    return o
}

@native public open class EventSource(url: String, eventSourceInitDict: EventSourceInit = noImpl) : EventTarget() {
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
    var withCredentials: Boolean? /* = false */
}

@Suppress("NOTHING_TO_INLINE")
public inline fun EventSourceInit(withCredentials: Boolean? = false): EventSourceInit {
    val o = js("({})")

    o["withCredentials"] = withCredentials

    return o
}

@native public open class WebSocket(url: String, protocols: dynamic = arrayOf<dynamic>()) : EventTarget() {
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
    var wasClean: Boolean? /* = false */
    var code: Short? /* = 0 */
    var reason: String? /* = "" */
}

@Suppress("NOTHING_TO_INLINE")
public inline fun CloseEventInit(wasClean: Boolean? = false, code: Short? = 0, reason: String? = "", bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): CloseEventInit {
    val o = js("({})")

    o["wasClean"] = wasClean
    o["code"] = code
    o["reason"] = reason
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable
    o["composed"] = composed

    return o
}

@native public open class MessageChannel {
    open val port1: MessagePort
        get() = noImpl
    open val port2: MessagePort
        get() = noImpl
}

@native public abstract class MessagePort : EventTarget(), UnionMessagePortOrWindow, UnionMessagePortOrServiceWorker, UnionClientOrMessagePortOrServiceWorker {
    open var onmessage: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    fun postMessage(message: Any?, transfer: Array<dynamic> = arrayOf()): Unit = noImpl
    fun start(): Unit = noImpl
    fun close(): Unit = noImpl
}

@native public open class BroadcastChannel(name: String) : EventTarget() {
    open val name: String
        get() = noImpl
    var onmessage: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    fun postMessage(message: Any?): Unit = noImpl
    fun close(): Unit = noImpl
}

@native public abstract class WorkerGlobalScope : EventTarget(), WindowOrWorkerGlobalScope, GlobalPerformance {
    open val self: WorkerGlobalScope
        get() = noImpl
    open val location: WorkerLocation
        get() = noImpl
    open val navigator: WorkerNavigator
        get() = noImpl
    open var onerror: ((dynamic, String, Int, Int, Any?) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    open var onlanguagechange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    open var onoffline: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    open var ononline: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    open var onrejectionhandled: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    open var onunhandledrejection: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    fun importScripts(vararg urls: String): Unit = noImpl
}

@native public abstract class DedicatedWorkerGlobalScope : WorkerGlobalScope() {
    open var onmessage: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    fun postMessage(message: Any?, transfer: Array<dynamic> = arrayOf()): Unit = noImpl
    fun close(): Unit = noImpl
}

@native public abstract class SharedWorkerGlobalScope : WorkerGlobalScope() {
    open val name: String
        get() = noImpl
    open val applicationCache: ApplicationCache
        get() = noImpl
    open var onconnect: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    fun close(): Unit = noImpl
}

@native public interface AbstractWorker {
    var onerror: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
}

@native public open class Worker(scriptURL: String, options: WorkerOptions = noImpl) : EventTarget(), AbstractWorker {
    var onmessage: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    fun terminate(): Unit = noImpl
    fun postMessage(message: Any?, transfer: Array<dynamic> = arrayOf()): Unit = noImpl
}

@native public interface WorkerOptions {
    var type: String? /* = "classic" */
    var credentials: String? /* = "omit" */
}

@Suppress("NOTHING_TO_INLINE")
public inline fun WorkerOptions(type: String? = "classic", credentials: String? = "omit"): WorkerOptions {
    val o = js("({})")

    o["type"] = type
    o["credentials"] = credentials

    return o
}

@native public open class SharedWorker(scriptURL: String, name: String = "", options: WorkerOptions = noImpl) : EventTarget(), AbstractWorker {
    open val port: MessagePort
        get() = noImpl
}

@native public interface NavigatorConcurrentHardware {
    val hardwareConcurrency: Int
        get() = noImpl
}

@native public abstract class WorkerNavigator : NavigatorID, NavigatorLanguage, NavigatorOnLine, NavigatorConcurrentHardware {
    open val serviceWorker: ServiceWorkerContainer
        get() = noImpl
}

@native public abstract class WorkerLocation {
    open var href: String
        get() = noImpl
        set(value) = noImpl
    open val origin: String
        get() = noImpl
    open val protocol: String
        get() = noImpl
    open val host: String
        get() = noImpl
    open val hostname: String
        get() = noImpl
    open val port: String
        get() = noImpl
    open val pathname: String
        get() = noImpl
    open val search: String
        get() = noImpl
    open val hash: String
        get() = noImpl
}

@native public abstract class Storage {
    open val length: Int
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

@native public interface WindowSessionStorage {
    val sessionStorage: Storage
        get() = noImpl
}

@native public interface WindowLocalStorage {
    val localStorage: Storage
        get() = noImpl
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
    var key: String? /* = null */
    var oldValue: String? /* = null */
    var newValue: String? /* = null */
    var url: String? /* = "" */
    var storageArea: Storage? /* = null */
}

@Suppress("NOTHING_TO_INLINE")
public inline fun StorageEventInit(key: String? = null, oldValue: String? = null, newValue: String? = null, url: String? = "", storageArea: Storage? = null, bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): StorageEventInit {
    val o = js("({})")

    o["key"] = key
    o["oldValue"] = oldValue
    o["newValue"] = newValue
    o["url"] = url
    o["storageArea"] = storageArea
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable
    o["composed"] = composed

    return o
}

@native public abstract class HTMLAppletElement : HTMLElement() {
    open var align: String
        get() = noImpl
        set(value) = noImpl
    open var alt: String
        get() = noImpl
        set(value) = noImpl
    open var archive: String
        get() = noImpl
        set(value) = noImpl
    open var code: String
        get() = noImpl
        set(value) = noImpl
    open var codeBase: String
        get() = noImpl
        set(value) = noImpl
    open var height: String
        get() = noImpl
        set(value) = noImpl
    open var hspace: Int
        get() = noImpl
        set(value) = noImpl
    open var name: String
        get() = noImpl
        set(value) = noImpl
    open var _object: String
        get() = noImpl
        set(value) = noImpl
    open var vspace: Int
        get() = noImpl
        set(value) = noImpl
    open var width: String
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class HTMLMarqueeElement : HTMLElement() {
    open var behavior: String
        get() = noImpl
        set(value) = noImpl
    open var bgColor: String
        get() = noImpl
        set(value) = noImpl
    open var direction: String
        get() = noImpl
        set(value) = noImpl
    open var height: String
        get() = noImpl
        set(value) = noImpl
    open var hspace: Int
        get() = noImpl
        set(value) = noImpl
    open var loop: Int
        get() = noImpl
        set(value) = noImpl
    open var scrollAmount: Int
        get() = noImpl
        set(value) = noImpl
    open var scrollDelay: Int
        get() = noImpl
        set(value) = noImpl
    open var trueSpeed: Boolean
        get() = noImpl
        set(value) = noImpl
    open var vspace: Int
        get() = noImpl
        set(value) = noImpl
    open var width: String
        get() = noImpl
        set(value) = noImpl
    open var onbounce: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    open var onfinish: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    open var onstart: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    fun start(): Unit = noImpl
    fun stop(): Unit = noImpl
}

@native public abstract class HTMLFrameSetElement : HTMLElement(), WindowEventHandlers {
    open var cols: String
        get() = noImpl
        set(value) = noImpl
    open var rows: String
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class HTMLFrameElement : HTMLElement() {
    open var name: String
        get() = noImpl
        set(value) = noImpl
    open var scrolling: String
        get() = noImpl
        set(value) = noImpl
    open var src: String
        get() = noImpl
        set(value) = noImpl
    open var frameBorder: String
        get() = noImpl
        set(value) = noImpl
    open var longDesc: String
        get() = noImpl
        set(value) = noImpl
    open var noResize: Boolean
        get() = noImpl
        set(value) = noImpl
    open val contentDocument: Document?
        get() = noImpl
    open val contentWindow: Window?
        get() = noImpl
    open var marginHeight: String
        get() = noImpl
        set(value) = noImpl
    open var marginWidth: String
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class HTMLDirectoryElement : HTMLElement() {
    open var compact: Boolean
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class HTMLFontElement : HTMLElement() {
    open var color: String
        get() = noImpl
        set(value) = noImpl
    open var face: String
        get() = noImpl
        set(value) = noImpl
    open var size: String
        get() = noImpl
        set(value) = noImpl
}

@native public interface External {
    fun AddSearchProvider(): Unit = noImpl
    fun IsSearchProviderInstalled(): Unit = noImpl
}

@native public interface EventInit {
    var bubbles: Boolean? /* = false */
    var cancelable: Boolean? /* = false */
    var composed: Boolean? /* = false */
}

@Suppress("NOTHING_TO_INLINE")
public inline fun EventInit(bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): EventInit {
    val o = js("({})")

    o["bubbles"] = bubbles
    o["cancelable"] = cancelable
    o["composed"] = composed

    return o
}

@native public open class CustomEvent(type: String, eventInitDict: CustomEventInit = noImpl) : Event(type, eventInitDict) {
    open val detail: Any?
        get() = noImpl
    fun initCustomEvent(type: String, bubbles: Boolean, cancelable: Boolean, detail: Any?): Unit = noImpl
}

@native public interface CustomEventInit : EventInit {
    var detail: Any? /* = null */
}

@Suppress("NOTHING_TO_INLINE")
public inline fun CustomEventInit(detail: Any? = null, bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): CustomEventInit {
    val o = js("({})")

    o["detail"] = detail
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable
    o["composed"] = composed

    return o
}

@native public interface EventListenerOptions {
    var capture: Boolean? /* = false */
}

@Suppress("NOTHING_TO_INLINE")
public inline fun EventListenerOptions(capture: Boolean? = false): EventListenerOptions {
    val o = js("({})")

    o["capture"] = capture

    return o
}

@native public interface AddEventListenerOptions : EventListenerOptions {
    var passive: Boolean? /* = false */
    var once: Boolean? /* = false */
}

@Suppress("NOTHING_TO_INLINE")
public inline fun AddEventListenerOptions(passive: Boolean? = false, once: Boolean? = false, capture: Boolean? = false): AddEventListenerOptions {
    val o = js("({})")

    o["passive"] = passive
    o["once"] = once
    o["capture"] = capture

    return o
}

@native public interface NonElementParentNode {
    fun getElementById(elementId: String): Element? = noImpl
}

@native public interface DocumentOrShadowRoot {
    val fullscreenElement: Element?
        get() = noImpl
}

@native public interface ParentNode {
    val children: HTMLCollection
        get() = noImpl
    val firstElementChild: Element?
        get() = noImpl
    val lastElementChild: Element?
        get() = noImpl
    val childElementCount: Int
        get() = noImpl
    fun prepend(vararg nodes: dynamic): Unit = noImpl
    fun append(vararg nodes: dynamic): Unit = noImpl
    fun querySelector(selectors: String): Element? = noImpl
    fun querySelectorAll(selectors: String): NodeList = noImpl
}

@native public interface NonDocumentTypeChildNode {
    val previousElementSibling: Element?
        get() = noImpl
    val nextElementSibling: Element?
        get() = noImpl
}

@native public interface ChildNode {
    fun before(vararg nodes: dynamic): Unit = noImpl
    fun after(vararg nodes: dynamic): Unit = noImpl
    fun replaceWith(vararg nodes: dynamic): Unit = noImpl
    fun remove(): Unit = noImpl
}

@native public interface Slotable {
    val assignedSlot: HTMLSlotElement?
        get() = noImpl
}

@native public abstract class NodeList {
    open val length: Int
        get() = noImpl
    fun item(index: Int): Node? = noImpl
    @nativeGetter
    operator fun get(index: Int): Node? = noImpl
}

@native public abstract class HTMLCollection : UnionElementOrHTMLCollection {
    open val length: Int
        get() = noImpl
    fun item(index: Int): Element? = noImpl
    @nativeGetter
    operator fun get(index: Int): Element? = noImpl
    fun namedItem(name: String): Element? = noImpl
    @nativeGetter
    operator fun get(name: String): Element? = noImpl
}

@native public open class MutationObserver(callback: (Array<MutationRecord>, MutationObserver) -> Unit) {
    fun observe(target: Node, options: MutationObserverInit = noImpl): Unit = noImpl
    fun disconnect(): Unit = noImpl
    fun takeRecords(): Array<MutationRecord> = noImpl
}

@native public interface MutationObserverInit {
    var childList: Boolean? /* = false */
    var attributes: Boolean?
    var characterData: Boolean?
    var subtree: Boolean? /* = false */
    var attributeOldValue: Boolean?
    var characterDataOldValue: Boolean?
    var attributeFilter: Array<String>?
}

@Suppress("NOTHING_TO_INLINE")
public inline fun MutationObserverInit(childList: Boolean? = false, attributes: Boolean?, characterData: Boolean?, subtree: Boolean? = false, attributeOldValue: Boolean?, characterDataOldValue: Boolean?, attributeFilter: Array<String>?): MutationObserverInit {
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

@native public abstract class MutationRecord {
    open val type: String
        get() = noImpl
    open val target: Node
        get() = noImpl
    open val addedNodes: NodeList
        get() = noImpl
    open val removedNodes: NodeList
        get() = noImpl
    open val previousSibling: Node?
        get() = noImpl
    open val nextSibling: Node?
        get() = noImpl
    open val attributeName: String?
        get() = noImpl
    open val attributeNamespace: String?
        get() = noImpl
    open val oldValue: String?
        get() = noImpl
}

@native public abstract class Node : EventTarget() {
    open val nodeType: Short
        get() = noImpl
    open val nodeName: String
        get() = noImpl
    open val baseURI: String
        get() = noImpl
    open val isConnected: Boolean
        get() = noImpl
    open val ownerDocument: Document?
        get() = noImpl
    open val parentNode: Node?
        get() = noImpl
    open val parentElement: Element?
        get() = noImpl
    open val childNodes: NodeList
        get() = noImpl
    open val firstChild: Node?
        get() = noImpl
    open val lastChild: Node?
        get() = noImpl
    open val previousSibling: Node?
        get() = noImpl
    open val nextSibling: Node?
        get() = noImpl
    open var nodeValue: String?
        get() = noImpl
        set(value) = noImpl
    open var textContent: String?
        get() = noImpl
        set(value) = noImpl
    fun getRootNode(options: GetRootNodeOptions = noImpl): Node = noImpl
    fun hasChildNodes(): Boolean = noImpl
    fun normalize(): Unit = noImpl
    fun cloneNode(deep: Boolean = false): Node = noImpl
    fun isEqualNode(otherNode: Node?): Boolean = noImpl
    fun isSameNode(otherNode: Node?): Boolean = noImpl
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

@native public interface GetRootNodeOptions {
    var composed: Boolean? /* = false */
}

@Suppress("NOTHING_TO_INLINE")
public inline fun GetRootNodeOptions(composed: Boolean? = false): GetRootNodeOptions {
    val o = js("({})")

    o["composed"] = composed

    return o
}

@native public open class XMLDocument : Document() {
}

@native public interface ElementCreationOptions {
    @native("is") var is_: String?
}

@Suppress("NOTHING_TO_INLINE")
public inline fun ElementCreationOptions(is_: String?): ElementCreationOptions {
    val o = js("({})")

    o["is"] = is_

    return o
}

@native public abstract class DOMImplementation {
    fun createDocumentType(qualifiedName: String, publicId: String, systemId: String): DocumentType = noImpl
    fun createDocument(namespace: String?, qualifiedName: String, doctype: DocumentType? = null): XMLDocument = noImpl
    fun createHTMLDocument(title: String = noImpl): Document = noImpl
    fun hasFeature(): Boolean = noImpl
}

@native public abstract class DocumentType : Node(), ChildNode {
    open val name: String
        get() = noImpl
    open val publicId: String
        get() = noImpl
    open val systemId: String
        get() = noImpl
}

@native public open class DocumentFragment : Node(), NonElementParentNode, ParentNode {
}

@native public open class ShadowRoot : DocumentFragment(), DocumentOrShadowRoot {
    open val mode: String
        get() = noImpl
    open val host: Element
        get() = noImpl
}

@native public abstract class Element : Node(), ParentNode, NonDocumentTypeChildNode, ChildNode, Slotable, GeometryUtils, UnionElementOrProcessingInstruction, UnionElementOrHTMLCollection, UnionElementOrRadioNodeList, UnionElementOrMouseEvent {
    open var innerHTML: String
        get() = noImpl
        set(value) = noImpl
    open var outerHTML: String
        get() = noImpl
        set(value) = noImpl
    open val namespaceURI: String?
        get() = noImpl
    open val prefix: String?
        get() = noImpl
    open val localName: String
        get() = noImpl
    open val tagName: String
        get() = noImpl
    open var id: String
        get() = noImpl
        set(value) = noImpl
    open var className: String
        get() = noImpl
        set(value) = noImpl
    open val classList: DOMTokenList
        get() = noImpl
    open var slot: String
        get() = noImpl
        set(value) = noImpl
    open val attributes: NamedNodeMap
        get() = noImpl
    open val shadowRoot: ShadowRoot?
        get() = noImpl
    open var scrollTop: Double
        get() = noImpl
        set(value) = noImpl
    open var scrollLeft: Double
        get() = noImpl
        set(value) = noImpl
    open val scrollWidth: Int
        get() = noImpl
    open val scrollHeight: Int
        get() = noImpl
    open val clientTop: Int
        get() = noImpl
    open val clientLeft: Int
        get() = noImpl
    open val clientWidth: Int
        get() = noImpl
    open val clientHeight: Int
        get() = noImpl
    fun requestFullscreen(): dynamic = noImpl
    fun insertAdjacentHTML(position: String, text: String): Unit = noImpl
    fun hasAttributes(): Boolean = noImpl
    fun getAttributeNames(): Array<String> = noImpl
    fun getAttribute(qualifiedName: String): String? = noImpl
    fun getAttributeNS(namespace: String?, localName: String): String? = noImpl
    fun setAttribute(qualifiedName: String, value: String): Unit = noImpl
    fun setAttributeNS(namespace: String?, qualifiedName: String, value: String): Unit = noImpl
    fun removeAttribute(qualifiedName: String): Unit = noImpl
    fun removeAttributeNS(namespace: String?, localName: String): Unit = noImpl
    fun hasAttribute(qualifiedName: String): Boolean = noImpl
    fun hasAttributeNS(namespace: String?, localName: String): Boolean = noImpl
    fun getAttributeNode(qualifiedName: String): Attr? = noImpl
    fun getAttributeNodeNS(namespace: String?, localName: String): Attr? = noImpl
    fun setAttributeNode(attr: Attr): Attr? = noImpl
    fun setAttributeNodeNS(attr: Attr): Attr? = noImpl
    fun removeAttributeNode(attr: Attr): Attr = noImpl
    fun attachShadow(init: ShadowRootInit): ShadowRoot = noImpl
    fun closest(selectors: String): Element? = noImpl
    fun matches(selectors: String): Boolean = noImpl
    fun webkitMatchesSelector(selectors: String): Boolean = noImpl
    fun getElementsByTagName(qualifiedName: String): HTMLCollection = noImpl
    fun getElementsByTagNameNS(namespace: String?, localName: String): HTMLCollection = noImpl
    fun getElementsByClassName(classNames: String): HTMLCollection = noImpl
    fun insertAdjacentElement(where: String, element: Element): Element? = noImpl
    fun insertAdjacentText(where: String, data: String): Unit = noImpl
    fun getClientRects(): Array<DOMRect> = noImpl
    fun getBoundingClientRect(): DOMRect = noImpl
    fun scrollIntoView(): Unit = noImpl
    fun scrollIntoView(arg: dynamic): Unit = noImpl
    fun scroll(options: ScrollToOptions = noImpl): Unit = noImpl
    fun scroll(x: Double, y: Double): Unit = noImpl
    fun scrollTo(options: ScrollToOptions = noImpl): Unit = noImpl
    fun scrollTo(x: Double, y: Double): Unit = noImpl
    fun scrollBy(options: ScrollToOptions = noImpl): Unit = noImpl
    fun scrollBy(x: Double, y: Double): Unit = noImpl
}

@native public interface ShadowRootInit {
    var mode: String?
}

@Suppress("NOTHING_TO_INLINE")
public inline fun ShadowRootInit(mode: String?): ShadowRootInit {
    val o = js("({})")

    o["mode"] = mode

    return o
}

@native public abstract class NamedNodeMap {
    open val length: Int
        get() = noImpl
    fun item(index: Int): Attr? = noImpl
    @nativeGetter
    operator fun get(index: Int): Attr? = noImpl
    fun getNamedItem(qualifiedName: String): Attr? = noImpl
    @nativeGetter
    operator fun get(qualifiedName: String): Attr? = noImpl
    fun getNamedItemNS(namespace: String?, localName: String): Attr? = noImpl
    fun setNamedItem(attr: Attr): Attr? = noImpl
    fun setNamedItemNS(attr: Attr): Attr? = noImpl
    fun removeNamedItem(qualifiedName: String): Attr = noImpl
    fun removeNamedItemNS(namespace: String?, localName: String): Attr = noImpl
}

@native public abstract class Attr : Node() {
    open val namespaceURI: String?
        get() = noImpl
    open val prefix: String?
        get() = noImpl
    open val localName: String
        get() = noImpl
    open val name: String
        get() = noImpl
    open var value: String
        get() = noImpl
        set(value) = noImpl
    open val ownerElement: Element?
        get() = noImpl
    open val specified: Boolean
        get() = noImpl
}

@native public abstract class CharacterData : Node(), NonDocumentTypeChildNode, ChildNode {
    open var data: String
        get() = noImpl
        set(value) = noImpl
    open val length: Int
        get() = noImpl
    fun substringData(offset: Int, count: Int): String = noImpl
    fun appendData(data: String): Unit = noImpl
    fun insertData(offset: Int, data: String): Unit = noImpl
    fun deleteData(offset: Int, count: Int): Unit = noImpl
    fun replaceData(offset: Int, count: Int, data: String): Unit = noImpl
}

@native public open class Text(data: String = "") : CharacterData(), Slotable, GeometryUtils {
    open val wholeText: String
        get() = noImpl
    fun splitText(offset: Int): Text = noImpl
}

@native public open class CDATASection : Text(noImpl) {
}

@native public abstract class ProcessingInstruction : CharacterData(), LinkStyle, UnionElementOrProcessingInstruction {
    open val target: String
        get() = noImpl
}

@native public open class Comment(data: String = "") : CharacterData() {
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
    fun getClientRects(): Array<DOMRect> = noImpl
    fun getBoundingClientRect(): DOMRect = noImpl

    companion object {
        val START_TO_START: Short = 0
        val START_TO_END: Short = 1
        val END_TO_END: Short = 2
        val END_TO_START: Short = 3
    }
}

@native public abstract class NodeIterator {
    open val root: Node
        get() = noImpl
    open val referenceNode: Node
        get() = noImpl
    open val pointerBeforeReferenceNode: Boolean
        get() = noImpl
    open val whatToShow: Int
        get() = noImpl
    open val filter: NodeFilter?
        get() = noImpl
    fun nextNode(): Node? = noImpl
    fun previousNode(): Node? = noImpl
    fun detach(): Unit = noImpl
}

@native public abstract class TreeWalker {
    open val root: Node
        get() = noImpl
    open val whatToShow: Int
        get() = noImpl
    open val filter: NodeFilter?
        get() = noImpl
    open var currentNode: Node
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

@native public abstract class DOMTokenList {
    open val length: Int
        get() = noImpl
    open var value: String
        get() = noImpl
        set(value) = noImpl
    fun item(index: Int): String? = noImpl
    @nativeGetter
    operator fun get(index: Int): String? = noImpl
    fun contains(token: String): Boolean = noImpl
    fun add(vararg tokens: String): Unit = noImpl
    fun remove(vararg tokens: String): Unit = noImpl
    fun toggle(token: String, force: Boolean = noImpl): Boolean = noImpl
    fun replace(token: String, newToken: String): Unit = noImpl
    fun supports(token: String): Boolean = noImpl
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
    var x: Double? /* = 0.0 */
    var y: Double? /* = 0.0 */
    var z: Double? /* = 0.0 */
    var w: Double? /* = 1.0 */
}

@Suppress("NOTHING_TO_INLINE")
public inline fun DOMPointInit(x: Double? = 0.0, y: Double? = 0.0, z: Double? = 0.0, w: Double? = 1.0): DOMPointInit {
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
    var x: Double? /* = 0.0 */
    var y: Double? /* = 0.0 */
    var width: Double? /* = 0.0 */
    var height: Double? /* = 0.0 */
}

@Suppress("NOTHING_TO_INLINE")
public inline fun DOMRectInit(x: Double? = 0.0, y: Double? = 0.0, width: Double? = 0.0, height: Double? = 0.0): DOMRectInit {
    val o = js("({})")

    o["x"] = x
    o["y"] = y
    o["width"] = width
    o["height"] = height

    return o
}

@native public interface DOMRectList {
    val length: Int
        get() = noImpl
    fun item(index: Int): DOMRect? = noImpl
    @nativeGetter
    operator fun get(index: Int): DOMRect? = noImpl
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
    var behavior: String? /* = "auto" */
}

@Suppress("NOTHING_TO_INLINE")
public inline fun ScrollOptions(behavior: String? = "auto"): ScrollOptions {
    val o = js("({})")

    o["behavior"] = behavior

    return o
}

@native public interface ScrollToOptions : ScrollOptions {
    var left: Double?
    var top: Double?
}

@Suppress("NOTHING_TO_INLINE")
public inline fun ScrollToOptions(left: Double?, top: Double?, behavior: String? = "auto"): ScrollToOptions {
    val o = js("({})")

    o["left"] = left
    o["top"] = top
    o["behavior"] = behavior

    return o
}

@native public abstract class MediaQueryList : EventTarget() {
    open val media: String
        get() = noImpl
    open val matches: Boolean
        get() = noImpl
    open var onchange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    fun addListener(listener: EventListener?): Unit = noImpl
    fun addListener(listener: ((Event) -> Unit)?): Unit = noImpl
    fun removeListener(listener: EventListener?): Unit = noImpl
    fun removeListener(listener: ((Event) -> Unit)?): Unit = noImpl
}

@native public open class MediaQueryListEvent(type: String, eventInitDict: MediaQueryListEventInit = noImpl) : Event(type, eventInitDict) {
    open val media: String
        get() = noImpl
    open val matches: Boolean
        get() = noImpl
}

@native public interface MediaQueryListEventInit : EventInit {
    var media: String? /* = "" */
    var matches: Boolean? /* = false */
}

@Suppress("NOTHING_TO_INLINE")
public inline fun MediaQueryListEventInit(media: String? = "", matches: Boolean? = false, bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): MediaQueryListEventInit {
    val o = js("({})")

    o["media"] = media
    o["matches"] = matches
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable
    o["composed"] = composed

    return o
}

@native public abstract class Screen {
    open val availWidth: Int
        get() = noImpl
    open val availHeight: Int
        get() = noImpl
    open val width: Int
        get() = noImpl
    open val height: Int
        get() = noImpl
    open val colorDepth: Int
        get() = noImpl
    open val pixelDepth: Int
        get() = noImpl
}

@native public abstract class CaretPosition {
    open val offsetNode: Node
        get() = noImpl
    open val offset: Int
        get() = noImpl
    fun getClientRect(): DOMRect? = noImpl
}

@native public interface ScrollIntoViewOptions : ScrollOptions {
    var block: String? /* = "center" */
    var inline: String? /* = "center" */
}

@Suppress("NOTHING_TO_INLINE")
public inline fun ScrollIntoViewOptions(block: String? = "center", inline: String? = "center", behavior: String? = "auto"): ScrollIntoViewOptions {
    val o = js("({})")

    o["block"] = block
    o["inline"] = inline
    o["behavior"] = behavior

    return o
}

@native public interface BoxQuadOptions {
    var box: String? /* = "border" */
    var relativeTo: dynamic
}

@Suppress("NOTHING_TO_INLINE")
public inline fun BoxQuadOptions(box: String? = "border", relativeTo: dynamic): BoxQuadOptions {
    val o = js("({})")

    o["box"] = box
    o["relativeTo"] = relativeTo

    return o
}

@native public interface ConvertCoordinateOptions {
    var fromBox: String? /* = "border" */
    var toBox: String? /* = "border" */
}

@Suppress("NOTHING_TO_INLINE")
public inline fun ConvertCoordinateOptions(fromBox: String? = "border", toBox: String? = "border"): ConvertCoordinateOptions {
    val o = js("({})")

    o["fromBox"] = fromBox
    o["toBox"] = toBox

    return o
}

@native public interface GeometryUtils {
    fun getBoxQuads(options: BoxQuadOptions = noImpl): Array<DOMQuad> = noImpl
    fun convertQuadFromNode(quad: dynamic, from: dynamic, options: ConvertCoordinateOptions = noImpl): DOMQuad = noImpl
    fun convertRectFromNode(rect: DOMRectReadOnly, from: dynamic, options: ConvertCoordinateOptions = noImpl): DOMQuad = noImpl
    fun convertPointFromNode(point: DOMPointInit, from: dynamic, options: ConvertCoordinateOptions = noImpl): DOMPoint = noImpl
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

@native public @marker interface HTMLOrSVGScriptElement {
}

@native public @marker interface RenderingContext {
}

@native public @marker interface HTMLOrSVGImageElement {
}

