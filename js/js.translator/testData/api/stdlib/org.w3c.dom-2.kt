
public external interface DocumentAndElementEventHandlers {
    public open var oncopy: ((org.w3c.dom.clipboard.ClipboardEvent) -> dynamic)?
        public open fun <get-oncopy>(): ((org.w3c.dom.clipboard.ClipboardEvent) -> dynamic)?
        public open fun <set-oncopy>(/*0*/ value: ((org.w3c.dom.clipboard.ClipboardEvent) -> dynamic)?): kotlin.Unit
    public open var oncut: ((org.w3c.dom.clipboard.ClipboardEvent) -> dynamic)?
        public open fun <get-oncut>(): ((org.w3c.dom.clipboard.ClipboardEvent) -> dynamic)?
        public open fun <set-oncut>(/*0*/ value: ((org.w3c.dom.clipboard.ClipboardEvent) -> dynamic)?): kotlin.Unit
    public open var onpaste: ((org.w3c.dom.clipboard.ClipboardEvent) -> dynamic)?
        public open fun <get-onpaste>(): ((org.w3c.dom.clipboard.ClipboardEvent) -> dynamic)?
        public open fun <set-onpaste>(/*0*/ value: ((org.w3c.dom.clipboard.ClipboardEvent) -> dynamic)?): kotlin.Unit
}

public open external class DocumentFragment : org.w3c.dom.Node, org.w3c.dom.NonElementParentNode, org.w3c.dom.ParentNode {
    /*primary*/ public constructor DocumentFragment()
    public open override /*1*/ val childElementCount: kotlin.Int
        public open override /*1*/ fun <get-childElementCount>(): kotlin.Int
    public open override /*1*/ val children: org.w3c.dom.HTMLCollection
        public open override /*1*/ fun <get-children>(): org.w3c.dom.HTMLCollection
    public open override /*1*/ val firstElementChild: org.w3c.dom.Element?
        public open override /*1*/ fun <get-firstElementChild>(): org.w3c.dom.Element?
    public open override /*1*/ val lastElementChild: org.w3c.dom.Element?
        public open override /*1*/ fun <get-lastElementChild>(): org.w3c.dom.Element?
    public open override /*1*/ fun append(/*0*/ vararg nodes: dynamic /*kotlin.Array<out dynamic>*/): kotlin.Unit
    public open override /*1*/ fun getElementById(/*0*/ elementId: kotlin.String): org.w3c.dom.Element?
    public open override /*1*/ fun prepend(/*0*/ vararg nodes: dynamic /*kotlin.Array<out dynamic>*/): kotlin.Unit
    public open override /*1*/ fun querySelector(/*0*/ selectors: kotlin.String): org.w3c.dom.Element?
    public open override /*1*/ fun querySelectorAll(/*0*/ selectors: kotlin.String): org.w3c.dom.NodeList

    public companion object Companion {
        public final val ATTRIBUTE_NODE: kotlin.Short
            public final fun <get-ATTRIBUTE_NODE>(): kotlin.Short
        public final val CDATA_SECTION_NODE: kotlin.Short
            public final fun <get-CDATA_SECTION_NODE>(): kotlin.Short
        public final val COMMENT_NODE: kotlin.Short
            public final fun <get-COMMENT_NODE>(): kotlin.Short
        public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short
            public final fun <get-DOCUMENT_FRAGMENT_NODE>(): kotlin.Short
        public final val DOCUMENT_NODE: kotlin.Short
            public final fun <get-DOCUMENT_NODE>(): kotlin.Short
        public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_CONTAINED_BY>(): kotlin.Short
        public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_CONTAINS>(): kotlin.Short
        public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_DISCONNECTED>(): kotlin.Short
        public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_FOLLOWING>(): kotlin.Short
        public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC>(): kotlin.Short
        public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_PRECEDING>(): kotlin.Short
        public final val DOCUMENT_TYPE_NODE: kotlin.Short
            public final fun <get-DOCUMENT_TYPE_NODE>(): kotlin.Short
        public final val ELEMENT_NODE: kotlin.Short
            public final fun <get-ELEMENT_NODE>(): kotlin.Short
        public final val ENTITY_NODE: kotlin.Short
            public final fun <get-ENTITY_NODE>(): kotlin.Short
        public final val ENTITY_REFERENCE_NODE: kotlin.Short
            public final fun <get-ENTITY_REFERENCE_NODE>(): kotlin.Short
        public final val NOTATION_NODE: kotlin.Short
            public final fun <get-NOTATION_NODE>(): kotlin.Short
        public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short
            public final fun <get-PROCESSING_INSTRUCTION_NODE>(): kotlin.Short
        public final val TEXT_NODE: kotlin.Short
            public final fun <get-TEXT_NODE>(): kotlin.Short
    }
}

public external interface DocumentOrShadowRoot {
    public open val fullscreenElement: org.w3c.dom.Element?
        public open fun <get-fullscreenElement>(): org.w3c.dom.Element?
}

public external interface DocumentReadyState {

    public companion object Companion {
    }
}

public abstract external class DocumentType : org.w3c.dom.Node, org.w3c.dom.ChildNode {
    /*primary*/ public constructor DocumentType()
    public open val name: kotlin.String
        public open fun <get-name>(): kotlin.String
    public open val publicId: kotlin.String
        public open fun <get-publicId>(): kotlin.String
    public open val systemId: kotlin.String
        public open fun <get-systemId>(): kotlin.String

    public companion object Companion {
        public final val ATTRIBUTE_NODE: kotlin.Short
            public final fun <get-ATTRIBUTE_NODE>(): kotlin.Short
        public final val CDATA_SECTION_NODE: kotlin.Short
            public final fun <get-CDATA_SECTION_NODE>(): kotlin.Short
        public final val COMMENT_NODE: kotlin.Short
            public final fun <get-COMMENT_NODE>(): kotlin.Short
        public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short
            public final fun <get-DOCUMENT_FRAGMENT_NODE>(): kotlin.Short
        public final val DOCUMENT_NODE: kotlin.Short
            public final fun <get-DOCUMENT_NODE>(): kotlin.Short
        public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_CONTAINED_BY>(): kotlin.Short
        public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_CONTAINS>(): kotlin.Short
        public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_DISCONNECTED>(): kotlin.Short
        public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_FOLLOWING>(): kotlin.Short
        public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC>(): kotlin.Short
        public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_PRECEDING>(): kotlin.Short
        public final val DOCUMENT_TYPE_NODE: kotlin.Short
            public final fun <get-DOCUMENT_TYPE_NODE>(): kotlin.Short
        public final val ELEMENT_NODE: kotlin.Short
            public final fun <get-ELEMENT_NODE>(): kotlin.Short
        public final val ENTITY_NODE: kotlin.Short
            public final fun <get-ENTITY_NODE>(): kotlin.Short
        public final val ENTITY_REFERENCE_NODE: kotlin.Short
            public final fun <get-ENTITY_REFERENCE_NODE>(): kotlin.Short
        public final val NOTATION_NODE: kotlin.Short
            public final fun <get-NOTATION_NODE>(): kotlin.Short
        public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short
            public final fun <get-PROCESSING_INSTRUCTION_NODE>(): kotlin.Short
        public final val TEXT_NODE: kotlin.Short
            public final fun <get-TEXT_NODE>(): kotlin.Short
    }
}

public open external class DragEvent : org.w3c.dom.events.MouseEvent {
    /*primary*/ public constructor DragEvent(/*0*/ type: kotlin.String, /*1*/ eventInitDict: org.w3c.dom.DragEventInit = ...)
    public open val dataTransfer: org.w3c.dom.DataTransfer?
        public open fun <get-dataTransfer>(): org.w3c.dom.DataTransfer?

    public companion object Companion {
        public final val AT_TARGET: kotlin.Short
            public final fun <get-AT_TARGET>(): kotlin.Short
        public final val BUBBLING_PHASE: kotlin.Short
            public final fun <get-BUBBLING_PHASE>(): kotlin.Short
        public final val CAPTURING_PHASE: kotlin.Short
            public final fun <get-CAPTURING_PHASE>(): kotlin.Short
        public final val NONE: kotlin.Short
            public final fun <get-NONE>(): kotlin.Short
    }
}

public external interface DragEventInit : org.w3c.dom.events.MouseEventInit {
    public open var dataTransfer: org.w3c.dom.DataTransfer?
        public open fun <get-dataTransfer>(): org.w3c.dom.DataTransfer?
        public open fun <set-dataTransfer>(/*0*/ value: org.w3c.dom.DataTransfer?): kotlin.Unit
}

public abstract external class Element : org.w3c.dom.Node, org.w3c.dom.ParentNode, org.w3c.dom.NonDocumentTypeChildNode, org.w3c.dom.ChildNode, org.w3c.dom.Slotable, org.w3c.dom.GeometryUtils, org.w3c.dom.UnionElementOrHTMLCollection, org.w3c.dom.UnionElementOrRadioNodeList, org.w3c.dom.UnionElementOrMouseEvent, org.w3c.dom.css.UnionElementOrProcessingInstruction {
    /*primary*/ public constructor Element()
    public open val attributes: org.w3c.dom.NamedNodeMap
        public open fun <get-attributes>(): org.w3c.dom.NamedNodeMap
    public open val classList: org.w3c.dom.DOMTokenList
        public open fun <get-classList>(): org.w3c.dom.DOMTokenList
    public open var className: kotlin.String
        public open fun <get-className>(): kotlin.String
        public open fun <set-className>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open val clientHeight: kotlin.Int
        public open fun <get-clientHeight>(): kotlin.Int
    public open val clientLeft: kotlin.Int
        public open fun <get-clientLeft>(): kotlin.Int
    public open val clientTop: kotlin.Int
        public open fun <get-clientTop>(): kotlin.Int
    public open val clientWidth: kotlin.Int
        public open fun <get-clientWidth>(): kotlin.Int
    public open var id: kotlin.String
        public open fun <get-id>(): kotlin.String
        public open fun <set-id>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var innerHTML: kotlin.String
        public open fun <get-innerHTML>(): kotlin.String
        public open fun <set-innerHTML>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open val localName: kotlin.String
        public open fun <get-localName>(): kotlin.String
    public open val namespaceURI: kotlin.String?
        public open fun <get-namespaceURI>(): kotlin.String?
    public open var outerHTML: kotlin.String
        public open fun <get-outerHTML>(): kotlin.String
        public open fun <set-outerHTML>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open val prefix: kotlin.String?
        public open fun <get-prefix>(): kotlin.String?
    public open val scrollHeight: kotlin.Int
        public open fun <get-scrollHeight>(): kotlin.Int
    public open var scrollLeft: kotlin.Double
        public open fun <get-scrollLeft>(): kotlin.Double
        public open fun <set-scrollLeft>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public open var scrollTop: kotlin.Double
        public open fun <get-scrollTop>(): kotlin.Double
        public open fun <set-scrollTop>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public open val scrollWidth: kotlin.Int
        public open fun <get-scrollWidth>(): kotlin.Int
    public open val shadowRoot: org.w3c.dom.ShadowRoot?
        public open fun <get-shadowRoot>(): org.w3c.dom.ShadowRoot?
    public open var slot: kotlin.String
        public open fun <get-slot>(): kotlin.String
        public open fun <set-slot>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open val tagName: kotlin.String
        public open fun <get-tagName>(): kotlin.String
    public final fun attachShadow(/*0*/ init: org.w3c.dom.ShadowRootInit): org.w3c.dom.ShadowRoot
    public final fun closest(/*0*/ selectors: kotlin.String): org.w3c.dom.Element?
    public final fun getAttribute(/*0*/ qualifiedName: kotlin.String): kotlin.String?
    public final fun getAttributeNS(/*0*/ namespace: kotlin.String?, /*1*/ localName: kotlin.String): kotlin.String?
    public final fun getAttributeNames(): kotlin.Array<kotlin.String>
    public final fun getAttributeNode(/*0*/ qualifiedName: kotlin.String): org.w3c.dom.Attr?
    public final fun getAttributeNodeNS(/*0*/ namespace: kotlin.String?, /*1*/ localName: kotlin.String): org.w3c.dom.Attr?
    public final fun getBoundingClientRect(): org.w3c.dom.DOMRect
    public final fun getClientRects(): kotlin.Array<org.w3c.dom.DOMRect>
    public final fun getElementsByClassName(/*0*/ classNames: kotlin.String): org.w3c.dom.HTMLCollection
    public final fun getElementsByTagName(/*0*/ qualifiedName: kotlin.String): org.w3c.dom.HTMLCollection
    public final fun getElementsByTagNameNS(/*0*/ namespace: kotlin.String?, /*1*/ localName: kotlin.String): org.w3c.dom.HTMLCollection
    public final fun hasAttribute(/*0*/ qualifiedName: kotlin.String): kotlin.Boolean
    public final fun hasAttributeNS(/*0*/ namespace: kotlin.String?, /*1*/ localName: kotlin.String): kotlin.Boolean
    public final fun hasAttributes(): kotlin.Boolean
    public final fun hasPointerCapture(/*0*/ pointerId: kotlin.Int): kotlin.Boolean
    public final fun insertAdjacentElement(/*0*/ where: kotlin.String, /*1*/ element: org.w3c.dom.Element): org.w3c.dom.Element?
    public final fun insertAdjacentHTML(/*0*/ position: kotlin.String, /*1*/ text: kotlin.String): kotlin.Unit
    public final fun insertAdjacentText(/*0*/ where: kotlin.String, /*1*/ data: kotlin.String): kotlin.Unit
    public final fun matches(/*0*/ selectors: kotlin.String): kotlin.Boolean
    public final fun releasePointerCapture(/*0*/ pointerId: kotlin.Int): kotlin.Unit
    public final fun removeAttribute(/*0*/ qualifiedName: kotlin.String): kotlin.Unit
    public final fun removeAttributeNS(/*0*/ namespace: kotlin.String?, /*1*/ localName: kotlin.String): kotlin.Unit
    public final fun removeAttributeNode(/*0*/ attr: org.w3c.dom.Attr): org.w3c.dom.Attr
    public final fun requestFullscreen(): kotlin.js.Promise<kotlin.Unit>
    public final fun scroll(/*0*/ x: kotlin.Double, /*1*/ y: kotlin.Double): kotlin.Unit
    public final fun scroll(/*0*/ options: org.w3c.dom.ScrollToOptions = ...): kotlin.Unit
    public final fun scrollBy(/*0*/ x: kotlin.Double, /*1*/ y: kotlin.Double): kotlin.Unit
    public final fun scrollBy(/*0*/ options: org.w3c.dom.ScrollToOptions = ...): kotlin.Unit
    public final fun scrollIntoView(): kotlin.Unit
    public final fun scrollIntoView(/*0*/ arg: dynamic): kotlin.Unit
    public final fun scrollTo(/*0*/ x: kotlin.Double, /*1*/ y: kotlin.Double): kotlin.Unit
    public final fun scrollTo(/*0*/ options: org.w3c.dom.ScrollToOptions = ...): kotlin.Unit
    public final fun setAttribute(/*0*/ qualifiedName: kotlin.String, /*1*/ value: kotlin.String): kotlin.Unit
    public final fun setAttributeNS(/*0*/ namespace: kotlin.String?, /*1*/ qualifiedName: kotlin.String, /*2*/ value: kotlin.String): kotlin.Unit
    public final fun setAttributeNode(/*0*/ attr: org.w3c.dom.Attr): org.w3c.dom.Attr?
    public final fun setAttributeNodeNS(/*0*/ attr: org.w3c.dom.Attr): org.w3c.dom.Attr?
    public final fun setPointerCapture(/*0*/ pointerId: kotlin.Int): kotlin.Unit
    public final fun webkitMatchesSelector(/*0*/ selectors: kotlin.String): kotlin.Boolean

    public companion object Companion {
        public final val ATTRIBUTE_NODE: kotlin.Short
            public final fun <get-ATTRIBUTE_NODE>(): kotlin.Short
        public final val CDATA_SECTION_NODE: kotlin.Short
            public final fun <get-CDATA_SECTION_NODE>(): kotlin.Short
        public final val COMMENT_NODE: kotlin.Short
            public final fun <get-COMMENT_NODE>(): kotlin.Short
        public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short
            public final fun <get-DOCUMENT_FRAGMENT_NODE>(): kotlin.Short
        public final val DOCUMENT_NODE: kotlin.Short
            public final fun <get-DOCUMENT_NODE>(): kotlin.Short
        public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_CONTAINED_BY>(): kotlin.Short
        public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_CONTAINS>(): kotlin.Short
        public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_DISCONNECTED>(): kotlin.Short
        public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_FOLLOWING>(): kotlin.Short
        public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC>(): kotlin.Short
        public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_PRECEDING>(): kotlin.Short
        public final val DOCUMENT_TYPE_NODE: kotlin.Short
            public final fun <get-DOCUMENT_TYPE_NODE>(): kotlin.Short
        public final val ELEMENT_NODE: kotlin.Short
            public final fun <get-ELEMENT_NODE>(): kotlin.Short
        public final val ENTITY_NODE: kotlin.Short
            public final fun <get-ENTITY_NODE>(): kotlin.Short
        public final val ENTITY_REFERENCE_NODE: kotlin.Short
            public final fun <get-ENTITY_REFERENCE_NODE>(): kotlin.Short
        public final val NOTATION_NODE: kotlin.Short
            public final fun <get-NOTATION_NODE>(): kotlin.Short
        public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short
            public final fun <get-PROCESSING_INSTRUCTION_NODE>(): kotlin.Short
        public final val TEXT_NODE: kotlin.Short
            public final fun <get-TEXT_NODE>(): kotlin.Short
    }
}

public external interface ElementContentEditable {
    public abstract var contentEditable: kotlin.String
        public abstract fun <get-contentEditable>(): kotlin.String
        public abstract fun <set-contentEditable>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public abstract val isContentEditable: kotlin.Boolean
        public abstract fun <get-isContentEditable>(): kotlin.Boolean
}

public external interface ElementCreationOptions {
    public open var `is`: kotlin.String?
        public open fun <get-is>(): kotlin.String?
        public open fun <set-is>(/*0*/ value: kotlin.String?): kotlin.Unit
}

public external interface ElementDefinitionOptions {
    public open var extends: kotlin.String?
        public open fun <get-extends>(): kotlin.String?
        public open fun <set-extends>(/*0*/ value: kotlin.String?): kotlin.Unit
}

public open external class ErrorEvent : org.w3c.dom.events.Event {
    /*primary*/ public constructor ErrorEvent(/*0*/ type: kotlin.String, /*1*/ eventInitDict: org.w3c.dom.ErrorEventInit = ...)
    public open val colno: kotlin.Int
        public open fun <get-colno>(): kotlin.Int
    public open val error: kotlin.Any?
        public open fun <get-error>(): kotlin.Any?
    public open val filename: kotlin.String
        public open fun <get-filename>(): kotlin.String
    public open val lineno: kotlin.Int
        public open fun <get-lineno>(): kotlin.Int
    public open val message: kotlin.String
        public open fun <get-message>(): kotlin.String

    public companion object Companion {
        public final val AT_TARGET: kotlin.Short
            public final fun <get-AT_TARGET>(): kotlin.Short
        public final val BUBBLING_PHASE: kotlin.Short
            public final fun <get-BUBBLING_PHASE>(): kotlin.Short
        public final val CAPTURING_PHASE: kotlin.Short
            public final fun <get-CAPTURING_PHASE>(): kotlin.Short
        public final val NONE: kotlin.Short
            public final fun <get-NONE>(): kotlin.Short
    }
}

public external interface ErrorEventInit : org.w3c.dom.EventInit {
    public open var colno: kotlin.Int?
        public open fun <get-colno>(): kotlin.Int?
        public open fun <set-colno>(/*0*/ value: kotlin.Int?): kotlin.Unit
    public open var error: kotlin.Any?
        public open fun <get-error>(): kotlin.Any?
        public open fun <set-error>(/*0*/ value: kotlin.Any?): kotlin.Unit
    public open var filename: kotlin.String?
        public open fun <get-filename>(): kotlin.String?
        public open fun <set-filename>(/*0*/ value: kotlin.String?): kotlin.Unit
    public open var lineno: kotlin.Int?
        public open fun <get-lineno>(): kotlin.Int?
        public open fun <set-lineno>(/*0*/ value: kotlin.Int?): kotlin.Unit
    public open var message: kotlin.String?
        public open fun <get-message>(): kotlin.String?
        public open fun <set-message>(/*0*/ value: kotlin.String?): kotlin.Unit
}

public external interface EventInit {
    public open var bubbles: kotlin.Boolean?
        public open fun <get-bubbles>(): kotlin.Boolean?
        public open fun <set-bubbles>(/*0*/ value: kotlin.Boolean?): kotlin.Unit
    public open var cancelable: kotlin.Boolean?
        public open fun <get-cancelable>(): kotlin.Boolean?
        public open fun <set-cancelable>(/*0*/ value: kotlin.Boolean?): kotlin.Unit
    public open var composed: kotlin.Boolean?
        public open fun <get-composed>(): kotlin.Boolean?
        public open fun <set-composed>(/*0*/ value: kotlin.Boolean?): kotlin.Unit
}

public external interface EventListenerOptions {
    public open var capture: kotlin.Boolean?
        public open fun <get-capture>(): kotlin.Boolean?
        public open fun <set-capture>(/*0*/ value: kotlin.Boolean?): kotlin.Unit
}

public open external class EventSource : org.w3c.dom.events.EventTarget {
    /*primary*/ public constructor EventSource(/*0*/ url: kotlin.String, /*1*/ eventSourceInitDict: org.w3c.dom.EventSourceInit = ...)
    public final var onerror: ((org.w3c.dom.events.Event) -> dynamic)?
        public final fun <get-onerror>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public final fun <set-onerror>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public final var onmessage: ((org.w3c.dom.MessageEvent) -> dynamic)?
        public final fun <get-onmessage>(): ((org.w3c.dom.MessageEvent) -> dynamic)?
        public final fun <set-onmessage>(/*0*/ <set-?>: ((org.w3c.dom.MessageEvent) -> dynamic)?): kotlin.Unit
    public final var onopen: ((org.w3c.dom.events.Event) -> dynamic)?
        public final fun <get-onopen>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public final fun <set-onopen>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open val readyState: kotlin.Short
        public open fun <get-readyState>(): kotlin.Short
    public open val url: kotlin.String
        public open fun <get-url>(): kotlin.String
    public open val withCredentials: kotlin.Boolean
        public open fun <get-withCredentials>(): kotlin.Boolean
    public final fun close(): kotlin.Unit

    public companion object Companion {
        public final val CLOSED: kotlin.Short
            public final fun <get-CLOSED>(): kotlin.Short
        public final val CONNECTING: kotlin.Short
            public final fun <get-CONNECTING>(): kotlin.Short
        public final val OPEN: kotlin.Short
            public final fun <get-OPEN>(): kotlin.Short
    }
}

public external interface EventSourceInit {
    public open var withCredentials: kotlin.Boolean?
        public open fun <get-withCredentials>(): kotlin.Boolean?
        public open fun <set-withCredentials>(/*0*/ value: kotlin.Boolean?): kotlin.Unit
}

public external interface External {
    public abstract fun AddSearchProvider(): kotlin.Unit
    public abstract fun IsSearchProviderInstalled(): kotlin.Unit
}

public external interface GeometryUtils {
    public abstract fun convertPointFromNode(/*0*/ point: org.w3c.dom.DOMPointInit, /*1*/ from: dynamic, /*2*/ options: org.w3c.dom.ConvertCoordinateOptions = ...): org.w3c.dom.DOMPoint
    public abstract fun convertQuadFromNode(/*0*/ quad: dynamic, /*1*/ from: dynamic, /*2*/ options: org.w3c.dom.ConvertCoordinateOptions = ...): org.w3c.dom.DOMQuad
    public abstract fun convertRectFromNode(/*0*/ rect: org.w3c.dom.DOMRectReadOnly, /*1*/ from: dynamic, /*2*/ options: org.w3c.dom.ConvertCoordinateOptions = ...): org.w3c.dom.DOMQuad
    public abstract fun getBoxQuads(/*0*/ options: org.w3c.dom.BoxQuadOptions = ...): kotlin.Array<org.w3c.dom.DOMQuad>
}

public external interface GetRootNodeOptions {
    public open var composed: kotlin.Boolean?
        public open fun <get-composed>(): kotlin.Boolean?
        public open fun <set-composed>(/*0*/ value: kotlin.Boolean?): kotlin.Unit
}

public external interface GlobalEventHandlers {
    public open var onabort: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onabort>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onabort>(/*0*/ value: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onblur: ((org.w3c.dom.events.FocusEvent) -> dynamic)?
        public open fun <get-onblur>(): ((org.w3c.dom.events.FocusEvent) -> dynamic)?
        public open fun <set-onblur>(/*0*/ value: ((org.w3c.dom.events.FocusEvent) -> dynamic)?): kotlin.Unit
    public open var oncancel: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-oncancel>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-oncancel>(/*0*/ value: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var oncanplay: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-oncanplay>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-oncanplay>(/*0*/ value: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var oncanplaythrough: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-oncanplaythrough>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-oncanplaythrough>(/*0*/ value: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onchange: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onchange>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onchange>(/*0*/ value: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onclick: ((org.w3c.dom.events.MouseEvent) -> dynamic)?
        public open fun <get-onclick>(): ((org.w3c.dom.events.MouseEvent) -> dynamic)?
        public open fun <set-onclick>(/*0*/ value: ((org.w3c.dom.events.MouseEvent) -> dynamic)?): kotlin.Unit
    public open var onclose: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onclose>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onclose>(/*0*/ value: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var oncontextmenu: ((org.w3c.dom.events.MouseEvent) -> dynamic)?
        public open fun <get-oncontextmenu>(): ((org.w3c.dom.events.MouseEvent) -> dynamic)?
        public open fun <set-oncontextmenu>(/*0*/ value: ((org.w3c.dom.events.MouseEvent) -> dynamic)?): kotlin.Unit
    public open var oncuechange: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-oncuechange>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-oncuechange>(/*0*/ value: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var ondblclick: ((org.w3c.dom.events.MouseEvent) -> dynamic)?
        public open fun <get-ondblclick>(): ((org.w3c.dom.events.MouseEvent) -> dynamic)?
        public open fun <set-ondblclick>(/*0*/ value: ((org.w3c.dom.events.MouseEvent) -> dynamic)?): kotlin.Unit
    public open var ondrag: ((org.w3c.dom.DragEvent) -> dynamic)?
        public open fun <get-ondrag>(): ((org.w3c.dom.DragEvent) -> dynamic)?
        public open fun <set-ondrag>(/*0*/ value: ((org.w3c.dom.DragEvent) -> dynamic)?): kotlin.Unit
    public open var ondragend: ((org.w3c.dom.DragEvent) -> dynamic)?
        public open fun <get-ondragend>(): ((org.w3c.dom.DragEvent) -> dynamic)?
        public open fun <set-ondragend>(/*0*/ value: ((org.w3c.dom.DragEvent) -> dynamic)?): kotlin.Unit
    public open var ondragenter: ((org.w3c.dom.DragEvent) -> dynamic)?
        public open fun <get-ondragenter>(): ((org.w3c.dom.DragEvent) -> dynamic)?
        public open fun <set-ondragenter>(/*0*/ value: ((org.w3c.dom.DragEvent) -> dynamic)?): kotlin.Unit
    public open var ondragexit: ((org.w3c.dom.DragEvent) -> dynamic)?
        public open fun <get-ondragexit>(): ((org.w3c.dom.DragEvent) -> dynamic)?
        public open fun <set-ondragexit>(/*0*/ value: ((org.w3c.dom.DragEvent) -> dynamic)?): kotlin.Unit
    public open var ondragleave: ((org.w3c.dom.DragEvent) -> dynamic)?
        public open fun <get-ondragleave>(): ((org.w3c.dom.DragEvent) -> dynamic)?
        public open fun <set-ondragleave>(/*0*/ value: ((org.w3c.dom.DragEvent) -> dynamic)?): kotlin.Unit
    public open var ondragover: ((org.w3c.dom.DragEvent) -> dynamic)?
        public open fun <get-ondragover>(): ((org.w3c.dom.DragEvent) -> dynamic)?
        public open fun <set-ondragover>(/*0*/ value: ((org.w3c.dom.DragEvent) -> dynamic)?): kotlin.Unit
    public open var ondragstart: ((org.w3c.dom.DragEvent) -> dynamic)?
        public open fun <get-ondragstart>(): ((org.w3c.dom.DragEvent) -> dynamic)?
        public open fun <set-ondragstart>(/*0*/ value: ((org.w3c.dom.DragEvent) -> dynamic)?): kotlin.Unit
    public open var ondrop: ((org.w3c.dom.DragEvent) -> dynamic)?
        public open fun <get-ondrop>(): ((org.w3c.dom.DragEvent) -> dynamic)?
        public open fun <set-ondrop>(/*0*/ value: ((org.w3c.dom.DragEvent) -> dynamic)?): kotlin.Unit
    public open var ondurationchange: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-ondurationchange>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-ondurationchange>(/*0*/ value: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onemptied: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onemptied>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onemptied>(/*0*/ value: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onended: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onended>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onended>(/*0*/ value: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onerror: ((dynamic, kotlin.String, kotlin.Int, kotlin.Int, kotlin.Any?) -> dynamic)?
        public open fun <get-onerror>(): ((dynamic, kotlin.String, kotlin.Int, kotlin.Int, kotlin.Any?) -> dynamic)?
        public open fun <set-onerror>(/*0*/ value: ((dynamic, kotlin.String, kotlin.Int, kotlin.Int, kotlin.Any?) -> dynamic)?): kotlin.Unit
    public open var onfocus: ((org.w3c.dom.events.FocusEvent) -> dynamic)?
        public open fun <get-onfocus>(): ((org.w3c.dom.events.FocusEvent) -> dynamic)?
        public open fun <set-onfocus>(/*0*/ value: ((org.w3c.dom.events.FocusEvent) -> dynamic)?): kotlin.Unit
    public open var ongotpointercapture: ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?
        public open fun <get-ongotpointercapture>(): ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?
        public open fun <set-ongotpointercapture>(/*0*/ value: ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?): kotlin.Unit
    public open var oninput: ((org.w3c.dom.events.InputEvent) -> dynamic)?
        public open fun <get-oninput>(): ((org.w3c.dom.events.InputEvent) -> dynamic)?
        public open fun <set-oninput>(/*0*/ value: ((org.w3c.dom.events.InputEvent) -> dynamic)?): kotlin.Unit
    public open var oninvalid: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-oninvalid>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-oninvalid>(/*0*/ value: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onkeydown: ((org.w3c.dom.events.KeyboardEvent) -> dynamic)?
        public open fun <get-onkeydown>(): ((org.w3c.dom.events.KeyboardEvent) -> dynamic)?
        public open fun <set-onkeydown>(/*0*/ value: ((org.w3c.dom.events.KeyboardEvent) -> dynamic)?): kotlin.Unit
    public open var onkeypress: ((org.w3c.dom.events.KeyboardEvent) -> dynamic)?
        public open fun <get-onkeypress>(): ((org.w3c.dom.events.KeyboardEvent) -> dynamic)?
        public open fun <set-onkeypress>(/*0*/ value: ((org.w3c.dom.events.KeyboardEvent) -> dynamic)?): kotlin.Unit
    public open var onkeyup: ((org.w3c.dom.events.KeyboardEvent) -> dynamic)?
        public open fun <get-onkeyup>(): ((org.w3c.dom.events.KeyboardEvent) -> dynamic)?
        public open fun <set-onkeyup>(/*0*/ value: ((org.w3c.dom.events.KeyboardEvent) -> dynamic)?): kotlin.Unit
    public open var onload: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onload>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onload>(/*0*/ value: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onloadeddata: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onloadeddata>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onloadeddata>(/*0*/ value: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onloadedmetadata: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onloadedmetadata>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onloadedmetadata>(/*0*/ value: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onloadend: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onloadend>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onloadend>(/*0*/ value: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onloadstart: ((org.w3c.xhr.ProgressEvent) -> dynamic)?
        public open fun <get-onloadstart>(): ((org.w3c.xhr.ProgressEvent) -> dynamic)?
        public open fun <set-onloadstart>(/*0*/ value: ((org.w3c.xhr.ProgressEvent) -> dynamic)?): kotlin.Unit
    public open var onlostpointercapture: ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?
        public open fun <get-onlostpointercapture>(): ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?
        public open fun <set-onlostpointercapture>(/*0*/ value: ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?): kotlin.Unit
    public open var onmousedown: ((org.w3c.dom.events.MouseEvent) -> dynamic)?
        public open fun <get-onmousedown>(): ((org.w3c.dom.events.MouseEvent) -> dynamic)?
        public open fun <set-onmousedown>(/*0*/ value: ((org.w3c.dom.events.MouseEvent) -> dynamic)?): kotlin.Unit
    public open var onmouseenter: ((org.w3c.dom.events.MouseEvent) -> dynamic)?
        public open fun <get-onmouseenter>(): ((org.w3c.dom.events.MouseEvent) -> dynamic)?
        public open fun <set-onmouseenter>(/*0*/ value: ((org.w3c.dom.events.MouseEvent) -> dynamic)?): kotlin.Unit
    public open var onmouseleave: ((org.w3c.dom.events.MouseEvent) -> dynamic)?
        public open fun <get-onmouseleave>(): ((org.w3c.dom.events.MouseEvent) -> dynamic)?
        public open fun <set-onmouseleave>(/*0*/ value: ((org.w3c.dom.events.MouseEvent) -> dynamic)?): kotlin.Unit
    public open var onmousemove: ((org.w3c.dom.events.MouseEvent) -> dynamic)?
        public open fun <get-onmousemove>(): ((org.w3c.dom.events.MouseEvent) -> dynamic)?
        public open fun <set-onmousemove>(/*0*/ value: ((org.w3c.dom.events.MouseEvent) -> dynamic)?): kotlin.Unit
    public open var onmouseout: ((org.w3c.dom.events.MouseEvent) -> dynamic)?
        public open fun <get-onmouseout>(): ((org.w3c.dom.events.MouseEvent) -> dynamic)?
        public open fun <set-onmouseout>(/*0*/ value: ((org.w3c.dom.events.MouseEvent) -> dynamic)?): kotlin.Unit
    public open var onmouseover: ((org.w3c.dom.events.MouseEvent) -> dynamic)?
        public open fun <get-onmouseover>(): ((org.w3c.dom.events.MouseEvent) -> dynamic)?
        public open fun <set-onmouseover>(/*0*/ value: ((org.w3c.dom.events.MouseEvent) -> dynamic)?): kotlin.Unit
    public open var onmouseup: ((org.w3c.dom.events.MouseEvent) -> dynamic)?
        public open fun <get-onmouseup>(): ((org.w3c.dom.events.MouseEvent) -> dynamic)?
        public open fun <set-onmouseup>(/*0*/ value: ((org.w3c.dom.events.MouseEvent) -> dynamic)?): kotlin.Unit
    public open var onpause: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onpause>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onpause>(/*0*/ value: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onplay: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onplay>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onplay>(/*0*/ value: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onplaying: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onplaying>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onplaying>(/*0*/ value: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onpointercancel: ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?
        public open fun <get-onpointercancel>(): ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?
        public open fun <set-onpointercancel>(/*0*/ value: ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?): kotlin.Unit
    public open var onpointerdown: ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?
        public open fun <get-onpointerdown>(): ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?
        public open fun <set-onpointerdown>(/*0*/ value: ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?): kotlin.Unit
    public open var onpointerenter: ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?
        public open fun <get-onpointerenter>(): ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?
        public open fun <set-onpointerenter>(/*0*/ value: ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?): kotlin.Unit
    public open var onpointerleave: ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?
        public open fun <get-onpointerleave>(): ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?
        public open fun <set-onpointerleave>(/*0*/ value: ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?): kotlin.Unit
    public open var onpointermove: ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?
        public open fun <get-onpointermove>(): ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?
        public open fun <set-onpointermove>(/*0*/ value: ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?): kotlin.Unit
    public open var onpointerout: ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?
        public open fun <get-onpointerout>(): ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?
        public open fun <set-onpointerout>(/*0*/ value: ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?): kotlin.Unit
    public open var onpointerover: ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?
        public open fun <get-onpointerover>(): ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?
        public open fun <set-onpointerover>(/*0*/ value: ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?): kotlin.Unit
    public open var onpointerup: ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?
        public open fun <get-onpointerup>(): ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?
        public open fun <set-onpointerup>(/*0*/ value: ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?): kotlin.Unit
    public open var onprogress: ((org.w3c.xhr.ProgressEvent) -> dynamic)?
        public open fun <get-onprogress>(): ((org.w3c.xhr.ProgressEvent) -> dynamic)?
        public open fun <set-onprogress>(/*0*/ value: ((org.w3c.xhr.ProgressEvent) -> dynamic)?): kotlin.Unit
    public open var onratechange: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onratechange>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onratechange>(/*0*/ value: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onreset: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onreset>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onreset>(/*0*/ value: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onresize: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onresize>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onresize>(/*0*/ value: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onscroll: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onscroll>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onscroll>(/*0*/ value: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onseeked: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onseeked>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onseeked>(/*0*/ value: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onseeking: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onseeking>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onseeking>(/*0*/ value: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onselect: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onselect>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onselect>(/*0*/ value: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onshow: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onshow>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onshow>(/*0*/ value: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onstalled: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onstalled>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onstalled>(/*0*/ value: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onsubmit: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onsubmit>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onsubmit>(/*0*/ value: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onsuspend: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onsuspend>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onsuspend>(/*0*/ value: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var ontimeupdate: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-ontimeupdate>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-ontimeupdate>(/*0*/ value: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var ontoggle: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-ontoggle>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-ontoggle>(/*0*/ value: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onvolumechange: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onvolumechange>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onvolumechange>(/*0*/ value: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onwaiting: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onwaiting>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onwaiting>(/*0*/ value: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onwheel: ((org.w3c.dom.events.WheelEvent) -> dynamic)?
        public open fun <get-onwheel>(): ((org.w3c.dom.events.WheelEvent) -> dynamic)?
        public open fun <set-onwheel>(/*0*/ value: ((org.w3c.dom.events.WheelEvent) -> dynamic)?): kotlin.Unit
}

public abstract external class HTMLAllCollection {
    /*primary*/ public constructor HTMLAllCollection()
    public open val length: kotlin.Int
        public open fun <get-length>(): kotlin.Int
    public final fun item(/*0*/ nameOrIndex: kotlin.String = ...): org.w3c.dom.UnionElementOrHTMLCollection?
    public final fun namedItem(/*0*/ name: kotlin.String): org.w3c.dom.UnionElementOrHTMLCollection?
}

public abstract external class HTMLAnchorElement : org.w3c.dom.HTMLElement, org.w3c.dom.HTMLHyperlinkElementUtils {
    /*primary*/ public constructor HTMLAnchorElement()
    public open var charset: kotlin.String
        public open fun <get-charset>(): kotlin.String
        public open fun <set-charset>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var coords: kotlin.String
        public open fun <get-coords>(): kotlin.String
        public open fun <set-coords>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var download: kotlin.String
        public open fun <get-download>(): kotlin.String
        public open fun <set-download>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var hreflang: kotlin.String
        public open fun <get-hreflang>(): kotlin.String
        public open fun <set-hreflang>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var name: kotlin.String
        public open fun <get-name>(): kotlin.String
        public open fun <set-name>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var ping: kotlin.String
        public open fun <get-ping>(): kotlin.String
        public open fun <set-ping>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var referrerPolicy: kotlin.String
        public open fun <get-referrerPolicy>(): kotlin.String
        public open fun <set-referrerPolicy>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var rel: kotlin.String
        public open fun <get-rel>(): kotlin.String
        public open fun <set-rel>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open val relList: org.w3c.dom.DOMTokenList
        public open fun <get-relList>(): org.w3c.dom.DOMTokenList
    public open var rev: kotlin.String
        public open fun <get-rev>(): kotlin.String
        public open fun <set-rev>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var shape: kotlin.String
        public open fun <get-shape>(): kotlin.String
        public open fun <set-shape>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var target: kotlin.String
        public open fun <get-target>(): kotlin.String
        public open fun <set-target>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var text: kotlin.String
        public open fun <get-text>(): kotlin.String
        public open fun <set-text>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var type: kotlin.String
        public open fun <get-type>(): kotlin.String
        public open fun <set-type>(/*0*/ <set-?>: kotlin.String): kotlin.Unit

    public companion object Companion {
        public final val ATTRIBUTE_NODE: kotlin.Short
            public final fun <get-ATTRIBUTE_NODE>(): kotlin.Short
        public final val CDATA_SECTION_NODE: kotlin.Short
            public final fun <get-CDATA_SECTION_NODE>(): kotlin.Short
        public final val COMMENT_NODE: kotlin.Short
            public final fun <get-COMMENT_NODE>(): kotlin.Short
        public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short
            public final fun <get-DOCUMENT_FRAGMENT_NODE>(): kotlin.Short
        public final val DOCUMENT_NODE: kotlin.Short
            public final fun <get-DOCUMENT_NODE>(): kotlin.Short
        public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_CONTAINED_BY>(): kotlin.Short
        public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_CONTAINS>(): kotlin.Short
        public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_DISCONNECTED>(): kotlin.Short
        public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_FOLLOWING>(): kotlin.Short
        public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC>(): kotlin.Short
        public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_PRECEDING>(): kotlin.Short
        public final val DOCUMENT_TYPE_NODE: kotlin.Short
            public final fun <get-DOCUMENT_TYPE_NODE>(): kotlin.Short
        public final val ELEMENT_NODE: kotlin.Short
            public final fun <get-ELEMENT_NODE>(): kotlin.Short
        public final val ENTITY_NODE: kotlin.Short
            public final fun <get-ENTITY_NODE>(): kotlin.Short
        public final val ENTITY_REFERENCE_NODE: kotlin.Short
            public final fun <get-ENTITY_REFERENCE_NODE>(): kotlin.Short
        public final val NOTATION_NODE: kotlin.Short
            public final fun <get-NOTATION_NODE>(): kotlin.Short
        public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short
            public final fun <get-PROCESSING_INSTRUCTION_NODE>(): kotlin.Short
        public final val TEXT_NODE: kotlin.Short
            public final fun <get-TEXT_NODE>(): kotlin.Short
    }
}

public abstract external class HTMLAppletElement : org.w3c.dom.HTMLElement {
    /*primary*/ public constructor HTMLAppletElement()
    public open var _object: kotlin.String
        public open fun <get-_object>(): kotlin.String
        public open fun <set-_object>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var align: kotlin.String
        public open fun <get-align>(): kotlin.String
        public open fun <set-align>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var alt: kotlin.String
        public open fun <get-alt>(): kotlin.String
        public open fun <set-alt>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var archive: kotlin.String
        public open fun <get-archive>(): kotlin.String
        public open fun <set-archive>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var code: kotlin.String
        public open fun <get-code>(): kotlin.String
        public open fun <set-code>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var codeBase: kotlin.String
        public open fun <get-codeBase>(): kotlin.String
        public open fun <set-codeBase>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var height: kotlin.String
        public open fun <get-height>(): kotlin.String
        public open fun <set-height>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var hspace: kotlin.Int
        public open fun <get-hspace>(): kotlin.Int
        public open fun <set-hspace>(/*0*/ <set-?>: kotlin.Int): kotlin.Unit
    public open var name: kotlin.String
        public open fun <get-name>(): kotlin.String
        public open fun <set-name>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var vspace: kotlin.Int
        public open fun <get-vspace>(): kotlin.Int
        public open fun <set-vspace>(/*0*/ <set-?>: kotlin.Int): kotlin.Unit
    public open var width: kotlin.String
        public open fun <get-width>(): kotlin.String
        public open fun <set-width>(/*0*/ <set-?>: kotlin.String): kotlin.Unit

    public companion object Companion {
        public final val ATTRIBUTE_NODE: kotlin.Short
            public final fun <get-ATTRIBUTE_NODE>(): kotlin.Short
        public final val CDATA_SECTION_NODE: kotlin.Short
            public final fun <get-CDATA_SECTION_NODE>(): kotlin.Short
        public final val COMMENT_NODE: kotlin.Short
            public final fun <get-COMMENT_NODE>(): kotlin.Short
        public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short
            public final fun <get-DOCUMENT_FRAGMENT_NODE>(): kotlin.Short
        public final val DOCUMENT_NODE: kotlin.Short
            public final fun <get-DOCUMENT_NODE>(): kotlin.Short
        public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_CONTAINED_BY>(): kotlin.Short
        public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_CONTAINS>(): kotlin.Short
        public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_DISCONNECTED>(): kotlin.Short
        public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_FOLLOWING>(): kotlin.Short
        public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC>(): kotlin.Short
        public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_PRECEDING>(): kotlin.Short
        public final val DOCUMENT_TYPE_NODE: kotlin.Short
            public final fun <get-DOCUMENT_TYPE_NODE>(): kotlin.Short
        public final val ELEMENT_NODE: kotlin.Short
            public final fun <get-ELEMENT_NODE>(): kotlin.Short
        public final val ENTITY_NODE: kotlin.Short
            public final fun <get-ENTITY_NODE>(): kotlin.Short
        public final val ENTITY_REFERENCE_NODE: kotlin.Short
            public final fun <get-ENTITY_REFERENCE_NODE>(): kotlin.Short
        public final val NOTATION_NODE: kotlin.Short
            public final fun <get-NOTATION_NODE>(): kotlin.Short
        public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short
            public final fun <get-PROCESSING_INSTRUCTION_NODE>(): kotlin.Short
        public final val TEXT_NODE: kotlin.Short
            public final fun <get-TEXT_NODE>(): kotlin.Short
    }
}

public abstract external class HTMLAreaElement : org.w3c.dom.HTMLElement, org.w3c.dom.HTMLHyperlinkElementUtils {
    /*primary*/ public constructor HTMLAreaElement()
    public open var alt: kotlin.String
        public open fun <get-alt>(): kotlin.String
        public open fun <set-alt>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var coords: kotlin.String
        public open fun <get-coords>(): kotlin.String
        public open fun <set-coords>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var download: kotlin.String
        public open fun <get-download>(): kotlin.String
        public open fun <set-download>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var noHref: kotlin.Boolean
        public open fun <get-noHref>(): kotlin.Boolean
        public open fun <set-noHref>(/*0*/ <set-?>: kotlin.Boolean): kotlin.Unit
    public open var ping: kotlin.String
        public open fun <get-ping>(): kotlin.String
        public open fun <set-ping>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var referrerPolicy: kotlin.String
        public open fun <get-referrerPolicy>(): kotlin.String
        public open fun <set-referrerPolicy>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var rel: kotlin.String
        public open fun <get-rel>(): kotlin.String
        public open fun <set-rel>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open val relList: org.w3c.dom.DOMTokenList
        public open fun <get-relList>(): org.w3c.dom.DOMTokenList
    public open var shape: kotlin.String
        public open fun <get-shape>(): kotlin.String
        public open fun <set-shape>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var target: kotlin.String
        public open fun <get-target>(): kotlin.String
        public open fun <set-target>(/*0*/ <set-?>: kotlin.String): kotlin.Unit

    public companion object Companion {
        public final val ATTRIBUTE_NODE: kotlin.Short
            public final fun <get-ATTRIBUTE_NODE>(): kotlin.Short
        public final val CDATA_SECTION_NODE: kotlin.Short
            public final fun <get-CDATA_SECTION_NODE>(): kotlin.Short
        public final val COMMENT_NODE: kotlin.Short
            public final fun <get-COMMENT_NODE>(): kotlin.Short
        public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short
            public final fun <get-DOCUMENT_FRAGMENT_NODE>(): kotlin.Short
        public final val DOCUMENT_NODE: kotlin.Short
            public final fun <get-DOCUMENT_NODE>(): kotlin.Short
        public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_CONTAINED_BY>(): kotlin.Short
        public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_CONTAINS>(): kotlin.Short
        public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_DISCONNECTED>(): kotlin.Short
        public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_FOLLOWING>(): kotlin.Short
        public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC>(): kotlin.Short
        public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_PRECEDING>(): kotlin.Short
        public final val DOCUMENT_TYPE_NODE: kotlin.Short
            public final fun <get-DOCUMENT_TYPE_NODE>(): kotlin.Short
        public final val ELEMENT_NODE: kotlin.Short
            public final fun <get-ELEMENT_NODE>(): kotlin.Short
        public final val ENTITY_NODE: kotlin.Short
            public final fun <get-ENTITY_NODE>(): kotlin.Short
        public final val ENTITY_REFERENCE_NODE: kotlin.Short
            public final fun <get-ENTITY_REFERENCE_NODE>(): kotlin.Short
        public final val NOTATION_NODE: kotlin.Short
            public final fun <get-NOTATION_NODE>(): kotlin.Short
        public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short
            public final fun <get-PROCESSING_INSTRUCTION_NODE>(): kotlin.Short
        public final val TEXT_NODE: kotlin.Short
            public final fun <get-TEXT_NODE>(): kotlin.Short
    }
}

public abstract external class HTMLAudioElement : org.w3c.dom.HTMLMediaElement {
    /*primary*/ public constructor HTMLAudioElement()

    public companion object Companion {
        public final val ATTRIBUTE_NODE: kotlin.Short
            public final fun <get-ATTRIBUTE_NODE>(): kotlin.Short
        public final val CDATA_SECTION_NODE: kotlin.Short
            public final fun <get-CDATA_SECTION_NODE>(): kotlin.Short
        public final val COMMENT_NODE: kotlin.Short
            public final fun <get-COMMENT_NODE>(): kotlin.Short
        public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short
            public final fun <get-DOCUMENT_FRAGMENT_NODE>(): kotlin.Short
        public final val DOCUMENT_NODE: kotlin.Short
            public final fun <get-DOCUMENT_NODE>(): kotlin.Short
        public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_CONTAINED_BY>(): kotlin.Short
        public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_CONTAINS>(): kotlin.Short
        public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_DISCONNECTED>(): kotlin.Short
        public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_FOLLOWING>(): kotlin.Short
        public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC>(): kotlin.Short
        public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_PRECEDING>(): kotlin.Short
        public final val DOCUMENT_TYPE_NODE: kotlin.Short
            public final fun <get-DOCUMENT_TYPE_NODE>(): kotlin.Short
        public final val ELEMENT_NODE: kotlin.Short
            public final fun <get-ELEMENT_NODE>(): kotlin.Short
        public final val ENTITY_NODE: kotlin.Short
            public final fun <get-ENTITY_NODE>(): kotlin.Short
        public final val ENTITY_REFERENCE_NODE: kotlin.Short
            public final fun <get-ENTITY_REFERENCE_NODE>(): kotlin.Short
        public final val HAVE_CURRENT_DATA: kotlin.Short
            public final fun <get-HAVE_CURRENT_DATA>(): kotlin.Short
        public final val HAVE_ENOUGH_DATA: kotlin.Short
            public final fun <get-HAVE_ENOUGH_DATA>(): kotlin.Short
        public final val HAVE_FUTURE_DATA: kotlin.Short
            public final fun <get-HAVE_FUTURE_DATA>(): kotlin.Short
        public final val HAVE_METADATA: kotlin.Short
            public final fun <get-HAVE_METADATA>(): kotlin.Short
        public final val HAVE_NOTHING: kotlin.Short
            public final fun <get-HAVE_NOTHING>(): kotlin.Short
        public final val NETWORK_EMPTY: kotlin.Short
            public final fun <get-NETWORK_EMPTY>(): kotlin.Short
        public final val NETWORK_IDLE: kotlin.Short
            public final fun <get-NETWORK_IDLE>(): kotlin.Short
        public final val NETWORK_LOADING: kotlin.Short
            public final fun <get-NETWORK_LOADING>(): kotlin.Short
        public final val NETWORK_NO_SOURCE: kotlin.Short
            public final fun <get-NETWORK_NO_SOURCE>(): kotlin.Short
        public final val NOTATION_NODE: kotlin.Short
            public final fun <get-NOTATION_NODE>(): kotlin.Short
        public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short
            public final fun <get-PROCESSING_INSTRUCTION_NODE>(): kotlin.Short
        public final val TEXT_NODE: kotlin.Short
            public final fun <get-TEXT_NODE>(): kotlin.Short
    }
}

public abstract external class HTMLBRElement : org.w3c.dom.HTMLElement {
    /*primary*/ public constructor HTMLBRElement()
    public open var clear: kotlin.String
        public open fun <get-clear>(): kotlin.String
        public open fun <set-clear>(/*0*/ <set-?>: kotlin.String): kotlin.Unit

    public companion object Companion {
        public final val ATTRIBUTE_NODE: kotlin.Short
            public final fun <get-ATTRIBUTE_NODE>(): kotlin.Short
        public final val CDATA_SECTION_NODE: kotlin.Short
            public final fun <get-CDATA_SECTION_NODE>(): kotlin.Short
        public final val COMMENT_NODE: kotlin.Short
            public final fun <get-COMMENT_NODE>(): kotlin.Short
        public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short
            public final fun <get-DOCUMENT_FRAGMENT_NODE>(): kotlin.Short
        public final val DOCUMENT_NODE: kotlin.Short
            public final fun <get-DOCUMENT_NODE>(): kotlin.Short
        public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_CONTAINED_BY>(): kotlin.Short
        public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_CONTAINS>(): kotlin.Short
        public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_DISCONNECTED>(): kotlin.Short
        public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_FOLLOWING>(): kotlin.Short
        public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC>(): kotlin.Short
        public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_PRECEDING>(): kotlin.Short
        public final val DOCUMENT_TYPE_NODE: kotlin.Short
            public final fun <get-DOCUMENT_TYPE_NODE>(): kotlin.Short
        public final val ELEMENT_NODE: kotlin.Short
            public final fun <get-ELEMENT_NODE>(): kotlin.Short
        public final val ENTITY_NODE: kotlin.Short
            public final fun <get-ENTITY_NODE>(): kotlin.Short
        public final val ENTITY_REFERENCE_NODE: kotlin.Short
            public final fun <get-ENTITY_REFERENCE_NODE>(): kotlin.Short
        public final val NOTATION_NODE: kotlin.Short
            public final fun <get-NOTATION_NODE>(): kotlin.Short
        public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short
            public final fun <get-PROCESSING_INSTRUCTION_NODE>(): kotlin.Short
        public final val TEXT_NODE: kotlin.Short
            public final fun <get-TEXT_NODE>(): kotlin.Short
    }
}

public abstract external class HTMLBaseElement : org.w3c.dom.HTMLElement {
    /*primary*/ public constructor HTMLBaseElement()
    public open var href: kotlin.String
        public open fun <get-href>(): kotlin.String
        public open fun <set-href>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var target: kotlin.String
        public open fun <get-target>(): kotlin.String
        public open fun <set-target>(/*0*/ <set-?>: kotlin.String): kotlin.Unit

    public companion object Companion {
        public final val ATTRIBUTE_NODE: kotlin.Short
            public final fun <get-ATTRIBUTE_NODE>(): kotlin.Short
        public final val CDATA_SECTION_NODE: kotlin.Short
            public final fun <get-CDATA_SECTION_NODE>(): kotlin.Short
        public final val COMMENT_NODE: kotlin.Short
            public final fun <get-COMMENT_NODE>(): kotlin.Short
        public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short
            public final fun <get-DOCUMENT_FRAGMENT_NODE>(): kotlin.Short
        public final val DOCUMENT_NODE: kotlin.Short
            public final fun <get-DOCUMENT_NODE>(): kotlin.Short
        public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_CONTAINED_BY>(): kotlin.Short
        public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_CONTAINS>(): kotlin.Short
        public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_DISCONNECTED>(): kotlin.Short
        public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_FOLLOWING>(): kotlin.Short
        public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC>(): kotlin.Short
        public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_PRECEDING>(): kotlin.Short
        public final val DOCUMENT_TYPE_NODE: kotlin.Short
            public final fun <get-DOCUMENT_TYPE_NODE>(): kotlin.Short
        public final val ELEMENT_NODE: kotlin.Short
            public final fun <get-ELEMENT_NODE>(): kotlin.Short
        public final val ENTITY_NODE: kotlin.Short
            public final fun <get-ENTITY_NODE>(): kotlin.Short
        public final val ENTITY_REFERENCE_NODE: kotlin.Short
            public final fun <get-ENTITY_REFERENCE_NODE>(): kotlin.Short
        public final val NOTATION_NODE: kotlin.Short
            public final fun <get-NOTATION_NODE>(): kotlin.Short
        public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short
            public final fun <get-PROCESSING_INSTRUCTION_NODE>(): kotlin.Short
        public final val TEXT_NODE: kotlin.Short
            public final fun <get-TEXT_NODE>(): kotlin.Short
    }
}
