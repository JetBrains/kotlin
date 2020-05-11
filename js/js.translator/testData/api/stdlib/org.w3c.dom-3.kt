
public abstract external class HTMLBodyElement : org.w3c.dom.HTMLElement, org.w3c.dom.WindowEventHandlers {
    /*primary*/ public constructor HTMLBodyElement()
    public open var aLink: kotlin.String
        public open fun <get-aLink>(): kotlin.String
        public open fun <set-aLink>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var background: kotlin.String
        public open fun <get-background>(): kotlin.String
        public open fun <set-background>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var bgColor: kotlin.String
        public open fun <get-bgColor>(): kotlin.String
        public open fun <set-bgColor>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var link: kotlin.String
        public open fun <get-link>(): kotlin.String
        public open fun <set-link>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var text: kotlin.String
        public open fun <get-text>(): kotlin.String
        public open fun <set-text>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var vLink: kotlin.String
        public open fun <get-vLink>(): kotlin.String
        public open fun <set-vLink>(/*0*/ <set-?>: kotlin.String): kotlin.Unit

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

public abstract external class HTMLButtonElement : org.w3c.dom.HTMLElement {
    /*primary*/ public constructor HTMLButtonElement()
    public open var autofocus: kotlin.Boolean
        public open fun <get-autofocus>(): kotlin.Boolean
        public open fun <set-autofocus>(/*0*/ <set-?>: kotlin.Boolean): kotlin.Unit
    public open var disabled: kotlin.Boolean
        public open fun <get-disabled>(): kotlin.Boolean
        public open fun <set-disabled>(/*0*/ <set-?>: kotlin.Boolean): kotlin.Unit
    public open val form: org.w3c.dom.HTMLFormElement?
        public open fun <get-form>(): org.w3c.dom.HTMLFormElement?
    public open var formAction: kotlin.String
        public open fun <get-formAction>(): kotlin.String
        public open fun <set-formAction>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var formEnctype: kotlin.String
        public open fun <get-formEnctype>(): kotlin.String
        public open fun <set-formEnctype>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var formMethod: kotlin.String
        public open fun <get-formMethod>(): kotlin.String
        public open fun <set-formMethod>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var formNoValidate: kotlin.Boolean
        public open fun <get-formNoValidate>(): kotlin.Boolean
        public open fun <set-formNoValidate>(/*0*/ <set-?>: kotlin.Boolean): kotlin.Unit
    public open var formTarget: kotlin.String
        public open fun <get-formTarget>(): kotlin.String
        public open fun <set-formTarget>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open val labels: org.w3c.dom.NodeList
        public open fun <get-labels>(): org.w3c.dom.NodeList
    public open var menu: org.w3c.dom.HTMLMenuElement?
        public open fun <get-menu>(): org.w3c.dom.HTMLMenuElement?
        public open fun <set-menu>(/*0*/ <set-?>: org.w3c.dom.HTMLMenuElement?): kotlin.Unit
    public open var name: kotlin.String
        public open fun <get-name>(): kotlin.String
        public open fun <set-name>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var type: kotlin.String
        public open fun <get-type>(): kotlin.String
        public open fun <set-type>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open val validationMessage: kotlin.String
        public open fun <get-validationMessage>(): kotlin.String
    public open val validity: org.w3c.dom.ValidityState
        public open fun <get-validity>(): org.w3c.dom.ValidityState
    public open var value: kotlin.String
        public open fun <get-value>(): kotlin.String
        public open fun <set-value>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open val willValidate: kotlin.Boolean
        public open fun <get-willValidate>(): kotlin.Boolean
    public final fun checkValidity(): kotlin.Boolean
    public final fun reportValidity(): kotlin.Boolean
    public final fun setCustomValidity(/*0*/ error: kotlin.String): kotlin.Unit

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

public abstract external class HTMLCanvasElement : org.w3c.dom.HTMLElement, org.w3c.dom.CanvasImageSource, org.khronos.webgl.TexImageSource {
    /*primary*/ public constructor HTMLCanvasElement()
    public open var height: kotlin.Int
        public open fun <get-height>(): kotlin.Int
        public open fun <set-height>(/*0*/ <set-?>: kotlin.Int): kotlin.Unit
    public open var width: kotlin.Int
        public open fun <get-width>(): kotlin.Int
        public open fun <set-width>(/*0*/ <set-?>: kotlin.Int): kotlin.Unit
    public final fun getContext(/*0*/ contextId: kotlin.String, /*1*/ vararg arguments: kotlin.Any? /*kotlin.Array<out kotlin.Any?>*/): org.w3c.dom.RenderingContext?
    public final fun toBlob(/*0*/ _callback: (org.w3c.files.Blob?) -> kotlin.Unit, /*1*/ type: kotlin.String = ..., /*2*/ quality: kotlin.Any? = ...): kotlin.Unit
    public final fun toDataURL(/*0*/ type: kotlin.String = ..., /*1*/ quality: kotlin.Any? = ...): kotlin.String

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

public abstract external class HTMLCollection : org.w3c.dom.ItemArrayLike<org.w3c.dom.Element>, org.w3c.dom.UnionElementOrHTMLCollection {
    /*primary*/ public constructor HTMLCollection()
    public open override /*1*/ fun item(/*0*/ index: kotlin.Int): org.w3c.dom.Element?
    public final fun namedItem(/*0*/ name: kotlin.String): org.w3c.dom.Element?
}

public abstract external class HTMLDListElement : org.w3c.dom.HTMLElement {
    /*primary*/ public constructor HTMLDListElement()
    public open var compact: kotlin.Boolean
        public open fun <get-compact>(): kotlin.Boolean
        public open fun <set-compact>(/*0*/ <set-?>: kotlin.Boolean): kotlin.Unit

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

public abstract external class HTMLDataElement : org.w3c.dom.HTMLElement {
    /*primary*/ public constructor HTMLDataElement()
    public open var value: kotlin.String
        public open fun <get-value>(): kotlin.String
        public open fun <set-value>(/*0*/ <set-?>: kotlin.String): kotlin.Unit

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

public abstract external class HTMLDataListElement : org.w3c.dom.HTMLElement {
    /*primary*/ public constructor HTMLDataListElement()
    public open val options: org.w3c.dom.HTMLCollection
        public open fun <get-options>(): org.w3c.dom.HTMLCollection

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

public abstract external class HTMLDetailsElement : org.w3c.dom.HTMLElement {
    /*primary*/ public constructor HTMLDetailsElement()
    public open var open: kotlin.Boolean
        public open fun <get-open>(): kotlin.Boolean
        public open fun <set-open>(/*0*/ <set-?>: kotlin.Boolean): kotlin.Unit

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

public abstract external class HTMLDialogElement : org.w3c.dom.HTMLElement {
    /*primary*/ public constructor HTMLDialogElement()
    public open var open: kotlin.Boolean
        public open fun <get-open>(): kotlin.Boolean
        public open fun <set-open>(/*0*/ <set-?>: kotlin.Boolean): kotlin.Unit
    public open var returnValue: kotlin.String
        public open fun <get-returnValue>(): kotlin.String
        public open fun <set-returnValue>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public final fun close(/*0*/ returnValue: kotlin.String = ...): kotlin.Unit
    public final fun show(/*0*/ anchor: org.w3c.dom.UnionElementOrMouseEvent = ...): kotlin.Unit
    public final fun showModal(/*0*/ anchor: org.w3c.dom.UnionElementOrMouseEvent = ...): kotlin.Unit

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

public abstract external class HTMLDirectoryElement : org.w3c.dom.HTMLElement {
    /*primary*/ public constructor HTMLDirectoryElement()
    public open var compact: kotlin.Boolean
        public open fun <get-compact>(): kotlin.Boolean
        public open fun <set-compact>(/*0*/ <set-?>: kotlin.Boolean): kotlin.Unit

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

public abstract external class HTMLDivElement : org.w3c.dom.HTMLElement {
    /*primary*/ public constructor HTMLDivElement()
    public open var align: kotlin.String
        public open fun <get-align>(): kotlin.String
        public open fun <set-align>(/*0*/ <set-?>: kotlin.String): kotlin.Unit

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

public abstract external class HTMLElement : org.w3c.dom.Element, org.w3c.dom.GlobalEventHandlers, org.w3c.dom.DocumentAndElementEventHandlers, org.w3c.dom.ElementContentEditable, org.w3c.dom.css.ElementCSSInlineStyle {
    /*primary*/ public constructor HTMLElement()
    public open var accessKey: kotlin.String
        public open fun <get-accessKey>(): kotlin.String
        public open fun <set-accessKey>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open val accessKeyLabel: kotlin.String
        public open fun <get-accessKeyLabel>(): kotlin.String
    public open var contextMenu: org.w3c.dom.HTMLMenuElement?
        public open fun <get-contextMenu>(): org.w3c.dom.HTMLMenuElement?
        public open fun <set-contextMenu>(/*0*/ <set-?>: org.w3c.dom.HTMLMenuElement?): kotlin.Unit
    public open val dataset: org.w3c.dom.DOMStringMap
        public open fun <get-dataset>(): org.w3c.dom.DOMStringMap
    public open var dir: kotlin.String
        public open fun <get-dir>(): kotlin.String
        public open fun <set-dir>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var draggable: kotlin.Boolean
        public open fun <get-draggable>(): kotlin.Boolean
        public open fun <set-draggable>(/*0*/ <set-?>: kotlin.Boolean): kotlin.Unit
    public open val dropzone: org.w3c.dom.DOMTokenList
        public open fun <get-dropzone>(): org.w3c.dom.DOMTokenList
    public open var hidden: kotlin.Boolean
        public open fun <get-hidden>(): kotlin.Boolean
        public open fun <set-hidden>(/*0*/ <set-?>: kotlin.Boolean): kotlin.Unit
    public open var innerText: kotlin.String
        public open fun <get-innerText>(): kotlin.String
        public open fun <set-innerText>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var lang: kotlin.String
        public open fun <get-lang>(): kotlin.String
        public open fun <set-lang>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open val offsetHeight: kotlin.Int
        public open fun <get-offsetHeight>(): kotlin.Int
    public open val offsetLeft: kotlin.Int
        public open fun <get-offsetLeft>(): kotlin.Int
    public open val offsetParent: org.w3c.dom.Element?
        public open fun <get-offsetParent>(): org.w3c.dom.Element?
    public open val offsetTop: kotlin.Int
        public open fun <get-offsetTop>(): kotlin.Int
    public open val offsetWidth: kotlin.Int
        public open fun <get-offsetWidth>(): kotlin.Int
    public open var spellcheck: kotlin.Boolean
        public open fun <get-spellcheck>(): kotlin.Boolean
        public open fun <set-spellcheck>(/*0*/ <set-?>: kotlin.Boolean): kotlin.Unit
    public open var tabIndex: kotlin.Int
        public open fun <get-tabIndex>(): kotlin.Int
        public open fun <set-tabIndex>(/*0*/ <set-?>: kotlin.Int): kotlin.Unit
    public open var title: kotlin.String
        public open fun <get-title>(): kotlin.String
        public open fun <set-title>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var translate: kotlin.Boolean
        public open fun <get-translate>(): kotlin.Boolean
        public open fun <set-translate>(/*0*/ <set-?>: kotlin.Boolean): kotlin.Unit
    public final fun blur(): kotlin.Unit
    public final fun click(): kotlin.Unit
    public final fun focus(): kotlin.Unit
    public final fun forceSpellCheck(): kotlin.Unit

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

public abstract external class HTMLEmbedElement : org.w3c.dom.HTMLElement {
    /*primary*/ public constructor HTMLEmbedElement()
    public open var align: kotlin.String
        public open fun <get-align>(): kotlin.String
        public open fun <set-align>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var height: kotlin.String
        public open fun <get-height>(): kotlin.String
        public open fun <set-height>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var name: kotlin.String
        public open fun <get-name>(): kotlin.String
        public open fun <set-name>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var src: kotlin.String
        public open fun <get-src>(): kotlin.String
        public open fun <set-src>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var type: kotlin.String
        public open fun <get-type>(): kotlin.String
        public open fun <set-type>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var width: kotlin.String
        public open fun <get-width>(): kotlin.String
        public open fun <set-width>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public final fun getSVGDocument(): org.w3c.dom.Document?

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

public abstract external class HTMLFieldSetElement : org.w3c.dom.HTMLElement {
    /*primary*/ public constructor HTMLFieldSetElement()
    public open var disabled: kotlin.Boolean
        public open fun <get-disabled>(): kotlin.Boolean
        public open fun <set-disabled>(/*0*/ <set-?>: kotlin.Boolean): kotlin.Unit
    public open val elements: org.w3c.dom.HTMLCollection
        public open fun <get-elements>(): org.w3c.dom.HTMLCollection
    public open val form: org.w3c.dom.HTMLFormElement?
        public open fun <get-form>(): org.w3c.dom.HTMLFormElement?
    public open var name: kotlin.String
        public open fun <get-name>(): kotlin.String
        public open fun <set-name>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open val type: kotlin.String
        public open fun <get-type>(): kotlin.String
    public open val validationMessage: kotlin.String
        public open fun <get-validationMessage>(): kotlin.String
    public open val validity: org.w3c.dom.ValidityState
        public open fun <get-validity>(): org.w3c.dom.ValidityState
    public open val willValidate: kotlin.Boolean
        public open fun <get-willValidate>(): kotlin.Boolean
    public final fun checkValidity(): kotlin.Boolean
    public final fun reportValidity(): kotlin.Boolean
    public final fun setCustomValidity(/*0*/ error: kotlin.String): kotlin.Unit

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

public abstract external class HTMLFontElement : org.w3c.dom.HTMLElement {
    /*primary*/ public constructor HTMLFontElement()
    public open var color: kotlin.String
        public open fun <get-color>(): kotlin.String
        public open fun <set-color>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var face: kotlin.String
        public open fun <get-face>(): kotlin.String
        public open fun <set-face>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var size: kotlin.String
        public open fun <get-size>(): kotlin.String
        public open fun <set-size>(/*0*/ <set-?>: kotlin.String): kotlin.Unit

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

public abstract external class HTMLFormControlsCollection : org.w3c.dom.HTMLCollection {
    /*primary*/ public constructor HTMLFormControlsCollection()
}

public abstract external class HTMLFormElement : org.w3c.dom.HTMLElement {
    /*primary*/ public constructor HTMLFormElement()
    public open var acceptCharset: kotlin.String
        public open fun <get-acceptCharset>(): kotlin.String
        public open fun <set-acceptCharset>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var action: kotlin.String
        public open fun <get-action>(): kotlin.String
        public open fun <set-action>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var autocomplete: kotlin.String
        public open fun <get-autocomplete>(): kotlin.String
        public open fun <set-autocomplete>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open val elements: org.w3c.dom.HTMLFormControlsCollection
        public open fun <get-elements>(): org.w3c.dom.HTMLFormControlsCollection
    public open var encoding: kotlin.String
        public open fun <get-encoding>(): kotlin.String
        public open fun <set-encoding>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var enctype: kotlin.String
        public open fun <get-enctype>(): kotlin.String
        public open fun <set-enctype>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open val length: kotlin.Int
        public open fun <get-length>(): kotlin.Int
    public open var method: kotlin.String
        public open fun <get-method>(): kotlin.String
        public open fun <set-method>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var name: kotlin.String
        public open fun <get-name>(): kotlin.String
        public open fun <set-name>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var noValidate: kotlin.Boolean
        public open fun <get-noValidate>(): kotlin.Boolean
        public open fun <set-noValidate>(/*0*/ <set-?>: kotlin.Boolean): kotlin.Unit
    public open var target: kotlin.String
        public open fun <get-target>(): kotlin.String
        public open fun <set-target>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public final fun checkValidity(): kotlin.Boolean
    public final fun reportValidity(): kotlin.Boolean
    public final fun reset(): kotlin.Unit
    public final fun submit(): kotlin.Unit

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

public abstract external class HTMLFrameElement : org.w3c.dom.HTMLElement {
    /*primary*/ public constructor HTMLFrameElement()
    public open val contentDocument: org.w3c.dom.Document?
        public open fun <get-contentDocument>(): org.w3c.dom.Document?
    public open val contentWindow: org.w3c.dom.Window?
        public open fun <get-contentWindow>(): org.w3c.dom.Window?
    public open var frameBorder: kotlin.String
        public open fun <get-frameBorder>(): kotlin.String
        public open fun <set-frameBorder>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var longDesc: kotlin.String
        public open fun <get-longDesc>(): kotlin.String
        public open fun <set-longDesc>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var marginHeight: kotlin.String
        public open fun <get-marginHeight>(): kotlin.String
        public open fun <set-marginHeight>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var marginWidth: kotlin.String
        public open fun <get-marginWidth>(): kotlin.String
        public open fun <set-marginWidth>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var name: kotlin.String
        public open fun <get-name>(): kotlin.String
        public open fun <set-name>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var noResize: kotlin.Boolean
        public open fun <get-noResize>(): kotlin.Boolean
        public open fun <set-noResize>(/*0*/ <set-?>: kotlin.Boolean): kotlin.Unit
    public open var scrolling: kotlin.String
        public open fun <get-scrolling>(): kotlin.String
        public open fun <set-scrolling>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var src: kotlin.String
        public open fun <get-src>(): kotlin.String
        public open fun <set-src>(/*0*/ <set-?>: kotlin.String): kotlin.Unit

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

public abstract external class HTMLFrameSetElement : org.w3c.dom.HTMLElement, org.w3c.dom.WindowEventHandlers {
    /*primary*/ public constructor HTMLFrameSetElement()
    public open var cols: kotlin.String
        public open fun <get-cols>(): kotlin.String
        public open fun <set-cols>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var rows: kotlin.String
        public open fun <get-rows>(): kotlin.String
        public open fun <set-rows>(/*0*/ <set-?>: kotlin.String): kotlin.Unit

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
