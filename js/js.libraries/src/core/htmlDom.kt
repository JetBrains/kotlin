package kotlin.js.dom.html

import org.w3c.dom.*

@native
@Deprecated("Use declarations from kotlin.browser instead")
public val window: Window = noImpl

@native
@Deprecated("Use declarations from kotlin.browser instead")
public var document: HTMLDocument = noImpl

@Deprecated("Do not use it")
public @native interface Object {
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface Image : HTMLImageElement {
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface Navigator {
    public @native var userAgent: String
    public @native var product: String
    public @native var appVersion: String
    public @native var appName: String
    public @native var appCodeName: String
    public @native var language: String
    public @native var mimeTypes: Array<Any?>
    public @native var platform: Array<Any?>
    public @native var plugins: String
    public @native fun taintEnabled(): Boolean
    public @native fun javaEnabled(): Boolean
    public @native fun preference(prefName: String, prefValue: String): String
    public @native fun preference(prefName: String): String
}

public @native interface Screen {
    public @native var width: Double
    public @native var height: Double
    public @native var availHeight: Double
    public @native var availWidth: Double
    public @native var pixelDepth: Double
    public @native var colorDepth: Double
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface Option {
    public @native var defaultSelected: Boolean
    public @native var selected: Boolean
    public @native var text: String
    public @native var value: String
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface Location {
    public @native var href: String
    public @native var hash: String
    public @native var port: String
    public @native var host: String
    public @native var hostname: String
    public @native var pathname: String
    public @native var protocol: String
    public @native var search: String
    public @native var target: String
    public @native fun reload(): Unit
    public @native fun replace(url: String): Unit
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface Event {
    public @native var data: Array<Any>
    public @native var height: Double
    public @native var screenX: Double
    public @native var clientX: Double
    public @native var pageX: Double
    public @native var screenY: Double
    public @native var clientY: Double
    public @native var pageY: Double
    public @native var width: Double
    public @native var modifiers: Double
    public @native var keyCode: Double
    public @native var type: String
    public @native var which: Any
    public @native var altKey: Boolean
    public @native var ctrlKey: Boolean
    public @native var shiftKey: Boolean
    public @native var button: Boolean
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface Selection {

}
@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface CSSRule {
    public @native var selectorText: String
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface Stylesheet {
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface History {
    public @native fun back(): Unit
    public @native fun forward(): Unit
    public @native fun go(count: Number): Unit
}

public @native interface Console {
    public @native fun log(message: Any): Unit
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface Window {
    public @native val document: HTMLDocument
    public @native val event: Event
    public @native val navigator: Navigator
    public @native val screen: Screen
    public @native val location: Location
    public @native var frameElement: Any
    public @native var opener: Window
    public @native var window: Window
    public @native var parent: Window
    public @native var top: Window
    public @native var self: Any
    public @native var frames: Array<Any?>
    public @native var innerHeight: Double
    public @native var innerWidth: Double
    public @native var outerHeight: Double
    public @native var outerWidth: Double
    public @native var scrollX: Double
    public @native var scrollY: Double
    public @native var pageXOffset: Double
    public @native var pageYOffset: Double
    public @native var scrollMaxX: Double
    public @native var scrollMaxY: Double
    public @native var status: String
    public @native var defaultStatus: String
    public @native var toolbar: Any
    public @native var menubar: Any
    public @native var scrollbars: Any
    public @native var directories: Any
    public @native var history: History
    public @native fun open(strUrl: String, strWindowName: String, strWindowFeatures: String): Window?
    public @native fun open(): Window?
    public @native fun open(strUrl: String): Window?
    public @native fun open(strUrl: String, strWindowName: String): Window?
    public @native fun print(): Unit
    public @native fun clearInterval(intervalId: Number): Unit
    public @native fun clearTimeout(intervalId: Number): Unit
    public @native fun setInterval(vCode: () -> Unit, iMillis: Number): Long
    public @native fun setInterval(vCode: String, iMillis: Number): Long
    public @native fun setTimeout(vCode: () -> Unit, iMillis: Number): Long
    public @native fun setTimeout(vCode: String, iMillis: Number): Long
    public @native fun scrollTo(x: Number, y: Number): Unit
    public @native fun scrollBy(xDelta: Number, yDelta: Number): Unit
    public @native fun moveTo(x: Number, y: Number): Unit
    public @native fun moveBy(xDelta: Number, yDelta: Number): Unit
    public @native fun resizeTo(width: Number, height: Number): Unit
    public @native fun resizeBy(widthDelta: Number, heightDelta: Number): Unit
    public @native var onload: () -> Unit
    public @native var onunload: () -> Unit
}

public @native interface Global {
    public @native val window: Window
    public @native fun escape(str: String): Unit
    public @native fun escape(): Unit
    public @native fun unescape(str: String): Unit
    public @native fun unescape(): Unit
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLCollection {
    public @native val length: Double
    public @native fun item(index: Number): Node?
    public @native fun namedItem(name: String): Node?
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLOptionsCollection {
    public @native val length: Double
    public @native fun item(index: Number): Node?
    public @native fun namedItem(name: String): Node?
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native class HTMLDocument : Document() {
    public @native fun open(): Unit
    public @native fun write(text: String): Unit
    public @native fun writeln(text: String): Unit
    public @native var onunload: () -> Unit
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLElement : Element {
    public @native var title: String
    public @native var lang: String
    public @native var dir: String
    public @native var style: CSSStyleDeclaration
    public @native var offsetWidth: Double
    public @native var offsetHeight: Double
    public @native var offsetTop: Double
    public @native var offsetLeft: Double
    public @native var offsetParent: HTMLElement?
    public @native var stylesheet: Stylesheet
    public @native var onblur: () -> Unit
    public @native var onclick: () -> Unit
    public @native var ondblclick: () -> Unit
    public @native var onfocus: () -> Unit
    public @native var onkeydown: () -> Unit
    public @native var onkeyup: () -> Unit
    public @native var onmouseup: () -> Unit
    public @native var onmousedown: () -> Unit
    public @native var onmouseout: () -> Unit
    public @native var onmouseover: () -> Unit
    public @native var onmousemove: () -> Unit
    public @native var onresize: () -> Unit
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface CSSStyleDeclaration {
    public @native var cssText: String
    public @native var length: Double
    public @native var parentRule: CSSRule
    public @native fun getPropertyPriority(propertyName: String): String
    public @native fun getPropertyValue(propertyName: String): String
    public @native fun item(index: Number): String
    public @native fun removeProperty(propertyName: String): String
    public @native fun setProperty(propertyName: String, value: String, priority: String): Unit
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLHtmlElement : HTMLElement {
    public @native var version: String
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLHeadElement : HTMLElement {
    public @native var profile: String
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLLinkElement : HTMLElement {
    public @native var disabled: Boolean
    public @native var charset: String
    public @native var href: String
    public @native var hreflang: String
    public @native var media: String
    public @native var rel: String
    public @native var rev: String
    public @native var target: String
    public @native var type: String
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLTitleElement : HTMLElement {
    public @native var text: String
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLMetaElement : HTMLElement {
    public @native var content: String
    public @native var httpEquiv: String
    public @native var name: String
    public @native var scheme: String
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLBaseElement : HTMLElement {
    public @native var href: String
    public @native var target: String
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLIsIndexElement : HTMLElement {
    public @native val form: HTMLFormElement
    public @native var prompt: String
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLStyleElement : HTMLElement {
    public @native var disabled: Boolean
    public @native var media: String
    public @native var type: String
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLBodyElement : HTMLElement {
    public @native var aLink: String
    public @native var background: String
    public @native var bgColor: String
    public @native var link: String
    public @native var text: String
    public @native var vLink: String
    public @native var onload: () -> Unit
    public @native var onunload: () -> Unit
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLFormElement {
    public @native val elements: HTMLCollection
    public @native val length: Double
    public @native var name: String
    public @native var acceptCharset: String
    public @native var action: String
    public @native var enctype: String
    public @native var method: String
    public @native var target: String
    public @native fun submit(): Unit
    public @native fun reset(): Unit
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLSelectElement : HTMLElement {
    public @native val type: String
    public @native var selectedIndex: Double
    public @native var value: String
    public @native var length: Double
    public @native val form: HTMLFormElement
    public @native val options: HTMLOptionsCollection
    public @native var disabled: Boolean
    public @native var multiple: Boolean
    public @native var name: String
    public @native var size: Double
    public @native var tabIndex: Double
    public @native fun add(element: HTMLElement, before: HTMLElement): Unit
    public @native fun remove(index: Number): Unit
    public @native fun blur(): Unit
    public @native fun focus(): Unit
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLOptGroupElement : HTMLElement {
    public @native var disabled: Boolean
    public @native var label: String
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLOptionElement : HTMLElement {
    public @native val form: HTMLFormElement
    public @native var defaultSelected: Boolean
    public @native var text: String
    public @native var index: Double
    public @native var disabled: Boolean
    public @native var label: String
    public @native var selected: Boolean
    public @native var value: String
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLInputElement : HTMLElement {
    public @native var defaultValue: String
    public @native var defaultChecked: Boolean
    public @native val form: HTMLFormElement
    public @native var accept: String
    public @native var accessKey: String
    public @native var align: String
    public @native var alt: String
    public @native var checked: Boolean
    public @native var disabled: Boolean
    public @native var maxLength: Double
    public @native var name: String
    public @native var readOnly: Boolean
    public @native var size: Double
    public @native var src: String
    public @native var tabIndex: Double
    public @native var type: String
    public @native var useMap: String
    public @native var value: String
    public @native fun blur(): Unit
    public @native fun focus(): Unit
    public @native fun select(): Unit
    public @native fun click(): Unit
    public @native var selectionStart: Double
    public @native var selectionEnd: Double
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLTextAreaElement : HTMLElement {
    public @native var defaultValue: String
    public @native val form: HTMLFormElement
    public @native var accessKey: String
    public @native var cols: Double
    public @native var disabled: Boolean
    public @native var name: String
    public @native var readOnly: Boolean
    public @native var rows: Double
    public @native var tabIndex: Double
    public @native var type: String
    public @native var value: String
    public @native fun blur(): Unit
    public @native fun focus(): Unit
    public @native fun select(): Unit
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLButtonElement : HTMLElement {
    public @native val form: HTMLFormElement
    public @native var accessKey: String
    public @native var disabled: Boolean
    public @native var name: String
    public @native var tabIndex: Double
    public @native var type: String
    public @native var value: String
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLLabelElement : HTMLElement {
    public @native val form: HTMLFormElement
    public @native var accessKey: String
    public @native var htmlFor: String
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLFieldSetElement : HTMLElement {
    public @native val form: HTMLFormElement
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLLegendElement : HTMLElement {
    public @native val form: HTMLFormElement
    public @native var accessKey: String
    public @native var align: String
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLUListElement : HTMLElement {
    public @native var compact: Boolean
    public @native var type: String
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLOListElement : HTMLElement {
    public @native var compact: Boolean
    public @native var start: Double
    public @native var type: String
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLDListElement : HTMLElement {
    public @native var compact: Boolean
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLDirectoryElement : HTMLElement {
    public @native var compact: Boolean
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLMenuElement : HTMLElement {
    public @native var compact: Boolean
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLLIElement : HTMLElement {
    public @native var type: String
    public @native var value: Double
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLDivElement : HTMLElement {
    public @native var align: String
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLParagraphElement : HTMLElement {
    public @native var align: String
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLHeadingElement : HTMLElement {
    public @native var align: String
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLQuoteElement : HTMLElement {
    public @native var cite: String
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLPreElement : HTMLElement {
    public @native var width: Double
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLBRElement : HTMLElement {
    public @native var clear: String
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLBaseFontElement : HTMLElement {
    public @native var color: String
    public @native var face: String
    public @native var size: Double
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLFontElement : HTMLElement {
    public @native var color: String
    public @native var face: String
    public @native var size: String
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLHRElement : HTMLElement {
    public @native var align: String
    public @native var noShade: Boolean
    public @native var size: String
    public @native var width: String
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLModElement : HTMLElement {
    public @native var cite: String
    public @native var dateTime: String
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLAnchorElement : HTMLElement {
    public @native var accessKey: String
    public @native var charset: String
    public @native var coords: String
    public @native var href: String
    public @native var hreflang: String
    public @native var name: String
    public @native var rel: String
    public @native var rev: String
    public @native var shape: String
    public @native var tabIndex: Double
    public @native var target: String
    public @native var type: String
    public @native fun blur(): Unit
    public @native fun focus(): Unit
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLImageElement : HTMLElement {
    public @native var name: String
    public @native var align: String
    public @native var alt: String
    public @native var border: String
    public @native var height: Double
    public @native var hspace: Double
    public @native var isMap: Boolean
    public @native var longDesc: String
    public @native var naturalHeight: Int
    public @native var naturalWidth: Int
    public @native var src: String
    public @native var useMap: String
    public @native var vspace: Double
    public @native var width: Double
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLObjectElement : HTMLElement {
    public @native val form: HTMLFormElement
    public @native var code: String
    public @native var align: String
    public @native var archive: String
    public @native var border: String
    public @native var codeBase: String
    public @native var codeType: String
    public @native var data: String
    public @native var declare: Boolean
    public @native var height: String
    public @native var hspace: Double
    public @native var name: String
    public @native var standby: String
    public @native var tabIndex: Double
    public @native var type: String
    public @native var useMap: String
    public @native var vspace: Double
    public @native var width: String
    public @native val contentDocument: Document
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLParamElement : HTMLElement {
    public @native var name: String
    public @native var type: String
    public @native var value: String
    public @native var valueType: String
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLAppletElement : HTMLElement {
    public @native var align: String
    public @native var alt: String
    public @native var archive: String
    public @native var code: String
    public @native var codeBase: String
    public @native var height: String
    public @native var hspace: Double
    public @native var name: String
    public @native var `object`: String
    public @native var vspace: Double
    public @native var width: String
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLMapElement : HTMLElement {
    public @native val areas: HTMLCollection
    public @native var name: String
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLAreaElement : HTMLElement {
    public @native var accessKey: String
    public @native var alt: String
    public @native var coords: String
    public @native var href: String
    public @native var noHref: Boolean
    public @native var shape: String
    public @native var tabIndex: Double
    public @native var target: String
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLScriptElement : HTMLElement {
    public @native var text: String
    public @native var htmlFor: String
    public @native var event: String
    public @native var charset: String
    public @native var defer: Boolean
    public @native var src: String
    public @native var type: String
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLTableElement : HTMLElement {
    public @native var caption: HTMLTableCaptionElement
    public @native var tHead: HTMLTableSectionElement
    public @native var tFoot: HTMLTableSectionElement
    public @native val rows: HTMLCollection
    public @native val tBodies: HTMLCollection
    public @native var align: String
    public @native var bgColor: String
    public @native var border: String
    public @native var cellPadding: String
    public @native var cellSpacing: String
    public @native var frame: String
    public @native var rules: String
    public @native var summary: String
    public @native var width: String
    public @native fun createTHead(): HTMLElement?
    public @native fun deleteTHead(): Unit
    public @native fun createTFoot(): HTMLElement?
    public @native fun deleteTFoot(): Unit
    public @native fun createCaption(): HTMLElement?
    public @native fun deleteCaption(): Unit
    public @native fun insertRow(index: Number): HTMLElement?
    public @native fun deleteRow(index: Number): Unit
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLTableCaptionElement : HTMLElement {
    public @native var align: String
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLTableColElement : HTMLElement {
    public @native var align: String
    public @native var ch: String
    public @native var chOff: String
    public @native var span: Double
    public @native var vAlign: String
    public @native var width: String
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLTableSectionElement : HTMLElement {
    public @native var align: String
    public @native var ch: String
    public @native var chOff: String
    public @native var vAlign: String
    public @native val rows: HTMLCollection
    public @native fun insertRow(index: Number): HTMLElement?
    public @native fun deleteRow(index: Number): Unit
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLTableRowElement : HTMLElement {
    public @native val rowIndex: Double
    public @native val sectionRowIndex: Double
    public @native val cells: HTMLCollection
    public @native var align: String
    public @native var bgColor: String
    public @native var ch: String
    public @native var chOff: String
    public @native var vAlign: String
    public @native fun insertCell(index: Number): HTMLElement?
    public @native fun deleteCell(index: Number): Unit
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLTableCellElement : HTMLElement {
    public @native val cellIndex: Double
    public @native var abbr: String
    public @native var align: String
    public @native var axis: String
    public @native var bgColor: String
    public @native var ch: String
    public @native var chOff: String
    public @native var colSpan: Double
    public @native var headers: String
    public @native var height: String
    public @native var noWrap: Boolean
    public @native var rowSpan: Double
    public @native var scope: String
    public @native var vAlign: String
    public @native var width: String
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLFrameSetElement : HTMLElement {
    public @native var cols: String
    public @native var rows: String
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLFrameElement : HTMLElement {
    public @native var frameBorder: String
    public @native var longDesc: String
    public @native var marginHeight: String
    public @native var marginWidth: String
    public @native var name: String
    public @native var noResize: Boolean
    public @native var scrolling: String
    public @native var src: String
}

@Deprecated("Use declarations from org.w3c.dom instead")
public @native interface HTMLIFrameElement : HTMLElement {
    public @native var align: String
    public @native var frameBorder: String
    public @native var height: String
    public @native var longDesc: String
    public @native var marginHeight: String
    public @native var marginWidth: String
    public @native var name: String
    public @native var scrolling: String
    public @native var src: String
    public @native var width: String
    public @native val contentDocument: Document
    public @native val contentWindow: Window
}

