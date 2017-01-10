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

public external open class Document : Node, GlobalEventHandlers, DocumentAndElementEventHandlers, NonElementParentNode, DocumentOrShadowRoot, ParentNode, GeometryUtils {
    open val fullscreenEnabled: Boolean
    open val fullscreen: Boolean
    var onfullscreenchange: ((Event) -> dynamic)?
    var onfullscreenerror: ((Event) -> dynamic)?
    open val rootElement: SVGSVGElement?
    var title: String
    open val referrer: String
    var domain: String
    open val activeElement: Element?
    open val location: Location?
    var cookie: String
    open val lastModified: String
    open val readyState: String
    var dir: String
    var body: HTMLElement?
    open val head: HTMLHeadElement?
    open val images: HTMLCollection
    open val embeds: HTMLCollection
    open val plugins: HTMLCollection
    open val links: HTMLCollection
    open val forms: HTMLCollection
    open val scripts: HTMLCollection
    open val currentScript: HTMLOrSVGScriptElement?
    open val defaultView: Window?
    var designMode: String
    var onreadystatechange: ((Event) -> dynamic)?
    var fgColor: String
    var linkColor: String
    var vlinkColor: String
    var alinkColor: String
    var bgColor: String
    open val anchors: HTMLCollection
    open val applets: HTMLCollection
    open val all: HTMLAllCollection
    open val implementation: DOMImplementation
    open val URL: String
    open val documentURI: String
    open val origin: String
    open val compatMode: String
    open val characterSet: String
    open val charset: String
    open val inputEncoding: String
    open val contentType: String
    open val doctype: DocumentType?
    open val documentElement: Element?
    open val scrollingElement: Element?
    open val styleSheets: StyleSheetList
    override var onabort: ((Event) -> dynamic)?
    override var onblur: ((Event) -> dynamic)?
    override var oncancel: ((Event) -> dynamic)?
    override var oncanplay: ((Event) -> dynamic)?
    override var oncanplaythrough: ((Event) -> dynamic)?
    override var onchange: ((Event) -> dynamic)?
    override var onclick: ((Event) -> dynamic)?
    override var onclose: ((Event) -> dynamic)?
    override var oncontextmenu: ((Event) -> dynamic)?
    override var oncuechange: ((Event) -> dynamic)?
    override var ondblclick: ((Event) -> dynamic)?
    override var ondrag: ((Event) -> dynamic)?
    override var ondragend: ((Event) -> dynamic)?
    override var ondragenter: ((Event) -> dynamic)?
    override var ondragexit: ((Event) -> dynamic)?
    override var ondragleave: ((Event) -> dynamic)?
    override var ondragover: ((Event) -> dynamic)?
    override var ondragstart: ((Event) -> dynamic)?
    override var ondrop: ((Event) -> dynamic)?
    override var ondurationchange: ((Event) -> dynamic)?
    override var onemptied: ((Event) -> dynamic)?
    override var onended: ((Event) -> dynamic)?
    override var onerror: ((dynamic, String, Int, Int, Any?) -> dynamic)?
    override var onfocus: ((Event) -> dynamic)?
    override var oninput: ((Event) -> dynamic)?
    override var oninvalid: ((Event) -> dynamic)?
    override var onkeydown: ((Event) -> dynamic)?
    override var onkeypress: ((Event) -> dynamic)?
    override var onkeyup: ((Event) -> dynamic)?
    override var onload: ((Event) -> dynamic)?
    override var onloadeddata: ((Event) -> dynamic)?
    override var onloadedmetadata: ((Event) -> dynamic)?
    override var onloadend: ((Event) -> dynamic)?
    override var onloadstart: ((Event) -> dynamic)?
    override var onmousedown: ((Event) -> dynamic)?
    override var onmouseenter: ((Event) -> dynamic)?
    override var onmouseleave: ((Event) -> dynamic)?
    override var onmousemove: ((Event) -> dynamic)?
    override var onmouseout: ((Event) -> dynamic)?
    override var onmouseover: ((Event) -> dynamic)?
    override var onmouseup: ((Event) -> dynamic)?
    override var onwheel: ((Event) -> dynamic)?
    override var onpause: ((Event) -> dynamic)?
    override var onplay: ((Event) -> dynamic)?
    override var onplaying: ((Event) -> dynamic)?
    override var onprogress: ((Event) -> dynamic)?
    override var onratechange: ((Event) -> dynamic)?
    override var onreset: ((Event) -> dynamic)?
    override var onresize: ((Event) -> dynamic)?
    override var onscroll: ((Event) -> dynamic)?
    override var onseeked: ((Event) -> dynamic)?
    override var onseeking: ((Event) -> dynamic)?
    override var onselect: ((Event) -> dynamic)?
    override var onshow: ((Event) -> dynamic)?
    override var onstalled: ((Event) -> dynamic)?
    override var onsubmit: ((Event) -> dynamic)?
    override var onsuspend: ((Event) -> dynamic)?
    override var ontimeupdate: ((Event) -> dynamic)?
    override var ontoggle: ((Event) -> dynamic)?
    override var onvolumechange: ((Event) -> dynamic)?
    override var onwaiting: ((Event) -> dynamic)?
    override var oncopy: ((Event) -> dynamic)?
    override var oncut: ((Event) -> dynamic)?
    override var onpaste: ((Event) -> dynamic)?
    override val fullscreenElement: Element?
    override val children: HTMLCollection
    override val firstElementChild: Element?
    override val lastElementChild: Element?
    override val childElementCount: Int
    fun exitFullscreen(): dynamic
    @nativeGetter
    operator fun get(name: String): dynamic
    fun getElementsByName(elementName: String): NodeList
    fun open(type: String = noImpl, replace: String = noImpl): Document
    fun open(url: String, name: String, features: String): Window
    fun close(): Unit
    fun write(vararg text: String): Unit
    fun writeln(vararg text: String): Unit
    fun hasFocus(): Boolean
    fun execCommand(commandId: String, showUI: Boolean = noImpl, value: String = noImpl): Boolean
    fun queryCommandEnabled(commandId: String): Boolean
    fun queryCommandIndeterm(commandId: String): Boolean
    fun queryCommandState(commandId: String): Boolean
    fun queryCommandSupported(commandId: String): Boolean
    fun queryCommandValue(commandId: String): String
    fun clear(): Unit
    fun captureEvents(): Unit
    fun releaseEvents(): Unit
    fun getElementsByTagName(qualifiedName: String): HTMLCollection
    fun getElementsByTagNameNS(namespace: String?, localName: String): HTMLCollection
    fun getElementsByClassName(classNames: String): HTMLCollection
    fun createElement(localName: String, options: ElementCreationOptions = noImpl): Element
    fun createElementNS(namespace: String?, qualifiedName: String, options: ElementCreationOptions = noImpl): Element
    fun createDocumentFragment(): DocumentFragment
    fun createTextNode(data: String): Text
    fun createCDATASection(data: String): CDATASection
    fun createComment(data: String): Comment
    fun createProcessingInstruction(target: String, data: String): ProcessingInstruction
    fun importNode(node: Node, deep: Boolean = noImpl): Node
    fun adoptNode(node: Node): Node
    fun createAttribute(localName: String): Attr
    fun createAttributeNS(namespace: String?, qualifiedName: String): Attr
    fun createEvent(interface_: String): Event
    fun createRange(): Range
    fun createNodeIterator(root: Node, whatToShow: Int = noImpl, filter: NodeFilter? = noImpl): NodeIterator
    fun createNodeIterator(root: Node, whatToShow: Int = noImpl, filter: ((Node) -> Short)? = noImpl): NodeIterator
    fun createTreeWalker(root: Node, whatToShow: Int = noImpl, filter: NodeFilter? = noImpl): TreeWalker
    fun createTreeWalker(root: Node, whatToShow: Int = noImpl, filter: ((Node) -> Short)? = noImpl): TreeWalker
    fun elementFromPoint(x: Double, y: Double): Element?
    fun elementsFromPoint(x: Double, y: Double): Array<Element>
    fun caretPositionFromPoint(x: Double, y: Double): CaretPosition?
    override fun getElementById(elementId: String): Element?
    override fun prepend(vararg nodes: dynamic): Unit
    override fun append(vararg nodes: dynamic): Unit
    override fun querySelector(selectors: String): Element?
    override fun querySelectorAll(selectors: String): NodeList
    override fun getBoxQuads(options: BoxQuadOptions /* = noImpl */): Array<DOMQuad>
    override fun convertQuadFromNode(quad: dynamic, from: dynamic, options: ConvertCoordinateOptions /* = noImpl */): DOMQuad
    override fun convertRectFromNode(rect: DOMRectReadOnly, from: dynamic, options: ConvertCoordinateOptions /* = noImpl */): DOMQuad
    override fun convertPointFromNode(point: DOMPointInit, from: dynamic, options: ConvertCoordinateOptions /* = noImpl */): DOMPoint
}

public external abstract class Window : EventTarget, GlobalEventHandlers, WindowEventHandlers, WindowOrWorkerGlobalScope, WindowSessionStorage, WindowLocalStorage, GlobalPerformance, UnionMessagePortOrWindow {
    open val window: Window
    open val self: Window
    open val document: Document
    open var name: String
    open val location: Location
    open val history: History
    open val customElements: CustomElementRegistry
    open val locationbar: BarProp
    open val menubar: BarProp
    open val personalbar: BarProp
    open val scrollbars: BarProp
    open val statusbar: BarProp
    open val toolbar: BarProp
    open var status: String
    open val closed: Boolean
    open val frames: Window
    open val length: Int
    open val top: Window
    open var opener: Any?
    open val parent: Window
    open val frameElement: Element?
    open val navigator: Navigator
    open val applicationCache: ApplicationCache
    open val external: External
    open val screen: Screen
    open val innerWidth: Int
    open val innerHeight: Int
    open val scrollX: Double
    open val pageXOffset: Double
    open val scrollY: Double
    open val pageYOffset: Double
    open val screenX: Int
    open val screenY: Int
    open val outerWidth: Int
    open val outerHeight: Int
    open val devicePixelRatio: Double
    fun close(): Unit
    fun stop(): Unit
    fun focus(): Unit
    fun blur(): Unit
    fun open(url: String = noImpl, target: String = noImpl, features: String = noImpl): Window?
    @nativeGetter
    operator fun get(name: String): dynamic
    fun alert(): Unit
    fun alert(message: String): Unit
    fun confirm(message: String = noImpl): Boolean
    fun prompt(message: String = noImpl, default: String = noImpl): String?
    fun print(): Unit
    fun requestAnimationFrame(callback: (Double) -> Unit): Int
    fun cancelAnimationFrame(handle: Int): Unit
    fun postMessage(message: Any?, targetOrigin: String, transfer: Array<dynamic> = noImpl): Unit
    fun captureEvents(): Unit
    fun releaseEvents(): Unit
    fun matchMedia(query: String): MediaQueryList
    fun moveTo(x: Int, y: Int): Unit
    fun moveBy(x: Int, y: Int): Unit
    fun resizeTo(x: Int, y: Int): Unit
    fun resizeBy(x: Int, y: Int): Unit
    fun scroll(options: ScrollToOptions = noImpl): Unit
    fun scroll(x: Double, y: Double): Unit
    fun scrollTo(options: ScrollToOptions = noImpl): Unit
    fun scrollTo(x: Double, y: Double): Unit
    fun scrollBy(options: ScrollToOptions = noImpl): Unit
    fun scrollBy(x: Double, y: Double): Unit
    fun getComputedStyle(elt: Element, pseudoElt: String? = noImpl): CSSStyleDeclaration
}

public external abstract class HTMLAllCollection {
    open val length: Int
//    @nativeGetter
//    operator fun get(index: Int): Element?
//    fun namedItem(name: String): UnionElementOrHTMLCollection?
//    @nativeGetter
//    operator fun get(name: String): UnionElementOrHTMLCollection?
    fun item(nameOrIndex: String = noImpl): UnionElementOrHTMLCollection?
}

public external abstract class HTMLFormControlsCollection : HTMLCollection {
//    override fun namedItem(name: String): UnionElementOrRadioNodeList?
//    @nativeGetter
//    operator override fun get(name: String): UnionElementOrRadioNodeList?
}

public external abstract class RadioNodeList : NodeList, UnionElementOrRadioNodeList {
    open var value: String
}

public external abstract class HTMLOptionsCollection : HTMLCollection {
    override var length: Int
    open var selectedIndex: Int
    @nativeSetter
    operator fun set(index: Int, option: HTMLOptionElement?): Unit
    fun add(element: UnionHTMLOptGroupElementOrHTMLOptionElement, before: dynamic = noImpl): Unit
    fun remove(index: Int): Unit
}

public external abstract class HTMLElement : Element, ElementCSSInlineStyle, GlobalEventHandlers, DocumentAndElementEventHandlers, ElementContentEditable {
    open var title: String
    open var lang: String
    open var translate: Boolean
    open var dir: String
    open val dataset: DOMStringMap
    open var hidden: Boolean
    open var tabIndex: Int
    open var accessKey: String
    open val accessKeyLabel: String
    open var draggable: Boolean
    open val dropzone: DOMTokenList
    open var contextMenu: HTMLMenuElement?
    open var spellcheck: Boolean
    open var innerText: String
    open val offsetParent: Element?
    open val offsetTop: Int
    open val offsetLeft: Int
    open val offsetWidth: Int
    open val offsetHeight: Int
    fun click(): Unit
    fun focus(): Unit
    fun blur(): Unit
    fun forceSpellCheck(): Unit
}

public external abstract class HTMLUnknownElement : HTMLElement {
}

public external abstract class DOMStringMap {
    @nativeGetter
    operator fun get(name: String): String?
    @nativeSetter
    operator fun set(name: String, value: String): Unit
}

public external abstract class HTMLHtmlElement : HTMLElement {
    open var version: String
}

public external abstract class HTMLHeadElement : HTMLElement {
}

public external abstract class HTMLTitleElement : HTMLElement {
    open var text: String
}

public external abstract class HTMLBaseElement : HTMLElement {
    open var href: String
    open var target: String
}

public external abstract class HTMLLinkElement : HTMLElement, LinkStyle {
    open var scope: String
    open var workerType: String
    open var href: String
    open var crossOrigin: String?
    open var rel: String
    @JsName("as") open var as_: String
    open val relList: DOMTokenList
    open var media: String
    open var nonce: String
    open var hreflang: String
    open var type: String
    open val sizes: DOMTokenList
    open var referrerPolicy: String
    open var charset: String
    open var rev: String
    open var target: String
}

public external abstract class HTMLMetaElement : HTMLElement {
    open var name: String
    open var httpEquiv: String
    open var content: String
    open var scheme: String
}

public external abstract class HTMLStyleElement : HTMLElement, LinkStyle {
    open var media: String
    open var nonce: String
    open var type: String
}

public external abstract class HTMLBodyElement : HTMLElement, WindowEventHandlers {
    open var text: String
    open var link: String
    open var vLink: String
    open var aLink: String
    open var bgColor: String
    open var background: String
}

public external abstract class HTMLHeadingElement : HTMLElement {
    open var align: String
}

public external abstract class HTMLParagraphElement : HTMLElement {
    open var align: String
}

public external abstract class HTMLHRElement : HTMLElement {
    open var align: String
    open var color: String
    open var noShade: Boolean
    open var size: String
    open var width: String
}

public external abstract class HTMLPreElement : HTMLElement {
    open var width: Int
}

public external abstract class HTMLQuoteElement : HTMLElement {
    open var cite: String
}

public external abstract class HTMLOListElement : HTMLElement {
    open var reversed: Boolean
    open var start: Int
    open var type: String
    open var compact: Boolean
}

public external abstract class HTMLUListElement : HTMLElement {
    open var compact: Boolean
    open var type: String
}

public external abstract class HTMLLIElement : HTMLElement {
    open var value: Int
    open var type: String
}

public external abstract class HTMLDListElement : HTMLElement {
    open var compact: Boolean
}

public external abstract class HTMLDivElement : HTMLElement {
    open var align: String
}

public external abstract class HTMLAnchorElement : HTMLElement, HTMLHyperlinkElementUtils {
    open var target: String
    open var download: String
    open var ping: String
    open var rel: String
    open val relList: DOMTokenList
    open var hreflang: String
    open var type: String
    open var text: String
    open var referrerPolicy: String
    open var coords: String
    open var charset: String
    open var name: String
    open var rev: String
    open var shape: String
}

public external abstract class HTMLDataElement : HTMLElement {
    open var value: String
}

public external abstract class HTMLTimeElement : HTMLElement {
    open var dateTime: String
}

public external abstract class HTMLSpanElement : HTMLElement {
}

public external abstract class HTMLBRElement : HTMLElement {
    open var clear: String
}

public external interface HTMLHyperlinkElementUtils {
    var href: String
    val origin: String
    var protocol: String
    var username: String
    var password: String
    var host: String
    var hostname: String
    var port: String
    var pathname: String
    var search: String
    var hash: String
}

public external abstract class HTMLModElement : HTMLElement {
    open var cite: String
    open var dateTime: String
}

public external abstract class HTMLPictureElement : HTMLElement {
}

public external abstract class HTMLSourceElement : HTMLElement {
    open var src: String
    open var type: String
    open var srcset: String
    open var sizes: String
    open var media: String
}

public external abstract class HTMLImageElement : HTMLElement, TexImageSource, HTMLOrSVGImageElement {
    open var alt: String
    open var src: String
    open var srcset: String
    open var sizes: String
    open var crossOrigin: String?
    open var useMap: String
    open var isMap: Boolean
    open var width: Int
    open var height: Int
    open val naturalWidth: Int
    open val naturalHeight: Int
    open val complete: Boolean
    open val currentSrc: String
    open var referrerPolicy: String
    open var name: String
    open var lowsrc: String
    open var align: String
    open var hspace: Int
    open var vspace: Int
    open var longDesc: String
    open var border: String
    open val x: Int
    open val y: Int
}

public external abstract class HTMLIFrameElement : HTMLElement {
    open var src: String
    open var srcdoc: String
    open var name: String
    open val sandbox: DOMTokenList
    open var allowFullscreen: Boolean
    open var allowUserMedia: Boolean
    open var width: String
    open var height: String
    open var referrerPolicy: String
    open val contentDocument: Document?
    open val contentWindow: Window?
    open var align: String
    open var scrolling: String
    open var frameBorder: String
    open var longDesc: String
    open var marginHeight: String
    open var marginWidth: String
    fun getSVGDocument(): Document?
}

public external abstract class HTMLEmbedElement : HTMLElement {
    open var src: String
    open var type: String
    open var width: String
    open var height: String
    open var align: String
    open var name: String
    fun getSVGDocument(): Document?
}

public external abstract class HTMLObjectElement : HTMLElement {
    open var data: String
    open var type: String
    open var typeMustMatch: Boolean
    open var name: String
    open var useMap: String
    open val form: HTMLFormElement?
    open var width: String
    open var height: String
    open val contentDocument: Document?
    open val contentWindow: Window?
    open val willValidate: Boolean
    open val validity: ValidityState
    open val validationMessage: String
    open var align: String
    open var archive: String
    open var code: String
    open var declare: Boolean
    open var hspace: Int
    open var standby: String
    open var vspace: Int
    open var codeBase: String
    open var codeType: String
    open var border: String
    fun getSVGDocument(): Document?
    fun checkValidity(): Boolean
    fun reportValidity(): Boolean
    fun setCustomValidity(error: String): Unit
}

public external abstract class HTMLParamElement : HTMLElement {
    open var name: String
    open var value: String
    open var type: String
    open var valueType: String
}

public external abstract class HTMLVideoElement : HTMLMediaElement, TexImageSource {
    open var width: Int
    open var height: Int
    open val videoWidth: Int
    open val videoHeight: Int
    open var poster: String
    open var playsInline: Boolean
}

public external abstract class HTMLAudioElement : HTMLMediaElement {
}

public external abstract class HTMLTrackElement : HTMLElement {
    open var kind: String
    open var src: String
    open var srclang: String
    open var label: String
    open var default: Boolean
    open val readyState: Short
    open val track: TextTrack

    companion object {
        val NONE: Short
        val LOADING: Short
        val LOADED: Short
        val ERROR: Short
    }
}

public external abstract class HTMLMediaElement : HTMLElement {
    open val error: MediaError?
    open var src: String
    open var srcObject: dynamic
    open val currentSrc: String
    open var crossOrigin: String?
    open val networkState: Short
    open var preload: String
    open val buffered: TimeRanges
    open val readyState: Short
    open val seeking: Boolean
    open var currentTime: Double
    open val duration: Double
    open val paused: Boolean
    open var defaultPlaybackRate: Double
    open var playbackRate: Double
    open val played: TimeRanges
    open val seekable: TimeRanges
    open val ended: Boolean
    open var autoplay: Boolean
    open var loop: Boolean
    open var controls: Boolean
    open var volume: Double
    open var muted: Boolean
    open var defaultMuted: Boolean
    open val audioTracks: AudioTrackList
    open val videoTracks: VideoTrackList
    open val textTracks: TextTrackList
    fun load(): Unit
    fun canPlayType(type: String): String
    fun fastSeek(time: Double): Unit
    fun getStartDate(): dynamic
    fun play(): dynamic
    fun pause(): Unit
    fun addTextTrack(kind: String, label: String = noImpl, language: String = noImpl): TextTrack

    companion object {
        val NETWORK_EMPTY: Short
        val NETWORK_IDLE: Short
        val NETWORK_LOADING: Short
        val NETWORK_NO_SOURCE: Short
        val HAVE_NOTHING: Short
        val HAVE_METADATA: Short
        val HAVE_CURRENT_DATA: Short
        val HAVE_FUTURE_DATA: Short
        val HAVE_ENOUGH_DATA: Short
    }
}

public external abstract class MediaError {
    open val code: Short

    companion object {
        val MEDIA_ERR_ABORTED: Short
        val MEDIA_ERR_NETWORK: Short
        val MEDIA_ERR_DECODE: Short
        val MEDIA_ERR_SRC_NOT_SUPPORTED: Short
    }
}

public external abstract class AudioTrackList : EventTarget {
    open val length: Int
    open var onchange: ((Event) -> dynamic)?
    open var onaddtrack: ((Event) -> dynamic)?
    open var onremovetrack: ((Event) -> dynamic)?
    @nativeGetter
    operator fun get(index: Int): AudioTrack?
    fun getTrackById(id: String): AudioTrack?
}

public external abstract class AudioTrack : UnionAudioTrackOrTextTrackOrVideoTrack {
    open val id: String
    open val kind: String
    open val label: String
    open val language: String
    open var enabled: Boolean
}

public external abstract class VideoTrackList : EventTarget {
    open val length: Int
    open val selectedIndex: Int
    open var onchange: ((Event) -> dynamic)?
    open var onaddtrack: ((Event) -> dynamic)?
    open var onremovetrack: ((Event) -> dynamic)?
    @nativeGetter
    operator fun get(index: Int): VideoTrack?
    fun getTrackById(id: String): VideoTrack?
}

public external abstract class VideoTrack : UnionAudioTrackOrTextTrackOrVideoTrack {
    open val id: String
    open val kind: String
    open val label: String
    open val language: String
    open var selected: Boolean
}

public external abstract class TextTrackList : EventTarget {
    open val length: Int
    open var onchange: ((Event) -> dynamic)?
    open var onaddtrack: ((Event) -> dynamic)?
    open var onremovetrack: ((Event) -> dynamic)?
    @nativeGetter
    operator fun get(index: Int): TextTrack?
    fun getTrackById(id: String): TextTrack?
}

public external abstract class TextTrack : EventTarget, UnionAudioTrackOrTextTrackOrVideoTrack {
    open val kind: String
    open val label: String
    open val language: String
    open val id: String
    open val inBandMetadataTrackDispatchType: String
    open var mode: String
    open val cues: TextTrackCueList?
    open val activeCues: TextTrackCueList?
    open var oncuechange: ((Event) -> dynamic)?
    fun addCue(cue: TextTrackCue): Unit
    fun removeCue(cue: TextTrackCue): Unit
}

public external abstract class TextTrackCueList {
    open val length: Int
    @nativeGetter
    operator fun get(index: Int): TextTrackCue?
    fun getCueById(id: String): TextTrackCue?
}

public external abstract class TextTrackCue : EventTarget {
    open val track: TextTrack?
    open var id: String
    open var startTime: Double
    open var endTime: Double
    open var pauseOnExit: Boolean
    open var onenter: ((Event) -> dynamic)?
    open var onexit: ((Event) -> dynamic)?
}

public external abstract class TimeRanges {
    open val length: Int
    fun start(index: Int): Double
    fun end(index: Int): Double
}

public external open class TrackEvent(type: String, eventInitDict: TrackEventInit = noImpl) : Event {
    open val track: UnionAudioTrackOrTextTrackOrVideoTrack?
}

public external interface TrackEventInit : EventInit {
    var track: UnionAudioTrackOrTextTrackOrVideoTrack? /* = null */
        get() = noImpl
        set(value) = noImpl
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

public external abstract class HTMLMapElement : HTMLElement {
    open var name: String
    open val areas: HTMLCollection
}

public external abstract class HTMLAreaElement : HTMLElement, HTMLHyperlinkElementUtils {
    open var alt: String
    open var coords: String
    open var shape: String
    open var target: String
    open var download: String
    open var ping: String
    open var rel: String
    open val relList: DOMTokenList
    open var referrerPolicy: String
    open var noHref: Boolean
}

public external abstract class HTMLTableElement : HTMLElement {
    open var caption: HTMLTableCaptionElement?
    open var tHead: HTMLTableSectionElement?
    open var tFoot: HTMLTableSectionElement?
    open val tBodies: HTMLCollection
    open val rows: HTMLCollection
    open var align: String
    open var border: String
    open var frame: String
    open var rules: String
    open var summary: String
    open var width: String
    open var bgColor: String
    open var cellPadding: String
    open var cellSpacing: String
    fun createCaption(): HTMLTableCaptionElement
    fun deleteCaption(): Unit
    fun createTHead(): HTMLTableSectionElement
    fun deleteTHead(): Unit
    fun createTFoot(): HTMLTableSectionElement
    fun deleteTFoot(): Unit
    fun createTBody(): HTMLTableSectionElement
    fun insertRow(index: Int = noImpl): HTMLTableRowElement
    fun deleteRow(index: Int): Unit
}

public external abstract class HTMLTableCaptionElement : HTMLElement {
    open var align: String
}

public external abstract class HTMLTableColElement : HTMLElement {
    open var span: Int
    open var align: String
    open var ch: String
    open var chOff: String
    open var vAlign: String
    open var width: String
}

public external abstract class HTMLTableSectionElement : HTMLElement {
    open val rows: HTMLCollection
    open var align: String
    open var ch: String
    open var chOff: String
    open var vAlign: String
    fun insertRow(index: Int = noImpl): HTMLElement
    fun deleteRow(index: Int): Unit
}

public external abstract class HTMLTableRowElement : HTMLElement {
    open val rowIndex: Int
    open val sectionRowIndex: Int
    open val cells: HTMLCollection
    open var align: String
    open var ch: String
    open var chOff: String
    open var vAlign: String
    open var bgColor: String
    fun insertCell(index: Int = noImpl): HTMLElement
    fun deleteCell(index: Int): Unit
}

public external abstract class HTMLTableCellElement : HTMLElement {
    open var colSpan: Int
    open var rowSpan: Int
    open var headers: String
    open val cellIndex: Int
    open var scope: String
    open var abbr: String
    open var align: String
    open var axis: String
    open var height: String
    open var width: String
    open var ch: String
    open var chOff: String
    open var noWrap: Boolean
    open var vAlign: String
    open var bgColor: String
}

public external abstract class HTMLFormElement : HTMLElement {
    open var acceptCharset: String
    open var action: String
    open var autocomplete: String
    open var enctype: String
    open var encoding: String
    open var method: String
    open var name: String
    open var noValidate: Boolean
    open var target: String
    open val elements: HTMLFormControlsCollection
    open val length: Int
    @nativeGetter
    operator fun get(index: Int): Element?
    @nativeGetter
    operator fun get(name: String): UnionElementOrRadioNodeList?
    fun submit(): Unit
    fun reset(): Unit
    fun checkValidity(): Boolean
    fun reportValidity(): Boolean
}

public external abstract class HTMLLabelElement : HTMLElement {
    open val form: HTMLFormElement?
    open var htmlFor: String
    open val control: HTMLElement?
}

public external abstract class HTMLInputElement : HTMLElement {
    open var accept: String
    open var alt: String
    open var autocomplete: String
    open var autofocus: Boolean
    open var defaultChecked: Boolean
    open var checked: Boolean
    open var dirName: String
    open var disabled: Boolean
    open val form: HTMLFormElement?
    open val files: FileList?
    open var formAction: String
    open var formEnctype: String
    open var formMethod: String
    open var formNoValidate: Boolean
    open var formTarget: String
    open var height: Int
    open var indeterminate: Boolean
    open var inputMode: String
    open val list: HTMLElement?
    open var max: String
    open var maxLength: Int
    open var min: String
    open var minLength: Int
    open var multiple: Boolean
    open var name: String
    open var pattern: String
    open var placeholder: String
    open var readOnly: Boolean
    open var required: Boolean
    open var size: Int
    open var src: String
    open var step: String
    open var type: String
    open var defaultValue: String
    open var value: String
    open var valueAsDate: dynamic
    open var valueAsNumber: Double
    open var width: Int
    open val willValidate: Boolean
    open val validity: ValidityState
    open val validationMessage: String
    open val labels: NodeList
    open var selectionStart: Int?
    open var selectionEnd: Int?
    open var selectionDirection: String?
    open var align: String
    open var useMap: String
    fun stepUp(n: Int = noImpl): Unit
    fun stepDown(n: Int = noImpl): Unit
    fun checkValidity(): Boolean
    fun reportValidity(): Boolean
    fun setCustomValidity(error: String): Unit
    fun select(): Unit
    fun setRangeText(replacement: String): Unit
    fun setRangeText(replacement: String, start: Int, end: Int, selectionMode: String = noImpl): Unit
    fun setSelectionRange(start: Int, end: Int, direction: String = noImpl): Unit
}

public external abstract class HTMLButtonElement : HTMLElement {
    open var autofocus: Boolean
    open var disabled: Boolean
    open val form: HTMLFormElement?
    open var formAction: String
    open var formEnctype: String
    open var formMethod: String
    open var formNoValidate: Boolean
    open var formTarget: String
    open var name: String
    open var type: String
    open var value: String
    open var menu: HTMLMenuElement?
    open val willValidate: Boolean
    open val validity: ValidityState
    open val validationMessage: String
    open val labels: NodeList
    fun checkValidity(): Boolean
    fun reportValidity(): Boolean
    fun setCustomValidity(error: String): Unit
}

public external abstract class HTMLSelectElement : HTMLElement {
    open var autocomplete: String
    open var autofocus: Boolean
    open var disabled: Boolean
    open val form: HTMLFormElement?
    open var multiple: Boolean
    open var name: String
    open var required: Boolean
    open var size: Int
    open val type: String
    open val options: HTMLOptionsCollection
    open var length: Int
    open val selectedOptions: HTMLCollection
    open var selectedIndex: Int
    open var value: String
    open val willValidate: Boolean
    open val validity: ValidityState
    open val validationMessage: String
    open val labels: NodeList
    fun item(index: Int): Element?
    @nativeGetter
    operator fun get(index: Int): Element?
    fun namedItem(name: String): HTMLOptionElement?
    fun add(element: UnionHTMLOptGroupElementOrHTMLOptionElement, before: dynamic = noImpl): Unit
    fun remove(index: Int): Unit
    @nativeSetter
    operator fun set(index: Int, option: HTMLOptionElement?): Unit
    fun checkValidity(): Boolean
    fun reportValidity(): Boolean
    fun setCustomValidity(error: String): Unit
}

public external abstract class HTMLDataListElement : HTMLElement {
    open val options: HTMLCollection
}

public external abstract class HTMLOptGroupElement : HTMLElement, UnionHTMLOptGroupElementOrHTMLOptionElement {
    open var disabled: Boolean
    open var label: String
}

public external abstract class HTMLOptionElement : HTMLElement, UnionHTMLOptGroupElementOrHTMLOptionElement {
    open var disabled: Boolean
    open val form: HTMLFormElement?
    open var label: String
    open var defaultSelected: Boolean
    open var selected: Boolean
    open var value: String
    open var text: String
    open val index: Int
}

public external abstract class HTMLTextAreaElement : HTMLElement {
    open var autocomplete: String
    open var autofocus: Boolean
    open var cols: Int
    open var dirName: String
    open var disabled: Boolean
    open val form: HTMLFormElement?
    open var inputMode: String
    open var maxLength: Int
    open var minLength: Int
    open var name: String
    open var placeholder: String
    open var readOnly: Boolean
    open var required: Boolean
    open var rows: Int
    open var wrap: String
    open val type: String
    open var defaultValue: String
    open var value: String
    open val textLength: Int
    open val willValidate: Boolean
    open val validity: ValidityState
    open val validationMessage: String
    open val labels: NodeList
    open var selectionStart: Int?
    open var selectionEnd: Int?
    open var selectionDirection: String?
    fun checkValidity(): Boolean
    fun reportValidity(): Boolean
    fun setCustomValidity(error: String): Unit
    fun select(): Unit
    fun setRangeText(replacement: String): Unit
    fun setRangeText(replacement: String, start: Int, end: Int, selectionMode: String = noImpl): Unit
    fun setSelectionRange(start: Int, end: Int, direction: String = noImpl): Unit
}

public external abstract class HTMLKeygenElement : HTMLElement {
    open var autofocus: Boolean
    open var challenge: String
    open var disabled: Boolean
    open val form: HTMLFormElement?
    open var keytype: String
    open var name: String
    open val type: String
    open val willValidate: Boolean
    open val validity: ValidityState
    open val validationMessage: String
    open val labels: NodeList
    fun checkValidity(): Boolean
    fun reportValidity(): Boolean
    fun setCustomValidity(error: String): Unit
}

public external abstract class HTMLOutputElement : HTMLElement {
    open val htmlFor: DOMTokenList
    open val form: HTMLFormElement?
    open var name: String
    open val type: String
    open var defaultValue: String
    open var value: String
    open val willValidate: Boolean
    open val validity: ValidityState
    open val validationMessage: String
    open val labels: NodeList
    fun checkValidity(): Boolean
    fun reportValidity(): Boolean
    fun setCustomValidity(error: String): Unit
}

public external abstract class HTMLProgressElement : HTMLElement {
    open var value: Double
    open var max: Double
    open val position: Double
    open val labels: NodeList
}

public external abstract class HTMLMeterElement : HTMLElement {
    open var value: Double
    open var min: Double
    open var max: Double
    open var low: Double
    open var high: Double
    open var optimum: Double
    open val labels: NodeList
}

public external abstract class HTMLFieldSetElement : HTMLElement {
    open var disabled: Boolean
    open val form: HTMLFormElement?
    open var name: String
    open val type: String
    open val elements: HTMLCollection
    open val willValidate: Boolean
    open val validity: ValidityState
    open val validationMessage: String
    fun checkValidity(): Boolean
    fun reportValidity(): Boolean
    fun setCustomValidity(error: String): Unit
}

public external abstract class HTMLLegendElement : HTMLElement {
    open val form: HTMLFormElement?
    open var align: String
}

public external abstract class ValidityState {
    open val valueMissing: Boolean
    open val typeMismatch: Boolean
    open val patternMismatch: Boolean
    open val tooLong: Boolean
    open val tooShort: Boolean
    open val rangeUnderflow: Boolean
    open val rangeOverflow: Boolean
    open val stepMismatch: Boolean
    open val badInput: Boolean
    open val customError: Boolean
    open val valid: Boolean
}

public external abstract class HTMLDetailsElement : HTMLElement {
    open var open: Boolean
}

public external abstract class HTMLMenuElement : HTMLElement {
    open var type: String
    open var label: String
    open var compact: Boolean
}

public external abstract class HTMLMenuItemElement : HTMLElement {
    open var type: String
    open var label: String
    open var icon: String
    open var disabled: Boolean
    open var checked: Boolean
    open var radiogroup: String
    open var default: Boolean
}

public external open class RelatedEvent(type: String, eventInitDict: RelatedEventInit = noImpl) : Event {
    open val relatedTarget: EventTarget?
}

public external interface RelatedEventInit : EventInit {
    var relatedTarget: EventTarget? /* = null */
        get() = noImpl
        set(value) = noImpl
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

public external abstract class HTMLDialogElement : HTMLElement {
    open var open: Boolean
    open var returnValue: String
    fun show(anchor: UnionElementOrMouseEvent = noImpl): Unit
    fun showModal(anchor: UnionElementOrMouseEvent = noImpl): Unit
    fun close(returnValue: String = noImpl): Unit
}

public external abstract class HTMLScriptElement : HTMLElement, HTMLOrSVGScriptElement {
    open var src: String
    open var type: String
    open var charset: String
    open var async: Boolean
    open var defer: Boolean
    open var crossOrigin: String?
    open var text: String
    open var nonce: String
    open var event: String
    open var htmlFor: String
}

public external abstract class HTMLTemplateElement : HTMLElement {
    open val content: DocumentFragment
}

public external abstract class HTMLSlotElement : HTMLElement {
    open var name: String
    fun assignedNodes(options: AssignedNodesOptions = noImpl): Array<Node>
}

public external interface AssignedNodesOptions {
    var flatten: Boolean? /* = false */
        get() = noImpl
        set(value) = noImpl
}

@Suppress("NOTHING_TO_INLINE")
public inline fun AssignedNodesOptions(flatten: Boolean? = false): AssignedNodesOptions {
    val o = js("({})")

    o["flatten"] = flatten

    return o
}

public external abstract class HTMLCanvasElement : HTMLElement, TexImageSource {
    open var width: Int
    open var height: Int
    fun getContext(contextId: String, vararg arguments: Any?): RenderingContext?
    fun toDataURL(type: String = noImpl, quality: Any? = noImpl): String
    fun toBlob(_callback: (Blob?) -> Unit, type: String = noImpl, quality: Any? = noImpl): Unit
}

public external interface CanvasRenderingContext2DSettings {
    var alpha: Boolean? /* = true */
        get() = noImpl
        set(value) = noImpl
}

@Suppress("NOTHING_TO_INLINE")
public inline fun CanvasRenderingContext2DSettings(alpha: Boolean? = true): CanvasRenderingContext2DSettings {
    val o = js("({})")

    o["alpha"] = alpha

    return o
}

public external abstract class CanvasRenderingContext2D : CanvasState, CanvasTransform, CanvasCompositing, CanvasImageSmoothing, CanvasFillStrokeStyles, CanvasShadowStyles, CanvasFilters, CanvasRect, CanvasDrawPath, CanvasUserInterface, CanvasText, CanvasDrawImage, CanvasHitRegion, CanvasImageData, CanvasPathDrawingStyles, CanvasTextDrawingStyles, CanvasPath, RenderingContext {
    open val canvas: HTMLCanvasElement
}

public external interface CanvasState {
    fun save(): Unit
    fun restore(): Unit
}

public external interface CanvasTransform {
    fun scale(x: Double, y: Double): Unit
    fun rotate(angle: Double): Unit
    fun translate(x: Double, y: Double): Unit
    fun transform(a: Double, b: Double, c: Double, d: Double, e: Double, f: Double): Unit
    fun getTransform(): DOMMatrix
    fun setTransform(a: Double, b: Double, c: Double, d: Double, e: Double, f: Double): Unit
    fun setTransform(transform: dynamic = noImpl): Unit
    fun resetTransform(): Unit
}

public external interface CanvasCompositing {
    var globalAlpha: Double
    var globalCompositeOperation: String
}

public external interface CanvasImageSmoothing {
    var imageSmoothingEnabled: Boolean
    var imageSmoothingQuality: String
}

public external interface CanvasFillStrokeStyles {
    var strokeStyle: dynamic
    var fillStyle: dynamic
    fun createLinearGradient(x0: Double, y0: Double, x1: Double, y1: Double): CanvasGradient
    fun createRadialGradient(x0: Double, y0: Double, r0: Double, x1: Double, y1: Double, r1: Double): CanvasGradient
    fun createPattern(image: dynamic, repetition: String): CanvasPattern?
}

public external interface CanvasShadowStyles {
    var shadowOffsetX: Double
    var shadowOffsetY: Double
    var shadowBlur: Double
    var shadowColor: String
}

public external interface CanvasFilters {
    var filter: String
}

public external interface CanvasRect {
    fun clearRect(x: Double, y: Double, w: Double, h: Double): Unit
    fun fillRect(x: Double, y: Double, w: Double, h: Double): Unit
    fun strokeRect(x: Double, y: Double, w: Double, h: Double): Unit
}

public external interface CanvasDrawPath {
    fun beginPath(): Unit
    fun fill(fillRule: String = noImpl): Unit
    fun fill(path: Path2D, fillRule: String = noImpl): Unit
    fun stroke(): Unit
    fun stroke(path: Path2D): Unit
    fun clip(fillRule: String = noImpl): Unit
    fun clip(path: Path2D, fillRule: String = noImpl): Unit
    fun resetClip(): Unit
    fun isPointInPath(x: Double, y: Double, fillRule: String = noImpl): Boolean
    fun isPointInPath(path: Path2D, x: Double, y: Double, fillRule: String = noImpl): Boolean
    fun isPointInStroke(x: Double, y: Double): Boolean
    fun isPointInStroke(path: Path2D, x: Double, y: Double): Boolean
}

public external interface CanvasUserInterface {
    fun drawFocusIfNeeded(element: Element): Unit
    fun drawFocusIfNeeded(path: Path2D, element: Element): Unit
    fun scrollPathIntoView(): Unit
    fun scrollPathIntoView(path: Path2D): Unit
}

public external interface CanvasText {
    fun fillText(text: String, x: Double, y: Double, maxWidth: Double = noImpl): Unit
    fun strokeText(text: String, x: Double, y: Double, maxWidth: Double = noImpl): Unit
    fun measureText(text: String): TextMetrics
}

public external interface CanvasDrawImage {
    fun drawImage(image: dynamic, dx: Double, dy: Double): Unit
    fun drawImage(image: dynamic, dx: Double, dy: Double, dw: Double, dh: Double): Unit
    fun drawImage(image: dynamic, sx: Double, sy: Double, sw: Double, sh: Double, dx: Double, dy: Double, dw: Double, dh: Double): Unit
}

public external interface CanvasHitRegion {
    fun addHitRegion(options: HitRegionOptions = noImpl): Unit
    fun removeHitRegion(id: String): Unit
    fun clearHitRegions(): Unit
}

public external interface CanvasImageData {
    fun createImageData(sw: Double, sh: Double): ImageData
    fun createImageData(imagedata: ImageData): ImageData
    fun getImageData(sx: Double, sy: Double, sw: Double, sh: Double): ImageData
    fun putImageData(imagedata: ImageData, dx: Double, dy: Double): Unit
    fun putImageData(imagedata: ImageData, dx: Double, dy: Double, dirtyX: Double, dirtyY: Double, dirtyWidth: Double, dirtyHeight: Double): Unit
}

public external interface CanvasPathDrawingStyles {
    var lineWidth: Double
    var lineCap: String
    var lineJoin: String
    var miterLimit: Double
    var lineDashOffset: Double
    fun setLineDash(segments: Array<Double>): Unit
    fun getLineDash(): Array<Double>
}

public external interface CanvasTextDrawingStyles {
    var font: String
    var textAlign: String
    var textBaseline: String
    var direction: String
}

public external interface CanvasPath {
    fun closePath(): Unit
    fun moveTo(x: Double, y: Double): Unit
    fun lineTo(x: Double, y: Double): Unit
    fun quadraticCurveTo(cpx: Double, cpy: Double, x: Double, y: Double): Unit
    fun bezierCurveTo(cp1x: Double, cp1y: Double, cp2x: Double, cp2y: Double, x: Double, y: Double): Unit
    fun arcTo(x1: Double, y1: Double, x2: Double, y2: Double, radius: Double): Unit
    fun arcTo(x1: Double, y1: Double, x2: Double, y2: Double, radiusX: Double, radiusY: Double, rotation: Double): Unit
    fun rect(x: Double, y: Double, w: Double, h: Double): Unit
    fun arc(x: Double, y: Double, radius: Double, startAngle: Double, endAngle: Double, anticlockwise: Boolean = noImpl): Unit
    fun ellipse(x: Double, y: Double, radiusX: Double, radiusY: Double, rotation: Double, startAngle: Double, endAngle: Double, anticlockwise: Boolean = noImpl): Unit
}

public external abstract class CanvasGradient {
    fun addColorStop(offset: Double, color: String): Unit
}

public external abstract class CanvasPattern {
    fun setTransform(transform: dynamic = noImpl): Unit
}

public external abstract class TextMetrics {
    open val width: Double
    open val actualBoundingBoxLeft: Double
    open val actualBoundingBoxRight: Double
    open val fontBoundingBoxAscent: Double
    open val fontBoundingBoxDescent: Double
    open val actualBoundingBoxAscent: Double
    open val actualBoundingBoxDescent: Double
    open val emHeightAscent: Double
    open val emHeightDescent: Double
    open val hangingBaseline: Double
    open val alphabeticBaseline: Double
    open val ideographicBaseline: Double
}

public external interface HitRegionOptions {
    var path: Path2D? /* = null */
        get() = noImpl
        set(value) = noImpl
    var fillRule: String? /* = "nonzero" */
        get() = noImpl
        set(value) = noImpl
    var id: String? /* = "" */
        get() = noImpl
        set(value) = noImpl
    var parentID: String? /* = null */
        get() = noImpl
        set(value) = noImpl
    var cursor: String? /* = "inherit" */
        get() = noImpl
        set(value) = noImpl
    var control: Element? /* = null */
        get() = noImpl
        set(value) = noImpl
    var label: String? /* = null */
        get() = noImpl
        set(value) = noImpl
    var role: String? /* = null */
        get() = noImpl
        set(value) = noImpl
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

public external open class ImageData : TexImageSource {
    constructor(sw: Int, sh: Int)
    constructor(data: Uint8ClampedArray, sw: Int, sh: Int = noImpl)
    open val width: Int
    open val height: Int
    open val data: Uint8ClampedArray
}

public external open class Path2D() : CanvasPath {
    constructor(path: Path2D)
    constructor(paths: Array<Path2D>, fillRule: String = noImpl)
    constructor(d: String)
    fun addPath(path: Path2D, transform: dynamic = noImpl): Unit
    override fun closePath(): Unit
    override fun moveTo(x: Double, y: Double): Unit
    override fun lineTo(x: Double, y: Double): Unit
    override fun quadraticCurveTo(cpx: Double, cpy: Double, x: Double, y: Double): Unit
    override fun bezierCurveTo(cp1x: Double, cp1y: Double, cp2x: Double, cp2y: Double, x: Double, y: Double): Unit
    override fun arcTo(x1: Double, y1: Double, x2: Double, y2: Double, radius: Double): Unit
    override fun arcTo(x1: Double, y1: Double, x2: Double, y2: Double, radiusX: Double, radiusY: Double, rotation: Double): Unit
    override fun rect(x: Double, y: Double, w: Double, h: Double): Unit
    override fun arc(x: Double, y: Double, radius: Double, startAngle: Double, endAngle: Double, anticlockwise: Boolean /* = noImpl */): Unit
    override fun ellipse(x: Double, y: Double, radiusX: Double, radiusY: Double, rotation: Double, startAngle: Double, endAngle: Double, anticlockwise: Boolean /* = noImpl */): Unit
}

public external abstract class Touch {
    open val region: String?
}

public external abstract class ImageBitmapRenderingContext {
    open val canvas: HTMLCanvasElement
    fun transferFromImageBitmap(bitmap: ImageBitmap?): Unit
}

public external interface ImageBitmapRenderingContextSettings {
    var alpha: Boolean? /* = true */
        get() = noImpl
        set(value) = noImpl
}

@Suppress("NOTHING_TO_INLINE")
public inline fun ImageBitmapRenderingContextSettings(alpha: Boolean? = true): ImageBitmapRenderingContextSettings {
    val o = js("({})")

    o["alpha"] = alpha

    return o
}

public external abstract class CustomElementRegistry {
    fun define(name: String, constructor: () -> dynamic, options: ElementDefinitionOptions = noImpl): Unit
    fun get(name: String): Any?
    fun whenDefined(name: String): dynamic
}

public external interface ElementDefinitionOptions {
    var extends: String?
        get() = noImpl
        set(value) = noImpl
}

@Suppress("NOTHING_TO_INLINE")
public inline fun ElementDefinitionOptions(extends: String? = null): ElementDefinitionOptions {
    val o = js("({})")

    o["extends"] = extends

    return o
}

public external interface ElementContentEditable {
    var contentEditable: String
    val isContentEditable: Boolean
}

public external abstract class DataTransfer {
    open var dropEffect: String
    open var effectAllowed: String
    open val items: DataTransferItemList
    open val types: dynamic
    open val files: FileList
    fun setDragImage(image: Element, x: Int, y: Int): Unit
    fun getData(format: String): String
    fun setData(format: String, data: String): Unit
    fun clearData(format: String = noImpl): Unit
}

public external abstract class DataTransferItemList {
    open val length: Int
    @nativeGetter
    operator fun get(index: Int): DataTransferItem?
    fun add(data: String, type: String): DataTransferItem?
    fun add(data: File): DataTransferItem?
    fun remove(index: Int): Unit
    fun clear(): Unit
}

public external abstract class DataTransferItem {
    open val kind: String
    open val type: String
    fun getAsString(_callback: ((String) -> Unit)?): Unit
    fun getAsFile(): File?
}

public external open class DragEvent(type: String, eventInitDict: DragEventInit = noImpl) : MouseEvent {
    open val dataTransfer: DataTransfer?
}

public external interface DragEventInit : MouseEventInit {
    var dataTransfer: DataTransfer? /* = null */
        get() = noImpl
        set(value) = noImpl
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

public external abstract class BarProp {
    open val visible: Boolean
}

public external abstract class History {
    open val length: Int
    open var scrollRestoration: String
    open val state: Any?
    fun go(delta: Int = noImpl): Unit
    fun back(): Unit
    fun forward(): Unit
    fun pushState(data: Any?, title: String, url: String? = noImpl): Unit
    fun replaceState(data: Any?, title: String, url: String? = noImpl): Unit
}

public external abstract class Location {
    open var href: String
    open val origin: String
    open var protocol: String
    open var host: String
    open var hostname: String
    open var port: String
    open var pathname: String
    open var search: String
    open var hash: String
    open val ancestorOrigins: dynamic
    fun assign(url: String): Unit
    fun replace(url: String): Unit
    fun reload(): Unit
}

public external open class PopStateEvent(type: String, eventInitDict: PopStateEventInit = noImpl) : Event {
    open val state: Any?
}

public external interface PopStateEventInit : EventInit {
    var state: Any? /* = null */
        get() = noImpl
        set(value) = noImpl
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

public external open class HashChangeEvent(type: String, eventInitDict: HashChangeEventInit = noImpl) : Event {
    open val oldURL: String
    open val newURL: String
}

public external interface HashChangeEventInit : EventInit {
    var oldURL: String? /* = "" */
        get() = noImpl
        set(value) = noImpl
    var newURL: String? /* = "" */
        get() = noImpl
        set(value) = noImpl
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

public external open class PageTransitionEvent(type: String, eventInitDict: PageTransitionEventInit = noImpl) : Event {
    open val persisted: Boolean
}

public external interface PageTransitionEventInit : EventInit {
    var persisted: Boolean? /* = false */
        get() = noImpl
        set(value) = noImpl
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

public external open class BeforeUnloadEvent : Event {
    var returnValue: String
}

public external abstract class ApplicationCache : EventTarget {
    open val status: Short
    open var onchecking: ((Event) -> dynamic)?
    open var onerror: ((Event) -> dynamic)?
    open var onnoupdate: ((Event) -> dynamic)?
    open var ondownloading: ((Event) -> dynamic)?
    open var onprogress: ((Event) -> dynamic)?
    open var onupdateready: ((Event) -> dynamic)?
    open var oncached: ((Event) -> dynamic)?
    open var onobsolete: ((Event) -> dynamic)?
    fun update(): Unit
    fun abort(): Unit
    fun swapCache(): Unit

    companion object {
        val UNCACHED: Short
        val IDLE: Short
        val CHECKING: Short
        val DOWNLOADING: Short
        val UPDATEREADY: Short
        val OBSOLETE: Short
    }
}

public external interface NavigatorOnLine {
    val onLine: Boolean
}

public external open class ErrorEvent(type: String, eventInitDict: ErrorEventInit = noImpl) : Event {
    open val message: String
    open val filename: String
    open val lineno: Int
    open val colno: Int
    open val error: Any?
}

public external interface ErrorEventInit : EventInit {
    var message: String? /* = "" */
        get() = noImpl
        set(value) = noImpl
    var filename: String? /* = "" */
        get() = noImpl
        set(value) = noImpl
    var lineno: Int? /* = 0 */
        get() = noImpl
        set(value) = noImpl
    var colno: Int? /* = 0 */
        get() = noImpl
        set(value) = noImpl
    var error: Any? /* = null */
        get() = noImpl
        set(value) = noImpl
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

public external open class PromiseRejectionEvent(type: String, eventInitDict: PromiseRejectionEventInit) : Event {
    open val promise: dynamic
    open val reason: Any?
}

public external interface PromiseRejectionEventInit : EventInit {
    var promise: dynamic
        get() = noImpl
        set(value) = noImpl
    var reason: Any?
        get() = noImpl
        set(value) = noImpl
}

@Suppress("NOTHING_TO_INLINE")
public inline fun PromiseRejectionEventInit(promise: dynamic, reason: Any? = null, bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): PromiseRejectionEventInit {
    val o = js("({})")

    o["promise"] = promise
    o["reason"] = reason
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable
    o["composed"] = composed

    return o
}

public external interface GlobalEventHandlers {
    var onabort: ((Event) -> dynamic)?
    var onblur: ((Event) -> dynamic)?
    var oncancel: ((Event) -> dynamic)?
    var oncanplay: ((Event) -> dynamic)?
    var oncanplaythrough: ((Event) -> dynamic)?
    var onchange: ((Event) -> dynamic)?
    var onclick: ((Event) -> dynamic)?
    var onclose: ((Event) -> dynamic)?
    var oncontextmenu: ((Event) -> dynamic)?
    var oncuechange: ((Event) -> dynamic)?
    var ondblclick: ((Event) -> dynamic)?
    var ondrag: ((Event) -> dynamic)?
    var ondragend: ((Event) -> dynamic)?
    var ondragenter: ((Event) -> dynamic)?
    var ondragexit: ((Event) -> dynamic)?
    var ondragleave: ((Event) -> dynamic)?
    var ondragover: ((Event) -> dynamic)?
    var ondragstart: ((Event) -> dynamic)?
    var ondrop: ((Event) -> dynamic)?
    var ondurationchange: ((Event) -> dynamic)?
    var onemptied: ((Event) -> dynamic)?
    var onended: ((Event) -> dynamic)?
    var onerror: ((dynamic, String, Int, Int, Any?) -> dynamic)?
    var onfocus: ((Event) -> dynamic)?
    var oninput: ((Event) -> dynamic)?
    var oninvalid: ((Event) -> dynamic)?
    var onkeydown: ((Event) -> dynamic)?
    var onkeypress: ((Event) -> dynamic)?
    var onkeyup: ((Event) -> dynamic)?
    var onload: ((Event) -> dynamic)?
    var onloadeddata: ((Event) -> dynamic)?
    var onloadedmetadata: ((Event) -> dynamic)?
    var onloadend: ((Event) -> dynamic)?
    var onloadstart: ((Event) -> dynamic)?
    var onmousedown: ((Event) -> dynamic)?
    var onmouseenter: ((Event) -> dynamic)?
    var onmouseleave: ((Event) -> dynamic)?
    var onmousemove: ((Event) -> dynamic)?
    var onmouseout: ((Event) -> dynamic)?
    var onmouseover: ((Event) -> dynamic)?
    var onmouseup: ((Event) -> dynamic)?
    var onwheel: ((Event) -> dynamic)?
    var onpause: ((Event) -> dynamic)?
    var onplay: ((Event) -> dynamic)?
    var onplaying: ((Event) -> dynamic)?
    var onprogress: ((Event) -> dynamic)?
    var onratechange: ((Event) -> dynamic)?
    var onreset: ((Event) -> dynamic)?
    var onresize: ((Event) -> dynamic)?
    var onscroll: ((Event) -> dynamic)?
    var onseeked: ((Event) -> dynamic)?
    var onseeking: ((Event) -> dynamic)?
    var onselect: ((Event) -> dynamic)?
    var onshow: ((Event) -> dynamic)?
    var onstalled: ((Event) -> dynamic)?
    var onsubmit: ((Event) -> dynamic)?
    var onsuspend: ((Event) -> dynamic)?
    var ontimeupdate: ((Event) -> dynamic)?
    var ontoggle: ((Event) -> dynamic)?
    var onvolumechange: ((Event) -> dynamic)?
    var onwaiting: ((Event) -> dynamic)?
}

public external interface WindowEventHandlers {
    var onafterprint: ((Event) -> dynamic)?
    var onbeforeprint: ((Event) -> dynamic)?
    var onbeforeunload: ((Event) -> String?)?
    var onhashchange: ((Event) -> dynamic)?
    var onlanguagechange: ((Event) -> dynamic)?
    var onmessage: ((Event) -> dynamic)?
    var onoffline: ((Event) -> dynamic)?
    var ononline: ((Event) -> dynamic)?
    var onpagehide: ((Event) -> dynamic)?
    var onpageshow: ((Event) -> dynamic)?
    var onpopstate: ((Event) -> dynamic)?
    var onrejectionhandled: ((Event) -> dynamic)?
    var onstorage: ((Event) -> dynamic)?
    var onunhandledrejection: ((Event) -> dynamic)?
    var onunload: ((Event) -> dynamic)?
}

public external interface DocumentAndElementEventHandlers {
    var oncopy: ((Event) -> dynamic)?
    var oncut: ((Event) -> dynamic)?
    var onpaste: ((Event) -> dynamic)?
}

public external interface WindowOrWorkerGlobalScope {
    val caches: CacheStorage
    val origin: String
    fun fetch(input: dynamic, init: RequestInit = noImpl): dynamic
    fun btoa(data: String): String
    fun atob(data: String): String
    fun setTimeout(handler: dynamic, timeout: Int = noImpl, vararg arguments: Any?): Int
    fun clearTimeout(handle: Int = noImpl): Unit
    fun setInterval(handler: dynamic, timeout: Int = noImpl, vararg arguments: Any?): Int
    fun clearInterval(handle: Int = noImpl): Unit
    fun createImageBitmap(image: dynamic, options: ImageBitmapOptions = noImpl): dynamic
    fun createImageBitmap(image: dynamic, sx: Int, sy: Int, sw: Int, sh: Int, options: ImageBitmapOptions = noImpl): dynamic
}

public external abstract class Navigator : NavigatorID, NavigatorLanguage, NavigatorOnLine, NavigatorContentUtils, NavigatorCookies, NavigatorPlugins, NavigatorConcurrentHardware {
    open val serviceWorker: ServiceWorkerContainer
    fun vibrate(pattern: dynamic): Boolean
}

public external interface NavigatorID {
    val appCodeName: String
    val appName: String
    val appVersion: String
    val platform: String
    val product: String
    val productSub: String
    val userAgent: String
    val vendor: String
    val vendorSub: String
    val oscpu: String
    fun taintEnabled(): Boolean
}

public external interface NavigatorLanguage {
    val language: String
    val languages: dynamic
}

public external interface NavigatorContentUtils {
    fun registerProtocolHandler(scheme: String, url: String, title: String): Unit
    fun registerContentHandler(mimeType: String, url: String, title: String): Unit
    fun isProtocolHandlerRegistered(scheme: String, url: String): String
    fun isContentHandlerRegistered(mimeType: String, url: String): String
    fun unregisterProtocolHandler(scheme: String, url: String): Unit
    fun unregisterContentHandler(mimeType: String, url: String): Unit
}

public external interface NavigatorCookies {
    val cookieEnabled: Boolean
}

public external interface NavigatorPlugins {
    val plugins: PluginArray
    val mimeTypes: MimeTypeArray
    fun javaEnabled(): Boolean
}

public external abstract class PluginArray {
    open val length: Int
    fun refresh(reload: Boolean = noImpl): Unit
    fun item(index: Int): Plugin?
    @nativeGetter
    operator fun get(index: Int): Plugin?
    fun namedItem(name: String): Plugin?
    @nativeGetter
    operator fun get(name: String): Plugin?
}

public external abstract class MimeTypeArray {
    open val length: Int
    fun item(index: Int): MimeType?
    @nativeGetter
    operator fun get(index: Int): MimeType?
    fun namedItem(name: String): MimeType?
    @nativeGetter
    operator fun get(name: String): MimeType?
}

public external abstract class Plugin {
    open val name: String
    open val description: String
    open val filename: String
    open val length: Int
    fun item(index: Int): MimeType?
    @nativeGetter
    operator fun get(index: Int): MimeType?
    fun namedItem(name: String): MimeType?
    @nativeGetter
    operator fun get(name: String): MimeType?
}

public external abstract class MimeType {
    open val type: String
    open val description: String
    open val suffixes: String
    open val enabledPlugin: Plugin
}

public external abstract class ImageBitmap : TexImageSource {
    open val width: Int
    open val height: Int
    fun close(): Unit
}

public external interface ImageBitmapOptions {
    var imageOrientation: String? /* = "none" */
        get() = noImpl
        set(value) = noImpl
    var premultiplyAlpha: String? /* = "default" */
        get() = noImpl
        set(value) = noImpl
    var colorSpaceConversion: String? /* = "default" */
        get() = noImpl
        set(value) = noImpl
    var resizeWidth: Int?
        get() = noImpl
        set(value) = noImpl
    var resizeHeight: Int?
        get() = noImpl
        set(value) = noImpl
    var resizeQuality: String? /* = "low" */
        get() = noImpl
        set(value) = noImpl
}

@Suppress("NOTHING_TO_INLINE")
public inline fun ImageBitmapOptions(imageOrientation: String? = "none", premultiplyAlpha: String? = "default", colorSpaceConversion: String? = "default", resizeWidth: Int? = null, resizeHeight: Int? = null, resizeQuality: String? = "low"): ImageBitmapOptions {
    val o = js("({})")

    o["imageOrientation"] = imageOrientation
    o["premultiplyAlpha"] = premultiplyAlpha
    o["colorSpaceConversion"] = colorSpaceConversion
    o["resizeWidth"] = resizeWidth
    o["resizeHeight"] = resizeHeight
    o["resizeQuality"] = resizeQuality

    return o
}

public external open class MessageEvent(type: String, eventInitDict: MessageEventInit = noImpl) : Event {
    open val data: Any?
    open val origin: String
    open val lastEventId: String
    open val source: UnionMessagePortOrWindow?
    open val ports: dynamic
    fun initMessageEvent(type: String, bubbles: Boolean, cancelable: Boolean, data: Any?, origin: String, lastEventId: String, source: UnionMessagePortOrWindow?, ports: Array<MessagePort>): Unit
}

public external interface MessageEventInit : EventInit {
    var data: Any? /* = null */
        get() = noImpl
        set(value) = noImpl
    var origin: String? /* = "" */
        get() = noImpl
        set(value) = noImpl
    var lastEventId: String? /* = "" */
        get() = noImpl
        set(value) = noImpl
    var source: UnionMessagePortOrWindow? /* = null */
        get() = noImpl
        set(value) = noImpl
    var ports: Array<MessagePort>? /* = arrayOf() */
        get() = noImpl
        set(value) = noImpl
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

public external open class EventSource(url: String, eventSourceInitDict: EventSourceInit = noImpl) : EventTarget {
    open val url: String
    open val withCredentials: Boolean
    open val readyState: Short
    var onopen: ((Event) -> dynamic)?
    var onmessage: ((Event) -> dynamic)?
    var onerror: ((Event) -> dynamic)?
    fun close(): Unit

    companion object {
        val CONNECTING: Short
        val OPEN: Short
        val CLOSED: Short
    }
}

public external interface EventSourceInit {
    var withCredentials: Boolean? /* = false */
        get() = noImpl
        set(value) = noImpl
}

@Suppress("NOTHING_TO_INLINE")
public inline fun EventSourceInit(withCredentials: Boolean? = false): EventSourceInit {
    val o = js("({})")

    o["withCredentials"] = withCredentials

    return o
}

public external open class WebSocket(url: String, protocols: dynamic = noImpl) : EventTarget {
    open val url: String
    open val readyState: Short
    open val bufferedAmount: Int
    var onopen: ((Event) -> dynamic)?
    var onerror: ((Event) -> dynamic)?
    var onclose: ((Event) -> dynamic)?
    open val extensions: String
    open val protocol: String
    var onmessage: ((Event) -> dynamic)?
    var binaryType: String
    fun close(code: Short = noImpl, reason: String = noImpl): Unit
    fun send(data: String): Unit
    fun send(data: Blob): Unit
    fun send(data: ArrayBuffer): Unit
    fun send(data: ArrayBufferView): Unit

    companion object {
        val CONNECTING: Short
        val OPEN: Short
        val CLOSING: Short
        val CLOSED: Short
    }
}

public external open class CloseEvent(type: String, eventInitDict: CloseEventInit = noImpl) : Event {
    open val wasClean: Boolean
    open val code: Short
    open val reason: String
}

public external interface CloseEventInit : EventInit {
    var wasClean: Boolean? /* = false */
        get() = noImpl
        set(value) = noImpl
    var code: Short? /* = 0 */
        get() = noImpl
        set(value) = noImpl
    var reason: String? /* = "" */
        get() = noImpl
        set(value) = noImpl
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

public external open class MessageChannel {
    open val port1: MessagePort
    open val port2: MessagePort
}

public external abstract class MessagePort : EventTarget, UnionMessagePortOrWindow, UnionMessagePortOrServiceWorker, UnionClientOrMessagePortOrServiceWorker {
    open var onmessage: ((Event) -> dynamic)?
    fun postMessage(message: Any?, transfer: Array<dynamic> = noImpl): Unit
    fun start(): Unit
    fun close(): Unit
}

public external open class BroadcastChannel(name: String) : EventTarget {
    open val name: String
    var onmessage: ((Event) -> dynamic)?
    fun postMessage(message: Any?): Unit
    fun close(): Unit
}

public external abstract class WorkerGlobalScope : EventTarget, WindowOrWorkerGlobalScope, GlobalPerformance {
    open val self: WorkerGlobalScope
    open val location: WorkerLocation
    open val navigator: WorkerNavigator
    open var onerror: ((dynamic, String, Int, Int, Any?) -> dynamic)?
    open var onlanguagechange: ((Event) -> dynamic)?
    open var onoffline: ((Event) -> dynamic)?
    open var ononline: ((Event) -> dynamic)?
    open var onrejectionhandled: ((Event) -> dynamic)?
    open var onunhandledrejection: ((Event) -> dynamic)?
    fun importScripts(vararg urls: String): Unit
}

public external abstract class DedicatedWorkerGlobalScope : WorkerGlobalScope {
    open var onmessage: ((Event) -> dynamic)?
    fun postMessage(message: Any?, transfer: Array<dynamic> = noImpl): Unit
    fun close(): Unit
}

public external abstract class SharedWorkerGlobalScope : WorkerGlobalScope {
    open val name: String
    open val applicationCache: ApplicationCache
    open var onconnect: ((Event) -> dynamic)?
    fun close(): Unit
}

public external interface AbstractWorker {
    var onerror: ((Event) -> dynamic)?
}

public external open class Worker(scriptURL: String, options: WorkerOptions = noImpl) : EventTarget, AbstractWorker {
    var onmessage: ((Event) -> dynamic)?
    override var onerror: ((Event) -> dynamic)?
    fun terminate(): Unit
    fun postMessage(message: Any?, transfer: Array<dynamic> = noImpl): Unit
}

public external interface WorkerOptions {
    var type: String? /* = "classic" */
        get() = noImpl
        set(value) = noImpl
    var credentials: String? /* = "omit" */
        get() = noImpl
        set(value) = noImpl
}

@Suppress("NOTHING_TO_INLINE")
public inline fun WorkerOptions(type: String? = "classic", credentials: String? = "omit"): WorkerOptions {
    val o = js("({})")

    o["type"] = type
    o["credentials"] = credentials

    return o
}

public external open class SharedWorker(scriptURL: String, name: String = noImpl, options: WorkerOptions = noImpl) : EventTarget, AbstractWorker {
    open val port: MessagePort
    override var onerror: ((Event) -> dynamic)?
}

public external interface NavigatorConcurrentHardware {
    val hardwareConcurrency: Int
}

public external abstract class WorkerNavigator : NavigatorID, NavigatorLanguage, NavigatorOnLine, NavigatorConcurrentHardware {
    open val serviceWorker: ServiceWorkerContainer
}

public external abstract class WorkerLocation {
    open var href: String
    open val origin: String
    open val protocol: String
    open val host: String
    open val hostname: String
    open val port: String
    open val pathname: String
    open val search: String
    open val hash: String
}

public external abstract class Storage {
    open val length: Int
    fun key(index: Int): String?
    fun getItem(key: String): String?
    @nativeGetter
    operator fun get(key: String): String?
    fun setItem(key: String, value: String): Unit
    @nativeSetter
    operator fun set(key: String, value: String): Unit
    fun removeItem(key: String): Unit
    fun clear(): Unit
}

public external interface WindowSessionStorage {
    val sessionStorage: Storage
}

public external interface WindowLocalStorage {
    val localStorage: Storage
}

public external open class StorageEvent(type: String, eventInitDict: StorageEventInit = noImpl) : Event {
    open val key: String?
    open val oldValue: String?
    open val newValue: String?
    open val url: String
    open val storageArea: Storage?
}

public external interface StorageEventInit : EventInit {
    var key: String? /* = null */
        get() = noImpl
        set(value) = noImpl
    var oldValue: String? /* = null */
        get() = noImpl
        set(value) = noImpl
    var newValue: String? /* = null */
        get() = noImpl
        set(value) = noImpl
    var url: String? /* = "" */
        get() = noImpl
        set(value) = noImpl
    var storageArea: Storage? /* = null */
        get() = noImpl
        set(value) = noImpl
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

public external abstract class HTMLAppletElement : HTMLElement {
    open var align: String
    open var alt: String
    open var archive: String
    open var code: String
    open var codeBase: String
    open var height: String
    open var hspace: Int
    open var name: String
    open var _object: String
    open var vspace: Int
    open var width: String
}

public external abstract class HTMLMarqueeElement : HTMLElement {
    open var behavior: String
    open var bgColor: String
    open var direction: String
    open var height: String
    open var hspace: Int
    open var loop: Int
    open var scrollAmount: Int
    open var scrollDelay: Int
    open var trueSpeed: Boolean
    open var vspace: Int
    open var width: String
    open var onbounce: ((Event) -> dynamic)?
    open var onfinish: ((Event) -> dynamic)?
    open var onstart: ((Event) -> dynamic)?
    fun start(): Unit
    fun stop(): Unit
}

public external abstract class HTMLFrameSetElement : HTMLElement, WindowEventHandlers {
    open var cols: String
    open var rows: String
}

public external abstract class HTMLFrameElement : HTMLElement {
    open var name: String
    open var scrolling: String
    open var src: String
    open var frameBorder: String
    open var longDesc: String
    open var noResize: Boolean
    open val contentDocument: Document?
    open val contentWindow: Window?
    open var marginHeight: String
    open var marginWidth: String
}

public external abstract class HTMLDirectoryElement : HTMLElement {
    open var compact: Boolean
}

public external abstract class HTMLFontElement : HTMLElement {
    open var color: String
    open var face: String
    open var size: String
}

public external interface External {
    fun AddSearchProvider(): Unit
    fun IsSearchProviderInstalled(): Unit
}

public external interface EventInit {
    var bubbles: Boolean? /* = false */
        get() = noImpl
        set(value) = noImpl
    var cancelable: Boolean? /* = false */
        get() = noImpl
        set(value) = noImpl
    var composed: Boolean? /* = false */
        get() = noImpl
        set(value) = noImpl
}

@Suppress("NOTHING_TO_INLINE")
public inline fun EventInit(bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): EventInit {
    val o = js("({})")

    o["bubbles"] = bubbles
    o["cancelable"] = cancelable
    o["composed"] = composed

    return o
}

public external open class CustomEvent(type: String, eventInitDict: CustomEventInit = noImpl) : Event {
    open val detail: Any?
    fun initCustomEvent(type: String, bubbles: Boolean, cancelable: Boolean, detail: Any?): Unit
}

public external interface CustomEventInit : EventInit {
    var detail: Any? /* = null */
        get() = noImpl
        set(value) = noImpl
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

public external interface EventListenerOptions {
    var capture: Boolean? /* = false */
        get() = noImpl
        set(value) = noImpl
}

@Suppress("NOTHING_TO_INLINE")
public inline fun EventListenerOptions(capture: Boolean? = false): EventListenerOptions {
    val o = js("({})")

    o["capture"] = capture

    return o
}

public external interface AddEventListenerOptions : EventListenerOptions {
    var passive: Boolean? /* = false */
        get() = noImpl
        set(value) = noImpl
    var once: Boolean? /* = false */
        get() = noImpl
        set(value) = noImpl
}

@Suppress("NOTHING_TO_INLINE")
public inline fun AddEventListenerOptions(passive: Boolean? = false, once: Boolean? = false, capture: Boolean? = false): AddEventListenerOptions {
    val o = js("({})")

    o["passive"] = passive
    o["once"] = once
    o["capture"] = capture

    return o
}

public external interface NonElementParentNode {
    fun getElementById(elementId: String): Element?
}

public external interface DocumentOrShadowRoot {
    val fullscreenElement: Element?
}

public external interface ParentNode {
    val children: HTMLCollection
    val firstElementChild: Element?
    val lastElementChild: Element?
    val childElementCount: Int
    fun prepend(vararg nodes: dynamic): Unit
    fun append(vararg nodes: dynamic): Unit
    fun querySelector(selectors: String): Element?
    fun querySelectorAll(selectors: String): NodeList
}

public external interface NonDocumentTypeChildNode {
    val previousElementSibling: Element?
    val nextElementSibling: Element?
}

public external interface ChildNode {
    fun before(vararg nodes: dynamic): Unit
    fun after(vararg nodes: dynamic): Unit
    fun replaceWith(vararg nodes: dynamic): Unit
    fun remove(): Unit
}

public external interface Slotable {
    val assignedSlot: HTMLSlotElement?
}

public external abstract class NodeList {
    open val length: Int
    fun item(index: Int): Node?
    @nativeGetter
    operator fun get(index: Int): Node?
}

public external abstract class HTMLCollection : UnionElementOrHTMLCollection {
    open val length: Int
    fun item(index: Int): Element?
    @nativeGetter
    operator fun get(index: Int): Element?
    fun namedItem(name: String): Element?
    @nativeGetter
    operator fun get(name: String): Element?
}

public external open class MutationObserver(callback: (Array<MutationRecord>, MutationObserver) -> Unit) {
    fun observe(target: Node, options: MutationObserverInit = noImpl): Unit
    fun disconnect(): Unit
    fun takeRecords(): Array<MutationRecord>
}

public external interface MutationObserverInit {
    var childList: Boolean? /* = false */
        get() = noImpl
        set(value) = noImpl
    var attributes: Boolean?
        get() = noImpl
        set(value) = noImpl
    var characterData: Boolean?
        get() = noImpl
        set(value) = noImpl
    var subtree: Boolean? /* = false */
        get() = noImpl
        set(value) = noImpl
    var attributeOldValue: Boolean?
        get() = noImpl
        set(value) = noImpl
    var characterDataOldValue: Boolean?
        get() = noImpl
        set(value) = noImpl
    var attributeFilter: Array<String>?
        get() = noImpl
        set(value) = noImpl
}

@Suppress("NOTHING_TO_INLINE")
public inline fun MutationObserverInit(childList: Boolean? = false, attributes: Boolean? = null, characterData: Boolean? = null, subtree: Boolean? = false, attributeOldValue: Boolean? = null, characterDataOldValue: Boolean? = null, attributeFilter: Array<String>? = null): MutationObserverInit {
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

public external abstract class MutationRecord {
    open val type: String
    open val target: Node
    open val addedNodes: NodeList
    open val removedNodes: NodeList
    open val previousSibling: Node?
    open val nextSibling: Node?
    open val attributeName: String?
    open val attributeNamespace: String?
    open val oldValue: String?
}

public external abstract class Node : EventTarget {
    open val nodeType: Short
    open val nodeName: String
    open val baseURI: String
    open val isConnected: Boolean
    open val ownerDocument: Document?
    open val parentNode: Node?
    open val parentElement: Element?
    open val childNodes: NodeList
    open val firstChild: Node?
    open val lastChild: Node?
    open val previousSibling: Node?
    open val nextSibling: Node?
    open var nodeValue: String?
    open var textContent: String?
    fun getRootNode(options: GetRootNodeOptions = noImpl): Node
    fun hasChildNodes(): Boolean
    fun normalize(): Unit
    fun cloneNode(deep: Boolean = noImpl): Node
    fun isEqualNode(otherNode: Node?): Boolean
    fun isSameNode(otherNode: Node?): Boolean
    fun compareDocumentPosition(other: Node): Short
    fun contains(other: Node?): Boolean
    fun lookupPrefix(namespace: String?): String?
    fun lookupNamespaceURI(prefix: String?): String?
    fun isDefaultNamespace(namespace: String?): Boolean
    fun insertBefore(node: Node, child: Node?): Node
    fun appendChild(node: Node): Node
    fun replaceChild(node: Node, child: Node): Node
    fun removeChild(child: Node): Node

    companion object {
        val ELEMENT_NODE: Short
        val ATTRIBUTE_NODE: Short
        val TEXT_NODE: Short
        val CDATA_SECTION_NODE: Short
        val ENTITY_REFERENCE_NODE: Short
        val ENTITY_NODE: Short
        val PROCESSING_INSTRUCTION_NODE: Short
        val COMMENT_NODE: Short
        val DOCUMENT_NODE: Short
        val DOCUMENT_TYPE_NODE: Short
        val DOCUMENT_FRAGMENT_NODE: Short
        val NOTATION_NODE: Short
        val DOCUMENT_POSITION_DISCONNECTED: Short
        val DOCUMENT_POSITION_PRECEDING: Short
        val DOCUMENT_POSITION_FOLLOWING: Short
        val DOCUMENT_POSITION_CONTAINS: Short
        val DOCUMENT_POSITION_CONTAINED_BY: Short
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

public external interface GetRootNodeOptions {
    var composed: Boolean? /* = false */
        get() = noImpl
        set(value) = noImpl
}

@Suppress("NOTHING_TO_INLINE")
public inline fun GetRootNodeOptions(composed: Boolean? = false): GetRootNodeOptions {
    val o = js("({})")

    o["composed"] = composed

    return o
}

public external open class XMLDocument : Document {
    override fun getElementById(elementId: String): Element?
    override fun prepend(vararg nodes: dynamic): Unit
    override fun append(vararg nodes: dynamic): Unit
    override fun querySelector(selectors: String): Element?
    override fun querySelectorAll(selectors: String): NodeList
    override fun getBoxQuads(options: BoxQuadOptions /* = noImpl */): Array<DOMQuad>
    override fun convertQuadFromNode(quad: dynamic, from: dynamic, options: ConvertCoordinateOptions /* = noImpl */): DOMQuad
    override fun convertRectFromNode(rect: DOMRectReadOnly, from: dynamic, options: ConvertCoordinateOptions /* = noImpl */): DOMQuad
    override fun convertPointFromNode(point: DOMPointInit, from: dynamic, options: ConvertCoordinateOptions /* = noImpl */): DOMPoint
}

public external interface ElementCreationOptions {
    @JsName("is") var is_: String?
        get() = noImpl
        set(value) = noImpl
}

@Suppress("NOTHING_TO_INLINE")
public inline fun ElementCreationOptions(is_: String? = null): ElementCreationOptions {
    val o = js("({})")

    o["is"] = is_

    return o
}

public external abstract class DOMImplementation {
    fun createDocumentType(qualifiedName: String, publicId: String, systemId: String): DocumentType
    fun createDocument(namespace: String?, qualifiedName: String, doctype: DocumentType? = noImpl): XMLDocument
    fun createHTMLDocument(title: String = noImpl): Document
    fun hasFeature(): Boolean
}

public external abstract class DocumentType : Node, ChildNode {
    open val name: String
    open val publicId: String
    open val systemId: String
}

public external open class DocumentFragment : Node, NonElementParentNode, ParentNode {
    override val children: HTMLCollection
    override val firstElementChild: Element?
    override val lastElementChild: Element?
    override val childElementCount: Int
    override fun getElementById(elementId: String): Element?
    override fun prepend(vararg nodes: dynamic): Unit
    override fun append(vararg nodes: dynamic): Unit
    override fun querySelector(selectors: String): Element?
    override fun querySelectorAll(selectors: String): NodeList
}

public external open class ShadowRoot : DocumentFragment, DocumentOrShadowRoot {
    open val mode: String
    open val host: Element
    override val fullscreenElement: Element?
    override fun getElementById(elementId: String): Element?
    override fun prepend(vararg nodes: dynamic): Unit
    override fun append(vararg nodes: dynamic): Unit
    override fun querySelector(selectors: String): Element?
    override fun querySelectorAll(selectors: String): NodeList
}

public external abstract class Element : Node, ParentNode, NonDocumentTypeChildNode, ChildNode, Slotable, GeometryUtils, UnionElementOrProcessingInstruction, UnionElementOrHTMLCollection, UnionElementOrRadioNodeList, UnionElementOrMouseEvent {
    open var innerHTML: String
    open var outerHTML: String
    open val namespaceURI: String?
    open val prefix: String?
    open val localName: String
    open val tagName: String
    open var id: String
    open var className: String
    open val classList: DOMTokenList
    open var slot: String
    open val attributes: NamedNodeMap
    open val shadowRoot: ShadowRoot?
    open var scrollTop: Double
    open var scrollLeft: Double
    open val scrollWidth: Int
    open val scrollHeight: Int
    open val clientTop: Int
    open val clientLeft: Int
    open val clientWidth: Int
    open val clientHeight: Int
    fun requestFullscreen(): dynamic
    fun insertAdjacentHTML(position: String, text: String): Unit
    fun hasAttributes(): Boolean
    fun getAttributeNames(): Array<String>
    fun getAttribute(qualifiedName: String): String?
    fun getAttributeNS(namespace: String?, localName: String): String?
    fun setAttribute(qualifiedName: String, value: String): Unit
    fun setAttributeNS(namespace: String?, qualifiedName: String, value: String): Unit
    fun removeAttribute(qualifiedName: String): Unit
    fun removeAttributeNS(namespace: String?, localName: String): Unit
    fun hasAttribute(qualifiedName: String): Boolean
    fun hasAttributeNS(namespace: String?, localName: String): Boolean
    fun getAttributeNode(qualifiedName: String): Attr?
    fun getAttributeNodeNS(namespace: String?, localName: String): Attr?
    fun setAttributeNode(attr: Attr): Attr?
    fun setAttributeNodeNS(attr: Attr): Attr?
    fun removeAttributeNode(attr: Attr): Attr
    fun attachShadow(init: ShadowRootInit): ShadowRoot
    fun closest(selectors: String): Element?
    fun matches(selectors: String): Boolean
    fun webkitMatchesSelector(selectors: String): Boolean
    fun getElementsByTagName(qualifiedName: String): HTMLCollection
    fun getElementsByTagNameNS(namespace: String?, localName: String): HTMLCollection
    fun getElementsByClassName(classNames: String): HTMLCollection
    fun insertAdjacentElement(where: String, element: Element): Element?
    fun insertAdjacentText(where: String, data: String): Unit
    fun getClientRects(): Array<DOMRect>
    fun getBoundingClientRect(): DOMRect
    fun scrollIntoView(): Unit
    fun scrollIntoView(arg: dynamic): Unit
    fun scroll(options: ScrollToOptions = noImpl): Unit
    fun scroll(x: Double, y: Double): Unit
    fun scrollTo(options: ScrollToOptions = noImpl): Unit
    fun scrollTo(x: Double, y: Double): Unit
    fun scrollBy(options: ScrollToOptions = noImpl): Unit
    fun scrollBy(x: Double, y: Double): Unit
}

public external interface ShadowRootInit {
    var mode: String?
        get() = noImpl
        set(value) = noImpl
}

@Suppress("NOTHING_TO_INLINE")
public inline fun ShadowRootInit(mode: String?): ShadowRootInit {
    val o = js("({})")

    o["mode"] = mode

    return o
}

public external abstract class NamedNodeMap {
    open val length: Int
    fun item(index: Int): Attr?
    @nativeGetter
    operator fun get(index: Int): Attr?
    fun getNamedItem(qualifiedName: String): Attr?
    @nativeGetter
    operator fun get(qualifiedName: String): Attr?
    fun getNamedItemNS(namespace: String?, localName: String): Attr?
    fun setNamedItem(attr: Attr): Attr?
    fun setNamedItemNS(attr: Attr): Attr?
    fun removeNamedItem(qualifiedName: String): Attr
    fun removeNamedItemNS(namespace: String?, localName: String): Attr
}

public external abstract class Attr : Node {
    open val namespaceURI: String?
    open val prefix: String?
    open val localName: String
    open val name: String
    open var value: String
    open val ownerElement: Element?
    open val specified: Boolean
}

public external abstract class CharacterData : Node, NonDocumentTypeChildNode, ChildNode {
    open var data: String
    open val length: Int
    fun substringData(offset: Int, count: Int): String
    fun appendData(data: String): Unit
    fun insertData(offset: Int, data: String): Unit
    fun deleteData(offset: Int, count: Int): Unit
    fun replaceData(offset: Int, count: Int, data: String): Unit
}

public external open class Text(data: String = noImpl) : CharacterData, Slotable, GeometryUtils {
    open val wholeText: String
    override val previousElementSibling: Element?
    override val nextElementSibling: Element?
    override val assignedSlot: HTMLSlotElement?
    fun splitText(offset: Int): Text
    override fun before(vararg nodes: dynamic): Unit
    override fun after(vararg nodes: dynamic): Unit
    override fun replaceWith(vararg nodes: dynamic): Unit
    override fun remove(): Unit
    override fun getBoxQuads(options: BoxQuadOptions /* = noImpl */): Array<DOMQuad>
    override fun convertQuadFromNode(quad: dynamic, from: dynamic, options: ConvertCoordinateOptions /* = noImpl */): DOMQuad
    override fun convertRectFromNode(rect: DOMRectReadOnly, from: dynamic, options: ConvertCoordinateOptions /* = noImpl */): DOMQuad
    override fun convertPointFromNode(point: DOMPointInit, from: dynamic, options: ConvertCoordinateOptions /* = noImpl */): DOMPoint
}

public external open class CDATASection : Text {
    override fun before(vararg nodes: dynamic): Unit
    override fun after(vararg nodes: dynamic): Unit
    override fun replaceWith(vararg nodes: dynamic): Unit
    override fun remove(): Unit
    override fun getBoxQuads(options: BoxQuadOptions /* = noImpl */): Array<DOMQuad>
    override fun convertQuadFromNode(quad: dynamic, from: dynamic, options: ConvertCoordinateOptions /* = noImpl */): DOMQuad
    override fun convertRectFromNode(rect: DOMRectReadOnly, from: dynamic, options: ConvertCoordinateOptions /* = noImpl */): DOMQuad
    override fun convertPointFromNode(point: DOMPointInit, from: dynamic, options: ConvertCoordinateOptions /* = noImpl */): DOMPoint
}

public external abstract class ProcessingInstruction : CharacterData, LinkStyle, UnionElementOrProcessingInstruction {
    open val target: String
}

public external open class Comment(data: String = noImpl) : CharacterData {
    override val previousElementSibling: Element?
    override val nextElementSibling: Element?
    override fun before(vararg nodes: dynamic): Unit
    override fun after(vararg nodes: dynamic): Unit
    override fun replaceWith(vararg nodes: dynamic): Unit
    override fun remove(): Unit
}

public external open class Range {
    open val startContainer: Node
    open val startOffset: Int
    open val endContainer: Node
    open val endOffset: Int
    open val collapsed: Boolean
    open val commonAncestorContainer: Node
    fun createContextualFragment(fragment: String): DocumentFragment
    fun setStart(node: Node, offset: Int): Unit
    fun setEnd(node: Node, offset: Int): Unit
    fun setStartBefore(node: Node): Unit
    fun setStartAfter(node: Node): Unit
    fun setEndBefore(node: Node): Unit
    fun setEndAfter(node: Node): Unit
    fun collapse(toStart: Boolean = noImpl): Unit
    fun selectNode(node: Node): Unit
    fun selectNodeContents(node: Node): Unit
    fun compareBoundaryPoints(how: Short, sourceRange: Range): Short
    fun deleteContents(): Unit
    fun extractContents(): DocumentFragment
    fun cloneContents(): DocumentFragment
    fun insertNode(node: Node): Unit
    fun surroundContents(newParent: Node): Unit
    fun cloneRange(): Range
    fun detach(): Unit
    fun isPointInRange(node: Node, offset: Int): Boolean
    fun comparePoint(node: Node, offset: Int): Short
    fun intersectsNode(node: Node): Boolean
    fun getClientRects(): Array<DOMRect>
    fun getBoundingClientRect(): DOMRect

    companion object {
        val START_TO_START: Short
        val START_TO_END: Short
        val END_TO_END: Short
        val END_TO_START: Short
    }
}

public external abstract class NodeIterator {
    open val root: Node
    open val referenceNode: Node
    open val pointerBeforeReferenceNode: Boolean
    open val whatToShow: Int
    open val filter: NodeFilter?
    fun nextNode(): Node?
    fun previousNode(): Node?
    fun detach(): Unit
}

public external abstract class TreeWalker {
    open val root: Node
    open val whatToShow: Int
    open val filter: NodeFilter?
    open var currentNode: Node
    fun parentNode(): Node?
    fun firstChild(): Node?
    fun lastChild(): Node?
    fun previousSibling(): Node?
    fun nextSibling(): Node?
    fun previousNode(): Node?
    fun nextNode(): Node?
}

public external interface NodeFilter {
    fun acceptNode(node: Node): Short

    companion object {
        val FILTER_ACCEPT: Short
        val FILTER_REJECT: Short
        val FILTER_SKIP: Short
        val SHOW_ALL: Int
        val SHOW_ELEMENT: Int
        val SHOW_ATTRIBUTE: Int
        val SHOW_TEXT: Int
        val SHOW_CDATA_SECTION: Int
        val SHOW_ENTITY_REFERENCE: Int
        val SHOW_ENTITY: Int
        val SHOW_PROCESSING_INSTRUCTION: Int
        val SHOW_COMMENT: Int
        val SHOW_DOCUMENT: Int
        val SHOW_DOCUMENT_TYPE: Int
        val SHOW_DOCUMENT_FRAGMENT: Int
        val SHOW_NOTATION: Int
    }
}

public external abstract class DOMTokenList {
    open val length: Int
    open var value: String
    fun item(index: Int): String?
    @nativeGetter
    operator fun get(index: Int): String?
    fun contains(token: String): Boolean
    fun add(vararg tokens: String): Unit
    fun remove(vararg tokens: String): Unit
    fun toggle(token: String, force: Boolean = noImpl): Boolean
    fun replace(token: String, newToken: String): Unit
    fun supports(token: String): Boolean
}

public external open class DOMPointReadOnly(x: Double, y: Double, z: Double, w: Double) {
    open val x: Double
    open val y: Double
    open val z: Double
    open val w: Double
    fun matrixTransform(matrix: DOMMatrixReadOnly): DOMPoint
}

public external open class DOMPoint : DOMPointReadOnly {
    constructor(point: DOMPointInit)
    constructor(x: Double = noImpl, y: Double = noImpl, z: Double = noImpl, w: Double = noImpl)
    override var x: Double
    override var y: Double
    override var z: Double
    override var w: Double
}

public external interface DOMPointInit {
    var x: Double? /* = 0.0 */
        get() = noImpl
        set(value) = noImpl
    var y: Double? /* = 0.0 */
        get() = noImpl
        set(value) = noImpl
    var z: Double? /* = 0.0 */
        get() = noImpl
        set(value) = noImpl
    var w: Double? /* = 1.0 */
        get() = noImpl
        set(value) = noImpl
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

public external open class DOMRect(x: Double = noImpl, y: Double = noImpl, width: Double = noImpl, height: Double = noImpl) : DOMRectReadOnly {
    override var x: Double
    override var y: Double
    override var width: Double
    override var height: Double
}

public external open class DOMRectReadOnly(x: Double, y: Double, width: Double, height: Double) {
    open val x: Double
    open val y: Double
    open val width: Double
    open val height: Double
    open val top: Double
    open val right: Double
    open val bottom: Double
    open val left: Double
}

public external interface DOMRectInit {
    var x: Double? /* = 0.0 */
        get() = noImpl
        set(value) = noImpl
    var y: Double? /* = 0.0 */
        get() = noImpl
        set(value) = noImpl
    var width: Double? /* = 0.0 */
        get() = noImpl
        set(value) = noImpl
    var height: Double? /* = 0.0 */
        get() = noImpl
        set(value) = noImpl
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

public external interface DOMRectList {
    val length: Int
    fun item(index: Int): DOMRect?
    @nativeGetter
    operator fun get(index: Int): DOMRect?
}

public external open class DOMQuad {
    constructor(p1: DOMPointInit = noImpl, p2: DOMPointInit = noImpl, p3: DOMPointInit = noImpl, p4: DOMPointInit = noImpl)
    constructor(rect: DOMRectInit)
    open val p1: DOMPoint
    open val p2: DOMPoint
    open val p3: DOMPoint
    open val p4: DOMPoint
    open val bounds: DOMRectReadOnly
}

public external open class DOMMatrixReadOnly(numberSequence: Array<Double>) {
    open val a: Double
    open val b: Double
    open val c: Double
    open val d: Double
    open val e: Double
    open val f: Double
    open val m11: Double
    open val m12: Double
    open val m13: Double
    open val m14: Double
    open val m21: Double
    open val m22: Double
    open val m23: Double
    open val m24: Double
    open val m31: Double
    open val m32: Double
    open val m33: Double
    open val m34: Double
    open val m41: Double
    open val m42: Double
    open val m43: Double
    open val m44: Double
    open val is2D: Boolean
    open val isIdentity: Boolean
    fun translate(tx: Double, ty: Double, tz: Double = noImpl): DOMMatrix
    fun scale(scale: Double, originX: Double = noImpl, originY: Double = noImpl): DOMMatrix
    fun scale3d(scale: Double, originX: Double = noImpl, originY: Double = noImpl, originZ: Double = noImpl): DOMMatrix
    fun scaleNonUniform(scaleX: Double, scaleY: Double = noImpl, scaleZ: Double = noImpl, originX: Double = noImpl, originY: Double = noImpl, originZ: Double = noImpl): DOMMatrix
    fun rotate(angle: Double, originX: Double = noImpl, originY: Double = noImpl): DOMMatrix
    fun rotateFromVector(x: Double, y: Double): DOMMatrix
    fun rotateAxisAngle(x: Double, y: Double, z: Double, angle: Double): DOMMatrix
    fun skewX(sx: Double): DOMMatrix
    fun skewY(sy: Double): DOMMatrix
    fun multiply(other: DOMMatrix): DOMMatrix
    fun flipX(): DOMMatrix
    fun flipY(): DOMMatrix
    fun inverse(): DOMMatrix
    fun transformPoint(point: DOMPointInit = noImpl): DOMPoint
    fun toFloat32Array(): Float32Array
    fun toFloat64Array(): Float64Array
}

public external open class DOMMatrix() : DOMMatrixReadOnly {
    constructor(transformList: String)
    constructor(other: DOMMatrixReadOnly)
    constructor(array32: Float32Array)
    constructor(array64: Float64Array)
    constructor(numberSequence: Array<Double>)
    override var a: Double
    override var b: Double
    override var c: Double
    override var d: Double
    override var e: Double
    override var f: Double
    override var m11: Double
    override var m12: Double
    override var m13: Double
    override var m14: Double
    override var m21: Double
    override var m22: Double
    override var m23: Double
    override var m24: Double
    override var m31: Double
    override var m32: Double
    override var m33: Double
    override var m34: Double
    override var m41: Double
    override var m42: Double
    override var m43: Double
    override var m44: Double
    fun multiplySelf(other: DOMMatrix): DOMMatrix
    fun preMultiplySelf(other: DOMMatrix): DOMMatrix
    fun translateSelf(tx: Double, ty: Double, tz: Double = noImpl): DOMMatrix
    fun scaleSelf(scale: Double, originX: Double = noImpl, originY: Double = noImpl): DOMMatrix
    fun scale3dSelf(scale: Double, originX: Double = noImpl, originY: Double = noImpl, originZ: Double = noImpl): DOMMatrix
    fun scaleNonUniformSelf(scaleX: Double, scaleY: Double = noImpl, scaleZ: Double = noImpl, originX: Double = noImpl, originY: Double = noImpl, originZ: Double = noImpl): DOMMatrix
    fun rotateSelf(angle: Double, originX: Double = noImpl, originY: Double = noImpl): DOMMatrix
    fun rotateFromVectorSelf(x: Double, y: Double): DOMMatrix
    fun rotateAxisAngleSelf(x: Double, y: Double, z: Double, angle: Double): DOMMatrix
    fun skewXSelf(sx: Double): DOMMatrix
    fun skewYSelf(sy: Double): DOMMatrix
    fun invertSelf(): DOMMatrix
    fun setMatrixValue(transformList: String): DOMMatrix
}

public external interface ScrollOptions {
    var behavior: String? /* = "auto" */
        get() = noImpl
        set(value) = noImpl
}

@Suppress("NOTHING_TO_INLINE")
public inline fun ScrollOptions(behavior: String? = "auto"): ScrollOptions {
    val o = js("({})")

    o["behavior"] = behavior

    return o
}

public external interface ScrollToOptions : ScrollOptions {
    var left: Double?
        get() = noImpl
        set(value) = noImpl
    var top: Double?
        get() = noImpl
        set(value) = noImpl
}

@Suppress("NOTHING_TO_INLINE")
public inline fun ScrollToOptions(left: Double? = null, top: Double? = null, behavior: String? = "auto"): ScrollToOptions {
    val o = js("({})")

    o["left"] = left
    o["top"] = top
    o["behavior"] = behavior

    return o
}

public external abstract class MediaQueryList : EventTarget {
    open val media: String
    open val matches: Boolean
    open var onchange: ((Event) -> dynamic)?
    fun addListener(listener: EventListener?): Unit
    fun addListener(listener: ((Event) -> Unit)?): Unit
    fun removeListener(listener: EventListener?): Unit
    fun removeListener(listener: ((Event) -> Unit)?): Unit
}

public external open class MediaQueryListEvent(type: String, eventInitDict: MediaQueryListEventInit = noImpl) : Event {
    open val media: String
    open val matches: Boolean
}

public external interface MediaQueryListEventInit : EventInit {
    var media: String? /* = "" */
        get() = noImpl
        set(value) = noImpl
    var matches: Boolean? /* = false */
        get() = noImpl
        set(value) = noImpl
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

public external abstract class Screen {
    open val availWidth: Int
    open val availHeight: Int
    open val width: Int
    open val height: Int
    open val colorDepth: Int
    open val pixelDepth: Int
}

public external abstract class CaretPosition {
    open val offsetNode: Node
    open val offset: Int
    fun getClientRect(): DOMRect?
}

public external interface ScrollIntoViewOptions : ScrollOptions {
    var block: String? /* = "center" */
        get() = noImpl
        set(value) = noImpl
    var inline: String? /* = "center" */
        get() = noImpl
        set(value) = noImpl
}

@Suppress("NOTHING_TO_INLINE")
public inline fun ScrollIntoViewOptions(block: String? = "center", inline: String? = "center", behavior: String? = "auto"): ScrollIntoViewOptions {
    val o = js("({})")

    o["block"] = block
    o["inline"] = inline
    o["behavior"] = behavior

    return o
}

public external interface BoxQuadOptions {
    var box: String? /* = "border" */
        get() = noImpl
        set(value) = noImpl
    var relativeTo: dynamic
        get() = noImpl
        set(value) = noImpl
}

@Suppress("NOTHING_TO_INLINE")
public inline fun BoxQuadOptions(box: String? = "border", relativeTo: dynamic = null): BoxQuadOptions {
    val o = js("({})")

    o["box"] = box
    o["relativeTo"] = relativeTo

    return o
}

public external interface ConvertCoordinateOptions {
    var fromBox: String? /* = "border" */
        get() = noImpl
        set(value) = noImpl
    var toBox: String? /* = "border" */
        get() = noImpl
        set(value) = noImpl
}

@Suppress("NOTHING_TO_INLINE")
public inline fun ConvertCoordinateOptions(fromBox: String? = "border", toBox: String? = "border"): ConvertCoordinateOptions {
    val o = js("({})")

    o["fromBox"] = fromBox
    o["toBox"] = toBox

    return o
}

public external interface GeometryUtils {
    fun getBoxQuads(options: BoxQuadOptions = noImpl): Array<DOMQuad>
    fun convertQuadFromNode(quad: dynamic, from: dynamic, options: ConvertCoordinateOptions = noImpl): DOMQuad
    fun convertRectFromNode(rect: DOMRectReadOnly, from: dynamic, options: ConvertCoordinateOptions = noImpl): DOMQuad
    fun convertPointFromNode(point: DOMPointInit, from: dynamic, options: ConvertCoordinateOptions = noImpl): DOMPoint
}

public external @marker interface UnionElementOrProcessingInstruction {
}

public external @marker interface UnionElementOrHTMLCollection {
}

public external @marker interface UnionElementOrRadioNodeList {
}

public external @marker interface UnionHTMLOptGroupElementOrHTMLOptionElement {
}

public external @marker interface UnionAudioTrackOrTextTrackOrVideoTrack {
}

public external @marker interface UnionElementOrMouseEvent {
}

public external @marker interface UnionMessagePortOrWindow {
}

public external @marker interface UnionMessagePortOrServiceWorker {
}

public external @marker interface HTMLOrSVGScriptElement {
}

public external @marker interface RenderingContext {
}

public external @marker interface HTMLOrSVGImageElement {
}

