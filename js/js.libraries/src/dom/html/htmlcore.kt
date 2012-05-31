package js.dom.html

import js.native
import js.noImpl
import js.dom.core.*

public native trait Object {

}

public native trait Image : HTMLImageElement {

}

public native trait Navigator {
    public native var userAgent: String = js.noImpl
    public native var product: String = js.noImpl
    public native var appVersion: String = js.noImpl
    public native var appName: String = js.noImpl
    public native var appCodeName: String = js.noImpl
    public native var language: String = js.noImpl
    public native var mimeTypes: Array<Any?> = js.noImpl
    public native var platform: Array<Any?> = js.noImpl
    public native var plugins: String = js.noImpl
    public native fun taintEnabled(): Boolean = js.noImpl
    public native fun javaEnabled(): Boolean = js.noImpl
    public native fun preference(prefName: String, prefValue: String): String = js.noImpl
    public native fun preference(prefName: String): String = js.noImpl
}

public native trait Screen {
    public native var width: Double = js.noImpl
    public native var height: Double = js.noImpl
    public native var availHeight: Double = js.noImpl
    public native var availWidth: Double = js.noImpl
    public native var pixelDepth: Double = js.noImpl
    public native var colorDepth: Double = js.noImpl
}

public native trait Option {
    public native var defaultSelected: Boolean = js.noImpl
    public native var selected: Boolean = js.noImpl
    public native var text: String = js.noImpl
    public native var value: String = js.noImpl
}

public native trait Location {
    public native var href: String = js.noImpl
    public native var hash: String = js.noImpl
    public native var port: String = js.noImpl
    public native var host: String = js.noImpl
    public native var hostname: String = js.noImpl
    public native var pathname: String = js.noImpl
    public native var protocol: String = js.noImpl
    public native var search: String = js.noImpl
    public native var target: String = js.noImpl
    public native fun reload(): Unit = js.noImpl
    public native fun replace(url: String): Unit = js.noImpl
}

public native trait Event {
    public native var data: Array<Any> = js.noImpl
    public native var height: Double = js.noImpl
    public native var screenX: Double = js.noImpl
    public native var clientX: Double = js.noImpl
    public native var pageX: Double = js.noImpl
    public native var screenY: Double = js.noImpl
    public native var clientY: Double = js.noImpl
    public native var pageY: Double = js.noImpl
    public native var width: Double = js.noImpl
    public native var modifiers: Double = js.noImpl
    public native var keyCode: Double = js.noImpl
    public native var `type`: String = js.noImpl
    public native var which: Any = js.noImpl
    public native var altKey: Boolean = js.noImpl
    public native var ctrlKey: Boolean = js.noImpl
    public native var shiftKey: Boolean = js.noImpl
    public native var button: Boolean = js.noImpl
}

public native trait Selection {

}

public native trait document {
    public native var styleSheets: Array<Stylesheet> = js.noImpl
}

public native trait CSSRule {
    public native var selectorText: String = js.noImpl
}

public native trait Stylesheet {

}

public native trait History {
    public native fun back(): Unit = js.noImpl
    public native fun forward(): Unit = js.noImpl
    public native fun go(count: Number): Unit = js.noImpl
}

public native trait Console {
    public native fun log(message: Any): Unit = js.noImpl
}

public native trait Window {
    public native val document: HTMLDocument = js.noImpl
    public native val event: Event = js.noImpl
    public native val navigator: Navigator = js.noImpl
    public native val screen: Screen = js.noImpl
    public native val location: Location = js.noImpl
    public native var frameElement: Any = js.noImpl
    public native var opener: Window = js.noImpl
    public native var window: Window = js.noImpl
    public native var parent: Window = js.noImpl
    public native var top: Window = js.noImpl
    public native var self: Any = js.noImpl
    public native var frames: Array<Any?> = js.noImpl
    public native var innerHeight: Double = js.noImpl
    public native var innerWidth: Double = js.noImpl
    public native var outerHeight: Double = js.noImpl
    public native var outerWidth: Double = js.noImpl
    public native var scrollX: Double = js.noImpl
    public native var scrollY: Double = js.noImpl
    public native var pageXOffset: Double = js.noImpl
    public native var pageYOffset: Double = js.noImpl
    public native var scrollMaxX: Double = js.noImpl
    public native var scrollMaxY: Double = js.noImpl
    public native var status: String = js.noImpl
    public native var defaultStatus: String = js.noImpl
    public native var toolbar: Any = js.noImpl
    public native var menubar: Any = js.noImpl
    public native var scrollbars: Any = js.noImpl
    public native var directories: Any = js.noImpl
    public native var history: History = js.noImpl
    public native fun open(strUrl: String, strWindowName: String, strWindowFeatures: String): Window? = js.noImpl
    public native fun open(): Window? = js.noImpl
    public native fun open(strUrl: String): Window? = js.noImpl
    public native fun open(strUrl: String, strWindowName: String): Window? = js.noImpl
    public native fun print(): Unit = js.noImpl
    public native fun clearInterval(intervalId: Number): Unit = js.noImpl
    public native fun clearTimeout(intervalId: Number): Unit = js.noImpl
    public native fun setInterval(vCode: () -> Unit, iMillis: Number): Double? = js.noImpl
    public native fun setInterval(vCode: String, iMillis: Number): Double? = js.noImpl
    public native fun setTimeout(vCode: () -> Unit, iMillis: Number): Double? = js.noImpl
    public native fun setTimeout(vCode: String, iMillis: Number): Double? = js.noImpl
    public native fun scrollTo(x: Number, y: Number): Unit = js.noImpl
    public native fun scrollBy(xDelta: Number, yDelta: Number): Unit = js.noImpl
    public native fun moveTo(x: Number, y: Number): Unit = js.noImpl
    public native fun moveBy(xDelta: Number, yDelta: Number): Unit = js.noImpl
    public native fun resizeTo(width: Number, height: Number): Unit = js.noImpl
    public native fun resizeBy(widthDelta: Number, heightDelta: Number): Unit = js.noImpl
    public native var onload: () -> Unit = js.noImpl
    public native var onunload: () -> Unit = js.noImpl
}

public native trait Global {
    public native val window: Window = js.noImpl
    public native fun escape(str: String): Unit = js.noImpl
    public native fun escape(): Unit = js.noImpl
    public native fun unescape(str: String): Unit = js.noImpl
    public native fun unescape(): Unit = js.noImpl
}

public native trait HTMLCollection {
    public native val length: Double = js.noImpl
    public native fun item(index: Number): Node? = js.noImpl
    public native fun namedItem(name: String): Node? = js.noImpl
}

public native trait HTMLOptionsCollection {
    public native val length: Double = js.noImpl
    public native fun item(index: Number): Node? = js.noImpl
    public native fun namedItem(name: String): Node? = js.noImpl
}

public native trait HTMLDocument : Document {
    public native var title: String = js.noImpl
    public native val referrer: String = js.noImpl
    public native val domain: String = js.noImpl
    public native val URL: String = js.noImpl
    public native var body: HTMLElement = js.noImpl
    public native val images: HTMLCollection = js.noImpl
    public native val applets: HTMLCollection = js.noImpl
    public native val links: HTMLCollection = js.noImpl
    public native val forms: HTMLCollection = js.noImpl
    public native val anchors: HTMLCollection = js.noImpl
    public native var cookie: HTMLCollection = js.noImpl
    public native fun open(): Unit = js.noImpl
    public native fun close(): Unit = js.noImpl
    public native fun write(text: String): Unit = js.noImpl
    public native fun writeln(text: String): Unit = js.noImpl
    public native fun getElementsByName(elementName: String): NodeList = js.noImpl
    public native var compatMode: String = js.noImpl
    public native var onload: () -> Unit = js.noImpl
    public native var onunload: () -> Unit = js.noImpl
}

public native trait HTMLElement : Element {
    public native var id: String = js.noImpl
    public native var title: String = js.noImpl
    public native var lang: String = js.noImpl
    public native var dir: String = js.noImpl
    public native var className: String = js.noImpl
    public native var style: CSSStyleDeclaration = js.noImpl
    public native var clientWidth: Double = js.noImpl
    public native var clientHeight: Double = js.noImpl
    public native var clientTop: Double = js.noImpl
    public native var clientLeft: Double = js.noImpl
    public native var innerHTML: String = js.noImpl
    public native var offsetWidth: Double = js.noImpl
    public native var offsetHeight: Double = js.noImpl
    public native var offsetTop: Double = js.noImpl
    public native var offsetLeft: Double = js.noImpl
    public native var offsetParent: HTMLElement? = js.noImpl
    public native var scrollWidth: Double = js.noImpl
    public native var scrollHeight: Double = js.noImpl
    public native var scrollTop: Double = js.noImpl
    public native var scrollLeft: Double = js.noImpl
    public native var stylesheet: Stylesheet = js.noImpl
    public native var onblur: () -> Unit = js.noImpl
    public native var onclick: () -> Unit = js.noImpl
    public native var ondblclick: () -> Unit = js.noImpl
    public native var onfocus: () -> Unit = js.noImpl
    public native var onkeydown: () -> Unit = js.noImpl
    public native var onkeyup: () -> Unit = js.noImpl
    public native var onmouseup: () -> Unit = js.noImpl
    public native var onmousedown: () -> Unit = js.noImpl
    public native var onmouseout: () -> Unit = js.noImpl
    public native var onmouseover: () -> Unit = js.noImpl
    public native var onmousemove: () -> Unit = js.noImpl
    public native var onresize: () -> Unit = js.noImpl
}

public native trait CSSStyleDeclaration {
    public native var cssText: String = js.noImpl
    public native var length: Double = js.noImpl
    public native var parentRule: CSSRule = js.noImpl
    public native fun getPropertyPriority(propertyName: String): String = js.noImpl
    public native fun getPropertyValue(propertyName: String): String = js.noImpl
    public native fun item(index: Number): String = js.noImpl
    public native fun removeProperty(propertyName: String): String = js.noImpl
    public native fun setProperty(propertyName: String, value: String, priority: String): Unit = js.noImpl
}

public native trait HTMLHtmlElement : HTMLElement {
    public native var version: String = js.noImpl
}

public native trait HTMLHeadElement : HTMLElement {
    public native var profile: String = js.noImpl
}

public native trait HTMLLinkElement : HTMLElement {
    public native var disabled: Boolean = js.noImpl
    public native var charset: String = js.noImpl
    public native var href: String = js.noImpl
    public native var hreflang: String = js.noImpl
    public native var media: String = js.noImpl
    public native var rel: String = js.noImpl
    public native var rev: String = js.noImpl
    public native var target: String = js.noImpl
    public native var `type`: String = js.noImpl
}

public native trait HTMLTitleElement : HTMLElement {
    public native var text: String = js.noImpl
}

public native trait HTMLMetaElement : HTMLElement {
    public native var content: String = js.noImpl
    public native var httpEquiv: String = js.noImpl
    public native var name: String = js.noImpl
    public native var scheme: String = js.noImpl
}

public native trait HTMLBaseElement : HTMLElement {
    public native var href: String = js.noImpl
    public native var target: String = js.noImpl
}

public native trait HTMLIsIndexElement : HTMLElement {
    public native val form: HTMLFormElement = js.noImpl
    public native var prompt: String = js.noImpl
}

public native trait HTMLStyleElement : HTMLElement {
    public native var disabled: Boolean = js.noImpl
    public native var media: String = js.noImpl
    public native var `type`: String = js.noImpl
}

public native trait HTMLBodyElement : HTMLElement {
    public native var aLink: String = js.noImpl
    public native var background: String = js.noImpl
    public native var bgColor: String = js.noImpl
    public native var link: String = js.noImpl
    public native var text: String = js.noImpl
    public native var vLink: String = js.noImpl
    public native var onload: () -> Unit = js.noImpl
    public native var onunload: () -> Unit = js.noImpl
}

public native trait HTMLFormElement {
    public native val elements: HTMLCollection = js.noImpl
    public native val length: Double = js.noImpl
    public native var name: String = js.noImpl
    public native var acceptCharset: String = js.noImpl
    public native var action: String = js.noImpl
    public native var enctype: String = js.noImpl
    public native var method: String = js.noImpl
    public native var target: String = js.noImpl
    public native fun submit(): Unit = js.noImpl
    public native fun reset(): Unit = js.noImpl
}

public native trait HTMLSelectElement : HTMLElement {
    public native val `type`: String = js.noImpl
    public native var selectedIndex: Double = js.noImpl
    public native var value: String = js.noImpl
    public native var length: Double = js.noImpl
    public native val form: HTMLFormElement = js.noImpl
    public native val options: HTMLOptionsCollection = js.noImpl
    public native var disabled: Boolean = js.noImpl
    public native var multiple: Boolean = js.noImpl
    public native var name: String = js.noImpl
    public native var size: Double = js.noImpl
    public native var tabIndex: Double = js.noImpl
    public native fun add(element: HTMLElement, before: HTMLElement): Unit = js.noImpl
    public native fun remove(index: Number): Unit = js.noImpl
    public native fun blur(): Unit = js.noImpl
    public native fun focus(): Unit = js.noImpl
}

public native trait HTMLOptGroupElement : HTMLElement {
    public native var disabled: Boolean = js.noImpl
    public native var label: String = js.noImpl
}

public native trait HTMLOptionElement : HTMLElement {
    public native val form: HTMLFormElement = js.noImpl
    public native var defaultSelected: Boolean = js.noImpl
    public native var text: String = js.noImpl
    public native var index: Double = js.noImpl
    public native var disabled: Boolean = js.noImpl
    public native var label: String = js.noImpl
    public native var selected: Boolean = js.noImpl
    public native var value: String = js.noImpl
}

public native trait HTMLInputElement : HTMLElement {
    public native var defaultValue: String = js.noImpl
    public native var defaultChecked: Boolean = js.noImpl
    public native val form: HTMLFormElement = js.noImpl
    public native var accept: String = js.noImpl
    public native var accessKey: String = js.noImpl
    public native var align: String = js.noImpl
    public native var alt: String = js.noImpl
    public native var checked: Boolean = js.noImpl
    public native var disabled: Boolean = js.noImpl
    public native var maxLength: Double = js.noImpl
    public native var name: String = js.noImpl
    public native var readOnly: Boolean = js.noImpl
    public native var size: Double = js.noImpl
    public native var src: String = js.noImpl
    public native var tabIndex: Double = js.noImpl
    public native var `type`: String = js.noImpl
    public native var useMap: String = js.noImpl
    public native var value: String = js.noImpl
    public native fun blur(): Unit = js.noImpl
    public native fun focus(): Unit = js.noImpl
    public native fun select(): Unit = js.noImpl
    public native fun click(): Unit = js.noImpl
    public native var selectionStart: Double = js.noImpl
    public native var selectionEnd: Double = js.noImpl
}

public native trait HTMLTextAreaElement : HTMLElement {
    public native var defaultValue: String = js.noImpl
    public native val form: HTMLFormElement = js.noImpl
    public native var accessKey: String = js.noImpl
    public native var cols: Double = js.noImpl
    public native var disabled: Boolean = js.noImpl
    public native var name: String = js.noImpl
    public native var readOnly: Boolean = js.noImpl
    public native var rows: Double = js.noImpl
    public native var tabIndex: Double = js.noImpl
    public native var `type`: String = js.noImpl
    public native var value: String = js.noImpl
    public native fun blur(): Unit = js.noImpl
    public native fun focus(): Unit = js.noImpl
    public native fun select(): Unit = js.noImpl
}

public native trait HTMLButtonElement : HTMLElement {
    public native val form: HTMLFormElement = js.noImpl
    public native var accessKey: String = js.noImpl
    public native var disabled: Boolean = js.noImpl
    public native var name: String = js.noImpl
    public native var tabIndex: Double = js.noImpl
    public native var `type`: String = js.noImpl
    public native var value: String = js.noImpl
}

public native trait HTMLLabelElement : HTMLElement {
    public native val form: HTMLFormElement = js.noImpl
    public native var accessKey: String = js.noImpl
    public native var htmlFor: String = js.noImpl
}

public native trait HTMLFieldSetElement : HTMLElement {
    public native val form: HTMLFormElement = js.noImpl
}

public native trait HTMLLegendElement : HTMLElement {
    public native val form: HTMLFormElement = js.noImpl
    public native var accessKey: String = js.noImpl
    public native var align: String = js.noImpl
}

public native trait HTMLUListElement : HTMLElement {
    public native var compact: Boolean = js.noImpl
    public native var `type`: String = js.noImpl
}

public native trait HTMLOListElement : HTMLElement {
    public native var compact: Boolean = js.noImpl
    public native var start: Double = js.noImpl
    public native var `type`: String = js.noImpl
}

public native trait HTMLDListElement : HTMLElement {
    public native var compact: Boolean = js.noImpl
}

public native trait HTMLDirectoryElement : HTMLElement {
    public native var compact: Boolean = js.noImpl
}

public native trait HTMLMenuElement : HTMLElement {
    public native var compact: Boolean = js.noImpl
}

public native trait HTMLLIElement : HTMLElement {
    public native var `type`: String = js.noImpl
    public native var value: Double = js.noImpl
}

public native trait HTMLDivElement : HTMLElement {
    public native var align: String = js.noImpl
}

public native trait HTMLParagraphElement : HTMLElement {
    public native var align: String = js.noImpl
}

public native trait HTMLHeadingElement : HTMLElement {
    public native var align: String = js.noImpl
}

public native trait HTMLQuoteElement : HTMLElement {
    public native var cite: String = js.noImpl
}

public native trait HTMLPreElement : HTMLElement {
    public native var width: Double = js.noImpl
}

public native trait HTMLBRElement : HTMLElement {
    public native var clear: String = js.noImpl
}

public native trait HTMLBaseFontElement : HTMLElement {
    public native var color: String = js.noImpl
    public native var face: String = js.noImpl
    public native var size: Double = js.noImpl
}

public native trait HTMLFontElement : HTMLElement {
    public native var color: String = js.noImpl
    public native var face: String = js.noImpl
    public native var size: String = js.noImpl
}

public native trait HTMLHRElement : HTMLElement {
    public native var align: String = js.noImpl
    public native var noShade: Boolean = js.noImpl
    public native var size: String = js.noImpl
    public native var width: String = js.noImpl
}

public native trait HTMLModElement : HTMLElement {
    public native var cite: String = js.noImpl
    public native var dateTime: String = js.noImpl
}

public native trait HTMLAnchorElement : HTMLElement {
    public native var accessKey: String = js.noImpl
    public native var charset: String = js.noImpl
    public native var coords: String = js.noImpl
    public native var href: String = js.noImpl
    public native var hreflang: String = js.noImpl
    public native var name: String = js.noImpl
    public native var rel: String = js.noImpl
    public native var rev: String = js.noImpl
    public native var shape: String = js.noImpl
    public native var tabIndex: Double = js.noImpl
    public native var target: String = js.noImpl
    public native var `type`: String = js.noImpl
    public native fun blur(): Unit = js.noImpl
    public native fun focus(): Unit = js.noImpl
}

public native trait HTMLImageElement : HTMLElement {
    public native var name: String = js.noImpl
    public native var align: String = js.noImpl
    public native var alt: String = js.noImpl
    public native var border: String = js.noImpl
    public native var height: Double = js.noImpl
    public native var hspace: Double = js.noImpl
    public native var isMap: Boolean = js.noImpl
    public native var longDesc: String = js.noImpl
    public native var src: String = js.noImpl
    public native var useMap: String = js.noImpl
    public native var vspace: Double = js.noImpl
    public native var width: Double = js.noImpl
}

public native trait HTMLObjectElement : HTMLElement {
    public native val form: HTMLFormElement = js.noImpl
    public native var code: String = js.noImpl
    public native var align: String = js.noImpl
    public native var archive: String = js.noImpl
    public native var border: String = js.noImpl
    public native var codeBase: String = js.noImpl
    public native var codeType: String = js.noImpl
    public native var data: String = js.noImpl
    public native var declare: Boolean = js.noImpl
    public native var height: String = js.noImpl
    public native var hspace: Double = js.noImpl
    public native var name: String = js.noImpl
    public native var standby: String = js.noImpl
    public native var tabIndex: Double = js.noImpl
    public native var `type`: String = js.noImpl
    public native var useMap: String = js.noImpl
    public native var vspace: Double = js.noImpl
    public native var width: String = js.noImpl
    public native val contentDocument: Document = js.noImpl
}

public native trait HTMLParamElement : HTMLElement {
    public native var name: String = js.noImpl
    public native var `type`: String = js.noImpl
    public native var value: String = js.noImpl
    public native var valueType: String = js.noImpl
}

public native trait HTMLAppletElement : HTMLElement {
    public native var align: String = js.noImpl
    public native var alt: String = js.noImpl
    public native var archive: String = js.noImpl
    public native var code: String = js.noImpl
    public native var codeBase: String = js.noImpl
    public native var height: String = js.noImpl
    public native var hspace: Double = js.noImpl
    public native var name: String = js.noImpl
    public native var `object`: String = js.noImpl
    public native var vspace: Double = js.noImpl
    public native var width: String = js.noImpl
}

public native trait HTMLMapElement : HTMLElement {
    public native val areas: HTMLCollection = js.noImpl
    public native var name: String = js.noImpl
}

public native trait HTMLAreaElement : HTMLElement {
    public native var accessKey: String = js.noImpl
    public native var alt: String = js.noImpl
    public native var coords: String = js.noImpl
    public native var href: String = js.noImpl
    public native var noHref: Boolean = js.noImpl
    public native var shape: String = js.noImpl
    public native var tabIndex: Double = js.noImpl
    public native var target: String = js.noImpl
}

public native trait HTMLScriptElement : HTMLElement {
    public native var text: String = js.noImpl
    public native var htmlFor: String = js.noImpl
    public native var event: String = js.noImpl
    public native var charset: String = js.noImpl
    public native var defer: Boolean = js.noImpl
    public native var src: String = js.noImpl
    public native var `type`: String = js.noImpl
}

public native trait HTMLTableElement : HTMLElement {
    public native var caption: HTMLTableCaptionElement = js.noImpl
    public native var tHead: HTMLTableSectionElement = js.noImpl
    public native var tFoot: HTMLTableSectionElement = js.noImpl
    public native val rows: HTMLCollection = js.noImpl
    public native val tBodies: HTMLCollection = js.noImpl
    public native var align: String = js.noImpl
    public native var bgColor: String = js.noImpl
    public native var border: String = js.noImpl
    public native var cellPadding: String = js.noImpl
    public native var cellSpacing: String = js.noImpl
    public native var frame: String = js.noImpl
    public native var rules: String = js.noImpl
    public native var summary: String = js.noImpl
    public native var width: String = js.noImpl
    public native fun createTHead(): HTMLElement? = js.noImpl
    public native fun deleteTHead(): Unit = js.noImpl
    public native fun createTFoot(): HTMLElement? = js.noImpl
    public native fun deleteTFoot(): Unit = js.noImpl
    public native fun createCaption(): HTMLElement? = js.noImpl
    public native fun deleteCaption(): Unit = js.noImpl
    public native fun insertRow(index: Number): HTMLElement? = js.noImpl
    public native fun deleteRow(index: Number): Unit = js.noImpl
}

public native trait HTMLTableCaptionElement : HTMLElement {
    public native var align: String = js.noImpl
}

public native trait HTMLTableColElement : HTMLElement {
    public native var align: String = js.noImpl
    public native var ch: String = js.noImpl
    public native var chOff: String = js.noImpl
    public native var span: Double = js.noImpl
    public native var vAlign: String = js.noImpl
    public native var width: String = js.noImpl
}

public native trait HTMLTableSectionElement : HTMLElement {
    public native var align: String = js.noImpl
    public native var ch: String = js.noImpl
    public native var chOff: String = js.noImpl
    public native var vAlign: String = js.noImpl
    public native val rows: HTMLCollection = js.noImpl
    public native fun insertRow(index: Number): HTMLElement? = js.noImpl
    public native fun deleteRow(index: Number): Unit = js.noImpl
}

public native trait HTMLTableRowElement : HTMLElement {
    public native val rowIndex: Double = js.noImpl
    public native val sectionRowIndex: Double = js.noImpl
    public native val cells: HTMLCollection = js.noImpl
    public native var align: String = js.noImpl
    public native var bgColor: String = js.noImpl
    public native var ch: String = js.noImpl
    public native var chOff: String = js.noImpl
    public native var vAlign: String = js.noImpl
    public native fun insertCell(index: Number): HTMLElement? = js.noImpl
    public native fun deleteCell(index: Number): Unit = js.noImpl
}

public native trait HTMLTableCellElement : HTMLElement {
    public native val cellIndex: Double = js.noImpl
    public native var abbr: String = js.noImpl
    public native var align: String = js.noImpl
    public native var axis: String = js.noImpl
    public native var bgColor: String = js.noImpl
    public native var ch: String = js.noImpl
    public native var chOff: String = js.noImpl
    public native var colSpan: Double = js.noImpl
    public native var headers: String = js.noImpl
    public native var height: String = js.noImpl
    public native var noWrap: Boolean = js.noImpl
    public native var rowSpan: Double = js.noImpl
    public native var scope: String = js.noImpl
    public native var vAlign: String = js.noImpl
    public native var width: String = js.noImpl
}

public native trait HTMLFrameSetElement : HTMLElement {
    public native var cols: String = js.noImpl
    public native var rows: String = js.noImpl
}

public native trait HTMLFrameElement : HTMLElement {
    public native var frameBorder: String = js.noImpl
    public native var longDesc: String = js.noImpl
    public native var marginHeight: String = js.noImpl
    public native var marginWidth: String = js.noImpl
    public native var name: String = js.noImpl
    public native var noResize: Boolean = js.noImpl
    public native var scrolling: String = js.noImpl
    public native var src: String = js.noImpl
}

public native trait HTMLIFrameElement : HTMLElement {
    public native var align: String = js.noImpl
    public native var frameBorder: String = js.noImpl
    public native var height: String = js.noImpl
    public native var longDesc: String = js.noImpl
    public native var marginHeight: String = js.noImpl
    public native var marginWidth: String = js.noImpl
    public native var name: String = js.noImpl
    public native var scrolling: String = js.noImpl
    public native var src: String = js.noImpl
    public native var width: String = js.noImpl
    public native val contentDocument: Document = js.noImpl
    public native val contentWindow: Window = js.noImpl
}

