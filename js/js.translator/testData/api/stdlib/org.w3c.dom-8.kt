public external interface NavigatorCookies {
    public abstract val cookieEnabled: kotlin.Boolean
        public abstract fun <get-cookieEnabled>(): kotlin.Boolean
}

public external interface NavigatorID {
    public abstract val appCodeName: kotlin.String
        public abstract fun <get-appCodeName>(): kotlin.String
    public abstract val appName: kotlin.String
        public abstract fun <get-appName>(): kotlin.String
    public abstract val appVersion: kotlin.String
        public abstract fun <get-appVersion>(): kotlin.String
    public abstract val oscpu: kotlin.String
        public abstract fun <get-oscpu>(): kotlin.String
    public abstract val platform: kotlin.String
        public abstract fun <get-platform>(): kotlin.String
    public abstract val product: kotlin.String
        public abstract fun <get-product>(): kotlin.String
    public abstract val productSub: kotlin.String
        public abstract fun <get-productSub>(): kotlin.String
    public abstract val userAgent: kotlin.String
        public abstract fun <get-userAgent>(): kotlin.String
    public abstract val vendor: kotlin.String
        public abstract fun <get-vendor>(): kotlin.String
    public abstract val vendorSub: kotlin.String
        public abstract fun <get-vendorSub>(): kotlin.String
    public abstract fun taintEnabled(): kotlin.Boolean
}

public external interface NavigatorLanguage {
    public abstract val language: kotlin.String
        public abstract fun <get-language>(): kotlin.String
    public abstract val languages: kotlin.Array<out kotlin.String>
        public abstract fun <get-languages>(): kotlin.Array<out kotlin.String>
}

public external interface NavigatorOnLine {
    public abstract val onLine: kotlin.Boolean
        public abstract fun <get-onLine>(): kotlin.Boolean
}

public external interface NavigatorPlugins {
    public abstract val mimeTypes: org.w3c.dom.MimeTypeArray
        public abstract fun <get-mimeTypes>(): org.w3c.dom.MimeTypeArray
    public abstract val plugins: org.w3c.dom.PluginArray
        public abstract fun <get-plugins>(): org.w3c.dom.PluginArray
    public abstract fun javaEnabled(): kotlin.Boolean
}

public abstract external class Node : org.w3c.dom.events.EventTarget {
    /*primary*/ public constructor Node()
    public open val baseURI: kotlin.String
        public open fun <get-baseURI>(): kotlin.String
    public open val childNodes: org.w3c.dom.NodeList
        public open fun <get-childNodes>(): org.w3c.dom.NodeList
    public open val firstChild: org.w3c.dom.Node?
        public open fun <get-firstChild>(): org.w3c.dom.Node?
    public open val isConnected: kotlin.Boolean
        public open fun <get-isConnected>(): kotlin.Boolean
    public open val lastChild: org.w3c.dom.Node?
        public open fun <get-lastChild>(): org.w3c.dom.Node?
    public open val nextSibling: org.w3c.dom.Node?
        public open fun <get-nextSibling>(): org.w3c.dom.Node?
    public open val nodeName: kotlin.String
        public open fun <get-nodeName>(): kotlin.String
    public open val nodeType: kotlin.Short
        public open fun <get-nodeType>(): kotlin.Short
    public open var nodeValue: kotlin.String?
        public open fun <get-nodeValue>(): kotlin.String?
        public open fun <set-nodeValue>(/*0*/ <set-?>: kotlin.String?): kotlin.Unit
    public open val ownerDocument: org.w3c.dom.Document?
        public open fun <get-ownerDocument>(): org.w3c.dom.Document?
    public open val parentElement: org.w3c.dom.Element?
        public open fun <get-parentElement>(): org.w3c.dom.Element?
    public open val parentNode: org.w3c.dom.Node?
        public open fun <get-parentNode>(): org.w3c.dom.Node?
    public open val previousSibling: org.w3c.dom.Node?
        public open fun <get-previousSibling>(): org.w3c.dom.Node?
    public open var textContent: kotlin.String?
        public open fun <get-textContent>(): kotlin.String?
        public open fun <set-textContent>(/*0*/ <set-?>: kotlin.String?): kotlin.Unit
    public final fun appendChild(/*0*/ node: org.w3c.dom.Node): org.w3c.dom.Node
    public final fun cloneNode(/*0*/ deep: kotlin.Boolean = ...): org.w3c.dom.Node
    public final fun compareDocumentPosition(/*0*/ other: org.w3c.dom.Node): kotlin.Short
    public final fun contains(/*0*/ other: org.w3c.dom.Node?): kotlin.Boolean
    public final fun getRootNode(/*0*/ options: org.w3c.dom.GetRootNodeOptions = ...): org.w3c.dom.Node
    public final fun hasChildNodes(): kotlin.Boolean
    public final fun insertBefore(/*0*/ node: org.w3c.dom.Node, /*1*/ child: org.w3c.dom.Node?): org.w3c.dom.Node
    public final fun isDefaultNamespace(/*0*/ namespace: kotlin.String?): kotlin.Boolean
    public final fun isEqualNode(/*0*/ otherNode: org.w3c.dom.Node?): kotlin.Boolean
    public final fun isSameNode(/*0*/ otherNode: org.w3c.dom.Node?): kotlin.Boolean
    public final fun lookupNamespaceURI(/*0*/ prefix: kotlin.String?): kotlin.String?
    public final fun lookupPrefix(/*0*/ namespace: kotlin.String?): kotlin.String?
    public final fun normalize(): kotlin.Unit
    public final fun removeChild(/*0*/ child: org.w3c.dom.Node): org.w3c.dom.Node
    public final fun replaceChild(/*0*/ node: org.w3c.dom.Node, /*1*/ child: org.w3c.dom.Node): org.w3c.dom.Node

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

public external interface NodeFilter {
    public abstract fun acceptNode(/*0*/ node: org.w3c.dom.Node): kotlin.Short

    public companion object Companion {
        public final val FILTER_ACCEPT: kotlin.Short
            public final fun <get-FILTER_ACCEPT>(): kotlin.Short
        public final val FILTER_REJECT: kotlin.Short
            public final fun <get-FILTER_REJECT>(): kotlin.Short
        public final val FILTER_SKIP: kotlin.Short
            public final fun <get-FILTER_SKIP>(): kotlin.Short
        public final val SHOW_ALL: kotlin.Int
            public final fun <get-SHOW_ALL>(): kotlin.Int
        public final val SHOW_ATTRIBUTE: kotlin.Int
            public final fun <get-SHOW_ATTRIBUTE>(): kotlin.Int
        public final val SHOW_CDATA_SECTION: kotlin.Int
            public final fun <get-SHOW_CDATA_SECTION>(): kotlin.Int
        public final val SHOW_COMMENT: kotlin.Int
            public final fun <get-SHOW_COMMENT>(): kotlin.Int
        public final val SHOW_DOCUMENT: kotlin.Int
            public final fun <get-SHOW_DOCUMENT>(): kotlin.Int
        public final val SHOW_DOCUMENT_FRAGMENT: kotlin.Int
            public final fun <get-SHOW_DOCUMENT_FRAGMENT>(): kotlin.Int
        public final val SHOW_DOCUMENT_TYPE: kotlin.Int
            public final fun <get-SHOW_DOCUMENT_TYPE>(): kotlin.Int
        public final val SHOW_ELEMENT: kotlin.Int
            public final fun <get-SHOW_ELEMENT>(): kotlin.Int
        public final val SHOW_ENTITY: kotlin.Int
            public final fun <get-SHOW_ENTITY>(): kotlin.Int
        public final val SHOW_ENTITY_REFERENCE: kotlin.Int
            public final fun <get-SHOW_ENTITY_REFERENCE>(): kotlin.Int
        public final val SHOW_NOTATION: kotlin.Int
            public final fun <get-SHOW_NOTATION>(): kotlin.Int
        public final val SHOW_PROCESSING_INSTRUCTION: kotlin.Int
            public final fun <get-SHOW_PROCESSING_INSTRUCTION>(): kotlin.Int
        public final val SHOW_TEXT: kotlin.Int
            public final fun <get-SHOW_TEXT>(): kotlin.Int
    }
}

public abstract external class NodeIterator {
    /*primary*/ public constructor NodeIterator()
    public open val filter: org.w3c.dom.NodeFilter?
        public open fun <get-filter>(): org.w3c.dom.NodeFilter?
    public open val pointerBeforeReferenceNode: kotlin.Boolean
        public open fun <get-pointerBeforeReferenceNode>(): kotlin.Boolean
    public open val referenceNode: org.w3c.dom.Node
        public open fun <get-referenceNode>(): org.w3c.dom.Node
    public open val root: org.w3c.dom.Node
        public open fun <get-root>(): org.w3c.dom.Node
    public open val whatToShow: kotlin.Int
        public open fun <get-whatToShow>(): kotlin.Int
    public final fun detach(): kotlin.Unit
    public final fun nextNode(): org.w3c.dom.Node?
    public final fun previousNode(): org.w3c.dom.Node?
}

public abstract external class NodeList : org.w3c.dom.ItemArrayLike<org.w3c.dom.Node> {
    /*primary*/ public constructor NodeList()
    public open override /*1*/ fun item(/*0*/ index: kotlin.Int): org.w3c.dom.Node?
}

public external interface NonDocumentTypeChildNode {
    public open val nextElementSibling: org.w3c.dom.Element?
        public open fun <get-nextElementSibling>(): org.w3c.dom.Element?
    public open val previousElementSibling: org.w3c.dom.Element?
        public open fun <get-previousElementSibling>(): org.w3c.dom.Element?
}

public external interface NonElementParentNode {
    public abstract fun getElementById(/*0*/ elementId: kotlin.String): org.w3c.dom.Element?
}

public open external class Option : org.w3c.dom.HTMLOptionElement {
    /*primary*/ public constructor Option(/*0*/ text: kotlin.String = ..., /*1*/ value: kotlin.String = ..., /*2*/ defaultSelected: kotlin.Boolean = ..., /*3*/ selected: kotlin.Boolean = ...)
    public open override /*1*/ val assignedSlot: org.w3c.dom.HTMLSlotElement?
        public open override /*1*/ fun <get-assignedSlot>(): org.w3c.dom.HTMLSlotElement?
    public open override /*1*/ val childElementCount: kotlin.Int
        public open override /*1*/ fun <get-childElementCount>(): kotlin.Int
    public open override /*1*/ val children: org.w3c.dom.HTMLCollection
        public open override /*1*/ fun <get-children>(): org.w3c.dom.HTMLCollection
    public open override /*1*/ var contentEditable: kotlin.String
        public open override /*1*/ fun <get-contentEditable>(): kotlin.String
        public open override /*1*/ fun <set-contentEditable>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open override /*1*/ val firstElementChild: org.w3c.dom.Element?
        public open override /*1*/ fun <get-firstElementChild>(): org.w3c.dom.Element?
    public open override /*1*/ val isContentEditable: kotlin.Boolean
        public open override /*1*/ fun <get-isContentEditable>(): kotlin.Boolean
    public open override /*1*/ val lastElementChild: org.w3c.dom.Element?
        public open override /*1*/ fun <get-lastElementChild>(): org.w3c.dom.Element?
    public open override /*1*/ val nextElementSibling: org.w3c.dom.Element?
        public open override /*1*/ fun <get-nextElementSibling>(): org.w3c.dom.Element?
    public open override /*1*/ var onabort: ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <get-onabort>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <set-onabort>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onblur: ((org.w3c.dom.events.FocusEvent) -> dynamic)?
        public open override /*1*/ fun <get-onblur>(): ((org.w3c.dom.events.FocusEvent) -> dynamic)?
        public open override /*1*/ fun <set-onblur>(/*0*/ <set-?>: ((org.w3c.dom.events.FocusEvent) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var oncancel: ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <get-oncancel>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <set-oncancel>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var oncanplay: ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <get-oncanplay>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <set-oncanplay>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var oncanplaythrough: ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <get-oncanplaythrough>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <set-oncanplaythrough>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onchange: ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <get-onchange>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <set-onchange>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onclick: ((org.w3c.dom.events.MouseEvent) -> dynamic)?
        public open override /*1*/ fun <get-onclick>(): ((org.w3c.dom.events.MouseEvent) -> dynamic)?
        public open override /*1*/ fun <set-onclick>(/*0*/ <set-?>: ((org.w3c.dom.events.MouseEvent) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onclose: ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <get-onclose>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <set-onclose>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var oncontextmenu: ((org.w3c.dom.events.MouseEvent) -> dynamic)?
        public open override /*1*/ fun <get-oncontextmenu>(): ((org.w3c.dom.events.MouseEvent) -> dynamic)?
        public open override /*1*/ fun <set-oncontextmenu>(/*0*/ <set-?>: ((org.w3c.dom.events.MouseEvent) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var oncopy: ((org.w3c.dom.clipboard.ClipboardEvent) -> dynamic)?
        public open override /*1*/ fun <get-oncopy>(): ((org.w3c.dom.clipboard.ClipboardEvent) -> dynamic)?
        public open override /*1*/ fun <set-oncopy>(/*0*/ <set-?>: ((org.w3c.dom.clipboard.ClipboardEvent) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var oncuechange: ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <get-oncuechange>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <set-oncuechange>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var oncut: ((org.w3c.dom.clipboard.ClipboardEvent) -> dynamic)?
        public open override /*1*/ fun <get-oncut>(): ((org.w3c.dom.clipboard.ClipboardEvent) -> dynamic)?
        public open override /*1*/ fun <set-oncut>(/*0*/ <set-?>: ((org.w3c.dom.clipboard.ClipboardEvent) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var ondblclick: ((org.w3c.dom.events.MouseEvent) -> dynamic)?
        public open override /*1*/ fun <get-ondblclick>(): ((org.w3c.dom.events.MouseEvent) -> dynamic)?
        public open override /*1*/ fun <set-ondblclick>(/*0*/ <set-?>: ((org.w3c.dom.events.MouseEvent) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var ondrag: ((org.w3c.dom.DragEvent) -> dynamic)?
        public open override /*1*/ fun <get-ondrag>(): ((org.w3c.dom.DragEvent) -> dynamic)?
        public open override /*1*/ fun <set-ondrag>(/*0*/ <set-?>: ((org.w3c.dom.DragEvent) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var ondragend: ((org.w3c.dom.DragEvent) -> dynamic)?
        public open override /*1*/ fun <get-ondragend>(): ((org.w3c.dom.DragEvent) -> dynamic)?
        public open override /*1*/ fun <set-ondragend>(/*0*/ <set-?>: ((org.w3c.dom.DragEvent) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var ondragenter: ((org.w3c.dom.DragEvent) -> dynamic)?
        public open override /*1*/ fun <get-ondragenter>(): ((org.w3c.dom.DragEvent) -> dynamic)?
        public open override /*1*/ fun <set-ondragenter>(/*0*/ <set-?>: ((org.w3c.dom.DragEvent) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var ondragexit: ((org.w3c.dom.DragEvent) -> dynamic)?
        public open override /*1*/ fun <get-ondragexit>(): ((org.w3c.dom.DragEvent) -> dynamic)?
        public open override /*1*/ fun <set-ondragexit>(/*0*/ <set-?>: ((org.w3c.dom.DragEvent) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var ondragleave: ((org.w3c.dom.DragEvent) -> dynamic)?
        public open override /*1*/ fun <get-ondragleave>(): ((org.w3c.dom.DragEvent) -> dynamic)?
        public open override /*1*/ fun <set-ondragleave>(/*0*/ <set-?>: ((org.w3c.dom.DragEvent) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var ondragover: ((org.w3c.dom.DragEvent) -> dynamic)?
        public open override /*1*/ fun <get-ondragover>(): ((org.w3c.dom.DragEvent) -> dynamic)?
        public open override /*1*/ fun <set-ondragover>(/*0*/ <set-?>: ((org.w3c.dom.DragEvent) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var ondragstart: ((org.w3c.dom.DragEvent) -> dynamic)?
        public open override /*1*/ fun <get-ondragstart>(): ((org.w3c.dom.DragEvent) -> dynamic)?
        public open override /*1*/ fun <set-ondragstart>(/*0*/ <set-?>: ((org.w3c.dom.DragEvent) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var ondrop: ((org.w3c.dom.DragEvent) -> dynamic)?
        public open override /*1*/ fun <get-ondrop>(): ((org.w3c.dom.DragEvent) -> dynamic)?
        public open override /*1*/ fun <set-ondrop>(/*0*/ <set-?>: ((org.w3c.dom.DragEvent) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var ondurationchange: ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <get-ondurationchange>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <set-ondurationchange>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onemptied: ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <get-onemptied>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <set-onemptied>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onended: ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <get-onended>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <set-onended>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onerror: ((dynamic, kotlin.String, kotlin.Int, kotlin.Int, kotlin.Any?) -> dynamic)?
        public open override /*1*/ fun <get-onerror>(): ((dynamic, kotlin.String, kotlin.Int, kotlin.Int, kotlin.Any?) -> dynamic)?
        public open override /*1*/ fun <set-onerror>(/*0*/ <set-?>: ((dynamic, kotlin.String, kotlin.Int, kotlin.Int, kotlin.Any?) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onfocus: ((org.w3c.dom.events.FocusEvent) -> dynamic)?
        public open override /*1*/ fun <get-onfocus>(): ((org.w3c.dom.events.FocusEvent) -> dynamic)?
        public open override /*1*/ fun <set-onfocus>(/*0*/ <set-?>: ((org.w3c.dom.events.FocusEvent) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var ongotpointercapture: ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?
        public open override /*1*/ fun <get-ongotpointercapture>(): ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?
        public open override /*1*/ fun <set-ongotpointercapture>(/*0*/ <set-?>: ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var oninput: ((org.w3c.dom.events.InputEvent) -> dynamic)?
        public open override /*1*/ fun <get-oninput>(): ((org.w3c.dom.events.InputEvent) -> dynamic)?
        public open override /*1*/ fun <set-oninput>(/*0*/ <set-?>: ((org.w3c.dom.events.InputEvent) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var oninvalid: ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <get-oninvalid>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <set-oninvalid>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onkeydown: ((org.w3c.dom.events.KeyboardEvent) -> dynamic)?
        public open override /*1*/ fun <get-onkeydown>(): ((org.w3c.dom.events.KeyboardEvent) -> dynamic)?
        public open override /*1*/ fun <set-onkeydown>(/*0*/ <set-?>: ((org.w3c.dom.events.KeyboardEvent) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onkeypress: ((org.w3c.dom.events.KeyboardEvent) -> dynamic)?
        public open override /*1*/ fun <get-onkeypress>(): ((org.w3c.dom.events.KeyboardEvent) -> dynamic)?
        public open override /*1*/ fun <set-onkeypress>(/*0*/ <set-?>: ((org.w3c.dom.events.KeyboardEvent) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onkeyup: ((org.w3c.dom.events.KeyboardEvent) -> dynamic)?
        public open override /*1*/ fun <get-onkeyup>(): ((org.w3c.dom.events.KeyboardEvent) -> dynamic)?
        public open override /*1*/ fun <set-onkeyup>(/*0*/ <set-?>: ((org.w3c.dom.events.KeyboardEvent) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onload: ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <get-onload>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <set-onload>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onloadeddata: ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <get-onloadeddata>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <set-onloadeddata>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onloadedmetadata: ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <get-onloadedmetadata>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <set-onloadedmetadata>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onloadend: ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <get-onloadend>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <set-onloadend>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onloadstart: ((org.w3c.xhr.ProgressEvent) -> dynamic)?
        public open override /*1*/ fun <get-onloadstart>(): ((org.w3c.xhr.ProgressEvent) -> dynamic)?
        public open override /*1*/ fun <set-onloadstart>(/*0*/ <set-?>: ((org.w3c.xhr.ProgressEvent) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onlostpointercapture: ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?
        public open override /*1*/ fun <get-onlostpointercapture>(): ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?
        public open override /*1*/ fun <set-onlostpointercapture>(/*0*/ <set-?>: ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onmousedown: ((org.w3c.dom.events.MouseEvent) -> dynamic)?
        public open override /*1*/ fun <get-onmousedown>(): ((org.w3c.dom.events.MouseEvent) -> dynamic)?
        public open override /*1*/ fun <set-onmousedown>(/*0*/ <set-?>: ((org.w3c.dom.events.MouseEvent) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onmouseenter: ((org.w3c.dom.events.MouseEvent) -> dynamic)?
        public open override /*1*/ fun <get-onmouseenter>(): ((org.w3c.dom.events.MouseEvent) -> dynamic)?
        public open override /*1*/ fun <set-onmouseenter>(/*0*/ <set-?>: ((org.w3c.dom.events.MouseEvent) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onmouseleave: ((org.w3c.dom.events.MouseEvent) -> dynamic)?
        public open override /*1*/ fun <get-onmouseleave>(): ((org.w3c.dom.events.MouseEvent) -> dynamic)?
        public open override /*1*/ fun <set-onmouseleave>(/*0*/ <set-?>: ((org.w3c.dom.events.MouseEvent) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onmousemove: ((org.w3c.dom.events.MouseEvent) -> dynamic)?
        public open override /*1*/ fun <get-onmousemove>(): ((org.w3c.dom.events.MouseEvent) -> dynamic)?
        public open override /*1*/ fun <set-onmousemove>(/*0*/ <set-?>: ((org.w3c.dom.events.MouseEvent) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onmouseout: ((org.w3c.dom.events.MouseEvent) -> dynamic)?
        public open override /*1*/ fun <get-onmouseout>(): ((org.w3c.dom.events.MouseEvent) -> dynamic)?
        public open override /*1*/ fun <set-onmouseout>(/*0*/ <set-?>: ((org.w3c.dom.events.MouseEvent) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onmouseover: ((org.w3c.dom.events.MouseEvent) -> dynamic)?
        public open override /*1*/ fun <get-onmouseover>(): ((org.w3c.dom.events.MouseEvent) -> dynamic)?
        public open override /*1*/ fun <set-onmouseover>(/*0*/ <set-?>: ((org.w3c.dom.events.MouseEvent) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onmouseup: ((org.w3c.dom.events.MouseEvent) -> dynamic)?
        public open override /*1*/ fun <get-onmouseup>(): ((org.w3c.dom.events.MouseEvent) -> dynamic)?
        public open override /*1*/ fun <set-onmouseup>(/*0*/ <set-?>: ((org.w3c.dom.events.MouseEvent) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onpaste: ((org.w3c.dom.clipboard.ClipboardEvent) -> dynamic)?
        public open override /*1*/ fun <get-onpaste>(): ((org.w3c.dom.clipboard.ClipboardEvent) -> dynamic)?
        public open override /*1*/ fun <set-onpaste>(/*0*/ <set-?>: ((org.w3c.dom.clipboard.ClipboardEvent) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onpause: ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <get-onpause>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <set-onpause>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onplay: ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <get-onplay>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <set-onplay>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onplaying: ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <get-onplaying>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <set-onplaying>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onpointercancel: ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?
        public open override /*1*/ fun <get-onpointercancel>(): ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?
        public open override /*1*/ fun <set-onpointercancel>(/*0*/ <set-?>: ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onpointerdown: ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?
        public open override /*1*/ fun <get-onpointerdown>(): ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?
        public open override /*1*/ fun <set-onpointerdown>(/*0*/ <set-?>: ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onpointerenter: ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?
        public open override /*1*/ fun <get-onpointerenter>(): ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?
        public open override /*1*/ fun <set-onpointerenter>(/*0*/ <set-?>: ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onpointerleave: ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?
        public open override /*1*/ fun <get-onpointerleave>(): ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?
        public open override /*1*/ fun <set-onpointerleave>(/*0*/ <set-?>: ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onpointermove: ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?
        public open override /*1*/ fun <get-onpointermove>(): ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?
        public open override /*1*/ fun <set-onpointermove>(/*0*/ <set-?>: ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onpointerout: ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?
        public open override /*1*/ fun <get-onpointerout>(): ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?
        public open override /*1*/ fun <set-onpointerout>(/*0*/ <set-?>: ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onpointerover: ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?
        public open override /*1*/ fun <get-onpointerover>(): ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?
        public open override /*1*/ fun <set-onpointerover>(/*0*/ <set-?>: ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onpointerup: ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?
        public open override /*1*/ fun <get-onpointerup>(): ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?
        public open override /*1*/ fun <set-onpointerup>(/*0*/ <set-?>: ((org.w3c.dom.pointerevents.PointerEvent) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onprogress: ((org.w3c.xhr.ProgressEvent) -> dynamic)?
        public open override /*1*/ fun <get-onprogress>(): ((org.w3c.xhr.ProgressEvent) -> dynamic)?
        public open override /*1*/ fun <set-onprogress>(/*0*/ <set-?>: ((org.w3c.xhr.ProgressEvent) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onratechange: ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <get-onratechange>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <set-onratechange>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onreset: ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <get-onreset>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <set-onreset>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onresize: ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <get-onresize>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <set-onresize>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onscroll: ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <get-onscroll>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <set-onscroll>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onseeked: ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <get-onseeked>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <set-onseeked>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onseeking: ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <get-onseeking>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <set-onseeking>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onselect: ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <get-onselect>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <set-onselect>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onshow: ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <get-onshow>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <set-onshow>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onstalled: ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <get-onstalled>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <set-onstalled>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onsubmit: ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <get-onsubmit>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <set-onsubmit>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onsuspend: ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <get-onsuspend>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <set-onsuspend>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var ontimeupdate: ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <get-ontimeupdate>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <set-ontimeupdate>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var ontoggle: ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <get-ontoggle>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <set-ontoggle>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onvolumechange: ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <get-onvolumechange>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <set-onvolumechange>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onwaiting: ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <get-onwaiting>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <set-onwaiting>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open override /*1*/ var onwheel: ((org.w3c.dom.events.WheelEvent) -> dynamic)?
        public open override /*1*/ fun <get-onwheel>(): ((org.w3c.dom.events.WheelEvent) -> dynamic)?
        public open override /*1*/ fun <set-onwheel>(/*0*/ <set-?>: ((org.w3c.dom.events.WheelEvent) -> dynamic)?): kotlin.Unit
    public open override /*1*/ val previousElementSibling: org.w3c.dom.Element?
        public open override /*1*/ fun <get-previousElementSibling>(): org.w3c.dom.Element?
    public open override /*1*/ val style: org.w3c.dom.css.CSSStyleDeclaration
        public open override /*1*/ fun <get-style>(): org.w3c.dom.css.CSSStyleDeclaration
    public open override /*1*/ fun after(/*0*/ vararg nodes: dynamic /*kotlin.Array<out dynamic>*/): kotlin.Unit
    public open override /*1*/ fun append(/*0*/ vararg nodes: dynamic /*kotlin.Array<out dynamic>*/): kotlin.Unit
    public open override /*1*/ fun before(/*0*/ vararg nodes: dynamic /*kotlin.Array<out dynamic>*/): kotlin.Unit
    public open override /*1*/ fun convertPointFromNode(/*0*/ point: org.w3c.dom.DOMPointInit, /*1*/ from: dynamic, /*2*/ options: org.w3c.dom.ConvertCoordinateOptions = ...): org.w3c.dom.DOMPoint
    public open override /*1*/ fun convertQuadFromNode(/*0*/ quad: dynamic, /*1*/ from: dynamic, /*2*/ options: org.w3c.dom.ConvertCoordinateOptions = ...): org.w3c.dom.DOMQuad
    public open override /*1*/ fun convertRectFromNode(/*0*/ rect: org.w3c.dom.DOMRectReadOnly, /*1*/ from: dynamic, /*2*/ options: org.w3c.dom.ConvertCoordinateOptions = ...): org.w3c.dom.DOMQuad
    public open override /*1*/ fun getBoxQuads(/*0*/ options: org.w3c.dom.BoxQuadOptions = ...): kotlin.Array<org.w3c.dom.DOMQuad>
    public open override /*1*/ fun prepend(/*0*/ vararg nodes: dynamic /*kotlin.Array<out dynamic>*/): kotlin.Unit
    public open override /*1*/ fun querySelector(/*0*/ selectors: kotlin.String): org.w3c.dom.Element?
    public open override /*1*/ fun querySelectorAll(/*0*/ selectors: kotlin.String): org.w3c.dom.NodeList
    public open override /*1*/ fun remove(): kotlin.Unit
    public open override /*1*/ fun replaceWith(/*0*/ vararg nodes: dynamic /*kotlin.Array<out dynamic>*/): kotlin.Unit

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

public open external class PageTransitionEvent : org.w3c.dom.events.Event {
    /*primary*/ public constructor PageTransitionEvent(/*0*/ type: kotlin.String, /*1*/ eventInitDict: org.w3c.dom.PageTransitionEventInit = ...)
    public open val persisted: kotlin.Boolean
        public open fun <get-persisted>(): kotlin.Boolean

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

public external interface PageTransitionEventInit : org.w3c.dom.EventInit {
    public open var persisted: kotlin.Boolean?
        public open fun <get-persisted>(): kotlin.Boolean?
        public open fun <set-persisted>(/*0*/ value: kotlin.Boolean?): kotlin.Unit
}

public external interface ParentNode {
    public abstract val childElementCount: kotlin.Int
        public abstract fun <get-childElementCount>(): kotlin.Int
    public abstract val children: org.w3c.dom.HTMLCollection
        public abstract fun <get-children>(): org.w3c.dom.HTMLCollection
    public open val firstElementChild: org.w3c.dom.Element?
        public open fun <get-firstElementChild>(): org.w3c.dom.Element?
    public open val lastElementChild: org.w3c.dom.Element?
        public open fun <get-lastElementChild>(): org.w3c.dom.Element?
    public abstract fun append(/*0*/ vararg nodes: dynamic /*kotlin.Array<out dynamic>*/): kotlin.Unit
    public abstract fun prepend(/*0*/ vararg nodes: dynamic /*kotlin.Array<out dynamic>*/): kotlin.Unit
    public abstract fun querySelector(/*0*/ selectors: kotlin.String): org.w3c.dom.Element?
    public abstract fun querySelectorAll(/*0*/ selectors: kotlin.String): org.w3c.dom.NodeList
}

public open external class Path2D : org.w3c.dom.CanvasPath {
    /*primary*/ public constructor Path2D()
    public constructor Path2D(/*0*/ paths: kotlin.Array<org.w3c.dom.Path2D>, /*1*/ fillRule: org.w3c.dom.CanvasFillRule = ...)
    public constructor Path2D(/*0*/ d: kotlin.String)
    public constructor Path2D(/*0*/ path: org.w3c.dom.Path2D)
    public final fun addPath(/*0*/ path: org.w3c.dom.Path2D, /*1*/ transform: dynamic = ...): kotlin.Unit
    public open override /*1*/ fun arc(/*0*/ x: kotlin.Double, /*1*/ y: kotlin.Double, /*2*/ radius: kotlin.Double, /*3*/ startAngle: kotlin.Double, /*4*/ endAngle: kotlin.Double, /*5*/ anticlockwise: kotlin.Boolean = ...): kotlin.Unit
    public open override /*1*/ fun arcTo(/*0*/ x1: kotlin.Double, /*1*/ y1: kotlin.Double, /*2*/ x2: kotlin.Double, /*3*/ y2: kotlin.Double, /*4*/ radius: kotlin.Double): kotlin.Unit
    public open override /*1*/ fun arcTo(/*0*/ x1: kotlin.Double, /*1*/ y1: kotlin.Double, /*2*/ x2: kotlin.Double, /*3*/ y2: kotlin.Double, /*4*/ radiusX: kotlin.Double, /*5*/ radiusY: kotlin.Double, /*6*/ rotation: kotlin.Double): kotlin.Unit
    public open override /*1*/ fun bezierCurveTo(/*0*/ cp1x: kotlin.Double, /*1*/ cp1y: kotlin.Double, /*2*/ cp2x: kotlin.Double, /*3*/ cp2y: kotlin.Double, /*4*/ x: kotlin.Double, /*5*/ y: kotlin.Double): kotlin.Unit
    public open override /*1*/ fun closePath(): kotlin.Unit
    public open override /*1*/ fun ellipse(/*0*/ x: kotlin.Double, /*1*/ y: kotlin.Double, /*2*/ radiusX: kotlin.Double, /*3*/ radiusY: kotlin.Double, /*4*/ rotation: kotlin.Double, /*5*/ startAngle: kotlin.Double, /*6*/ endAngle: kotlin.Double, /*7*/ anticlockwise: kotlin.Boolean = ...): kotlin.Unit
    public open override /*1*/ fun lineTo(/*0*/ x: kotlin.Double, /*1*/ y: kotlin.Double): kotlin.Unit
    public open override /*1*/ fun moveTo(/*0*/ x: kotlin.Double, /*1*/ y: kotlin.Double): kotlin.Unit
    public open override /*1*/ fun quadraticCurveTo(/*0*/ cpx: kotlin.Double, /*1*/ cpy: kotlin.Double, /*2*/ x: kotlin.Double, /*3*/ y: kotlin.Double): kotlin.Unit
    public open override /*1*/ fun rect(/*0*/ x: kotlin.Double, /*1*/ y: kotlin.Double, /*2*/ w: kotlin.Double, /*3*/ h: kotlin.Double): kotlin.Unit
}

public abstract external class Plugin : org.w3c.dom.ItemArrayLike<org.w3c.dom.MimeType> {
    /*primary*/ public constructor Plugin()
    public open val description: kotlin.String
        public open fun <get-description>(): kotlin.String
    public open val filename: kotlin.String
        public open fun <get-filename>(): kotlin.String
    public open val name: kotlin.String
        public open fun <get-name>(): kotlin.String
    public open override /*1*/ fun item(/*0*/ index: kotlin.Int): org.w3c.dom.MimeType?
    public final fun namedItem(/*0*/ name: kotlin.String): org.w3c.dom.MimeType?
}

public abstract external class PluginArray : org.w3c.dom.ItemArrayLike<org.w3c.dom.Plugin> {
    /*primary*/ public constructor PluginArray()
    public open override /*1*/ fun item(/*0*/ index: kotlin.Int): org.w3c.dom.Plugin?
    public final fun namedItem(/*0*/ name: kotlin.String): org.w3c.dom.Plugin?
    public final fun refresh(/*0*/ reload: kotlin.Boolean = ...): kotlin.Unit
}

public open external class PopStateEvent : org.w3c.dom.events.Event {
    /*primary*/ public constructor PopStateEvent(/*0*/ type: kotlin.String, /*1*/ eventInitDict: org.w3c.dom.PopStateEventInit = ...)
    public open val state: kotlin.Any?
        public open fun <get-state>(): kotlin.Any?

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

public external interface PopStateEventInit : org.w3c.dom.EventInit {
    public open var state: kotlin.Any?
        public open fun <get-state>(): kotlin.Any?
        public open fun <set-state>(/*0*/ value: kotlin.Any?): kotlin.Unit
}

public external interface PremultiplyAlpha {

    public companion object Companion {
    }
}

public abstract external class ProcessingInstruction : org.w3c.dom.CharacterData, org.w3c.dom.css.LinkStyle, org.w3c.dom.css.UnionElementOrProcessingInstruction {
    /*primary*/ public constructor ProcessingInstruction()
    public open val target: kotlin.String
        public open fun <get-target>(): kotlin.String

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

public open external class PromiseRejectionEvent : org.w3c.dom.events.Event {
    /*primary*/ public constructor PromiseRejectionEvent(/*0*/ type: kotlin.String, /*1*/ eventInitDict: org.w3c.dom.PromiseRejectionEventInit)
    public open val promise: kotlin.js.Promise<kotlin.Any?>
        public open fun <get-promise>(): kotlin.js.Promise<kotlin.Any?>
    public open val reason: kotlin.Any?
        public open fun <get-reason>(): kotlin.Any?

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

public external interface PromiseRejectionEventInit : org.w3c.dom.EventInit {
    public open var promise: kotlin.js.Promise<kotlin.Any?>?
        public open fun <get-promise>(): kotlin.js.Promise<kotlin.Any?>?
        public open fun <set-promise>(/*0*/ value: kotlin.js.Promise<kotlin.Any?>?): kotlin.Unit
    public open var reason: kotlin.Any?
        public open fun <get-reason>(): kotlin.Any?
        public open fun <set-reason>(/*0*/ value: kotlin.Any?): kotlin.Unit
}

public abstract external class RadioNodeList : org.w3c.dom.NodeList, org.w3c.dom.UnionElementOrRadioNodeList {
    /*primary*/ public constructor RadioNodeList()
    public open var value: kotlin.String
        public open fun <get-value>(): kotlin.String
        public open fun <set-value>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
}

public open external class Range {
    /*primary*/ public constructor Range()
    public open val collapsed: kotlin.Boolean
        public open fun <get-collapsed>(): kotlin.Boolean
    public open val commonAncestorContainer: org.w3c.dom.Node
        public open fun <get-commonAncestorContainer>(): org.w3c.dom.Node
    public open val endContainer: org.w3c.dom.Node
        public open fun <get-endContainer>(): org.w3c.dom.Node
    public open val endOffset: kotlin.Int
        public open fun <get-endOffset>(): kotlin.Int
    public open val startContainer: org.w3c.dom.Node
        public open fun <get-startContainer>(): org.w3c.dom.Node
    public open val startOffset: kotlin.Int
        public open fun <get-startOffset>(): kotlin.Int
    public final fun cloneContents(): org.w3c.dom.DocumentFragment
    public final fun cloneRange(): org.w3c.dom.Range
    public final fun collapse(/*0*/ toStart: kotlin.Boolean = ...): kotlin.Unit
    public final fun compareBoundaryPoints(/*0*/ how: kotlin.Short, /*1*/ sourceRange: org.w3c.dom.Range): kotlin.Short
    public final fun comparePoint(/*0*/ node: org.w3c.dom.Node, /*1*/ offset: kotlin.Int): kotlin.Short
    public final fun createContextualFragment(/*0*/ fragment: kotlin.String): org.w3c.dom.DocumentFragment
    public final fun deleteContents(): kotlin.Unit
    public final fun detach(): kotlin.Unit
    public final fun extractContents(): org.w3c.dom.DocumentFragment
    public final fun getBoundingClientRect(): org.w3c.dom.DOMRect
    public final fun getClientRects(): kotlin.Array<org.w3c.dom.DOMRect>
    public final fun insertNode(/*0*/ node: org.w3c.dom.Node): kotlin.Unit
    public final fun intersectsNode(/*0*/ node: org.w3c.dom.Node): kotlin.Boolean
    public final fun isPointInRange(/*0*/ node: org.w3c.dom.Node, /*1*/ offset: kotlin.Int): kotlin.Boolean
    public final fun selectNode(/*0*/ node: org.w3c.dom.Node): kotlin.Unit
    public final fun selectNodeContents(/*0*/ node: org.w3c.dom.Node): kotlin.Unit
    public final fun setEnd(/*0*/ node: org.w3c.dom.Node, /*1*/ offset: kotlin.Int): kotlin.Unit
    public final fun setEndAfter(/*0*/ node: org.w3c.dom.Node): kotlin.Unit
    public final fun setEndBefore(/*0*/ node: org.w3c.dom.Node): kotlin.Unit
    public final fun setStart(/*0*/ node: org.w3c.dom.Node, /*1*/ offset: kotlin.Int): kotlin.Unit
    public final fun setStartAfter(/*0*/ node: org.w3c.dom.Node): kotlin.Unit
    public final fun setStartBefore(/*0*/ node: org.w3c.dom.Node): kotlin.Unit
    public final fun surroundContents(/*0*/ newParent: org.w3c.dom.Node): kotlin.Unit

    public companion object Companion {
        public final val END_TO_END: kotlin.Short
            public final fun <get-END_TO_END>(): kotlin.Short
        public final val END_TO_START: kotlin.Short
            public final fun <get-END_TO_START>(): kotlin.Short
        public final val START_TO_END: kotlin.Short
            public final fun <get-START_TO_END>(): kotlin.Short
        public final val START_TO_START: kotlin.Short
            public final fun <get-START_TO_START>(): kotlin.Short
    }
}

public open external class RelatedEvent : org.w3c.dom.events.Event {
    /*primary*/ public constructor RelatedEvent(/*0*/ type: kotlin.String, /*1*/ eventInitDict: org.w3c.dom.RelatedEventInit = ...)
    public open val relatedTarget: org.w3c.dom.events.EventTarget?
        public open fun <get-relatedTarget>(): org.w3c.dom.events.EventTarget?

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

public external interface RelatedEventInit : org.w3c.dom.EventInit {
    public open var relatedTarget: org.w3c.dom.events.EventTarget?
        public open fun <get-relatedTarget>(): org.w3c.dom.events.EventTarget?
        public open fun <set-relatedTarget>(/*0*/ value: org.w3c.dom.events.EventTarget?): kotlin.Unit
}

public external interface RenderingContext {
}

public external interface ResizeQuality {

    public companion object Companion {
    }
}

public abstract external class Screen {
    /*primary*/ public constructor Screen()
    public open val availHeight: kotlin.Int
        public open fun <get-availHeight>(): kotlin.Int
    public open val availWidth: kotlin.Int
        public open fun <get-availWidth>(): kotlin.Int
    public open val colorDepth: kotlin.Int
        public open fun <get-colorDepth>(): kotlin.Int
    public open val height: kotlin.Int
        public open fun <get-height>(): kotlin.Int
    public open val pixelDepth: kotlin.Int
        public open fun <get-pixelDepth>(): kotlin.Int
    public open val width: kotlin.Int
        public open fun <get-width>(): kotlin.Int
}

public external interface ScrollBehavior {

    public companion object Companion {
    }
}

public external interface ScrollIntoViewOptions : org.w3c.dom.ScrollOptions {
    public open var block: org.w3c.dom.ScrollLogicalPosition?
        public open fun <get-block>(): org.w3c.dom.ScrollLogicalPosition?
        public open fun <set-block>(/*0*/ value: org.w3c.dom.ScrollLogicalPosition?): kotlin.Unit
    public open var inline: org.w3c.dom.ScrollLogicalPosition?
        public open fun <get-inline>(): org.w3c.dom.ScrollLogicalPosition?
        public open fun <set-inline>(/*0*/ value: org.w3c.dom.ScrollLogicalPosition?): kotlin.Unit
}

public external interface ScrollLogicalPosition {

    public companion object Companion {
    }
}

public external interface ScrollOptions {
    public open var behavior: org.w3c.dom.ScrollBehavior?
        public open fun <get-behavior>(): org.w3c.dom.ScrollBehavior?
        public open fun <set-behavior>(/*0*/ value: org.w3c.dom.ScrollBehavior?): kotlin.Unit
}

public external interface ScrollRestoration {

    public companion object Companion {
    }
}

public external interface ScrollToOptions : org.w3c.dom.ScrollOptions {
    public open var left: kotlin.Double?
        public open fun <get-left>(): kotlin.Double?
        public open fun <set-left>(/*0*/ value: kotlin.Double?): kotlin.Unit
    public open var top: kotlin.Double?
        public open fun <get-top>(): kotlin.Double?
        public open fun <set-top>(/*0*/ value: kotlin.Double?): kotlin.Unit
}

public external interface SelectionMode {

    public companion object Companion {
    }
}

public open external class ShadowRoot : org.w3c.dom.DocumentFragment, org.w3c.dom.DocumentOrShadowRoot {
    /*primary*/ public constructor ShadowRoot()
    public open override /*1*/ val fullscreenElement: org.w3c.dom.Element?
        public open override /*1*/ fun <get-fullscreenElement>(): org.w3c.dom.Element?
    public open val host: org.w3c.dom.Element
        public open fun <get-host>(): org.w3c.dom.Element
    public open val mode: org.w3c.dom.ShadowRootMode
        public open fun <get-mode>(): org.w3c.dom.ShadowRootMode

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

public external interface ShadowRootInit {
    public open var mode: org.w3c.dom.ShadowRootMode?
        public open fun <get-mode>(): org.w3c.dom.ShadowRootMode?
        public open fun <set-mode>(/*0*/ value: org.w3c.dom.ShadowRootMode?): kotlin.Unit
}

public external interface ShadowRootMode {

    public companion object Companion {
    }
}

public open external class SharedWorker : org.w3c.dom.events.EventTarget, org.w3c.dom.AbstractWorker {
    /*primary*/ public constructor SharedWorker(/*0*/ scriptURL: kotlin.String, /*1*/ name: kotlin.String = ..., /*2*/ options: org.w3c.dom.WorkerOptions = ...)
    public open override /*1*/ var onerror: ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <get-onerror>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <set-onerror>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open val port: org.w3c.dom.MessagePort
        public open fun <get-port>(): org.w3c.dom.MessagePort
}

public abstract external class SharedWorkerGlobalScope : org.w3c.dom.WorkerGlobalScope {
    /*primary*/ public constructor SharedWorkerGlobalScope()
    public open val applicationCache: org.w3c.dom.ApplicationCache
        public open fun <get-applicationCache>(): org.w3c.dom.ApplicationCache
    public open val name: kotlin.String
        public open fun <get-name>(): kotlin.String
    public open var onconnect: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onconnect>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onconnect>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public final fun close(): kotlin.Unit
}

public external interface Slotable {
    public open val assignedSlot: org.w3c.dom.HTMLSlotElement?
        public open fun <get-assignedSlot>(): org.w3c.dom.HTMLSlotElement?
}

public abstract external class Storage {
    /*primary*/ public constructor Storage()
    public open val length: kotlin.Int
        public open fun <get-length>(): kotlin.Int
    public final fun clear(): kotlin.Unit
    public final fun getItem(/*0*/ key: kotlin.String): kotlin.String?
    public final fun key(/*0*/ index: kotlin.Int): kotlin.String?
    public final fun removeItem(/*0*/ key: kotlin.String): kotlin.Unit
    public final fun setItem(/*0*/ key: kotlin.String, /*1*/ value: kotlin.String): kotlin.Unit
}

public open external class StorageEvent : org.w3c.dom.events.Event {
    /*primary*/ public constructor StorageEvent(/*0*/ type: kotlin.String, /*1*/ eventInitDict: org.w3c.dom.StorageEventInit = ...)
    public open val key: kotlin.String?
        public open fun <get-key>(): kotlin.String?
    public open val newValue: kotlin.String?
        public open fun <get-newValue>(): kotlin.String?
    public open val oldValue: kotlin.String?
        public open fun <get-oldValue>(): kotlin.String?
    public open val storageArea: org.w3c.dom.Storage?
        public open fun <get-storageArea>(): org.w3c.dom.Storage?
    public open val url: kotlin.String
        public open fun <get-url>(): kotlin.String

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

public external interface StorageEventInit : org.w3c.dom.EventInit {
    public open var key: kotlin.String?
        public open fun <get-key>(): kotlin.String?
        public open fun <set-key>(/*0*/ value: kotlin.String?): kotlin.Unit
    public open var newValue: kotlin.String?
        public open fun <get-newValue>(): kotlin.String?
        public open fun <set-newValue>(/*0*/ value: kotlin.String?): kotlin.Unit
    public open var oldValue: kotlin.String?
        public open fun <get-oldValue>(): kotlin.String?
        public open fun <set-oldValue>(/*0*/ value: kotlin.String?): kotlin.Unit
    public open var storageArea: org.w3c.dom.Storage?
        public open fun <get-storageArea>(): org.w3c.dom.Storage?
        public open fun <set-storageArea>(/*0*/ value: org.w3c.dom.Storage?): kotlin.Unit
    public open var url: kotlin.String?
        public open fun <get-url>(): kotlin.String?
        public open fun <set-url>(/*0*/ value: kotlin.String?): kotlin.Unit
}

public open external class Text : org.w3c.dom.CharacterData, org.w3c.dom.Slotable, org.w3c.dom.GeometryUtils {
    /*primary*/ public constructor Text(/*0*/ data: kotlin.String = ...)
    public open override /*1*/ val assignedSlot: org.w3c.dom.HTMLSlotElement?
        public open override /*1*/ fun <get-assignedSlot>(): org.w3c.dom.HTMLSlotElement?
    public open override /*1*/ val nextElementSibling: org.w3c.dom.Element?
        public open override /*1*/ fun <get-nextElementSibling>(): org.w3c.dom.Element?
    public open override /*1*/ val previousElementSibling: org.w3c.dom.Element?
        public open override /*1*/ fun <get-previousElementSibling>(): org.w3c.dom.Element?
    public open val wholeText: kotlin.String
        public open fun <get-wholeText>(): kotlin.String
    public open override /*1*/ fun after(/*0*/ vararg nodes: dynamic /*kotlin.Array<out dynamic>*/): kotlin.Unit
    public open override /*1*/ fun before(/*0*/ vararg nodes: dynamic /*kotlin.Array<out dynamic>*/): kotlin.Unit
    public open override /*1*/ fun convertPointFromNode(/*0*/ point: org.w3c.dom.DOMPointInit, /*1*/ from: dynamic, /*2*/ options: org.w3c.dom.ConvertCoordinateOptions = ...): org.w3c.dom.DOMPoint
    public open override /*1*/ fun convertQuadFromNode(/*0*/ quad: dynamic, /*1*/ from: dynamic, /*2*/ options: org.w3c.dom.ConvertCoordinateOptions = ...): org.w3c.dom.DOMQuad
    public open override /*1*/ fun convertRectFromNode(/*0*/ rect: org.w3c.dom.DOMRectReadOnly, /*1*/ from: dynamic, /*2*/ options: org.w3c.dom.ConvertCoordinateOptions = ...): org.w3c.dom.DOMQuad
    public open override /*1*/ fun getBoxQuads(/*0*/ options: org.w3c.dom.BoxQuadOptions = ...): kotlin.Array<org.w3c.dom.DOMQuad>
    public open override /*1*/ fun remove(): kotlin.Unit
    public open override /*1*/ fun replaceWith(/*0*/ vararg nodes: dynamic /*kotlin.Array<out dynamic>*/): kotlin.Unit
    public final fun splitText(/*0*/ offset: kotlin.Int): org.w3c.dom.Text

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
