/*
 * Generated file
 * DO NOT EDIT
 * 
 * See libraries/tools/idl2k for details
 */

package org.w3c.dom3

native trait HTMLAllCollection : HTMLCollection {
    fun item(name : String) : dynamic = noImpl
    override fun namedItem(name : String) : dynamic = noImpl
    nativeGetter override fun get(name : String) : dynamic = noImpl
}
native trait HTMLFormControlsCollection : HTMLCollection {
    override fun namedItem(name : String) : dynamic = noImpl
    nativeGetter override fun get(name : String) : dynamic = noImpl
}
native trait RadioNodeList : NodeList {
    var value : String
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLOptionsCollection : HTMLCollection {
    var selectedIndex : Int
        get() = noImpl
        set(value) = noImpl
    fun set(index : Int, option : HTMLOptionElement?) : Unit? = noImpl
    fun add(element : UnionHTMLOptGroupElementOrHTMLOptionElement, before : dynamic = null) : Unit = noImpl
    fun remove(index : Int) : Unit = noImpl
}
native trait HTMLPropertiesCollection : HTMLCollection {
    var names : dynamic
        get() = noImpl
        set(value) = noImpl
//    override fun namedItem(name : String) : PropertyNodeList? = noImpl
//    nativeGetter override fun get(name : String) : PropertyNodeList? = noImpl
}
native trait PropertyNodeList : NodeList {
    fun getValues() : dynamic = noImpl
}
native trait DOMStringMap {
    nativeGetter fun get(name : String) : String? = noImpl
    fun set(name : String, value : String) : Unit? = noImpl
}
native trait DOMElementMap {
    nativeGetter fun get(name : String) : Element? = noImpl
    fun set(name : String, value : Element) : Unit? = noImpl
}
native open class Document : Node {
    var location : Location?
        get() = noImpl
        set(value) = noImpl
    var domain : String
        get() = noImpl
        set(value) = noImpl
    var referrer : String
        get() = noImpl
        set(value) = noImpl
    var cookie : String
        get() = noImpl
        set(value) = noImpl
    var lastModified : String
        get() = noImpl
        set(value) = noImpl
    var readyState : String
        get() = noImpl
        set(value) = noImpl
    var title : String
        get() = noImpl
        set(value) = noImpl
    var dir : String
        get() = noImpl
        set(value) = noImpl
    var body : HTMLElement?
        get() = noImpl
        set(value) = noImpl
    var head : HTMLHeadElement?
        get() = noImpl
        set(value) = noImpl
    var images : HTMLCollection
        get() = noImpl
        set(value) = noImpl
    var embeds : HTMLCollection
        get() = noImpl
        set(value) = noImpl
    var plugins : HTMLCollection
        get() = noImpl
        set(value) = noImpl
    var links : HTMLCollection
        get() = noImpl
        set(value) = noImpl
    var forms : HTMLCollection
        get() = noImpl
        set(value) = noImpl
    var scripts : HTMLCollection
        get() = noImpl
        set(value) = noImpl
    var cssElementMap : DOMElementMap
        get() = noImpl
        set(value) = noImpl
    var currentScript : HTMLScriptElement?
        get() = noImpl
        set(value) = noImpl
    var defaultView : Window?
        get() = noImpl
        set(value) = noImpl
    var activeElement : Element?
        get() = noImpl
        set(value) = noImpl
    var designMode : String
        get() = noImpl
        set(value) = noImpl
    var commands : HTMLCollection
        get() = noImpl
        set(value) = noImpl
    var onreadystatechange : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var fgColor : String
        get() = noImpl
        set(value) = noImpl
    var linkColor : String
        get() = noImpl
        set(value) = noImpl
    var vlinkColor : String
        get() = noImpl
        set(value) = noImpl
    var alinkColor : String
        get() = noImpl
        set(value) = noImpl
    var bgColor : String
        get() = noImpl
        set(value) = noImpl
    var anchors : HTMLCollection
        get() = noImpl
        set(value) = noImpl
    var applets : HTMLCollection
        get() = noImpl
        set(value) = noImpl
    var all : HTMLAllCollection
        get() = noImpl
        set(value) = noImpl
    var implementation : DOMImplementation
        get() = noImpl
        set(value) = noImpl
    var URL : String
        get() = noImpl
        set(value) = noImpl
    var documentURI : String
        get() = noImpl
        set(value) = noImpl
    var origin : String
        get() = noImpl
        set(value) = noImpl
    var compatMode : String
        get() = noImpl
        set(value) = noImpl
    var characterSet : String
        get() = noImpl
        set(value) = noImpl
    var inputEncoding : String
        get() = noImpl
        set(value) = noImpl
    var contentType : String
        get() = noImpl
        set(value) = noImpl
    var doctype : DocumentType?
        get() = noImpl
        set(value) = noImpl
    var documentElement : Element?
        get() = noImpl
        set(value) = noImpl
    var fullscreenEnabled : Boolean
        get() = noImpl
        set(value) = noImpl
    var fullscreenElement : Element?
        get() = noImpl
        set(value) = noImpl
    var onfullscreenchange : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onfullscreenerror : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var styleSheets : StyleSheetList
        get() = noImpl
        set(value) = noImpl
    var selectedStyleSheetSet : String?
        get() = noImpl
        set(value) = noImpl
    var lastStyleSheetSet : String?
        get() = noImpl
        set(value) = noImpl
    var preferredStyleSheetSet : String?
        get() = noImpl
        set(value) = noImpl
    var styleSheetSets : dynamic
        get() = noImpl
        set(value) = noImpl
    var onabort : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onautocomplete : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onautocompleteerror : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onblur : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var oncancel : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var oncanplay : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var oncanplaythrough : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onchange : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onclick : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onclose : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var oncontextmenu : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var oncuechange : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var ondblclick : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var ondrag : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var ondragend : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var ondragenter : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var ondragexit : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var ondragleave : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var ondragover : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var ondragstart : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var ondrop : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var ondurationchange : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onemptied : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onended : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onerror : dynamic
        get() = noImpl
        set(value) = noImpl
    var onfocus : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var oninput : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var oninvalid : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onkeydown : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onkeypress : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onkeyup : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onload : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onloadeddata : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onloadedmetadata : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onloadstart : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onmousedown : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onmouseenter : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onmouseleave : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onmousemove : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onmouseout : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onmouseover : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onmouseup : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onmousewheel : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onpause : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onplay : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onplaying : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onprogress : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onratechange : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onreset : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onresize : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onscroll : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onseeked : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onseeking : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onselect : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onshow : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onsort : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onstalled : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onsubmit : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onsuspend : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var ontimeupdate : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var ontoggle : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onvolumechange : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onwaiting : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var children : HTMLCollection
        get() = noImpl
        set(value) = noImpl
    var firstElementChild : Element?
        get() = noImpl
        set(value) = noImpl
    var lastElementChild : Element?
        get() = noImpl
        set(value) = noImpl
    var childElementCount : Int
        get() = noImpl
        set(value) = noImpl
    nativeGetter fun get(name : String) : dynamic = noImpl
    fun getElementsByName(elementName : String) : NodeList = noImpl
    fun getItems(typeNames : String = "") : NodeList = noImpl
    fun open(type : String = "text/html", replace : String = "") : Document = noImpl
    fun open(url : String, name : String, features : String, replace : Boolean = false) : Window = noImpl
    fun close() : Unit = noImpl
    fun write(vararg text : String) : Unit = noImpl
    fun writeln(vararg text : String) : Unit = noImpl
    fun hasFocus() : Boolean = noImpl
    fun execCommand(commandId : String, showUI : Boolean = false, value : String = "") : Boolean = noImpl
    fun queryCommandEnabled(commandId : String) : Boolean = noImpl
    fun queryCommandIndeterm(commandId : String) : Boolean = noImpl
    fun queryCommandState(commandId : String) : Boolean = noImpl
    fun queryCommandSupported(commandId : String) : Boolean = noImpl
    fun queryCommandValue(commandId : String) : String = noImpl
    fun clear() : Unit = noImpl
    fun captureEvents() : Unit = noImpl
    fun releaseEvents() : Unit = noImpl
    fun getElementsByTagName(localName : String) : HTMLCollection = noImpl
    fun getElementsByTagNameNS(namespace : String?, localName : String) : HTMLCollection = noImpl
    fun getElementsByClassName(classNames : String) : HTMLCollection = noImpl
    fun createElement(localName : String) : Element = noImpl
    fun createElementNS(namespace : String?, qualifiedName : String) : Element = noImpl
    fun createDocumentFragment() : DocumentFragment = noImpl
    fun createTextNode(data : String) : Text = noImpl
    fun createComment(data : String) : Comment = noImpl
    fun createProcessingInstruction(target : String, data : String) : ProcessingInstruction = noImpl
    fun importNode(node : Node, deep : Boolean = false) : Node = noImpl
    fun adoptNode(node : Node) : Node = noImpl
    fun createAttribute(localName : String) : Attr = noImpl
    fun createAttributeNS(namespace : String?, name : String) : Attr = noImpl
    fun createEvent(interface_ : String) : Event = noImpl
    fun createRange() : Range = noImpl
    fun createNodeIterator(root : Node, whatToShow : Int = noImpl, filter : NodeFilter? = null) : NodeIterator = noImpl
    fun createTreeWalker(root : Node, whatToShow : Int = noImpl, filter : NodeFilter? = null) : TreeWalker = noImpl
    fun getSelection() : Selection = noImpl
    fun exitFullscreen() : Unit = noImpl
    fun enableStyleSheetsForSet(name : String?) : Unit = noImpl
    fun getElementById(elementId : String) : Element? = noImpl
    fun prepend(vararg nodes : dynamic) : Unit = noImpl
    fun append(vararg nodes : dynamic) : Unit = noImpl
    fun query(relativeSelectors : String) : Element? = noImpl
    fun queryAll(relativeSelectors : String) : dynamic = noImpl
    fun querySelector(selectors : String) : Element? = noImpl
    fun querySelectorAll(selectors : String) : NodeList = noImpl
}
native open class XMLDocument : Document() {
    fun load(url : String) : Boolean = noImpl
}
native trait HTMLElement : Element {
    var title : String
        get() = noImpl
        set(value) = noImpl
    var lang : String
        get() = noImpl
        set(value) = noImpl
    var translate : Boolean
        get() = noImpl
        set(value) = noImpl
    var dir : String
        get() = noImpl
        set(value) = noImpl
    var dataset : DOMStringMap
        get() = noImpl
        set(value) = noImpl
    var itemScope : Boolean
        get() = noImpl
        set(value) = noImpl
    var itemType : DOMSettableTokenList
        get() = noImpl
        set(value) = noImpl
    var itemId : String
        get() = noImpl
        set(value) = noImpl
    var itemRef : DOMSettableTokenList
        get() = noImpl
        set(value) = noImpl
    var itemProp : DOMSettableTokenList
        get() = noImpl
        set(value) = noImpl
    var properties : HTMLPropertiesCollection
        get() = noImpl
        set(value) = noImpl
    var itemValue : Any
        get() = noImpl
        set(value) = noImpl
    var hidden : Boolean
        get() = noImpl
        set(value) = noImpl
    var tabIndex : Int
        get() = noImpl
        set(value) = noImpl
    var accessKey : String
        get() = noImpl
        set(value) = noImpl
    var accessKeyLabel : String
        get() = noImpl
        set(value) = noImpl
    var draggable : Boolean
        get() = noImpl
        set(value) = noImpl
    var dropzone : DOMSettableTokenList
        get() = noImpl
        set(value) = noImpl
    var contextMenu : HTMLMenuElement?
        get() = noImpl
        set(value) = noImpl
    var spellcheck : Boolean
        get() = noImpl
        set(value) = noImpl
    var commandType : String?
        get() = noImpl
        set(value) = noImpl
    var commandLabel : String?
        get() = noImpl
        set(value) = noImpl
    var commandIcon : String?
        get() = noImpl
        set(value) = noImpl
    var commandHidden : Boolean?
        get() = noImpl
        set(value) = noImpl
    var commandDisabled : Boolean?
        get() = noImpl
        set(value) = noImpl
    var commandChecked : Boolean?
        get() = noImpl
        set(value) = noImpl
    var onabort : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onautocomplete : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onautocompleteerror : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onblur : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var oncancel : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var oncanplay : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var oncanplaythrough : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onchange : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onclick : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onclose : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var oncontextmenu : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var oncuechange : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var ondblclick : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var ondrag : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var ondragend : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var ondragenter : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var ondragexit : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var ondragleave : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var ondragover : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var ondragstart : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var ondrop : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var ondurationchange : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onemptied : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onended : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onerror : dynamic
        get() = noImpl
        set(value) = noImpl
    var onfocus : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var oninput : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var oninvalid : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onkeydown : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onkeypress : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onkeyup : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onload : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onloadeddata : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onloadedmetadata : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onloadstart : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onmousedown : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onmouseenter : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onmouseleave : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onmousemove : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onmouseout : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onmouseover : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onmouseup : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onmousewheel : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onpause : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onplay : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onplaying : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onprogress : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onratechange : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onreset : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onresize : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onscroll : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onseeked : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onseeking : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onselect : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onshow : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onsort : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onstalled : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onsubmit : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onsuspend : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var ontimeupdate : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var ontoggle : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onvolumechange : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onwaiting : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var contentEditable : String
        get() = noImpl
        set(value) = noImpl
    var isContentEditable : Boolean
        get() = noImpl
        set(value) = noImpl
    var style : CSSStyleDeclaration
        get() = noImpl
        set(value) = noImpl
    fun click() : Unit = noImpl
    fun focus() : Unit = noImpl
    fun blur() : Unit = noImpl
    fun forceSpellCheck() : Unit = noImpl
}
native trait HTMLUnknownElement : HTMLElement {
}
native trait HTMLHtmlElement : HTMLElement {
    var version : String
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLHeadElement : HTMLElement {
}
native trait HTMLTitleElement : HTMLElement {
    var text : String
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLBaseElement : HTMLElement {
    var href : String
        get() = noImpl
        set(value) = noImpl
    var target : String
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLLinkElement : HTMLElement {
    var href : String
        get() = noImpl
        set(value) = noImpl
    var crossOrigin : String?
        get() = noImpl
        set(value) = noImpl
    var rel : String
        get() = noImpl
        set(value) = noImpl
    var relList : DOMTokenList
        get() = noImpl
        set(value) = noImpl
    var media : String
        get() = noImpl
        set(value) = noImpl
    var hreflang : String
        get() = noImpl
        set(value) = noImpl
    var type : String
        get() = noImpl
        set(value) = noImpl
    var sizes : DOMSettableTokenList
        get() = noImpl
        set(value) = noImpl
    var charset : String
        get() = noImpl
        set(value) = noImpl
    var rev : String
        get() = noImpl
        set(value) = noImpl
    var target : String
        get() = noImpl
        set(value) = noImpl
    var sheet : StyleSheet?
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLMetaElement : HTMLElement {
    var name : String
        get() = noImpl
        set(value) = noImpl
    var httpEquiv : String
        get() = noImpl
        set(value) = noImpl
    var content : String
        get() = noImpl
        set(value) = noImpl
    var scheme : String
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLStyleElement : HTMLElement {
    var media : String
        get() = noImpl
        set(value) = noImpl
    var type : String
        get() = noImpl
        set(value) = noImpl
    var scoped : Boolean
        get() = noImpl
        set(value) = noImpl
    var sheet : StyleSheet?
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLBodyElement : HTMLElement {
    var text : String
        get() = noImpl
        set(value) = noImpl
    var link : String
        get() = noImpl
        set(value) = noImpl
    var vLink : String
        get() = noImpl
        set(value) = noImpl
    var aLink : String
        get() = noImpl
        set(value) = noImpl
    var bgColor : String
        get() = noImpl
        set(value) = noImpl
    var background : String
        get() = noImpl
        set(value) = noImpl
    var onafterprint : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onbeforeprint : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onbeforeunload : dynamic
        get() = noImpl
        set(value) = noImpl
    var onhashchange : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onlanguagechange : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onmessage : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onoffline : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var ononline : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onpagehide : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onpageshow : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onpopstate : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onstorage : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onunload : () -> Unit
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLHeadingElement : HTMLElement {
    var align : String
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLParagraphElement : HTMLElement {
    var align : String
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLHRElement : HTMLElement {
    var align : String
        get() = noImpl
        set(value) = noImpl
    var color : String
        get() = noImpl
        set(value) = noImpl
    var noShade : Boolean
        get() = noImpl
        set(value) = noImpl
    var size : String
        get() = noImpl
        set(value) = noImpl
    var width : String
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLPreElement : HTMLElement {
    var width : Int
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLQuoteElement : HTMLElement {
    var cite : String
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLOListElement : HTMLElement {
    var reversed : Boolean
        get() = noImpl
        set(value) = noImpl
    var start : Int
        get() = noImpl
        set(value) = noImpl
    var type : String
        get() = noImpl
        set(value) = noImpl
    var compact : Boolean
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLUListElement : HTMLElement {
    var compact : Boolean
        get() = noImpl
        set(value) = noImpl
    var type : String
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLLIElement : HTMLElement {
    var value : Int
        get() = noImpl
        set(value) = noImpl
    var type : String
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLDListElement : HTMLElement {
    var compact : Boolean
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLDivElement : HTMLElement {
    var align : String
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLAnchorElement : HTMLElement {
    var target : String
        get() = noImpl
        set(value) = noImpl
    var download : String
        get() = noImpl
        set(value) = noImpl
    var ping : DOMSettableTokenList
        get() = noImpl
        set(value) = noImpl
    var rel : String
        get() = noImpl
        set(value) = noImpl
    var relList : DOMTokenList
        get() = noImpl
        set(value) = noImpl
    var hreflang : String
        get() = noImpl
        set(value) = noImpl
    var type : String
        get() = noImpl
        set(value) = noImpl
    var text : String
        get() = noImpl
        set(value) = noImpl
    var coords : String
        get() = noImpl
        set(value) = noImpl
    var charset : String
        get() = noImpl
        set(value) = noImpl
    var name : String
        get() = noImpl
        set(value) = noImpl
    var rev : String
        get() = noImpl
        set(value) = noImpl
    var shape : String
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLDataElement : HTMLElement {
    var value : String
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLTimeElement : HTMLElement {
    var dateTime : String
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLSpanElement : HTMLElement {
}
native trait HTMLBRElement : HTMLElement {
    var clear : String
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLModElement : HTMLElement {
    var cite : String
        get() = noImpl
        set(value) = noImpl
    var dateTime : String
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLIFrameElement : HTMLElement {
    var src : String
        get() = noImpl
        set(value) = noImpl
    var srcdoc : String
        get() = noImpl
        set(value) = noImpl
    var name : String
        get() = noImpl
        set(value) = noImpl
    var sandbox : DOMSettableTokenList
        get() = noImpl
        set(value) = noImpl
    var seamless : Boolean
        get() = noImpl
        set(value) = noImpl
    var allowFullscreen : Boolean
        get() = noImpl
        set(value) = noImpl
    var width : String
        get() = noImpl
        set(value) = noImpl
    var height : String
        get() = noImpl
        set(value) = noImpl
    var contentDocument : Document?
        get() = noImpl
        set(value) = noImpl
    var contentWindow : Window?
        get() = noImpl
        set(value) = noImpl
    var align : String
        get() = noImpl
        set(value) = noImpl
    var scrolling : String
        get() = noImpl
        set(value) = noImpl
    var frameBorder : String
        get() = noImpl
        set(value) = noImpl
    var longDesc : String
        get() = noImpl
        set(value) = noImpl
    var marginHeight : String
        get() = noImpl
        set(value) = noImpl
    var marginWidth : String
        get() = noImpl
        set(value) = noImpl
    fun getSVGDocument() : Document? = noImpl
}
native trait HTMLEmbedElement : HTMLElement {
    var src : String
        get() = noImpl
        set(value) = noImpl
    var type : String
        get() = noImpl
        set(value) = noImpl
    var width : String
        get() = noImpl
        set(value) = noImpl
    var height : String
        get() = noImpl
        set(value) = noImpl
    var align : String
        get() = noImpl
        set(value) = noImpl
    var name : String
        get() = noImpl
        set(value) = noImpl
    fun getSVGDocument() : Document? = noImpl
}
native trait HTMLObjectElement : HTMLElement {
    var data : String
        get() = noImpl
        set(value) = noImpl
    var type : String
        get() = noImpl
        set(value) = noImpl
    var typeMustMatch : Boolean
        get() = noImpl
        set(value) = noImpl
    var name : String
        get() = noImpl
        set(value) = noImpl
    var useMap : String
        get() = noImpl
        set(value) = noImpl
    var form : HTMLFormElement?
        get() = noImpl
        set(value) = noImpl
    var width : String
        get() = noImpl
        set(value) = noImpl
    var height : String
        get() = noImpl
        set(value) = noImpl
    var contentDocument : Document?
        get() = noImpl
        set(value) = noImpl
    var contentWindow : Window?
        get() = noImpl
        set(value) = noImpl
    var willValidate : Boolean
        get() = noImpl
        set(value) = noImpl
    var validity : ValidityState
        get() = noImpl
        set(value) = noImpl
    var validationMessage : String
        get() = noImpl
        set(value) = noImpl
    var align : String
        get() = noImpl
        set(value) = noImpl
    var archive : String
        get() = noImpl
        set(value) = noImpl
    var code : String
        get() = noImpl
        set(value) = noImpl
    var declare : Boolean
        get() = noImpl
        set(value) = noImpl
    var hspace : Int
        get() = noImpl
        set(value) = noImpl
    var standby : String
        get() = noImpl
        set(value) = noImpl
    var vspace : Int
        get() = noImpl
        set(value) = noImpl
    var codeBase : String
        get() = noImpl
        set(value) = noImpl
    var codeType : String
        get() = noImpl
        set(value) = noImpl
    var border : String
        get() = noImpl
        set(value) = noImpl
    fun getSVGDocument() : Document? = noImpl
    fun checkValidity() : Boolean = noImpl
    fun reportValidity() : Boolean = noImpl
    fun setCustomValidity(error : String) : Unit = noImpl
}
native trait HTMLParamElement : HTMLElement {
    var name : String
        get() = noImpl
        set(value) = noImpl
    var value : String
        get() = noImpl
        set(value) = noImpl
    var type : String
        get() = noImpl
        set(value) = noImpl
    var valueType : String
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLVideoElement : HTMLMediaElement {
    var width : Int
        get() = noImpl
        set(value) = noImpl
    var height : Int
        get() = noImpl
        set(value) = noImpl
    var videoWidth : Int
        get() = noImpl
        set(value) = noImpl
    var videoHeight : Int
        get() = noImpl
        set(value) = noImpl
    var poster : String
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLAudioElement : HTMLMediaElement {
}
native trait HTMLSourceElement : HTMLElement {
    var src : String
        get() = noImpl
        set(value) = noImpl
    var type : String
        get() = noImpl
        set(value) = noImpl
    var srcset : String
        get() = noImpl
        set(value) = noImpl
    var sizes : String
        get() = noImpl
        set(value) = noImpl
    var media : String
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLTrackElement : HTMLElement {
    var kind : String
        get() = noImpl
        set(value) = noImpl
    var src : String
        get() = noImpl
        set(value) = noImpl
    var srclang : String
        get() = noImpl
        set(value) = noImpl
    var label : String
        get() = noImpl
        set(value) = noImpl
    var default : Boolean
        get() = noImpl
        set(value) = noImpl
    var readyState : Short
        get() = noImpl
        set(value) = noImpl
    var track : TextTrack
        get() = noImpl
        set(value) = noImpl
companion object {
    val NONE : Short = 0
    val LOADING : Short = 1
    val LOADED : Short = 2
    val ERROR : Short = 3
}
}
native trait HTMLMediaElement : HTMLElement {
    var error : MediaError?
        get() = noImpl
        set(value) = noImpl
    var src : String
        get() = noImpl
        set(value) = noImpl
    var srcObject : dynamic
        get() = noImpl
        set(value) = noImpl
    var currentSrc : String
        get() = noImpl
        set(value) = noImpl
    var crossOrigin : String?
        get() = noImpl
        set(value) = noImpl
    var networkState : Short
        get() = noImpl
        set(value) = noImpl
    var preload : String
        get() = noImpl
        set(value) = noImpl
    var buffered : TimeRanges
        get() = noImpl
        set(value) = noImpl
    var readyState : Short
        get() = noImpl
        set(value) = noImpl
    var seeking : Boolean
        get() = noImpl
        set(value) = noImpl
    var currentTime : Double
        get() = noImpl
        set(value) = noImpl
    var duration : Double
        get() = noImpl
        set(value) = noImpl
    var paused : Boolean
        get() = noImpl
        set(value) = noImpl
    var defaultPlaybackRate : Double
        get() = noImpl
        set(value) = noImpl
    var playbackRate : Double
        get() = noImpl
        set(value) = noImpl
    var played : TimeRanges
        get() = noImpl
        set(value) = noImpl
    var seekable : TimeRanges
        get() = noImpl
        set(value) = noImpl
    var ended : Boolean
        get() = noImpl
        set(value) = noImpl
    var autoplay : Boolean
        get() = noImpl
        set(value) = noImpl
    var loop : Boolean
        get() = noImpl
        set(value) = noImpl
    var mediaGroup : String
        get() = noImpl
        set(value) = noImpl
    var controller : MediaController?
        get() = noImpl
        set(value) = noImpl
    var controls : Boolean
        get() = noImpl
        set(value) = noImpl
    var volume : Double
        get() = noImpl
        set(value) = noImpl
    var muted : Boolean
        get() = noImpl
        set(value) = noImpl
    var defaultMuted : Boolean
        get() = noImpl
        set(value) = noImpl
    var audioTracks : AudioTrackList
        get() = noImpl
        set(value) = noImpl
    var videoTracks : VideoTrackList
        get() = noImpl
        set(value) = noImpl
    var textTracks : TextTrackList
        get() = noImpl
        set(value) = noImpl
    fun load() : Unit = noImpl
    fun canPlayType(type : String) : String = noImpl
    fun fastSeek(time : Double) : Unit = noImpl
    fun getStartDate() : dynamic = noImpl
    fun play() : Unit = noImpl
    fun pause() : Unit = noImpl
    fun addTextTrack(kind : String, label : String = "", language : String = "") : TextTrack = noImpl
companion object {
    val NETWORK_EMPTY : Short = 0
    val NETWORK_IDLE : Short = 1
    val NETWORK_LOADING : Short = 2
    val NETWORK_NO_SOURCE : Short = 3
    val HAVE_NOTHING : Short = 0
    val HAVE_METADATA : Short = 1
    val HAVE_CURRENT_DATA : Short = 2
    val HAVE_FUTURE_DATA : Short = 3
    val HAVE_ENOUGH_DATA : Short = 4
}
}
native trait MediaError {
    var code : Short
        get() = noImpl
        set(value) = noImpl
companion object {
    val MEDIA_ERR_ABORTED : Short = 1
    val MEDIA_ERR_NETWORK : Short = 2
    val MEDIA_ERR_DECODE : Short = 3
    val MEDIA_ERR_SRC_NOT_SUPPORTED : Short = 4
}
}
native trait AudioTrackList : EventTarget {
    var length : Int
        get() = noImpl
        set(value) = noImpl
    var onchange : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onaddtrack : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onremovetrack : () -> Unit
        get() = noImpl
        set(value) = noImpl
    nativeGetter fun get(index : Int) : AudioTrack? = noImpl
    fun getTrackById(id : String) : AudioTrack? = noImpl
}
native trait AudioTrack {
    var id : String
        get() = noImpl
        set(value) = noImpl
    var kind : String
        get() = noImpl
        set(value) = noImpl
    var label : String
        get() = noImpl
        set(value) = noImpl
    var language : String
        get() = noImpl
        set(value) = noImpl
    var enabled : Boolean
        get() = noImpl
        set(value) = noImpl
}
native trait VideoTrackList : EventTarget {
    var length : Int
        get() = noImpl
        set(value) = noImpl
    var selectedIndex : Int
        get() = noImpl
        set(value) = noImpl
    var onchange : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onaddtrack : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onremovetrack : () -> Unit
        get() = noImpl
        set(value) = noImpl
    nativeGetter fun get(index : Int) : VideoTrack? = noImpl
    fun getTrackById(id : String) : VideoTrack? = noImpl
}
native trait VideoTrack {
    var id : String
        get() = noImpl
        set(value) = noImpl
    var kind : String
        get() = noImpl
        set(value) = noImpl
    var label : String
        get() = noImpl
        set(value) = noImpl
    var language : String
        get() = noImpl
        set(value) = noImpl
    var selected : Boolean
        get() = noImpl
        set(value) = noImpl
}
native open class MediaController : EventTarget {
    var readyState : Short
        get() = noImpl
        set(value) = noImpl
    var buffered : TimeRanges
        get() = noImpl
        set(value) = noImpl
    var seekable : TimeRanges
        get() = noImpl
        set(value) = noImpl
    var duration : Double
        get() = noImpl
        set(value) = noImpl
    var currentTime : Double
        get() = noImpl
        set(value) = noImpl
    var paused : Boolean
        get() = noImpl
        set(value) = noImpl
    var playbackState : String
        get() = noImpl
        set(value) = noImpl
    var played : TimeRanges
        get() = noImpl
        set(value) = noImpl
    var defaultPlaybackRate : Double
        get() = noImpl
        set(value) = noImpl
    var playbackRate : Double
        get() = noImpl
        set(value) = noImpl
    var volume : Double
        get() = noImpl
        set(value) = noImpl
    var muted : Boolean
        get() = noImpl
        set(value) = noImpl
    var onemptied : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onloadedmetadata : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onloadeddata : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var oncanplay : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var oncanplaythrough : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onplaying : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onended : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onwaiting : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var ondurationchange : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var ontimeupdate : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onplay : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onpause : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onratechange : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onvolumechange : () -> Unit
        get() = noImpl
        set(value) = noImpl
    fun pause() : Unit = noImpl
    fun unpause() : Unit = noImpl
    fun play() : Unit = noImpl
}
native trait TextTrackList : EventTarget {
    var length : Int
        get() = noImpl
        set(value) = noImpl
    var onchange : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onaddtrack : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onremovetrack : () -> Unit
        get() = noImpl
        set(value) = noImpl
    nativeGetter fun get(index : Int) : TextTrack? = noImpl
    fun getTrackById(id : String) : TextTrack? = noImpl
}
native trait TextTrack : EventTarget {
    var kind : String
        get() = noImpl
        set(value) = noImpl
    var label : String
        get() = noImpl
        set(value) = noImpl
    var language : String
        get() = noImpl
        set(value) = noImpl
    var id : String
        get() = noImpl
        set(value) = noImpl
    var inBandMetadataTrackDispatchType : String
        get() = noImpl
        set(value) = noImpl
    var mode : String
        get() = noImpl
        set(value) = noImpl
    var cues : TextTrackCueList?
        get() = noImpl
        set(value) = noImpl
    var activeCues : TextTrackCueList?
        get() = noImpl
        set(value) = noImpl
    var oncuechange : () -> Unit
        get() = noImpl
        set(value) = noImpl
    fun addCue(cue : TextTrackCue) : Unit = noImpl
    fun removeCue(cue : TextTrackCue) : Unit = noImpl
}
native trait TextTrackCueList {
    var length : Int
        get() = noImpl
        set(value) = noImpl
    nativeGetter fun get(index : Int) : TextTrackCue? = noImpl
    fun getCueById(id : String) : TextTrackCue? = noImpl
}
native trait TextTrackCue : EventTarget {
    var track : TextTrack?
        get() = noImpl
        set(value) = noImpl
    var id : String
        get() = noImpl
        set(value) = noImpl
    var startTime : Double
        get() = noImpl
        set(value) = noImpl
    var endTime : Double
        get() = noImpl
        set(value) = noImpl
    var pauseOnExit : Boolean
        get() = noImpl
        set(value) = noImpl
    var onenter : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onexit : () -> Unit
        get() = noImpl
        set(value) = noImpl
}
native trait TimeRanges {
    var length : Int
        get() = noImpl
        set(value) = noImpl
    fun start(index : Int) : Double = noImpl
    fun end(index : Int) : Double = noImpl
}
native open class TrackEvent(type : String, eventInitDict : TrackEventInit = noImpl) : Event(type, eventInitDict) {
    var track : dynamic
        get() = noImpl
        set(value) = noImpl
}
native open class TrackEventInit : EventInit() {
    var track : dynamic
}
native trait HTMLMapElement : HTMLElement {
    var name : String
        get() = noImpl
        set(value) = noImpl
    var areas : HTMLCollection
        get() = noImpl
        set(value) = noImpl
    var images : HTMLCollection
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLAreaElement : HTMLElement {
    var alt : String
        get() = noImpl
        set(value) = noImpl
    var coords : String
        get() = noImpl
        set(value) = noImpl
    var shape : String
        get() = noImpl
        set(value) = noImpl
    var target : String
        get() = noImpl
        set(value) = noImpl
    var download : String
        get() = noImpl
        set(value) = noImpl
    var ping : DOMSettableTokenList
        get() = noImpl
        set(value) = noImpl
    var rel : String
        get() = noImpl
        set(value) = noImpl
    var relList : DOMTokenList
        get() = noImpl
        set(value) = noImpl
    var hreflang : String
        get() = noImpl
        set(value) = noImpl
    var type : String
        get() = noImpl
        set(value) = noImpl
    var noHref : Boolean
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLTableElement : HTMLElement {
    var caption : HTMLTableCaptionElement?
        get() = noImpl
        set(value) = noImpl
    var tHead : HTMLTableSectionElement?
        get() = noImpl
        set(value) = noImpl
    var tFoot : HTMLTableSectionElement?
        get() = noImpl
        set(value) = noImpl
    var tBodies : HTMLCollection
        get() = noImpl
        set(value) = noImpl
    var rows : HTMLCollection
        get() = noImpl
        set(value) = noImpl
    var sortable : Boolean
        get() = noImpl
        set(value) = noImpl
    var align : String
        get() = noImpl
        set(value) = noImpl
    var border : String
        get() = noImpl
        set(value) = noImpl
    var frame : String
        get() = noImpl
        set(value) = noImpl
    var rules : String
        get() = noImpl
        set(value) = noImpl
    var summary : String
        get() = noImpl
        set(value) = noImpl
    var width : String
        get() = noImpl
        set(value) = noImpl
    var bgColor : String
        get() = noImpl
        set(value) = noImpl
    var cellPadding : String
        get() = noImpl
        set(value) = noImpl
    var cellSpacing : String
        get() = noImpl
        set(value) = noImpl
    fun createCaption() : HTMLElement = noImpl
    fun deleteCaption() : Unit = noImpl
    fun createTHead() : HTMLElement = noImpl
    fun deleteTHead() : Unit = noImpl
    fun createTFoot() : HTMLElement = noImpl
    fun deleteTFoot() : Unit = noImpl
    fun createTBody() : HTMLElement = noImpl
    fun insertRow(index : Int = -1) : HTMLElement = noImpl
    fun deleteRow(index : Int) : Unit = noImpl
    fun stopSorting() : Unit = noImpl
}
native trait HTMLTableCaptionElement : HTMLElement {
    var align : String
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLTableColElement : HTMLElement {
    var span : Int
        get() = noImpl
        set(value) = noImpl
    var align : String
        get() = noImpl
        set(value) = noImpl
    var ch : String
        get() = noImpl
        set(value) = noImpl
    var chOff : String
        get() = noImpl
        set(value) = noImpl
    var vAlign : String
        get() = noImpl
        set(value) = noImpl
    var width : String
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLTableSectionElement : HTMLElement {
    var rows : HTMLCollection
        get() = noImpl
        set(value) = noImpl
    var align : String
        get() = noImpl
        set(value) = noImpl
    var ch : String
        get() = noImpl
        set(value) = noImpl
    var chOff : String
        get() = noImpl
        set(value) = noImpl
    var vAlign : String
        get() = noImpl
        set(value) = noImpl
    fun insertRow(index : Int = -1) : HTMLElement = noImpl
    fun deleteRow(index : Int) : Unit = noImpl
}
native trait HTMLTableRowElement : HTMLElement {
    var rowIndex : Int
        get() = noImpl
        set(value) = noImpl
    var sectionRowIndex : Int
        get() = noImpl
        set(value) = noImpl
    var cells : HTMLCollection
        get() = noImpl
        set(value) = noImpl
    var align : String
        get() = noImpl
        set(value) = noImpl
    var ch : String
        get() = noImpl
        set(value) = noImpl
    var chOff : String
        get() = noImpl
        set(value) = noImpl
    var vAlign : String
        get() = noImpl
        set(value) = noImpl
    var bgColor : String
        get() = noImpl
        set(value) = noImpl
    fun insertCell(index : Int = -1) : HTMLElement = noImpl
    fun deleteCell(index : Int) : Unit = noImpl
}
native trait HTMLTableDataCellElement : HTMLTableCellElement {
    var abbr : String
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLTableHeaderCellElement : HTMLTableCellElement {
    var scope : String
        get() = noImpl
        set(value) = noImpl
    var abbr : String
        get() = noImpl
        set(value) = noImpl
    var sorted : String
        get() = noImpl
        set(value) = noImpl
    fun sort() : Unit = noImpl
}
native trait HTMLTableCellElement : HTMLElement {
    var colSpan : Int
        get() = noImpl
        set(value) = noImpl
    var rowSpan : Int
        get() = noImpl
        set(value) = noImpl
    var headers : DOMSettableTokenList
        get() = noImpl
        set(value) = noImpl
    var cellIndex : Int
        get() = noImpl
        set(value) = noImpl
    var align : String
        get() = noImpl
        set(value) = noImpl
    var axis : String
        get() = noImpl
        set(value) = noImpl
    var height : String
        get() = noImpl
        set(value) = noImpl
    var width : String
        get() = noImpl
        set(value) = noImpl
    var ch : String
        get() = noImpl
        set(value) = noImpl
    var chOff : String
        get() = noImpl
        set(value) = noImpl
    var noWrap : Boolean
        get() = noImpl
        set(value) = noImpl
    var vAlign : String
        get() = noImpl
        set(value) = noImpl
    var bgColor : String
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLFormElement : HTMLElement {
    var acceptCharset : String
        get() = noImpl
        set(value) = noImpl
    var action : String
        get() = noImpl
        set(value) = noImpl
    var autocomplete : String
        get() = noImpl
        set(value) = noImpl
    var enctype : String
        get() = noImpl
        set(value) = noImpl
    var encoding : String
        get() = noImpl
        set(value) = noImpl
    var method : String
        get() = noImpl
        set(value) = noImpl
    var name : String
        get() = noImpl
        set(value) = noImpl
    var noValidate : Boolean
        get() = noImpl
        set(value) = noImpl
    var target : String
        get() = noImpl
        set(value) = noImpl
    var elements : HTMLFormControlsCollection
        get() = noImpl
        set(value) = noImpl
    var length : Int
        get() = noImpl
        set(value) = noImpl
    nativeGetter fun get(index : Int) : Element? = noImpl
    nativeGetter fun get(name : String) : dynamic = noImpl
    fun submit() : Unit = noImpl
    fun reset() : Unit = noImpl
    fun checkValidity() : Boolean = noImpl
    fun reportValidity() : Boolean = noImpl
    fun requestAutocomplete() : Unit = noImpl
}
native trait HTMLLabelElement : HTMLElement {
    var form : HTMLFormElement?
        get() = noImpl
        set(value) = noImpl
    var htmlFor : String
        get() = noImpl
        set(value) = noImpl
    var control : HTMLElement?
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLInputElement : HTMLElement {
    var accept : String
        get() = noImpl
        set(value) = noImpl
    var alt : String
        get() = noImpl
        set(value) = noImpl
    var autocomplete : String
        get() = noImpl
        set(value) = noImpl
    var autofocus : Boolean
        get() = noImpl
        set(value) = noImpl
    var defaultChecked : Boolean
        get() = noImpl
        set(value) = noImpl
    var checked : Boolean
        get() = noImpl
        set(value) = noImpl
    var dirName : String
        get() = noImpl
        set(value) = noImpl
    var disabled : Boolean
        get() = noImpl
        set(value) = noImpl
    var form : HTMLFormElement?
        get() = noImpl
        set(value) = noImpl
    var files : FileList?
        get() = noImpl
        set(value) = noImpl
    var formAction : String
        get() = noImpl
        set(value) = noImpl
    var formEnctype : String
        get() = noImpl
        set(value) = noImpl
    var formMethod : String
        get() = noImpl
        set(value) = noImpl
    var formNoValidate : Boolean
        get() = noImpl
        set(value) = noImpl
    var formTarget : String
        get() = noImpl
        set(value) = noImpl
    var height : Int
        get() = noImpl
        set(value) = noImpl
    var indeterminate : Boolean
        get() = noImpl
        set(value) = noImpl
    var inputMode : String
        get() = noImpl
        set(value) = noImpl
    var list : HTMLElement?
        get() = noImpl
        set(value) = noImpl
    var max : String
        get() = noImpl
        set(value) = noImpl
    var maxLength : Int
        get() = noImpl
        set(value) = noImpl
    var min : String
        get() = noImpl
        set(value) = noImpl
    var minLength : Int
        get() = noImpl
        set(value) = noImpl
    var multiple : Boolean
        get() = noImpl
        set(value) = noImpl
    var name : String
        get() = noImpl
        set(value) = noImpl
    var pattern : String
        get() = noImpl
        set(value) = noImpl
    var placeholder : String
        get() = noImpl
        set(value) = noImpl
    var readOnly : Boolean
        get() = noImpl
        set(value) = noImpl
    var required : Boolean
        get() = noImpl
        set(value) = noImpl
    var size : Int
        get() = noImpl
        set(value) = noImpl
    var src : String
        get() = noImpl
        set(value) = noImpl
    var step : String
        get() = noImpl
        set(value) = noImpl
    var type : String
        get() = noImpl
        set(value) = noImpl
    var defaultValue : String
        get() = noImpl
        set(value) = noImpl
    var value : String
        get() = noImpl
        set(value) = noImpl
    var valueAsDate : dynamic
        get() = noImpl
        set(value) = noImpl
    var valueAsNumber : Double
        get() = noImpl
        set(value) = noImpl
    var valueLow : Double
        get() = noImpl
        set(value) = noImpl
    var valueHigh : Double
        get() = noImpl
        set(value) = noImpl
    var width : Int
        get() = noImpl
        set(value) = noImpl
    var willValidate : Boolean
        get() = noImpl
        set(value) = noImpl
    var validity : ValidityState
        get() = noImpl
        set(value) = noImpl
    var validationMessage : String
        get() = noImpl
        set(value) = noImpl
    var labels : NodeList
        get() = noImpl
        set(value) = noImpl
    var selectionStart : Int
        get() = noImpl
        set(value) = noImpl
    var selectionEnd : Int
        get() = noImpl
        set(value) = noImpl
    var selectionDirection : String
        get() = noImpl
        set(value) = noImpl
    var align : String
        get() = noImpl
        set(value) = noImpl
    var useMap : String
        get() = noImpl
        set(value) = noImpl
    fun stepUp(n : Int = 1) : Unit = noImpl
    fun stepDown(n : Int = 1) : Unit = noImpl
    fun checkValidity() : Boolean = noImpl
    fun reportValidity() : Boolean = noImpl
    fun setCustomValidity(error : String) : Unit = noImpl
    fun select() : Unit = noImpl
    fun setRangeText(replacement : String) : Unit = noImpl
    fun setRangeText(replacement : String, start : Int, end : Int, selectionMode : String = "preserve") : Unit = noImpl
    fun setSelectionRange(start : Int, end : Int, direction : String = noImpl) : Unit = noImpl
}
native trait HTMLButtonElement : HTMLElement {
    var autofocus : Boolean
        get() = noImpl
        set(value) = noImpl
    var disabled : Boolean
        get() = noImpl
        set(value) = noImpl
    var form : HTMLFormElement?
        get() = noImpl
        set(value) = noImpl
    var formAction : String
        get() = noImpl
        set(value) = noImpl
    var formEnctype : String
        get() = noImpl
        set(value) = noImpl
    var formMethod : String
        get() = noImpl
        set(value) = noImpl
    var formNoValidate : Boolean
        get() = noImpl
        set(value) = noImpl
    var formTarget : String
        get() = noImpl
        set(value) = noImpl
    var name : String
        get() = noImpl
        set(value) = noImpl
    var type : String
        get() = noImpl
        set(value) = noImpl
    var value : String
        get() = noImpl
        set(value) = noImpl
    var menu : HTMLMenuElement?
        get() = noImpl
        set(value) = noImpl
    var willValidate : Boolean
        get() = noImpl
        set(value) = noImpl
    var validity : ValidityState
        get() = noImpl
        set(value) = noImpl
    var validationMessage : String
        get() = noImpl
        set(value) = noImpl
    var labels : NodeList
        get() = noImpl
        set(value) = noImpl
    fun checkValidity() : Boolean = noImpl
    fun reportValidity() : Boolean = noImpl
    fun setCustomValidity(error : String) : Unit = noImpl
}
native trait HTMLSelectElement : HTMLElement {
    var autocomplete : String
        get() = noImpl
        set(value) = noImpl
    var autofocus : Boolean
        get() = noImpl
        set(value) = noImpl
    var disabled : Boolean
        get() = noImpl
        set(value) = noImpl
    var form : HTMLFormElement?
        get() = noImpl
        set(value) = noImpl
    var multiple : Boolean
        get() = noImpl
        set(value) = noImpl
    var name : String
        get() = noImpl
        set(value) = noImpl
    var required : Boolean
        get() = noImpl
        set(value) = noImpl
    var size : Int
        get() = noImpl
        set(value) = noImpl
    var type : String
        get() = noImpl
        set(value) = noImpl
    var options : HTMLOptionsCollection
        get() = noImpl
        set(value) = noImpl
    var length : Int
        get() = noImpl
        set(value) = noImpl
    var selectedOptions : HTMLCollection
        get() = noImpl
        set(value) = noImpl
    var selectedIndex : Int
        get() = noImpl
        set(value) = noImpl
    var value : String
        get() = noImpl
        set(value) = noImpl
    var willValidate : Boolean
        get() = noImpl
        set(value) = noImpl
    var validity : ValidityState
        get() = noImpl
        set(value) = noImpl
    var validationMessage : String
        get() = noImpl
        set(value) = noImpl
    var labels : NodeList
        get() = noImpl
        set(value) = noImpl
    fun item(index : Int) : Element? = noImpl
    nativeGetter fun get(index : Int) : Element? = noImpl
    fun namedItem(name : String) : HTMLOptionElement? = noImpl
    fun add(element : UnionHTMLOptGroupElementOrHTMLOptionElement, before : dynamic = null) : Unit = noImpl
    fun remove(index : Int) : Unit = noImpl
    fun set(index : Int, option : HTMLOptionElement?) : Unit? = noImpl
    fun checkValidity() : Boolean = noImpl
    fun reportValidity() : Boolean = noImpl
    fun setCustomValidity(error : String) : Unit = noImpl
}
native trait HTMLDataListElement : HTMLElement {
    var options : HTMLCollection
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLOptGroupElement : HTMLElement, UnionHTMLOptGroupElementOrHTMLOptionElement {
    var disabled : Boolean
        get() = noImpl
        set(value) = noImpl
    var label : String
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLOptionElement : HTMLElement, UnionHTMLOptGroupElementOrHTMLOptionElement {
    var disabled : Boolean
        get() = noImpl
        set(value) = noImpl
    var form : HTMLFormElement?
        get() = noImpl
        set(value) = noImpl
    var label : String
        get() = noImpl
        set(value) = noImpl
    var defaultSelected : Boolean
        get() = noImpl
        set(value) = noImpl
    var selected : Boolean
        get() = noImpl
        set(value) = noImpl
    var value : String
        get() = noImpl
        set(value) = noImpl
    var text : String
        get() = noImpl
        set(value) = noImpl
    var index : Int
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLTextAreaElement : HTMLElement {
    var autocomplete : String
        get() = noImpl
        set(value) = noImpl
    var autofocus : Boolean
        get() = noImpl
        set(value) = noImpl
    var cols : Int
        get() = noImpl
        set(value) = noImpl
    var dirName : String
        get() = noImpl
        set(value) = noImpl
    var disabled : Boolean
        get() = noImpl
        set(value) = noImpl
    var form : HTMLFormElement?
        get() = noImpl
        set(value) = noImpl
    var inputMode : String
        get() = noImpl
        set(value) = noImpl
    var maxLength : Int
        get() = noImpl
        set(value) = noImpl
    var minLength : Int
        get() = noImpl
        set(value) = noImpl
    var name : String
        get() = noImpl
        set(value) = noImpl
    var placeholder : String
        get() = noImpl
        set(value) = noImpl
    var readOnly : Boolean
        get() = noImpl
        set(value) = noImpl
    var required : Boolean
        get() = noImpl
        set(value) = noImpl
    var rows : Int
        get() = noImpl
        set(value) = noImpl
    var wrap : String
        get() = noImpl
        set(value) = noImpl
    var type : String
        get() = noImpl
        set(value) = noImpl
    var defaultValue : String
        get() = noImpl
        set(value) = noImpl
    var value : String
        get() = noImpl
        set(value) = noImpl
    var textLength : Int
        get() = noImpl
        set(value) = noImpl
    var willValidate : Boolean
        get() = noImpl
        set(value) = noImpl
    var validity : ValidityState
        get() = noImpl
        set(value) = noImpl
    var validationMessage : String
        get() = noImpl
        set(value) = noImpl
    var labels : NodeList
        get() = noImpl
        set(value) = noImpl
    var selectionStart : Int
        get() = noImpl
        set(value) = noImpl
    var selectionEnd : Int
        get() = noImpl
        set(value) = noImpl
    var selectionDirection : String
        get() = noImpl
        set(value) = noImpl
    fun checkValidity() : Boolean = noImpl
    fun reportValidity() : Boolean = noImpl
    fun setCustomValidity(error : String) : Unit = noImpl
    fun select() : Unit = noImpl
    fun setRangeText(replacement : String) : Unit = noImpl
    fun setRangeText(replacement : String, start : Int, end : Int, selectionMode : String = "preserve") : Unit = noImpl
    fun setSelectionRange(start : Int, end : Int, direction : String = noImpl) : Unit = noImpl
}
native trait HTMLKeygenElement : HTMLElement {
    var autofocus : Boolean
        get() = noImpl
        set(value) = noImpl
    var challenge : String
        get() = noImpl
        set(value) = noImpl
    var disabled : Boolean
        get() = noImpl
        set(value) = noImpl
    var form : HTMLFormElement?
        get() = noImpl
        set(value) = noImpl
    var keytype : String
        get() = noImpl
        set(value) = noImpl
    var name : String
        get() = noImpl
        set(value) = noImpl
    var type : String
        get() = noImpl
        set(value) = noImpl
    var willValidate : Boolean
        get() = noImpl
        set(value) = noImpl
    var validity : ValidityState
        get() = noImpl
        set(value) = noImpl
    var validationMessage : String
        get() = noImpl
        set(value) = noImpl
    var labels : NodeList
        get() = noImpl
        set(value) = noImpl
    fun checkValidity() : Boolean = noImpl
    fun reportValidity() : Boolean = noImpl
    fun setCustomValidity(error : String) : Unit = noImpl
}
native trait HTMLOutputElement : HTMLElement {
    var htmlFor : DOMSettableTokenList
        get() = noImpl
        set(value) = noImpl
    var form : HTMLFormElement?
        get() = noImpl
        set(value) = noImpl
    var name : String
        get() = noImpl
        set(value) = noImpl
    var type : String
        get() = noImpl
        set(value) = noImpl
    var defaultValue : String
        get() = noImpl
        set(value) = noImpl
    var value : String
        get() = noImpl
        set(value) = noImpl
    var willValidate : Boolean
        get() = noImpl
        set(value) = noImpl
    var validity : ValidityState
        get() = noImpl
        set(value) = noImpl
    var validationMessage : String
        get() = noImpl
        set(value) = noImpl
    var labels : NodeList
        get() = noImpl
        set(value) = noImpl
    fun checkValidity() : Boolean = noImpl
    fun reportValidity() : Boolean = noImpl
    fun setCustomValidity(error : String) : Unit = noImpl
}
native trait HTMLProgressElement : HTMLElement {
    var value : Double
        get() = noImpl
        set(value) = noImpl
    var max : Double
        get() = noImpl
        set(value) = noImpl
    var position : Double
        get() = noImpl
        set(value) = noImpl
    var labels : NodeList
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLMeterElement : HTMLElement {
    var value : Double
        get() = noImpl
        set(value) = noImpl
    var min : Double
        get() = noImpl
        set(value) = noImpl
    var max : Double
        get() = noImpl
        set(value) = noImpl
    var low : Double
        get() = noImpl
        set(value) = noImpl
    var high : Double
        get() = noImpl
        set(value) = noImpl
    var optimum : Double
        get() = noImpl
        set(value) = noImpl
    var labels : NodeList
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLFieldSetElement : HTMLElement {
    var disabled : Boolean
        get() = noImpl
        set(value) = noImpl
    var form : HTMLFormElement?
        get() = noImpl
        set(value) = noImpl
    var name : String
        get() = noImpl
        set(value) = noImpl
    var type : String
        get() = noImpl
        set(value) = noImpl
    var elements : HTMLFormControlsCollection
        get() = noImpl
        set(value) = noImpl
    var willValidate : Boolean
        get() = noImpl
        set(value) = noImpl
    var validity : ValidityState
        get() = noImpl
        set(value) = noImpl
    var validationMessage : String
        get() = noImpl
        set(value) = noImpl
    fun checkValidity() : Boolean = noImpl
    fun reportValidity() : Boolean = noImpl
    fun setCustomValidity(error : String) : Unit = noImpl
}
native trait HTMLLegendElement : HTMLElement {
    var form : HTMLFormElement?
        get() = noImpl
        set(value) = noImpl
    var align : String
        get() = noImpl
        set(value) = noImpl
}
native open class AutocompleteErrorEvent(type : String, eventInitDict : AutocompleteErrorEventInit = noImpl) : Event(type, eventInitDict) {
    var reason : String
        get() = noImpl
        set(value) = noImpl
}
native open class AutocompleteErrorEventInit : EventInit() {
    var reason : dynamic
}
native trait ValidityState {
    var valueMissing : Boolean
        get() = noImpl
        set(value) = noImpl
    var typeMismatch : Boolean
        get() = noImpl
        set(value) = noImpl
    var patternMismatch : Boolean
        get() = noImpl
        set(value) = noImpl
    var tooLong : Boolean
        get() = noImpl
        set(value) = noImpl
    var tooShort : Boolean
        get() = noImpl
        set(value) = noImpl
    var rangeUnderflow : Boolean
        get() = noImpl
        set(value) = noImpl
    var rangeOverflow : Boolean
        get() = noImpl
        set(value) = noImpl
    var stepMismatch : Boolean
        get() = noImpl
        set(value) = noImpl
    var badInput : Boolean
        get() = noImpl
        set(value) = noImpl
    var customError : Boolean
        get() = noImpl
        set(value) = noImpl
    var valid : Boolean
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLDetailsElement : HTMLElement {
    var open : Boolean
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLMenuElement : HTMLElement {
    var type : String
        get() = noImpl
        set(value) = noImpl
    var label : String
        get() = noImpl
        set(value) = noImpl
    var compact : Boolean
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLMenuItemElement : HTMLElement {
    var type : String
        get() = noImpl
        set(value) = noImpl
    var label : String
        get() = noImpl
        set(value) = noImpl
    var icon : String
        get() = noImpl
        set(value) = noImpl
    var disabled : Boolean
        get() = noImpl
        set(value) = noImpl
    var checked : Boolean
        get() = noImpl
        set(value) = noImpl
    var radiogroup : String
        get() = noImpl
        set(value) = noImpl
    var default : Boolean
        get() = noImpl
        set(value) = noImpl
    var command : HTMLElement?
        get() = noImpl
        set(value) = noImpl
}
native open class RelatedEvent(type : String, eventInitDict : RelatedEventInit = noImpl) : Event(type, eventInitDict) {
    var relatedTarget : EventTarget?
        get() = noImpl
        set(value) = noImpl
}
native open class RelatedEventInit : EventInit() {
    var relatedTarget : dynamic
}
native trait HTMLDialogElement : HTMLElement {
    var open : Boolean
        get() = noImpl
        set(value) = noImpl
    var returnValue : String
        get() = noImpl
        set(value) = noImpl
    fun show(anchor : UnionElementOrMouseEvent = noImpl) : Unit = noImpl
    fun showModal(anchor : UnionElementOrMouseEvent = noImpl) : Unit = noImpl
    fun close(returnValue : String = noImpl) : Unit = noImpl
}
native trait HTMLScriptElement : HTMLElement {
    var src : String
        get() = noImpl
        set(value) = noImpl
    var type : String
        get() = noImpl
        set(value) = noImpl
    var charset : String
        get() = noImpl
        set(value) = noImpl
    var async : Boolean
        get() = noImpl
        set(value) = noImpl
    var defer : Boolean
        get() = noImpl
        set(value) = noImpl
    var crossOrigin : String?
        get() = noImpl
        set(value) = noImpl
    var text : String
        get() = noImpl
        set(value) = noImpl
    var event : String
        get() = noImpl
        set(value) = noImpl
    var htmlFor : String
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLTemplateElement : HTMLElement {
    var content : DocumentFragment
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLCanvasElement : HTMLElement {
    var width : Int
        get() = noImpl
        set(value) = noImpl
    var height : Int
        get() = noImpl
        set(value) = noImpl
    fun getContext(contextId : String, vararg arguments : Any) : dynamic = noImpl
    fun probablySupportsContext(contextId : String, vararg arguments : Any) : Boolean = noImpl
    fun setContext(context : dynamic) : Unit = noImpl
    fun transferControlToProxy() : CanvasProxy = noImpl
    fun toDataURL(type : String = noImpl, vararg arguments : Any) : String = noImpl
    fun toBlob(_callback : () -> Unit?, type : String = noImpl, vararg arguments : Any) : Unit = noImpl
}
native trait CanvasProxy {
    fun setContext(context : dynamic) : Unit = noImpl
}
native open class CanvasRenderingContext2DSettings {
    var alpha : dynamic = true
}
native open class CanvasRenderingContext2D {
    var canvas : HTMLCanvasElement
        get() = noImpl
        set(value) = noImpl
    var width : Int
        get() = noImpl
        set(value) = noImpl
    var height : Int
        get() = noImpl
        set(value) = noImpl
    var currentTransform : dynamic
        get() = noImpl
        set(value) = noImpl
    var globalAlpha : Double
        get() = noImpl
        set(value) = noImpl
    var globalCompositeOperation : String
        get() = noImpl
        set(value) = noImpl
    var imageSmoothingEnabled : Boolean
        get() = noImpl
        set(value) = noImpl
    var strokeStyle : dynamic
        get() = noImpl
        set(value) = noImpl
    var fillStyle : dynamic
        get() = noImpl
        set(value) = noImpl
    var shadowOffsetX : Double
        get() = noImpl
        set(value) = noImpl
    var shadowOffsetY : Double
        get() = noImpl
        set(value) = noImpl
    var shadowBlur : Double
        get() = noImpl
        set(value) = noImpl
    var shadowColor : String
        get() = noImpl
        set(value) = noImpl
    var lineWidth : Double
        get() = noImpl
        set(value) = noImpl
    var lineCap : String
        get() = noImpl
        set(value) = noImpl
    var lineJoin : String
        get() = noImpl
        set(value) = noImpl
    var miterLimit : Double
        get() = noImpl
        set(value) = noImpl
    var lineDashOffset : Double
        get() = noImpl
        set(value) = noImpl
    var font : String
        get() = noImpl
        set(value) = noImpl
    var textAlign : String
        get() = noImpl
        set(value) = noImpl
    var textBaseline : String
        get() = noImpl
        set(value) = noImpl
    var direction : String
        get() = noImpl
        set(value) = noImpl
    fun commit() : Unit = noImpl
    fun save() : Unit = noImpl
    fun restore() : Unit = noImpl
    fun scale(x : Double, y : Double) : Unit = noImpl
    fun rotate(angle : Double) : Unit = noImpl
    fun translate(x : Double, y : Double) : Unit = noImpl
    fun transform(a : Double, b : Double, c : Double, d : Double, e : Double, f : Double) : Unit = noImpl
    fun setTransform(a : Double, b : Double, c : Double, d : Double, e : Double, f : Double) : Unit = noImpl
    fun resetTransform() : Unit = noImpl
    fun createLinearGradient(x0 : Double, y0 : Double, x1 : Double, y1 : Double) : CanvasGradient = noImpl
    fun createRadialGradient(x0 : Double, y0 : Double, r0 : Double, x1 : Double, y1 : Double, r1 : Double) : CanvasGradient = noImpl
    fun createPattern(image : dynamic, repetition : String) : CanvasPattern = noImpl
    fun clearRect(x : Double, y : Double, w : Double, h : Double) : Unit = noImpl
    fun fillRect(x : Double, y : Double, w : Double, h : Double) : Unit = noImpl
    fun strokeRect(x : Double, y : Double, w : Double, h : Double) : Unit = noImpl
    fun beginPath() : Unit = noImpl
    fun fill(fillRule : String = "nonzero") : Unit = noImpl
    fun fill(path : Path2D, fillRule : String = "nonzero") : Unit = noImpl
    fun stroke() : Unit = noImpl
    fun stroke(path : Path2D) : Unit = noImpl
    fun drawFocusIfNeeded(element : Element) : Unit = noImpl
    fun drawFocusIfNeeded(path : Path2D, element : Element) : Unit = noImpl
    fun scrollPathIntoView() : Unit = noImpl
    fun scrollPathIntoView(path : Path2D) : Unit = noImpl
    fun clip(fillRule : String = "nonzero") : Unit = noImpl
    fun clip(path : Path2D, fillRule : String = "nonzero") : Unit = noImpl
    fun resetClip() : Unit = noImpl
    fun isPointInPath(x : Double, y : Double, fillRule : String = "nonzero") : Boolean = noImpl
    fun isPointInPath(path : Path2D, x : Double, y : Double, fillRule : String = "nonzero") : Boolean = noImpl
    fun isPointInStroke(x : Double, y : Double) : Boolean = noImpl
    fun isPointInStroke(path : Path2D, x : Double, y : Double) : Boolean = noImpl
    fun fillText(text : String, x : Double, y : Double, maxWidth : Double = noImpl) : Unit = noImpl
    fun strokeText(text : String, x : Double, y : Double, maxWidth : Double = noImpl) : Unit = noImpl
    fun measureText(text : String) : TextMetrics = noImpl
    fun drawImage(image : dynamic, dx : Double, dy : Double) : Unit = noImpl
    fun drawImage(image : dynamic, dx : Double, dy : Double, dw : Double, dh : Double) : Unit = noImpl
    fun drawImage(image : dynamic, sx : Double, sy : Double, sw : Double, sh : Double, dx : Double, dy : Double, dw : Double, dh : Double) : Unit = noImpl
    fun addHitRegion(options : HitRegionOptions = noImpl) : Unit = noImpl
    fun removeHitRegion(id : String) : Unit = noImpl
    fun clearHitRegions() : Unit = noImpl
    fun createImageData(sw : Double, sh : Double) : ImageData = noImpl
    fun createImageData(imagedata : ImageData) : ImageData = noImpl
    fun getImageData(sx : Double, sy : Double, sw : Double, sh : Double) : ImageData = noImpl
    fun putImageData(imagedata : ImageData, dx : Double, dy : Double) : Unit = noImpl
    fun putImageData(imagedata : ImageData, dx : Double, dy : Double, dirtyX : Double, dirtyY : Double, dirtyWidth : Double, dirtyHeight : Double) : Unit = noImpl
    fun setLineDash(segments : Any) : Unit = noImpl
    fun getLineDash() : Any = noImpl
    fun closePath() : Unit = noImpl
    fun moveTo(x : Double, y : Double) : Unit = noImpl
    fun lineTo(x : Double, y : Double) : Unit = noImpl
    fun quadraticCurveTo(cpx : Double, cpy : Double, x : Double, y : Double) : Unit = noImpl
    fun bezierCurveTo(cp1x : Double, cp1y : Double, cp2x : Double, cp2y : Double, x : Double, y : Double) : Unit = noImpl
    fun arcTo(x1 : Double, y1 : Double, x2 : Double, y2 : Double, radius : Double) : Unit = noImpl
    fun arcTo(x1 : Double, y1 : Double, x2 : Double, y2 : Double, radiusX : Double, radiusY : Double, rotation : Double) : Unit = noImpl
    fun rect(x : Double, y : Double, w : Double, h : Double) : Unit = noImpl
    fun arc(x : Double, y : Double, radius : Double, startAngle : Double, endAngle : Double, anticlockwise : Boolean = false) : Unit = noImpl
    fun ellipse(x : Double, y : Double, radiusX : Double, radiusY : Double, rotation : Double, startAngle : Double, endAngle : Double, anticlockwise : Boolean = false) : Unit = noImpl
}
native trait CanvasGradient {
    fun addColorStop(offset : Double, color : String) : Unit = noImpl
}
native trait CanvasPattern {
    fun setTransform(transform : dynamic) : Unit = noImpl
}
native trait TextMetrics {
    var width : Double
        get() = noImpl
        set(value) = noImpl
    var actualBoundingBoxLeft : Double
        get() = noImpl
        set(value) = noImpl
    var actualBoundingBoxRight : Double
        get() = noImpl
        set(value) = noImpl
    var fontBoundingBoxAscent : Double
        get() = noImpl
        set(value) = noImpl
    var fontBoundingBoxDescent : Double
        get() = noImpl
        set(value) = noImpl
    var actualBoundingBoxAscent : Double
        get() = noImpl
        set(value) = noImpl
    var actualBoundingBoxDescent : Double
        get() = noImpl
        set(value) = noImpl
    var emHeightAscent : Double
        get() = noImpl
        set(value) = noImpl
    var emHeightDescent : Double
        get() = noImpl
        set(value) = noImpl
    var hangingBaseline : Double
        get() = noImpl
        set(value) = noImpl
    var alphabeticBaseline : Double
        get() = noImpl
        set(value) = noImpl
    var ideographicBaseline : Double
        get() = noImpl
        set(value) = noImpl
}
native open class HitRegionOptions {
    var path : dynamic = null
    var fillRule : dynamic = "nonzero"
    var id : dynamic = ""
    var parentID : dynamic = null
    var cursor : dynamic = "inherit"
    var control : dynamic = null
    var label : dynamic = null
    var role : dynamic = null
}
native open class ImageData(sw : Int, sh : Int) {
    var width : Int
        get() = noImpl
        set(value) = noImpl
    var height : Int
        get() = noImpl
        set(value) = noImpl
    var data : dynamic
        get() = noImpl
        set(value) = noImpl
}
native open class DrawingStyle(scope : Element = noImpl) {
    var lineWidth : Double
        get() = noImpl
        set(value) = noImpl
    var lineCap : String
        get() = noImpl
        set(value) = noImpl
    var lineJoin : String
        get() = noImpl
        set(value) = noImpl
    var miterLimit : Double
        get() = noImpl
        set(value) = noImpl
    var lineDashOffset : Double
        get() = noImpl
        set(value) = noImpl
    var font : String
        get() = noImpl
        set(value) = noImpl
    var textAlign : String
        get() = noImpl
        set(value) = noImpl
    var textBaseline : String
        get() = noImpl
        set(value) = noImpl
    var direction : String
        get() = noImpl
        set(value) = noImpl
    fun setLineDash(segments : Any) : Unit = noImpl
    fun getLineDash() : Any = noImpl
}
native open class Path2D {
    fun addPath(path : Path2D, transformation : dynamic = null) : Unit = noImpl
    fun addPathByStrokingPath(path : Path2D, styles : dynamic, transformation : dynamic = null) : Unit = noImpl
    fun addText(text : String, styles : dynamic, transformation : dynamic, x : Double, y : Double, maxWidth : Double = noImpl) : Unit = noImpl
    fun addPathByStrokingText(text : String, styles : dynamic, transformation : dynamic, x : Double, y : Double, maxWidth : Double = noImpl) : Unit = noImpl
    fun addText(text : String, styles : dynamic, transformation : dynamic, path : Path2D, maxWidth : Double = noImpl) : Unit = noImpl
    fun addPathByStrokingText(text : String, styles : dynamic, transformation : dynamic, path : Path2D, maxWidth : Double = noImpl) : Unit = noImpl
    fun closePath() : Unit = noImpl
    fun moveTo(x : Double, y : Double) : Unit = noImpl
    fun lineTo(x : Double, y : Double) : Unit = noImpl
    fun quadraticCurveTo(cpx : Double, cpy : Double, x : Double, y : Double) : Unit = noImpl
    fun bezierCurveTo(cp1x : Double, cp1y : Double, cp2x : Double, cp2y : Double, x : Double, y : Double) : Unit = noImpl
    fun arcTo(x1 : Double, y1 : Double, x2 : Double, y2 : Double, radius : Double) : Unit = noImpl
    fun arcTo(x1 : Double, y1 : Double, x2 : Double, y2 : Double, radiusX : Double, radiusY : Double, rotation : Double) : Unit = noImpl
    fun rect(x : Double, y : Double, w : Double, h : Double) : Unit = noImpl
    fun arc(x : Double, y : Double, radius : Double, startAngle : Double, endAngle : Double, anticlockwise : Boolean = false) : Unit = noImpl
    fun ellipse(x : Double, y : Double, radiusX : Double, radiusY : Double, rotation : Double, startAngle : Double, endAngle : Double, anticlockwise : Boolean = false) : Unit = noImpl
}
native open class MouseEvent(typeArg : String, mouseEventInitDict : MouseEventInit = noImpl) : UIEvent(noImpl, noImpl), UnionElementOrMouseEvent {
    var region : String?
        get() = noImpl
        set(value) = noImpl
    var screenX : Int
        get() = noImpl
        set(value) = noImpl
    var screenY : Int
        get() = noImpl
        set(value) = noImpl
    var clientX : Int
        get() = noImpl
        set(value) = noImpl
    var clientY : Int
        get() = noImpl
        set(value) = noImpl
    var ctrlKey : Boolean
        get() = noImpl
        set(value) = noImpl
    var shiftKey : Boolean
        get() = noImpl
        set(value) = noImpl
    var altKey : Boolean
        get() = noImpl
        set(value) = noImpl
    var metaKey : Boolean
        get() = noImpl
        set(value) = noImpl
    var button : Short
        get() = noImpl
        set(value) = noImpl
    var relatedTarget : EventTarget?
        get() = noImpl
        set(value) = noImpl
    var buttons : Short
        get() = noImpl
        set(value) = noImpl
    fun getModifierState(keyArg : String) : Boolean = noImpl
    fun initMouseEvent(typeArg : String, bubblesArg : Boolean, cancelableArg : Boolean, viewArg : Window?, detailArg : Int, screenXArg : Int, screenYArg : Int, clientXArg : Int, clientYArg : Int, ctrlKeyArg : Boolean, altKeyArg : Boolean, shiftKeyArg : Boolean, metaKeyArg : Boolean, buttonArg : Short, relatedTargetArg : EventTarget?) : Unit = noImpl
}
native trait Touch {
    var region : String?
        get() = noImpl
        set(value) = noImpl
}
native trait DataTransfer {
    var dropEffect : String
        get() = noImpl
        set(value) = noImpl
    var effectAllowed : String
        get() = noImpl
        set(value) = noImpl
    var items : DataTransferItemList
        get() = noImpl
        set(value) = noImpl
    var types : dynamic
        get() = noImpl
        set(value) = noImpl
    var files : FileList
        get() = noImpl
        set(value) = noImpl
    fun setDragImage(image : Element, x : Int, y : Int) : Unit = noImpl
    fun getData(format : String) : String = noImpl
    fun setData(format : String, data : String) : Unit = noImpl
    fun clearData(format : String = noImpl) : Unit = noImpl
}
native trait DataTransferItemList {
    var length : Int
        get() = noImpl
        set(value) = noImpl
    nativeGetter fun get(index : Int) : DataTransferItem? = noImpl
    fun add(data : String, type : String) : DataTransferItem? = noImpl
    fun add(data : File) : DataTransferItem? = noImpl
    fun remove(index : Int) : Unit = noImpl
    fun clear() : Unit = noImpl
}
native trait DataTransferItem {
    var kind : String
        get() = noImpl
        set(value) = noImpl
    var type : String
        get() = noImpl
        set(value) = noImpl
    fun getAsString(_callback : () -> Unit?) : Unit = noImpl
    fun getAsFile() : File? = noImpl
}
native open class DragEvent(type : String, eventInitDict : DragEventInit = noImpl) : MouseEvent(noImpl, noImpl) {
    var dataTransfer : DataTransfer?
        get() = noImpl
        set(value) = noImpl
}
native open class DragEventInit : MouseEventInit() {
    var dataTransfer : dynamic
}
native trait Window : EventTarget {
    var window : Window
        get() = noImpl
        set(value) = noImpl
    var self : Window
        get() = noImpl
        set(value) = noImpl
    var document : Document
        get() = noImpl
        set(value) = noImpl
    var name : String
        get() = noImpl
        set(value) = noImpl
    var location : Location
        get() = noImpl
        set(value) = noImpl
    var history : History
        get() = noImpl
        set(value) = noImpl
    var locationbar : BarProp
        get() = noImpl
        set(value) = noImpl
    var menubar : BarProp
        get() = noImpl
        set(value) = noImpl
    var personalbar : BarProp
        get() = noImpl
        set(value) = noImpl
    var scrollbars : BarProp
        get() = noImpl
        set(value) = noImpl
    var statusbar : BarProp
        get() = noImpl
        set(value) = noImpl
    var toolbar : BarProp
        get() = noImpl
        set(value) = noImpl
    var status : String
        get() = noImpl
        set(value) = noImpl
    var closed : Boolean
        get() = noImpl
        set(value) = noImpl
    var frames : Window
        get() = noImpl
        set(value) = noImpl
    var length : Int
        get() = noImpl
        set(value) = noImpl
    var top : Window
        get() = noImpl
        set(value) = noImpl
    var opener : Any
        get() = noImpl
        set(value) = noImpl
    var parent : Window
        get() = noImpl
        set(value) = noImpl
    var frameElement : Element?
        get() = noImpl
        set(value) = noImpl
    var navigator : Navigator
        get() = noImpl
        set(value) = noImpl
    var external : External
        get() = noImpl
        set(value) = noImpl
    var applicationCache : ApplicationCache
        get() = noImpl
        set(value) = noImpl
    var caches : CacheStorage
        get() = noImpl
        set(value) = noImpl
    var onabort : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onautocomplete : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onautocompleteerror : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onblur : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var oncancel : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var oncanplay : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var oncanplaythrough : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onchange : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onclick : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onclose : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var oncontextmenu : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var oncuechange : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var ondblclick : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var ondrag : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var ondragend : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var ondragenter : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var ondragexit : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var ondragleave : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var ondragover : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var ondragstart : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var ondrop : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var ondurationchange : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onemptied : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onended : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onerror : dynamic
        get() = noImpl
        set(value) = noImpl
    var onfocus : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var oninput : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var oninvalid : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onkeydown : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onkeypress : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onkeyup : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onload : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onloadeddata : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onloadedmetadata : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onloadstart : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onmousedown : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onmouseenter : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onmouseleave : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onmousemove : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onmouseout : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onmouseover : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onmouseup : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onmousewheel : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onpause : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onplay : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onplaying : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onprogress : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onratechange : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onreset : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onresize : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onscroll : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onseeked : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onseeking : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onselect : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onshow : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onsort : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onstalled : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onsubmit : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onsuspend : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var ontimeupdate : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var ontoggle : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onvolumechange : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onwaiting : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onafterprint : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onbeforeprint : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onbeforeunload : dynamic
        get() = noImpl
        set(value) = noImpl
    var onhashchange : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onlanguagechange : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onmessage : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onoffline : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var ononline : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onpagehide : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onpageshow : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onpopstate : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onstorage : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onunload : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var sessionStorage : Storage
        get() = noImpl
        set(value) = noImpl
    var localStorage : Storage
        get() = noImpl
        set(value) = noImpl
    fun close() : Unit = noImpl
    fun stop() : Unit = noImpl
    fun focus() : Unit = noImpl
    fun blur() : Unit = noImpl
    fun open(url : String = "about:blank", target : String = "_blank", features : String = "", replace : Boolean = false) : Window = noImpl
    nativeGetter fun get(index : Int) : Window? = noImpl
    nativeGetter fun get(name : String) : dynamic = noImpl
    fun alert() : Unit = noImpl
    fun alert(message : String) : Unit = noImpl
    fun confirm(message : String = "") : Boolean = noImpl
    fun prompt(message : String = "", default : String = "") : String? = noImpl
    fun print() : Unit = noImpl
    fun showModalDialog(url : String, argument : Any = noImpl) : Any = noImpl
    fun requestAnimationFrame(callback : () -> Unit) : Int = noImpl
    fun cancelAnimationFrame(handle : Int) : Unit = noImpl
    fun postMessage(message : Any, targetOrigin : String, transfer : Any = noImpl) : Unit = noImpl
    fun captureEvents() : Unit = noImpl
    fun releaseEvents() : Unit = noImpl
    fun getSelection() : Selection = noImpl
    fun getComputedStyle(elt : Element, pseudoElt : String? = noImpl) : CSSStyleDeclaration = noImpl
    fun btoa(btoa : String) : String = noImpl
    fun atob(atob : String) : String = noImpl
    fun setTimeout(handler : () -> dynamic, timeout : Int = 0, vararg arguments : Any) : Int = noImpl
    fun setTimeout(handler : String, timeout : Int = 0, vararg arguments : Any) : Int = noImpl
    fun clearTimeout(handle : Int = 0) : Unit = noImpl
    fun setInterval(handler : () -> dynamic, timeout : Int = 0, vararg arguments : Any) : Int = noImpl
    fun setInterval(handler : String, timeout : Int = 0, vararg arguments : Any) : Int = noImpl
    fun clearInterval(handle : Int = 0) : Unit = noImpl
    fun createImageBitmap(image : dynamic) : dynamic = noImpl
    fun createImageBitmap(image : dynamic, sx : Int, sy : Int, sw : Int, sh : Int) : dynamic = noImpl
    fun fetch(input : dynamic, init : RequestInit = noImpl) : dynamic = noImpl
}
native trait BarProp {
    var visible : Boolean
        get() = noImpl
        set(value) = noImpl
}
native trait History {
    var length : Int
        get() = noImpl
        set(value) = noImpl
    var state : Any
        get() = noImpl
        set(value) = noImpl
    fun go(delta : Int = noImpl) : Unit = noImpl
    fun back() : Unit = noImpl
    fun forward() : Unit = noImpl
    fun pushState(data : Any, title : String, url : String? = null) : Unit = noImpl
    fun replaceState(data : Any, title : String, url : String? = null) : Unit = noImpl
}
native trait Location {
    var ancestorOrigins : dynamic
        get() = noImpl
        set(value) = noImpl
    fun assign(url : String) : Unit = noImpl
    fun replace(url : String) : Unit = noImpl
    fun reload() : Unit = noImpl
}
native open class PopStateEvent(type : String, eventInitDict : PopStateEventInit = noImpl) : Event(type, eventInitDict) {
    var state : Any
        get() = noImpl
        set(value) = noImpl
}
native open class PopStateEventInit : EventInit() {
    var state : dynamic
}
native open class HashChangeEvent(type : String, eventInitDict : HashChangeEventInit = noImpl) : Event(type, eventInitDict) {
    var oldURL : String
        get() = noImpl
        set(value) = noImpl
    var newURL : String
        get() = noImpl
        set(value) = noImpl
}
native open class HashChangeEventInit : EventInit() {
    var oldURL : dynamic
    var newURL : dynamic
}
native open class PageTransitionEvent(type : String, eventInitDict : PageTransitionEventInit = noImpl) : Event(type, eventInitDict) {
    var persisted : Boolean
        get() = noImpl
        set(value) = noImpl
}
native open class PageTransitionEventInit : EventInit() {
    var persisted : dynamic
}
native open class BeforeUnloadEvent : Event(noImpl, noImpl) {
    var returnValue : String
        get() = noImpl
        set(value) = noImpl
}
native trait ApplicationCache : EventTarget {
    var status : Short
        get() = noImpl
        set(value) = noImpl
    var onchecking : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onerror : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onnoupdate : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var ondownloading : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onprogress : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onupdateready : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var oncached : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onobsolete : () -> Unit
        get() = noImpl
        set(value) = noImpl
    fun update() : Unit = noImpl
    fun abort() : Unit = noImpl
    fun swapCache() : Unit = noImpl
companion object {
    val UNCACHED : Short = 0
    val IDLE : Short = 1
    val CHECKING : Short = 2
    val DOWNLOADING : Short = 3
    val UPDATEREADY : Short = 4
    val OBSOLETE : Short = 5
}
}
native open class ErrorEvent(type : String, eventInitDict : ErrorEventInit = noImpl) : Event(type, eventInitDict) {
    var message : String
        get() = noImpl
        set(value) = noImpl
    var filename : String
        get() = noImpl
        set(value) = noImpl
    var lineno : Int
        get() = noImpl
        set(value) = noImpl
    var colno : Int
        get() = noImpl
        set(value) = noImpl
    var error : Any
        get() = noImpl
        set(value) = noImpl
}
native open class ErrorEventInit : EventInit() {
    var message : dynamic
    var filename : dynamic
    var lineno : dynamic
    var colno : dynamic
    var error : dynamic
}
native trait Navigator {
    var serviceWorker : ServiceWorkerContainer
        get() = noImpl
        set(value) = noImpl
    var appCodeName : String
        get() = noImpl
        set(value) = noImpl
    var appName : String
        get() = noImpl
        set(value) = noImpl
    var appVersion : String
        get() = noImpl
        set(value) = noImpl
    var platform : String
        get() = noImpl
        set(value) = noImpl
    var product : String
        get() = noImpl
        set(value) = noImpl
    var userAgent : String
        get() = noImpl
        set(value) = noImpl
    var vendorSub : String
        get() = noImpl
        set(value) = noImpl
    var language : String?
        get() = noImpl
        set(value) = noImpl
    var languages : dynamic
        get() = noImpl
        set(value) = noImpl
    var onLine : Boolean
        get() = noImpl
        set(value) = noImpl
    var cookieEnabled : Boolean
        get() = noImpl
        set(value) = noImpl
    var plugins : PluginArray
        get() = noImpl
        set(value) = noImpl
    var mimeTypes : MimeTypeArray
        get() = noImpl
        set(value) = noImpl
    var javaEnabled : Boolean
        get() = noImpl
        set(value) = noImpl
    fun vibrate(pattern : dynamic) : Boolean = noImpl
    fun taintEnabled() : Boolean = noImpl
    fun registerProtocolHandler(scheme : String, url : String, title : String) : Unit = noImpl
    fun registerContentHandler(mimeType : String, url : String, title : String) : Unit = noImpl
    fun isProtocolHandlerRegistered(scheme : String, url : String) : String = noImpl
    fun isContentHandlerRegistered(mimeType : String, url : String) : String = noImpl
    fun unregisterProtocolHandler(scheme : String, url : String) : Unit = noImpl
    fun unregisterContentHandler(mimeType : String, url : String) : Unit = noImpl
    fun yieldForStorageUpdates() : Unit = noImpl
}
native trait PluginArray {
    var length : Int
        get() = noImpl
        set(value) = noImpl
    fun refresh(reload : Boolean = false) : Unit = noImpl
    fun item(index : Int) : Plugin? = noImpl
    nativeGetter fun get(index : Int) : Plugin? = noImpl
    fun namedItem(name : String) : Plugin? = noImpl
    nativeGetter fun get(name : String) : Plugin? = noImpl
}
native trait MimeTypeArray {
    var length : Int
        get() = noImpl
        set(value) = noImpl
    fun item(index : Int) : MimeType? = noImpl
    nativeGetter fun get(index : Int) : MimeType? = noImpl
    fun namedItem(name : String) : MimeType? = noImpl
    nativeGetter fun get(name : String) : MimeType? = noImpl
}
native trait Plugin {
    var name : String
        get() = noImpl
        set(value) = noImpl
    var description : String
        get() = noImpl
        set(value) = noImpl
    var filename : String
        get() = noImpl
        set(value) = noImpl
    var length : Int
        get() = noImpl
        set(value) = noImpl
    fun item(index : Int) : MimeType? = noImpl
    nativeGetter fun get(index : Int) : MimeType? = noImpl
    fun namedItem(name : String) : MimeType? = noImpl
    nativeGetter fun get(name : String) : MimeType? = noImpl
}
native trait MimeType {
    var type : String
        get() = noImpl
        set(value) = noImpl
    var description : String
        get() = noImpl
        set(value) = noImpl
    var suffixes : String
        get() = noImpl
        set(value) = noImpl
    var enabledPlugin : Plugin
        get() = noImpl
        set(value) = noImpl
}
native trait External {
    fun AddSearchProvider(engineURL : String) : Unit = noImpl
    fun IsSearchProviderInstalled(engineURL : String) : Int = noImpl
}
native trait ImageBitmap {
    var width : Int
        get() = noImpl
        set(value) = noImpl
    var height : Int
        get() = noImpl
        set(value) = noImpl
}
native open class MessageEvent(type : String, eventInitDict : MessageEventInit = noImpl) : Event(type, eventInitDict) {
    var data : Any
        get() = noImpl
        set(value) = noImpl
    var origin : String
        get() = noImpl
        set(value) = noImpl
    var lastEventId : String
        get() = noImpl
        set(value) = noImpl
    var source : dynamic
        get() = noImpl
        set(value) = noImpl
    var ports : dynamic
        get() = noImpl
        set(value) = noImpl
    fun initMessageEvent(typeArg : String, canBubbleArg : Boolean, cancelableArg : Boolean, dataArg : Any, originArg : String, lastEventIdArg : String, sourceArg : dynamic, portsArg : Any?) : Unit = noImpl
}
native open class MessageEventInit : EventInit() {
    var data : dynamic
    var origin : dynamic
    var lastEventId : dynamic
    var source : dynamic
    var ports : dynamic
}
native open class EventSource(url : String, eventSourceInitDict : EventSourceInit = noImpl) : EventTarget {
    var url : String
        get() = noImpl
        set(value) = noImpl
    var withCredentials : Boolean
        get() = noImpl
        set(value) = noImpl
    var readyState : Short
        get() = noImpl
        set(value) = noImpl
    var onopen : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onmessage : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onerror : () -> Unit
        get() = noImpl
        set(value) = noImpl
    fun close() : Unit = noImpl
companion object {
    val CONNECTING : Short = 0
    val OPEN : Short = 1
    val CLOSED : Short = 2
}
}
native open class EventSourceInit {
    var withCredentials : dynamic = false
}
native open class WebSocket(url : String, protocols : dynamic = noImpl) : EventTarget {
    var url : String
        get() = noImpl
        set(value) = noImpl
    var readyState : Short
        get() = noImpl
        set(value) = noImpl
    var bufferedAmount : Int
        get() = noImpl
        set(value) = noImpl
    var onopen : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onerror : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onclose : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var extensions : String
        get() = noImpl
        set(value) = noImpl
    var protocol : String
        get() = noImpl
        set(value) = noImpl
    var onmessage : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var binaryType : String
        get() = noImpl
        set(value) = noImpl
    fun close(code : Short = noImpl, reason : String = noImpl) : Unit = noImpl
    fun send(data : String) : Unit = noImpl
    fun send(data : Blob) : Unit = noImpl
    fun send(data : dynamic) : Unit = noImpl
companion object {
    val CONNECTING : Short = 0
    val OPEN : Short = 1
    val CLOSING : Short = 2
    val CLOSED : Short = 3
}
}
native open class CloseEvent(type : String, eventInitDict : CloseEventInit = noImpl) : Event(type, eventInitDict) {
    var wasClean : Boolean
        get() = noImpl
        set(value) = noImpl
    var code : Short
        get() = noImpl
        set(value) = noImpl
    var reason : String
        get() = noImpl
        set(value) = noImpl
}
native open class CloseEventInit : EventInit() {
    var wasClean : dynamic
    var code : dynamic
    var reason : dynamic
}
native open class MessageChannel {
    var port1 : MessagePort
        get() = noImpl
        set(value) = noImpl
    var port2 : MessagePort
        get() = noImpl
        set(value) = noImpl
}
native trait MessagePort : EventTarget {
    var onmessage : () -> Unit
        get() = noImpl
        set(value) = noImpl
    fun postMessage(message : Any, transfer : Any = noImpl) : Unit = noImpl
    fun start() : Unit = noImpl
    fun close() : Unit = noImpl
}
native open class PortCollection {
    fun add(port : MessagePort) : Unit = noImpl
    fun remove(port : MessagePort) : Unit = noImpl
    fun clear() : Unit = noImpl
    fun iterate(callback : () -> Unit) : Unit = noImpl
}
native open class BroadcastChannel(channel : String) : EventTarget {
    var name : String
        get() = noImpl
        set(value) = noImpl
    var onmessage : () -> Unit
        get() = noImpl
        set(value) = noImpl
    fun postMessage(message : Any) : Unit = noImpl
    fun close() : Unit = noImpl
}
native trait WorkerGlobalScope : EventTarget {
    var self : WorkerGlobalScope
        get() = noImpl
        set(value) = noImpl
    var location : WorkerLocation
        get() = noImpl
        set(value) = noImpl
    var onerror : dynamic
        get() = noImpl
        set(value) = noImpl
    var onlanguagechange : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onoffline : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var ononline : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var navigator : WorkerNavigator
        get() = noImpl
        set(value) = noImpl
    var caches : CacheStorage
        get() = noImpl
        set(value) = noImpl
    fun close() : Unit = noImpl
    fun importScripts(vararg urls : String) : Unit = noImpl
    fun createImageBitmap(image : dynamic) : dynamic = noImpl
    fun createImageBitmap(image : dynamic, sx : Int, sy : Int, sw : Int, sh : Int) : dynamic = noImpl
    fun setTimeout(handler : () -> dynamic, timeout : Int = 0, vararg arguments : Any) : Int = noImpl
    fun setTimeout(handler : String, timeout : Int = 0, vararg arguments : Any) : Int = noImpl
    fun clearTimeout(handle : Int = 0) : Unit = noImpl
    fun setInterval(handler : () -> dynamic, timeout : Int = 0, vararg arguments : Any) : Int = noImpl
    fun setInterval(handler : String, timeout : Int = 0, vararg arguments : Any) : Int = noImpl
    fun clearInterval(handle : Int = 0) : Unit = noImpl
    fun btoa(btoa : String) : String = noImpl
    fun atob(atob : String) : String = noImpl
    fun fetch(input : dynamic, init : RequestInit = noImpl) : dynamic = noImpl
}
native trait DedicatedWorkerGlobalScope : WorkerGlobalScope {
    var onmessage : () -> Unit
        get() = noImpl
        set(value) = noImpl
    fun postMessage(message : Any, transfer : Any = noImpl) : Unit = noImpl
}
native trait SharedWorkerGlobalScope : WorkerGlobalScope {
    var name : String
        get() = noImpl
        set(value) = noImpl
    var applicationCache : ApplicationCache
        get() = noImpl
        set(value) = noImpl
    var onconnect : () -> Unit
        get() = noImpl
        set(value) = noImpl
}
native open class Worker(scriptURL : String) : EventTarget {
    var onmessage : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onerror : () -> Unit
        get() = noImpl
        set(value) = noImpl
    fun terminate() : Unit = noImpl
    fun postMessage(message : Any, transfer : Any = noImpl) : Unit = noImpl
}
native open class SharedWorker(scriptURL : String, name : String = noImpl) : EventTarget {
    var port : MessagePort
        get() = noImpl
        set(value) = noImpl
    var onerror : () -> Unit
        get() = noImpl
        set(value) = noImpl
}
native trait WorkerNavigator {
    var serviceWorker : ServiceWorkerContainer
        get() = noImpl
        set(value) = noImpl
    var appCodeName : String
        get() = noImpl
        set(value) = noImpl
    var appName : String
        get() = noImpl
        set(value) = noImpl
    var appVersion : String
        get() = noImpl
        set(value) = noImpl
    var platform : String
        get() = noImpl
        set(value) = noImpl
    var product : String
        get() = noImpl
        set(value) = noImpl
    var userAgent : String
        get() = noImpl
        set(value) = noImpl
    var vendorSub : String
        get() = noImpl
        set(value) = noImpl
    var language : String?
        get() = noImpl
        set(value) = noImpl
    var languages : dynamic
        get() = noImpl
        set(value) = noImpl
    var onLine : Boolean
        get() = noImpl
        set(value) = noImpl
    fun taintEnabled() : Boolean = noImpl
}
native trait WorkerLocation {
}
native trait Storage {
    var length : Int
        get() = noImpl
        set(value) = noImpl
    fun key(index : Int) : String? = noImpl
    fun getItem(key : String) : String? = noImpl
    nativeGetter fun get(key : String) : String? = noImpl
    fun setItem(key : String, value : String) : Unit = noImpl
    fun set(key : String, value : String) : Unit? = noImpl
    fun removeItem(key : String) : Unit = noImpl
    fun clear() : Unit = noImpl
}
native open class StorageEvent(type : String, eventInitDict : StorageEventInit = noImpl) : Event(type, eventInitDict) {
    var key : String?
        get() = noImpl
        set(value) = noImpl
    var oldValue : String?
        get() = noImpl
        set(value) = noImpl
    var newValue : String?
        get() = noImpl
        set(value) = noImpl
    var url : String
        get() = noImpl
        set(value) = noImpl
    var storageArea : Storage?
        get() = noImpl
        set(value) = noImpl
}
native open class StorageEventInit : EventInit() {
    var key : dynamic
    var oldValue : dynamic
    var newValue : dynamic
    var url : dynamic
    var storageArea : dynamic
}
native trait HTMLAppletElement : HTMLElement {
    var align : String
        get() = noImpl
        set(value) = noImpl
    var alt : String
        get() = noImpl
        set(value) = noImpl
    var archive : String
        get() = noImpl
        set(value) = noImpl
    var code : String
        get() = noImpl
        set(value) = noImpl
    var codeBase : String
        get() = noImpl
        set(value) = noImpl
    var height : String
        get() = noImpl
        set(value) = noImpl
    var hspace : Int
        get() = noImpl
        set(value) = noImpl
    var name : String
        get() = noImpl
        set(value) = noImpl
    var _object : String
        get() = noImpl
        set(value) = noImpl
    var vspace : Int
        get() = noImpl
        set(value) = noImpl
    var width : String
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLMarqueeElement : HTMLElement {
    var behavior : String
        get() = noImpl
        set(value) = noImpl
    var bgColor : String
        get() = noImpl
        set(value) = noImpl
    var direction : String
        get() = noImpl
        set(value) = noImpl
    var height : String
        get() = noImpl
        set(value) = noImpl
    var hspace : Int
        get() = noImpl
        set(value) = noImpl
    var loop : Int
        get() = noImpl
        set(value) = noImpl
    var scrollAmount : Int
        get() = noImpl
        set(value) = noImpl
    var scrollDelay : Int
        get() = noImpl
        set(value) = noImpl
    var trueSpeed : Boolean
        get() = noImpl
        set(value) = noImpl
    var vspace : Int
        get() = noImpl
        set(value) = noImpl
    var width : String
        get() = noImpl
        set(value) = noImpl
    var onbounce : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onfinish : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onstart : () -> Unit
        get() = noImpl
        set(value) = noImpl
    fun start() : Unit = noImpl
    fun stop() : Unit = noImpl
}
native trait HTMLFrameSetElement : HTMLElement {
    var cols : String
        get() = noImpl
        set(value) = noImpl
    var rows : String
        get() = noImpl
        set(value) = noImpl
    var onafterprint : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onbeforeprint : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onbeforeunload : dynamic
        get() = noImpl
        set(value) = noImpl
    var onhashchange : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onlanguagechange : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onmessage : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onoffline : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var ononline : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onpagehide : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onpageshow : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onpopstate : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onstorage : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onunload : () -> Unit
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLFrameElement : HTMLElement {
    var name : String
        get() = noImpl
        set(value) = noImpl
    var scrolling : String
        get() = noImpl
        set(value) = noImpl
    var src : String
        get() = noImpl
        set(value) = noImpl
    var frameBorder : String
        get() = noImpl
        set(value) = noImpl
    var longDesc : String
        get() = noImpl
        set(value) = noImpl
    var noResize : Boolean
        get() = noImpl
        set(value) = noImpl
    var contentDocument : Document?
        get() = noImpl
        set(value) = noImpl
    var contentWindow : Window?
        get() = noImpl
        set(value) = noImpl
    var marginHeight : String
        get() = noImpl
        set(value) = noImpl
    var marginWidth : String
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLDirectoryElement : HTMLElement {
    var compact : Boolean
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLFontElement : HTMLElement {
    var color : String
        get() = noImpl
        set(value) = noImpl
    var face : String
        get() = noImpl
        set(value) = noImpl
    var size : String
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLImageElement : HTMLElement {
    var name : String
        get() = noImpl
        set(value) = noImpl
    var lowsrc : String
        get() = noImpl
        set(value) = noImpl
    var align : String
        get() = noImpl
        set(value) = noImpl
    var hspace : Int
        get() = noImpl
        set(value) = noImpl
    var vspace : Int
        get() = noImpl
        set(value) = noImpl
    var longDesc : String
        get() = noImpl
        set(value) = noImpl
    var border : String
        get() = noImpl
        set(value) = noImpl
    var alt : String
        get() = noImpl
        set(value) = noImpl
    var src : String
        get() = noImpl
        set(value) = noImpl
    var srcset : String
        get() = noImpl
        set(value) = noImpl
    var sizes : String
        get() = noImpl
        set(value) = noImpl
    var crossOrigin : String?
        get() = noImpl
        set(value) = noImpl
    var useMap : String
        get() = noImpl
        set(value) = noImpl
    var isMap : Boolean
        get() = noImpl
        set(value) = noImpl
    var width : Int
        get() = noImpl
        set(value) = noImpl
    var height : Int
        get() = noImpl
        set(value) = noImpl
    var naturalWidth : Int
        get() = noImpl
        set(value) = noImpl
    var naturalHeight : Int
        get() = noImpl
        set(value) = noImpl
    var complete : Boolean
        get() = noImpl
        set(value) = noImpl
    var currentSrc : String
        get() = noImpl
        set(value) = noImpl
}
native trait HTMLPictureElement : HTMLElement {
}
native open class Event(type : String, eventInitDict : EventInit = noImpl) {
    var type : String
        get() = noImpl
        set(value) = noImpl
    var target : EventTarget?
        get() = noImpl
        set(value) = noImpl
    var currentTarget : EventTarget?
        get() = noImpl
        set(value) = noImpl
    var eventPhase : Short
        get() = noImpl
        set(value) = noImpl
    var bubbles : Boolean
        get() = noImpl
        set(value) = noImpl
    var cancelable : Boolean
        get() = noImpl
        set(value) = noImpl
    var defaultPrevented : Boolean
        get() = noImpl
        set(value) = noImpl
    var isTrusted : Boolean
        get() = noImpl
        set(value) = noImpl
    var timeStamp : Number
        get() = noImpl
        set(value) = noImpl
    fun stopPropagation() : Unit = noImpl
    fun stopImmediatePropagation() : Unit = noImpl
    fun preventDefault() : Unit = noImpl
    fun initEvent(type : String, bubbles : Boolean, cancelable : Boolean) : Unit = noImpl
companion object {
    val NONE : Short = 0
    val CAPTURING_PHASE : Short = 1
    val AT_TARGET : Short = 2
    val BUBBLING_PHASE : Short = 3
}
}
native open class EventInit {
    var bubbles : dynamic = false
    var cancelable : dynamic = false
}
native open class CustomEvent(type : String, eventInitDict : CustomEventInit = noImpl) : Event(type, eventInitDict) {
    var detail : Any
        get() = noImpl
        set(value) = noImpl
    fun initCustomEvent(type : String, bubbles : Boolean, cancelable : Boolean, detail : Any) : Unit = noImpl
}
native open class CustomEventInit : EventInit() {
    var detail : dynamic = null
}
native trait EventTarget {
    fun addEventListener(type : String, callback : EventListener?, capture : Boolean = false) : Unit = noImpl
    fun removeEventListener(type : String, callback : EventListener?, capture : Boolean = false) : Unit = noImpl
    fun dispatchEvent(event : Event) : Boolean = noImpl
}
native trait EventListener {
    fun handleEvent(event : Event) : Unit = noImpl
}
native trait NodeList {
    var length : Int
        get() = noImpl
        set(value) = noImpl
    fun item(index : Int) : Node? = noImpl
    nativeGetter fun get(index : Int) : Node? = noImpl
}
native trait HTMLCollection {
    var length : Int
        get() = noImpl
        set(value) = noImpl
    fun item(index : Int) : Element? = noImpl
    nativeGetter fun get(index : Int) : Element? = noImpl
    fun namedItem(name : String) : Element? = noImpl
    nativeGetter fun get(name : String) : Element? = noImpl
}
native open class MutationObserver(callback : () -> Unit) {
    fun observe(target : Node, options : MutationObserverInit) : Unit = noImpl
    fun disconnect() : Unit = noImpl
    fun takeRecords() : Any = noImpl
}
native open class MutationObserverInit {
    var childList : dynamic = false
    var attributes : dynamic
    var characterData : dynamic
    var subtree : dynamic = false
    var attributeOldValue : dynamic
    var characterDataOldValue : dynamic
    var attributeFilter : dynamic
}
native trait MutationRecord {
    var type : String
        get() = noImpl
        set(value) = noImpl
    var target : Node
        get() = noImpl
        set(value) = noImpl
    var addedNodes : NodeList
        get() = noImpl
        set(value) = noImpl
    var removedNodes : NodeList
        get() = noImpl
        set(value) = noImpl
    var previousSibling : Node?
        get() = noImpl
        set(value) = noImpl
    var nextSibling : Node?
        get() = noImpl
        set(value) = noImpl
    var attributeName : String?
        get() = noImpl
        set(value) = noImpl
    var attributeNamespace : String?
        get() = noImpl
        set(value) = noImpl
    var oldValue : String?
        get() = noImpl
        set(value) = noImpl
}
native trait Node : EventTarget {
    var nodeType : Short
        get() = noImpl
        set(value) = noImpl
    var nodeName : String
        get() = noImpl
        set(value) = noImpl
    var baseURI : String?
        get() = noImpl
        set(value) = noImpl
    var ownerDocument : Document?
        get() = noImpl
        set(value) = noImpl
    var parentNode : Node?
        get() = noImpl
        set(value) = noImpl
    var parentElement : Element?
        get() = noImpl
        set(value) = noImpl
    var childNodes : NodeList
        get() = noImpl
        set(value) = noImpl
    var firstChild : Node?
        get() = noImpl
        set(value) = noImpl
    var lastChild : Node?
        get() = noImpl
        set(value) = noImpl
    var previousSibling : Node?
        get() = noImpl
        set(value) = noImpl
    var nextSibling : Node?
        get() = noImpl
        set(value) = noImpl
    var nodeValue : String?
        get() = noImpl
        set(value) = noImpl
    var textContent : String?
        get() = noImpl
        set(value) = noImpl
    fun hasChildNodes() : Boolean = noImpl
    fun normalize() : Unit = noImpl
    fun cloneNode(deep : Boolean = false) : Node = noImpl
    fun isEqualNode(otherNode : Node?) : Boolean = noImpl
    fun compareDocumentPosition(other : Node) : Short = noImpl
    fun contains(other : Node?) : Boolean = noImpl
    fun lookupPrefix(namespace : String?) : String? = noImpl
    fun lookupNamespaceURI(prefix : String?) : String? = noImpl
    fun isDefaultNamespace(namespace : String?) : Boolean = noImpl
    fun insertBefore(node : Node, child : Node?) : Node = noImpl
    fun appendChild(node : Node) : Node = noImpl
    fun replaceChild(node : Node, child : Node) : Node = noImpl
    fun removeChild(child : Node) : Node = noImpl
companion object {
    val ELEMENT_NODE : Short = 1
    val ATTRIBUTE_NODE : Short = 2
    val TEXT_NODE : Short = 3
    val CDATA_SECTION_NODE : Short = 4
    val ENTITY_REFERENCE_NODE : Short = 5
    val ENTITY_NODE : Short = 6
    val PROCESSING_INSTRUCTION_NODE : Short = 7
    val COMMENT_NODE : Short = 8
    val DOCUMENT_NODE : Short = 9
    val DOCUMENT_TYPE_NODE : Short = 10
    val DOCUMENT_FRAGMENT_NODE : Short = 11
    val NOTATION_NODE : Short = 12
    val DOCUMENT_POSITION_DISCONNECTED : Short = 0x01
    val DOCUMENT_POSITION_PRECEDING : Short = 0x02
    val DOCUMENT_POSITION_FOLLOWING : Short = 0x04
    val DOCUMENT_POSITION_CONTAINS : Short = 0x08
    val DOCUMENT_POSITION_CONTAINED_BY : Short = 0x10
    val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC : Short = 0x20
}
}
native trait DOMImplementation {
    fun createDocumentType(qualifiedName : String, publicId : String, systemId : String) : DocumentType = noImpl
    fun createDocument(namespace : String?, qualifiedName : String, doctype : DocumentType? = null) : XMLDocument = noImpl
    fun createHTMLDocument(title : String = noImpl) : Document = noImpl
    fun hasFeature() : Boolean = noImpl
}
native open class DocumentFragment : Node {
    var children : HTMLCollection
        get() = noImpl
        set(value) = noImpl
    var firstElementChild : Element?
        get() = noImpl
        set(value) = noImpl
    var lastElementChild : Element?
        get() = noImpl
        set(value) = noImpl
    var childElementCount : Int
        get() = noImpl
        set(value) = noImpl
    fun getElementById(elementId : String) : Element? = noImpl
    fun prepend(vararg nodes : dynamic) : Unit = noImpl
    fun append(vararg nodes : dynamic) : Unit = noImpl
    fun query(relativeSelectors : String) : Element? = noImpl
    fun queryAll(relativeSelectors : String) : dynamic = noImpl
    fun querySelector(selectors : String) : Element? = noImpl
    fun querySelectorAll(selectors : String) : NodeList = noImpl
}
native trait DocumentType : Node {
    var name : String
        get() = noImpl
        set(value) = noImpl
    var publicId : String
        get() = noImpl
        set(value) = noImpl
    var systemId : String
        get() = noImpl
        set(value) = noImpl
    fun before(vararg nodes : dynamic) : Unit = noImpl
    fun after(vararg nodes : dynamic) : Unit = noImpl
    fun replaceWith(vararg nodes : dynamic) : Unit = noImpl
    fun remove() : Unit = noImpl
}
native trait Element : Node, UnionElementOrMouseEvent {
    var namespaceURI : String?
        get() = noImpl
        set(value) = noImpl
    var prefix : String?
        get() = noImpl
        set(value) = noImpl
    var localName : String
        get() = noImpl
        set(value) = noImpl
    var tagName : String
        get() = noImpl
        set(value) = noImpl
    var id : String
        get() = noImpl
        set(value) = noImpl
    var className : String
        get() = noImpl
        set(value) = noImpl
    var classList : DOMTokenList
        get() = noImpl
        set(value) = noImpl
    var attributes : NamedNodeMap
        get() = noImpl
        set(value) = noImpl
    var innerHTML : String
        get() = noImpl
        set(value) = noImpl
    var outerHTML : String
        get() = noImpl
        set(value) = noImpl
    var children : HTMLCollection
        get() = noImpl
        set(value) = noImpl
    var firstElementChild : Element?
        get() = noImpl
        set(value) = noImpl
    var lastElementChild : Element?
        get() = noImpl
        set(value) = noImpl
    var childElementCount : Int
        get() = noImpl
        set(value) = noImpl
    var previousElementSibling : Element?
        get() = noImpl
        set(value) = noImpl
    var nextElementSibling : Element?
        get() = noImpl
        set(value) = noImpl
    var cascadedStyle : CSSStyleDeclaration
        get() = noImpl
        set(value) = noImpl
    var defaultStyle : CSSStyleDeclaration
        get() = noImpl
        set(value) = noImpl
    var rawComputedStyle : CSSStyleDeclaration
        get() = noImpl
        set(value) = noImpl
    var usedStyle : CSSStyleDeclaration
        get() = noImpl
        set(value) = noImpl
    fun hasAttributes() : Boolean = noImpl
    fun getAttribute(name : String) : String? = noImpl
    fun getAttributeNS(namespace : String?, localName : String) : String? = noImpl
    fun setAttribute(name : String, value : String) : Unit = noImpl
    fun setAttributeNS(namespace : String?, name : String, value : String) : Unit = noImpl
    fun removeAttribute(name : String) : Unit = noImpl
    fun removeAttributeNS(namespace : String?, localName : String) : Unit = noImpl
    fun hasAttribute(name : String) : Boolean = noImpl
    fun hasAttributeNS(namespace : String?, localName : String) : Boolean = noImpl
    fun getAttributeNode(name : String) : Attr? = noImpl
    fun getAttributeNodeNS(namespace : String?, localName : String) : Attr? = noImpl
    fun setAttributeNode(attr : Attr) : Attr? = noImpl
    fun setAttributeNodeNS(attr : Attr) : Attr? = noImpl
    fun removeAttributeNode(attr : Attr) : Attr = noImpl
    fun closest(selectors : String) : Element? = noImpl
    fun matches(selectors : String) : Boolean = noImpl
    fun getElementsByTagName(localName : String) : HTMLCollection = noImpl
    fun getElementsByTagNameNS(namespace : String?, localName : String) : HTMLCollection = noImpl
    fun getElementsByClassName(classNames : String) : HTMLCollection = noImpl
    fun requestFullscreen() : Unit = noImpl
    fun insertAdjacentHTML(position : String, text : String) : Unit = noImpl
    fun pseudo(pseudoElt : String) : PseudoElement? = noImpl
    fun prepend(vararg nodes : dynamic) : Unit = noImpl
    fun append(vararg nodes : dynamic) : Unit = noImpl
    fun query(relativeSelectors : String) : Element? = noImpl
    fun queryAll(relativeSelectors : String) : dynamic = noImpl
    fun querySelector(selectors : String) : Element? = noImpl
    fun querySelectorAll(selectors : String) : NodeList = noImpl
    fun before(vararg nodes : dynamic) : Unit = noImpl
    fun after(vararg nodes : dynamic) : Unit = noImpl
    fun replaceWith(vararg nodes : dynamic) : Unit = noImpl
    fun remove() : Unit = noImpl
}
native trait NamedNodeMap {
    var length : Int
        get() = noImpl
        set(value) = noImpl
    fun item(index : Int) : Attr? = noImpl
    nativeGetter fun get(index : Int) : Attr? = noImpl
    fun getNamedItem(name : String) : Attr? = noImpl
    nativeGetter fun get(name : String) : Attr? = noImpl
    fun getNamedItemNS(namespace : String?, localName : String) : Attr? = noImpl
    fun setNamedItem(attr : Attr) : Attr? = noImpl
    fun setNamedItemNS(attr : Attr) : Attr? = noImpl
    fun removeNamedItem(name : String) : Attr = noImpl
    fun removeNamedItemNS(namespace : String?, localName : String) : Attr = noImpl
}
native trait Attr {
    var namespaceURI : String?
        get() = noImpl
        set(value) = noImpl
    var prefix : String?
        get() = noImpl
        set(value) = noImpl
    var localName : String
        get() = noImpl
        set(value) = noImpl
    var name : String
        get() = noImpl
        set(value) = noImpl
    var value : String
        get() = noImpl
        set(value) = noImpl
    var nodeValue : String
        get() = noImpl
        set(value) = noImpl
    var textContent : String
        get() = noImpl
        set(value) = noImpl
    var ownerElement : Element?
        get() = noImpl
        set(value) = noImpl
    var specified : Boolean
        get() = noImpl
        set(value) = noImpl
}
native trait CharacterData : Node {
    var data : String
        get() = noImpl
        set(value) = noImpl
    var length : Int
        get() = noImpl
        set(value) = noImpl
    var previousElementSibling : Element?
        get() = noImpl
        set(value) = noImpl
    var nextElementSibling : Element?
        get() = noImpl
        set(value) = noImpl
    fun substringData(offset : Int, count : Int) : String = noImpl
    fun appendData(data : String) : Unit = noImpl
    fun insertData(offset : Int, data : String) : Unit = noImpl
    fun deleteData(offset : Int, count : Int) : Unit = noImpl
    fun replaceData(offset : Int, count : Int, data : String) : Unit = noImpl
    fun before(vararg nodes : dynamic) : Unit = noImpl
    fun after(vararg nodes : dynamic) : Unit = noImpl
    fun replaceWith(vararg nodes : dynamic) : Unit = noImpl
    fun remove() : Unit = noImpl
}
native open class Text(data : String = "") : CharacterData {
    var wholeText : String
        get() = noImpl
        set(value) = noImpl
    fun splitText(offset : Int) : Text = noImpl
}
native trait ProcessingInstruction : CharacterData {
    var target : String
        get() = noImpl
        set(value) = noImpl
    var sheet : StyleSheet?
        get() = noImpl
        set(value) = noImpl
}
native open class Comment(data : String = "") : CharacterData {
}
native open class Range {
    var startContainer : Node
        get() = noImpl
        set(value) = noImpl
    var startOffset : Int
        get() = noImpl
        set(value) = noImpl
    var endContainer : Node
        get() = noImpl
        set(value) = noImpl
    var endOffset : Int
        get() = noImpl
        set(value) = noImpl
    var collapsed : Boolean
        get() = noImpl
        set(value) = noImpl
    var commonAncestorContainer : Node
        get() = noImpl
        set(value) = noImpl
    fun setStart(node : Node, offset : Int) : Unit = noImpl
    fun setEnd(node : Node, offset : Int) : Unit = noImpl
    fun setStartBefore(node : Node) : Unit = noImpl
    fun setStartAfter(node : Node) : Unit = noImpl
    fun setEndBefore(node : Node) : Unit = noImpl
    fun setEndAfter(node : Node) : Unit = noImpl
    fun collapse(toStart : Boolean = false) : Unit = noImpl
    fun selectNode(node : Node) : Unit = noImpl
    fun selectNodeContents(node : Node) : Unit = noImpl
    fun compareBoundaryPoints(how : Short, sourceRange : Range) : Short = noImpl
    fun deleteContents() : Unit = noImpl
    fun extractContents() : DocumentFragment = noImpl
    fun cloneContents() : DocumentFragment = noImpl
    fun insertNode(node : Node) : Unit = noImpl
    fun surroundContents(newParent : Node) : Unit = noImpl
    fun cloneRange() : Range = noImpl
    fun detach() : Unit = noImpl
    fun isPointInRange(node : Node, offset : Int) : Boolean = noImpl
    fun comparePoint(node : Node, offset : Int) : Short = noImpl
    fun intersectsNode(node : Node) : Boolean = noImpl
    fun createContextualFragment(fragment : String) : DocumentFragment = noImpl
companion object {
    val START_TO_START : Short = 0
    val START_TO_END : Short = 1
    val END_TO_END : Short = 2
    val END_TO_START : Short = 3
}
}
native trait NodeIterator {
    var root : Node
        get() = noImpl
        set(value) = noImpl
    var referenceNode : Node
        get() = noImpl
        set(value) = noImpl
    var pointerBeforeReferenceNode : Boolean
        get() = noImpl
        set(value) = noImpl
    var whatToShow : Int
        get() = noImpl
        set(value) = noImpl
    var filter : NodeFilter?
        get() = noImpl
        set(value) = noImpl
    fun nextNode() : Node? = noImpl
    fun previousNode() : Node? = noImpl
    fun detach() : Unit = noImpl
}
native trait TreeWalker {
    var root : Node
        get() = noImpl
        set(value) = noImpl
    var whatToShow : Int
        get() = noImpl
        set(value) = noImpl
    var filter : NodeFilter?
        get() = noImpl
        set(value) = noImpl
    var currentNode : Node
        get() = noImpl
        set(value) = noImpl
    fun parentNode() : Node? = noImpl
    fun firstChild() : Node? = noImpl
    fun lastChild() : Node? = noImpl
    fun previousSibling() : Node? = noImpl
    fun nextSibling() : Node? = noImpl
    fun previousNode() : Node? = noImpl
    fun nextNode() : Node? = noImpl
}
native trait NodeFilter {
    fun acceptNode(node : Node) : Short = noImpl
companion object {
    val FILTER_ACCEPT : Short = 1
    val FILTER_REJECT : Short = 2
    val FILTER_SKIP : Short = 3
    val SHOW_ALL : Int = noImpl
    val SHOW_ELEMENT : Int = 0x1
    val SHOW_ATTRIBUTE : Int = 0x2
    val SHOW_TEXT : Int = 0x4
    val SHOW_CDATA_SECTION : Int = 0x8
    val SHOW_ENTITY_REFERENCE : Int = 0x10
    val SHOW_ENTITY : Int = 0x20
    val SHOW_PROCESSING_INSTRUCTION : Int = 0x40
    val SHOW_COMMENT : Int = 0x80
    val SHOW_DOCUMENT : Int = 0x100
    val SHOW_DOCUMENT_TYPE : Int = 0x200
    val SHOW_DOCUMENT_FRAGMENT : Int = 0x400
    val SHOW_NOTATION : Int = 0x800
}
}
native trait DOMTokenList {
    var length : Int
        get() = noImpl
        set(value) = noImpl
    fun item(index : Int) : String? = noImpl
    nativeGetter fun get(index : Int) : String? = noImpl
    fun contains(token : String) : Boolean = noImpl
    fun add(vararg tokens : String) : Unit = noImpl
    fun remove(vararg tokens : String) : Unit = noImpl
    fun toggle(token : String, force : Boolean = noImpl) : Boolean = noImpl
}
native trait DOMSettableTokenList : DOMTokenList {
    var value : String
        get() = noImpl
        set(value) = noImpl
}
native open class UIEvent(type : String, eventInitDict : UIEventInit = noImpl) : Event(type, eventInitDict) {
    var view : Window?
        get() = noImpl
        set(value) = noImpl
    var detail : Int
        get() = noImpl
        set(value) = noImpl
    fun initUIEvent(typeArg : String, bubblesArg : Boolean, cancelableArg : Boolean, viewArg : Window?, detailArg : Int) : Unit = noImpl
}
native open class UIEventInit : EventInit() {
    var view : dynamic = null
    var detail : dynamic = 0
}
native open class FocusEvent(typeArg : String, focusEventInitDict : FocusEventInit = noImpl) : UIEvent(noImpl, noImpl) {
    var relatedTarget : EventTarget?
        get() = noImpl
        set(value) = noImpl
    fun initFocusEvent(typeArg : String, bubblesArg : Boolean, cancelableArg : Boolean, viewArg : Window?, detailArg : Int, relatedTargetArg : EventTarget?) : Unit = noImpl
}
native open class FocusEventInit : UIEventInit() {
    var relatedTarget : dynamic = null
}
native open class MouseEventInit : EventModifierInit() {
    var screenX : dynamic = 0
    var screenY : dynamic = 0
    var clientX : dynamic = 0
    var clientY : dynamic = 0
    var button : dynamic = 0
    var buttons : dynamic = 0
    var relatedTarget : dynamic = null
}
native open class EventModifierInit : UIEventInit() {
    var ctrlKey : dynamic = false
    var shiftKey : dynamic = false
    var altKey : dynamic = false
    var metaKey : dynamic = false
    var modifierAltGraph : dynamic = false
    var modifierCapsLock : dynamic = false
    var modifierFn : dynamic = false
    var modifierFnLock : dynamic = false
    var modifierHyper : dynamic = false
    var modifierNumLock : dynamic = false
    var modifierOS : dynamic = false
    var modifierScrollLock : dynamic = false
    var modifierSuper : dynamic = false
    var modifierSymbol : dynamic = false
    var modifierSymbolLock : dynamic = false
}
native open class WheelEvent(typeArg : String, wheelEventInitDict : WheelEventInit = noImpl) : MouseEvent(typeArg, noImpl) {
    var deltaX : Double
        get() = noImpl
        set(value) = noImpl
    var deltaY : Double
        get() = noImpl
        set(value) = noImpl
    var deltaZ : Double
        get() = noImpl
        set(value) = noImpl
    var deltaMode : Int
        get() = noImpl
        set(value) = noImpl
    fun initWheelEvent(typeArg : String, bubblesArg : Boolean, cancelableArg : Boolean, viewArg : Window?, detailArg : Int, screenXArg : Int, screenYArg : Int, clientXArg : Int, clientYArg : Int, buttonArg : Short, relatedTargetArg : EventTarget?, modifiersListArg : String, deltaXArg : Double, deltaYArg : Double, deltaZArg : Double, deltaMode : Int) : Unit = noImpl
companion object {
    val DOM_DELTA_PIXEL : Int = 0x00
    val DOM_DELTA_LINE : Int = 0x01
    val DOM_DELTA_PAGE : Int = 0x02
}
}
native open class WheelEventInit : MouseEventInit() {
    var deltaX : dynamic = 0.0
    var deltaY : dynamic = 0.0
    var deltaZ : dynamic = 0.0
    var deltaMode : dynamic = 0
}
native open class KeyboardEvent(typeArg : String, keyboardEventInitDict : KeyboardEventInit = noImpl) : UIEvent(noImpl, noImpl) {
    var key : String
        get() = noImpl
        set(value) = noImpl
    var code : String
        get() = noImpl
        set(value) = noImpl
    var location : Int
        get() = noImpl
        set(value) = noImpl
    var ctrlKey : Boolean
        get() = noImpl
        set(value) = noImpl
    var shiftKey : Boolean
        get() = noImpl
        set(value) = noImpl
    var altKey : Boolean
        get() = noImpl
        set(value) = noImpl
    var metaKey : Boolean
        get() = noImpl
        set(value) = noImpl
    var repeat : Boolean
        get() = noImpl
        set(value) = noImpl
    var isComposing : Boolean
        get() = noImpl
        set(value) = noImpl
    var charCode : Int
        get() = noImpl
        set(value) = noImpl
    var keyCode : Int
        get() = noImpl
        set(value) = noImpl
    var which : Int
        get() = noImpl
        set(value) = noImpl
    fun getModifierState(keyArg : String) : Boolean = noImpl
    fun initKeyboardEvent(typeArg : String, bubblesArg : Boolean, cancelableArg : Boolean, viewArg : Window?, keyArg : String, locationArg : Int, modifiersListArg : String, repeat : Boolean, locale : String) : Unit = noImpl
companion object {
    val DOM_KEY_LOCATION_STANDARD : Int = 0x00
    val DOM_KEY_LOCATION_LEFT : Int = 0x01
    val DOM_KEY_LOCATION_RIGHT : Int = 0x02
    val DOM_KEY_LOCATION_NUMPAD : Int = 0x03
}
}
native open class KeyboardEventInit : EventModifierInit() {
    var key : dynamic = ""
    var code : dynamic = ""
    var location : dynamic = 0
    var repeat : dynamic = false
    var isComposing : dynamic = false
}
native open class CompositionEvent(typeArg : String, compositionEventInitDict : CompositionEventInit = noImpl) : UIEvent(noImpl, noImpl) {
    var data : String
        get() = noImpl
        set(value) = noImpl
    fun initCompositionEvent(typeArg : String, bubblesArg : Boolean, cancelableArg : Boolean, viewArg : Window?, dataArg : String, locale : String) : Unit = noImpl
}
native open class CompositionEventInit : UIEventInit() {
    var data : dynamic = ""
}
native open class MutationEvent : Event(noImpl, noImpl) {
    var relatedNode : Node?
        get() = noImpl
        set(value) = noImpl
    var prevValue : String
        get() = noImpl
        set(value) = noImpl
    var newValue : String
        get() = noImpl
        set(value) = noImpl
    var attrName : String
        get() = noImpl
        set(value) = noImpl
    var attrChange : Short
        get() = noImpl
        set(value) = noImpl
    fun initMutationEvent(typeArg : String, bubblesArg : Boolean, cancelableArg : Boolean, relatedNodeArg : Node?, prevValueArg : String, newValueArg : String, attrNameArg : String, attrChangeArg : Short) : Unit = noImpl
companion object {
    val MODIFICATION : Short = 1
    val ADDITION : Short = 2
    val REMOVAL : Short = 3
}
}
native trait Selection {
    var anchorNode : Node?
        get() = noImpl
        set(value) = noImpl
    var anchorOffset : Int
        get() = noImpl
        set(value) = noImpl
    var focusNode : Node?
        get() = noImpl
        set(value) = noImpl
    var focusOffset : Int
        get() = noImpl
        set(value) = noImpl
    var isCollapsed : Boolean
        get() = noImpl
        set(value) = noImpl
    var rangeCount : Int
        get() = noImpl
        set(value) = noImpl
    fun collapse(node : Node, offset : Int) : Unit = noImpl
    fun collapseToStart() : Unit = noImpl
    fun collapseToEnd() : Unit = noImpl
    fun extend(node : Node, offset : Int) : Unit = noImpl
    fun selectAllChildren(node : Node) : Unit = noImpl
    fun deleteFromDocument() : Unit = noImpl
    fun getRangeAt(index : Int) : Range = noImpl
    fun addRange(range : Range) : Unit = noImpl
    fun removeRange(range : Range) : Unit = noImpl
    fun removeAllRanges() : Unit = noImpl
}
native open class EditingBeforeInputEvent(type : String, eventInitDict : EditingBeforeInputEventInit = noImpl) : Event(type, eventInitDict) {
    var command : String
        get() = noImpl
        set(value) = noImpl
    var value : String
        get() = noImpl
        set(value) = noImpl
}
native open class EditingBeforeInputEventInit : EventInit() {
    var command : dynamic
    var value : dynamic
}
native open class EditingInputEvent(type : String, eventInitDict : EditingInputEventInit = noImpl) : Event(type, eventInitDict) {
    var command : String
        get() = noImpl
        set(value) = noImpl
    var value : String
        get() = noImpl
        set(value) = noImpl
}
native open class EditingInputEventInit : EventInit() {
    var command : dynamic
    var value : dynamic
}
native trait XMLHttpRequestEventTarget : EventTarget {
    var onloadstart : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onprogress : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onabort : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onerror : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onload : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var ontimeout : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onloadend : () -> Unit
        get() = noImpl
        set(value) = noImpl
}
native trait XMLHttpRequestUpload : XMLHttpRequestEventTarget {
}
native open class XMLHttpRequest : XMLHttpRequestEventTarget {
    var onreadystatechange : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var readyState : Short
        get() = noImpl
        set(value) = noImpl
    var timeout : Int
        get() = noImpl
        set(value) = noImpl
    var withCredentials : Boolean
        get() = noImpl
        set(value) = noImpl
    var upload : XMLHttpRequestUpload
        get() = noImpl
        set(value) = noImpl
    var responseURL : String
        get() = noImpl
        set(value) = noImpl
    var status : Short
        get() = noImpl
        set(value) = noImpl
    var statusText : String
        get() = noImpl
        set(value) = noImpl
    var responseType : String
        get() = noImpl
        set(value) = noImpl
    var response : Any
        get() = noImpl
        set(value) = noImpl
    var responseText : String
        get() = noImpl
        set(value) = noImpl
    var responseXML : Document?
        get() = noImpl
        set(value) = noImpl
    fun open(method : String, url : String) : Unit = noImpl
    fun open(method : String, url : String, async : Boolean, username : String? = null, password : String? = null) : Unit = noImpl
    fun setRequestHeader(name : String, value : String) : Unit = noImpl
    fun send(body : dynamic = null) : Unit = noImpl
    fun abort() : Unit = noImpl
    fun getResponseHeader(name : String) : String? = noImpl
    fun getAllResponseHeaders() : String = noImpl
    fun overrideMimeType(mime : String) : Unit = noImpl
companion object {
    val UNSENT : Short = 0
    val OPENED : Short = 1
    val HEADERS_RECEIVED : Short = 2
    val LOADING : Short = 3
    val DONE : Short = 4
}
}
native open class FormData(form : HTMLFormElement = noImpl) {
    fun append(name : String, value : Blob, filename : String = noImpl) : Unit = noImpl
    fun append(name : String, value : String) : Unit = noImpl
    fun delete(name : String) : Unit = noImpl
    fun get(name : String) : dynamic = noImpl
    fun getAll(name : String) : Any = noImpl
    fun has(name : String) : Boolean = noImpl
    fun set(name : String, value : Blob, filename : String = noImpl) : Unit = noImpl
    fun set(name : String, value : String) : Unit = noImpl
}
native open class ProgressEvent(type : String, eventInitDict : ProgressEventInit = noImpl) : Event(type, eventInitDict) {
    var lengthComputable : Boolean
        get() = noImpl
        set(value) = noImpl
    var loaded : Long
        get() = noImpl
        set(value) = noImpl
    var total : Long
        get() = noImpl
        set(value) = noImpl
}
native open class ProgressEventInit : EventInit() {
    var lengthComputable : dynamic = false
    var loaded : dynamic = 0
    var total : dynamic = 0
}
native open class Blob {
    var size : Long
        get() = noImpl
        set(value) = noImpl
    var type : String
        get() = noImpl
        set(value) = noImpl
    var isClosed : Boolean
        get() = noImpl
        set(value) = noImpl
    fun slice(start : Long = noImpl, end : Long = noImpl, contentType : String = noImpl) : Blob = noImpl
    fun close() : Unit = noImpl
}
native open class BlobPropertyBag {
    var type : dynamic = ""
}
native open class File(fileBits : Any, fileName : String, options : FilePropertyBag = noImpl) : Blob() {
    var name : String
        get() = noImpl
        set(value) = noImpl
    var lastModified : Long
        get() = noImpl
        set(value) = noImpl
}
native open class FilePropertyBag {
    var type : dynamic = ""
    var lastModified : dynamic
}
native trait FileList {
    var length : Int
        get() = noImpl
        set(value) = noImpl
    fun item(index : Int) : File? = noImpl
    nativeGetter fun get(index : Int) : File? = noImpl
}
native open class FileReader : EventTarget {
    var readyState : Short
        get() = noImpl
        set(value) = noImpl
    var result : dynamic
        get() = noImpl
        set(value) = noImpl
    var error : dynamic
        get() = noImpl
        set(value) = noImpl
    var onloadstart : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onprogress : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onload : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onabort : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onerror : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onloadend : () -> Unit
        get() = noImpl
        set(value) = noImpl
    fun readAsArrayBuffer(blob : Blob) : Unit = noImpl
    fun readAsText(blob : Blob, label : String = noImpl) : Unit = noImpl
    fun readAsDataURL(blob : Blob) : Unit = noImpl
    fun abort() : Unit = noImpl
companion object {
    val EMPTY : Short = 0
    val LOADING : Short = 1
    val DONE : Short = 2
}
}
native open class FileReaderSync {
    fun readAsArrayBuffer(blob : Blob) : dynamic = noImpl
    fun readAsText(blob : Blob, label : String = noImpl) : String = noImpl
    fun readAsDataURL(blob : Blob) : String = noImpl
}
native trait URL {
}
native open class Notification(title : String, options : NotificationOptions = noImpl) : EventTarget {
    var permission : String
        get() = noImpl
        set(value) = noImpl
    var onclick : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onerror : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var title : String
        get() = noImpl
        set(value) = noImpl
    var dir : String
        get() = noImpl
        set(value) = noImpl
    var lang : String
        get() = noImpl
        set(value) = noImpl
    var body : String
        get() = noImpl
        set(value) = noImpl
    var tag : String
        get() = noImpl
        set(value) = noImpl
    var icon : String
        get() = noImpl
        set(value) = noImpl
    var sound : String
        get() = noImpl
        set(value) = noImpl
    var renotify : Boolean
        get() = noImpl
        set(value) = noImpl
    var silent : Boolean
        get() = noImpl
        set(value) = noImpl
    var noscreen : Boolean
        get() = noImpl
        set(value) = noImpl
    var sticky : Boolean
        get() = noImpl
        set(value) = noImpl
    var data : Any
        get() = noImpl
        set(value) = noImpl
    fun close() : Unit = noImpl
}
native open class NotificationOptions {
    var dir : dynamic = "auto"
    var lang : dynamic = ""
    var body : dynamic = ""
    var tag : dynamic = ""
    var icon : dynamic
    var sound : dynamic
    var vibrate : dynamic
    var renotify : dynamic = false
    var silent : dynamic = false
    var noscreen : dynamic = false
    var sticky : dynamic = false
    var data : dynamic = null
}
native open class GetNotificationOptions {
    var tag : dynamic = ""
}
native trait ServiceWorkerRegistration : EventTarget {
    var installing : ServiceWorker?
        get() = noImpl
        set(value) = noImpl
    var waiting : ServiceWorker?
        get() = noImpl
        set(value) = noImpl
    var active : ServiceWorker?
        get() = noImpl
        set(value) = noImpl
    var scope : String
        get() = noImpl
        set(value) = noImpl
    var onupdatefound : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var APISpace : dynamic
        get() = noImpl
        set(value) = noImpl
    fun showNotification(title : String, options : NotificationOptions = noImpl) : dynamic = noImpl
    fun getNotifications(filter : GetNotificationOptions = noImpl) : dynamic = noImpl
    fun update() : Unit = noImpl
    fun unregister() : dynamic = noImpl
    fun methodName(of : dynamic) : dynamic = noImpl
}
native open class NotificationEvent(type : String, eventInitDict : NotificationEventInit = noImpl) : ExtendableEvent(type, eventInitDict) {
    var notification : Notification
        get() = noImpl
        set(value) = noImpl
}
native open class NotificationEventInit : ExtendableEventInit() {
    var notification : dynamic
}
native trait ServiceWorkerGlobalScope : WorkerGlobalScope {
    var onnotificationclick : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var clients : Clients
        get() = noImpl
        set(value) = noImpl
    var registration : ServiceWorkerRegistration
        get() = noImpl
        set(value) = noImpl
    var oninstall : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onactivate : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onfetch : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onmessage : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onfunctionalevent : () -> Unit
        get() = noImpl
        set(value) = noImpl
    fun skipWaiting() : dynamic = noImpl
}
native open class DOMParser {
    fun parseFromString(str : String, type : dynamic) : Document = noImpl
}
native open class XMLSerializer {
    fun serializeToString(root : Node) : String = noImpl
}
native trait ServiceWorker : EventTarget {
    var scriptURL : String
        get() = noImpl
        set(value) = noImpl
    var state : String
        get() = noImpl
        set(value) = noImpl
    var id : String
        get() = noImpl
        set(value) = noImpl
    var onstatechange : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onerror : () -> Unit
        get() = noImpl
        set(value) = noImpl
    fun postMessage(message : Any, transfer : Any = noImpl) : Unit = noImpl
}
native trait ServiceWorkerContainer : EventTarget {
    var controller : ServiceWorker?
        get() = noImpl
        set(value) = noImpl
    var ready : dynamic
        get() = noImpl
        set(value) = noImpl
    var oncontrollerchange : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onerror : () -> Unit
        get() = noImpl
        set(value) = noImpl
    var onmessage : () -> Unit
        get() = noImpl
        set(value) = noImpl
    fun register(scriptURL : String, options : RegistrationOptions = noImpl) : dynamic = noImpl
    fun getRegistration(clientURL : String = "") : dynamic = noImpl
    fun getRegistrations() : dynamic = noImpl
}
native open class RegistrationOptions {
    var scope : dynamic
}
native trait Client {
    var url : String
        get() = noImpl
        set(value) = noImpl
    var frameType : String
        get() = noImpl
        set(value) = noImpl
    var id : String
        get() = noImpl
        set(value) = noImpl
    fun postMessage(message : Any, transfer : Any = noImpl) : Unit = noImpl
}
native trait WindowClient : Client {
    var visibilityState : dynamic
        get() = noImpl
        set(value) = noImpl
    var focused : Boolean
        get() = noImpl
        set(value) = noImpl
    fun focus() : dynamic = noImpl
}
native trait Clients {
    fun matchAll(options : ClientQueryOptions = noImpl) : dynamic = noImpl
    fun openWindow(url : String) : dynamic = noImpl
    fun claim() : dynamic = noImpl
}
native open class ClientQueryOptions {
    var includeUncontrolled : dynamic = false
    var type : dynamic = "window"
}
native open class ExtendableEvent(type : String, eventInitDict : ExtendableEventInit = noImpl) : Event(type, eventInitDict) {
    fun waitUntil(f : dynamic) : Unit = noImpl
}
native open class ExtendableEventInit : EventInit() {
}
native open class FetchEvent(type : String, eventInitDict : FetchEventInit = noImpl) : ExtendableEvent(type, eventInitDict) {
    var request : Request
        get() = noImpl
        set(value) = noImpl
    var client : Client
        get() = noImpl
        set(value) = noImpl
    var isReload : Boolean
        get() = noImpl
        set(value) = noImpl
    fun respondWith(r : dynamic) : Unit = noImpl
}
native open class FetchEventInit : ExtendableEventInit() {
    var request : dynamic
    var client : dynamic
    var isReload : dynamic = false
}
native trait Cache {
    fun match(request : dynamic, options : CacheQueryOptions = noImpl) : dynamic = noImpl
    fun matchAll(request : dynamic = noImpl, options : CacheQueryOptions = noImpl) : dynamic = noImpl
    fun add(request : dynamic) : dynamic = noImpl
    fun addAll(requests : Any) : dynamic = noImpl
    fun put(request : dynamic, response : Response) : dynamic = noImpl
    fun delete(request : dynamic, options : CacheQueryOptions = noImpl) : dynamic = noImpl
    fun keys(request : dynamic = noImpl, options : CacheQueryOptions = noImpl) : dynamic = noImpl
}
native open class CacheQueryOptions {
    var ignoreSearch : dynamic = false
    var ignoreMethod : dynamic = false
    var ignoreVary : dynamic = false
    var cacheName : dynamic
}
native open class CacheBatchOperation {
    var type : dynamic
    var request : dynamic
    var response : dynamic
    var options : dynamic
}
native trait CacheStorage {
    fun match(request : dynamic, options : CacheQueryOptions = noImpl) : dynamic = noImpl
    fun has(cacheName : String) : dynamic = noImpl
    fun open(cacheName : String) : dynamic = noImpl
    fun delete(cacheName : String) : dynamic = noImpl
    fun keys() : dynamic = noImpl
}
native open class FunctionalEvent : ExtendableEvent(noImpl, noImpl) {
}
native open class Headers(init : dynamic = noImpl) {
    fun append(name : String, value : String) : Unit = noImpl
    fun delete(name : String) : Unit = noImpl
    fun get(name : String) : String? = noImpl
    fun getAll(name : String) : Any = noImpl
    fun has(name : String) : Boolean = noImpl
    fun set(name : String, value : String) : Unit = noImpl
}
native open class Request(input : dynamic, init : RequestInit = noImpl) {
    var method : String
        get() = noImpl
        set(value) = noImpl
    var url : String
        get() = noImpl
        set(value) = noImpl
    var headers : Headers
        get() = noImpl
        set(value) = noImpl
    var context : String
        get() = noImpl
        set(value) = noImpl
    var referrer : String
        get() = noImpl
        set(value) = noImpl
    var mode : String
        get() = noImpl
        set(value) = noImpl
    var credentials : String
        get() = noImpl
        set(value) = noImpl
    var cache : String
        get() = noImpl
        set(value) = noImpl
    var redirect : String
        get() = noImpl
        set(value) = noImpl
    var bodyUsed : Boolean
        get() = noImpl
        set(value) = noImpl
    fun clone() : Request = noImpl
    fun arrayBuffer() : dynamic = noImpl
    fun blob() : dynamic = noImpl
    fun formData() : dynamic = noImpl
    fun json() : dynamic = noImpl
    fun text() : dynamic = noImpl
}
native open class RequestInit {
    var method : dynamic
    var headers : dynamic
    var body : dynamic
    var mode : dynamic
    var credentials : dynamic
    var cache : dynamic
    var redirect : dynamic
}
native open class Response(body : dynamic = noImpl, init : ResponseInit = noImpl) {
    var type : String
        get() = noImpl
        set(value) = noImpl
    var url : String
        get() = noImpl
        set(value) = noImpl
    var status : Short
        get() = noImpl
        set(value) = noImpl
    var ok : Boolean
        get() = noImpl
        set(value) = noImpl
    var statusText : String
        get() = noImpl
        set(value) = noImpl
    var headers : Headers
        get() = noImpl
        set(value) = noImpl
    var bodyUsed : Boolean
        get() = noImpl
        set(value) = noImpl
    fun clone() : Response = noImpl
    fun arrayBuffer() : dynamic = noImpl
    fun blob() : dynamic = noImpl
    fun formData() : dynamic = noImpl
    fun json() : dynamic = noImpl
    fun text() : dynamic = noImpl
}
native open class ResponseInit {
    var status : dynamic = 200
    var statusText : dynamic = "OK"
    var headers : dynamic
}
native trait MediaList {
    var mediaText : String
        get() = noImpl
        set(value) = noImpl
    var length : Int
        get() = noImpl
        set(value) = noImpl
    fun item(index : Int) : String? = noImpl
    nativeGetter fun get(index : Int) : String? = noImpl
    fun appendMedium(medium : String) : Unit = noImpl
    fun deleteMedium(medium : String) : Unit = noImpl
}
native trait StyleSheet {
    var type : String
        get() = noImpl
        set(value) = noImpl
    var href : String?
        get() = noImpl
        set(value) = noImpl
    var ownerNode : dynamic
        get() = noImpl
        set(value) = noImpl
    var parentStyleSheet : StyleSheet?
        get() = noImpl
        set(value) = noImpl
    var title : String?
        get() = noImpl
        set(value) = noImpl
    var media : MediaList
        get() = noImpl
        set(value) = noImpl
    var disabled : Boolean
        get() = noImpl
        set(value) = noImpl
}
native trait CSSStyleSheet : StyleSheet {
    var ownerRule : CSSRule?
        get() = noImpl
        set(value) = noImpl
    var cssRules : CSSRuleList
        get() = noImpl
        set(value) = noImpl
    fun insertRule(rule : String, index : Int) : Int = noImpl
    fun deleteRule(index : Int) : Unit = noImpl
}
native trait StyleSheetList {
    var length : Int
        get() = noImpl
        set(value) = noImpl
    fun item(index : Int) : StyleSheet? = noImpl
    nativeGetter fun get(index : Int) : StyleSheet? = noImpl
}
native trait CSSRuleList {
    var length : Int
        get() = noImpl
        set(value) = noImpl
    fun item(index : Int) : CSSRule? = noImpl
    nativeGetter fun get(index : Int) : CSSRule? = noImpl
}
native trait CSSRule {
    var type : Short
        get() = noImpl
        set(value) = noImpl
    var cssText : String
        get() = noImpl
        set(value) = noImpl
    var parentRule : CSSRule?
        get() = noImpl
        set(value) = noImpl
    var parentStyleSheet : CSSStyleSheet?
        get() = noImpl
        set(value) = noImpl
companion object {
    val STYLE_RULE : Short = 1
    val CHARSET_RULE : Short = 2
    val IMPORT_RULE : Short = 3
    val MEDIA_RULE : Short = 4
    val FONT_FACE_RULE : Short = 5
    val PAGE_RULE : Short = 6
    val MARGIN_RULE : Short = 9
    val NAMESPACE_RULE : Short = 10
}
}
native trait CSSStyleRule : CSSRule {
    var selectorText : String
        get() = noImpl
        set(value) = noImpl
    var style : CSSStyleDeclaration
        get() = noImpl
        set(value) = noImpl
}
native trait CSSImportRule : CSSRule {
    var href : String
        get() = noImpl
        set(value) = noImpl
    var media : MediaList
        get() = noImpl
        set(value) = noImpl
    var styleSheet : CSSStyleSheet
        get() = noImpl
        set(value) = noImpl
}
native trait CSSGroupingRule : CSSRule {
    var cssRules : CSSRuleList
        get() = noImpl
        set(value) = noImpl
    fun insertRule(rule : String, index : Int) : Int = noImpl
    fun deleteRule(index : Int) : Unit = noImpl
}
native trait CSSMediaRule : CSSGroupingRule {
    var media : MediaList
        get() = noImpl
        set(value) = noImpl
}
native trait CSSPageRule : CSSGroupingRule {
    var selectorText : String
        get() = noImpl
        set(value) = noImpl
    var style : CSSStyleDeclaration
        get() = noImpl
        set(value) = noImpl
}
native trait CSSMarginRule : CSSRule {
    var name : String
        get() = noImpl
        set(value) = noImpl
    var style : CSSStyleDeclaration
        get() = noImpl
        set(value) = noImpl
}
native trait CSSNamespaceRule : CSSRule {
    var namespaceURI : String
        get() = noImpl
        set(value) = noImpl
    var prefix : String
        get() = noImpl
        set(value) = noImpl
}
native trait CSSStyleDeclaration {
    var cssText : String
        get() = noImpl
        set(value) = noImpl
    var length : Int
        get() = noImpl
        set(value) = noImpl
    var parentRule : CSSRule?
        get() = noImpl
        set(value) = noImpl
    var cssFloat : String
        get() = noImpl
        set(value) = noImpl
    var _dashed_attribute : String
        get() = noImpl
        set(value) = noImpl
    var _camel_cased_attribute : String
        get() = noImpl
        set(value) = noImpl
    fun item(index : Int) : String = noImpl
    nativeGetter fun get(index : Int) : String? = noImpl
    fun getPropertyValue(property : String) : String = noImpl
    fun getPropertyPriority(property : String) : String = noImpl
    fun setProperty(property : String, value : String, priority : String = "") : Unit = noImpl
    fun setPropertyValue(property : String, value : String) : Unit = noImpl
    fun setPropertyPriority(property : String, priority : String) : Unit = noImpl
    fun removeProperty(property : String) : String = noImpl
}
native trait PseudoElement {
    var cascadedStyle : CSSStyleDeclaration
        get() = noImpl
        set(value) = noImpl
    var defaultStyle : CSSStyleDeclaration
        get() = noImpl
        set(value) = noImpl
    var rawComputedStyle : CSSStyleDeclaration
        get() = noImpl
        set(value) = noImpl
    var usedStyle : CSSStyleDeclaration
        get() = noImpl
        set(value) = noImpl
}
native trait CSS {
}
native trait UnionHTMLOptGroupElementOrHTMLOptionElement {
}
native trait UnionElementOrMouseEvent {
}
