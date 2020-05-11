public external interface CanvasTextDrawingStyles {
    public abstract var direction: org.w3c.dom.CanvasDirection
        public abstract fun <get-direction>(): org.w3c.dom.CanvasDirection
        public abstract fun <set-direction>(/*0*/ <set-?>: org.w3c.dom.CanvasDirection): kotlin.Unit
    public abstract var font: kotlin.String
        public abstract fun <get-font>(): kotlin.String
        public abstract fun <set-font>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public abstract var textAlign: org.w3c.dom.CanvasTextAlign
        public abstract fun <get-textAlign>(): org.w3c.dom.CanvasTextAlign
        public abstract fun <set-textAlign>(/*0*/ <set-?>: org.w3c.dom.CanvasTextAlign): kotlin.Unit
    public abstract var textBaseline: org.w3c.dom.CanvasTextBaseline
        public abstract fun <get-textBaseline>(): org.w3c.dom.CanvasTextBaseline
        public abstract fun <set-textBaseline>(/*0*/ <set-?>: org.w3c.dom.CanvasTextBaseline): kotlin.Unit
}

public external interface CanvasTransform {
    public abstract fun getTransform(): org.w3c.dom.DOMMatrix
    public abstract fun resetTransform(): kotlin.Unit
    public abstract fun rotate(/*0*/ angle: kotlin.Double): kotlin.Unit
    public abstract fun scale(/*0*/ x: kotlin.Double, /*1*/ y: kotlin.Double): kotlin.Unit
    public abstract fun setTransform(/*0*/ transform: dynamic = ...): kotlin.Unit
    public abstract fun setTransform(/*0*/ a: kotlin.Double, /*1*/ b: kotlin.Double, /*2*/ c: kotlin.Double, /*3*/ d: kotlin.Double, /*4*/ e: kotlin.Double, /*5*/ f: kotlin.Double): kotlin.Unit
    public abstract fun transform(/*0*/ a: kotlin.Double, /*1*/ b: kotlin.Double, /*2*/ c: kotlin.Double, /*3*/ d: kotlin.Double, /*4*/ e: kotlin.Double, /*5*/ f: kotlin.Double): kotlin.Unit
    public abstract fun translate(/*0*/ x: kotlin.Double, /*1*/ y: kotlin.Double): kotlin.Unit
}

public external interface CanvasUserInterface {
    public abstract fun drawFocusIfNeeded(/*0*/ element: org.w3c.dom.Element): kotlin.Unit
    public abstract fun drawFocusIfNeeded(/*0*/ path: org.w3c.dom.Path2D, /*1*/ element: org.w3c.dom.Element): kotlin.Unit
    public abstract fun scrollPathIntoView(): kotlin.Unit
    public abstract fun scrollPathIntoView(/*0*/ path: org.w3c.dom.Path2D): kotlin.Unit
}

public abstract external class CaretPosition {
    /*primary*/ public constructor CaretPosition()
    public open val offset: kotlin.Int
        public open fun <get-offset>(): kotlin.Int
    public open val offsetNode: org.w3c.dom.Node
        public open fun <get-offsetNode>(): org.w3c.dom.Node
    public final fun getClientRect(): org.w3c.dom.DOMRect?
}

public abstract external class CharacterData : org.w3c.dom.Node, org.w3c.dom.NonDocumentTypeChildNode, org.w3c.dom.ChildNode {
    /*primary*/ public constructor CharacterData()
    public open var data: kotlin.String
        public open fun <get-data>(): kotlin.String
        public open fun <set-data>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open val length: kotlin.Int
        public open fun <get-length>(): kotlin.Int
    public final fun appendData(/*0*/ data: kotlin.String): kotlin.Unit
    public final fun deleteData(/*0*/ offset: kotlin.Int, /*1*/ count: kotlin.Int): kotlin.Unit
    public final fun insertData(/*0*/ offset: kotlin.Int, /*1*/ data: kotlin.String): kotlin.Unit
    public final fun replaceData(/*0*/ offset: kotlin.Int, /*1*/ count: kotlin.Int, /*2*/ data: kotlin.String): kotlin.Unit
    public final fun substringData(/*0*/ offset: kotlin.Int, /*1*/ count: kotlin.Int): kotlin.String

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

public external interface ChildNode {
    public abstract fun after(/*0*/ vararg nodes: dynamic /*kotlin.Array<out dynamic>*/): kotlin.Unit
    public abstract fun before(/*0*/ vararg nodes: dynamic /*kotlin.Array<out dynamic>*/): kotlin.Unit
    public abstract fun remove(): kotlin.Unit
    public abstract fun replaceWith(/*0*/ vararg nodes: dynamic /*kotlin.Array<out dynamic>*/): kotlin.Unit
}

public open external class CloseEvent : org.w3c.dom.events.Event {
    /*primary*/ public constructor CloseEvent(/*0*/ type: kotlin.String, /*1*/ eventInitDict: org.w3c.dom.CloseEventInit = ...)
    public open val code: kotlin.Short
        public open fun <get-code>(): kotlin.Short
    public open val reason: kotlin.String
        public open fun <get-reason>(): kotlin.String
    public open val wasClean: kotlin.Boolean
        public open fun <get-wasClean>(): kotlin.Boolean

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

public external interface CloseEventInit : org.w3c.dom.EventInit {
    public open var code: kotlin.Short?
        public open fun <get-code>(): kotlin.Short?
        public open fun <set-code>(/*0*/ value: kotlin.Short?): kotlin.Unit
    public open var reason: kotlin.String?
        public open fun <get-reason>(): kotlin.String?
        public open fun <set-reason>(/*0*/ value: kotlin.String?): kotlin.Unit
    public open var wasClean: kotlin.Boolean?
        public open fun <get-wasClean>(): kotlin.Boolean?
        public open fun <set-wasClean>(/*0*/ value: kotlin.Boolean?): kotlin.Unit
}

public external interface ColorSpaceConversion {

    public companion object Companion {
    }
}

public open external class Comment : org.w3c.dom.CharacterData {
    /*primary*/ public constructor Comment(/*0*/ data: kotlin.String = ...)
    public open override /*1*/ val nextElementSibling: org.w3c.dom.Element?
        public open override /*1*/ fun <get-nextElementSibling>(): org.w3c.dom.Element?
    public open override /*1*/ val previousElementSibling: org.w3c.dom.Element?
        public open override /*1*/ fun <get-previousElementSibling>(): org.w3c.dom.Element?
    public open override /*1*/ fun after(/*0*/ vararg nodes: dynamic /*kotlin.Array<out dynamic>*/): kotlin.Unit
    public open override /*1*/ fun before(/*0*/ vararg nodes: dynamic /*kotlin.Array<out dynamic>*/): kotlin.Unit
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

public external interface ConvertCoordinateOptions {
    public open var fromBox: org.w3c.dom.CSSBoxType?
        public open fun <get-fromBox>(): org.w3c.dom.CSSBoxType?
        public open fun <set-fromBox>(/*0*/ value: org.w3c.dom.CSSBoxType?): kotlin.Unit
    public open var toBox: org.w3c.dom.CSSBoxType?
        public open fun <get-toBox>(): org.w3c.dom.CSSBoxType?
        public open fun <set-toBox>(/*0*/ value: org.w3c.dom.CSSBoxType?): kotlin.Unit
}

public abstract external class CustomElementRegistry {
    /*primary*/ public constructor CustomElementRegistry()
    public final fun define(/*0*/ name: kotlin.String, /*1*/ constructor: () -> dynamic, /*2*/ options: org.w3c.dom.ElementDefinitionOptions = ...): kotlin.Unit
    public final fun get(/*0*/ name: kotlin.String): kotlin.Any?
    public final fun whenDefined(/*0*/ name: kotlin.String): kotlin.js.Promise<kotlin.Unit>
}

public open external class CustomEvent : org.w3c.dom.events.Event {
    /*primary*/ public constructor CustomEvent(/*0*/ type: kotlin.String, /*1*/ eventInitDict: org.w3c.dom.CustomEventInit = ...)
    public open val detail: kotlin.Any?
        public open fun <get-detail>(): kotlin.Any?
    public final fun initCustomEvent(/*0*/ type: kotlin.String, /*1*/ bubbles: kotlin.Boolean, /*2*/ cancelable: kotlin.Boolean, /*3*/ detail: kotlin.Any?): kotlin.Unit

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

public external interface CustomEventInit : org.w3c.dom.EventInit {
    public open var detail: kotlin.Any?
        public open fun <get-detail>(): kotlin.Any?
        public open fun <set-detail>(/*0*/ value: kotlin.Any?): kotlin.Unit
}

public abstract external class DOMImplementation {
    /*primary*/ public constructor DOMImplementation()
    public final fun createDocument(/*0*/ namespace: kotlin.String?, /*1*/ qualifiedName: kotlin.String, /*2*/ doctype: org.w3c.dom.DocumentType? = ...): org.w3c.dom.XMLDocument
    public final fun createDocumentType(/*0*/ qualifiedName: kotlin.String, /*1*/ publicId: kotlin.String, /*2*/ systemId: kotlin.String): org.w3c.dom.DocumentType
    public final fun createHTMLDocument(/*0*/ title: kotlin.String = ...): org.w3c.dom.Document
    public final fun hasFeature(): kotlin.Boolean
}

public open external class DOMMatrix : org.w3c.dom.DOMMatrixReadOnly {
    /*primary*/ public constructor DOMMatrix()
    public constructor DOMMatrix(/*0*/ numberSequence: kotlin.Array<kotlin.Double>)
    public constructor DOMMatrix(/*0*/ transformList: kotlin.String)
    public constructor DOMMatrix(/*0*/ array32: org.khronos.webgl.Float32Array)
    public constructor DOMMatrix(/*0*/ array64: org.khronos.webgl.Float64Array)
    public constructor DOMMatrix(/*0*/ other: org.w3c.dom.DOMMatrixReadOnly)
    public open override /*1*/ var a: kotlin.Double
        public open override /*1*/ fun <get-a>(): kotlin.Double
        public open fun <set-a>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public open override /*1*/ var b: kotlin.Double
        public open override /*1*/ fun <get-b>(): kotlin.Double
        public open fun <set-b>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public open override /*1*/ var c: kotlin.Double
        public open override /*1*/ fun <get-c>(): kotlin.Double
        public open fun <set-c>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public open override /*1*/ var d: kotlin.Double
        public open override /*1*/ fun <get-d>(): kotlin.Double
        public open fun <set-d>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public open override /*1*/ var e: kotlin.Double
        public open override /*1*/ fun <get-e>(): kotlin.Double
        public open fun <set-e>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public open override /*1*/ var f: kotlin.Double
        public open override /*1*/ fun <get-f>(): kotlin.Double
        public open fun <set-f>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public open override /*1*/ var m11: kotlin.Double
        public open override /*1*/ fun <get-m11>(): kotlin.Double
        public open fun <set-m11>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public open override /*1*/ var m12: kotlin.Double
        public open override /*1*/ fun <get-m12>(): kotlin.Double
        public open fun <set-m12>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public open override /*1*/ var m13: kotlin.Double
        public open override /*1*/ fun <get-m13>(): kotlin.Double
        public open fun <set-m13>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public open override /*1*/ var m14: kotlin.Double
        public open override /*1*/ fun <get-m14>(): kotlin.Double
        public open fun <set-m14>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public open override /*1*/ var m21: kotlin.Double
        public open override /*1*/ fun <get-m21>(): kotlin.Double
        public open fun <set-m21>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public open override /*1*/ var m22: kotlin.Double
        public open override /*1*/ fun <get-m22>(): kotlin.Double
        public open fun <set-m22>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public open override /*1*/ var m23: kotlin.Double
        public open override /*1*/ fun <get-m23>(): kotlin.Double
        public open fun <set-m23>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public open override /*1*/ var m24: kotlin.Double
        public open override /*1*/ fun <get-m24>(): kotlin.Double
        public open fun <set-m24>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public open override /*1*/ var m31: kotlin.Double
        public open override /*1*/ fun <get-m31>(): kotlin.Double
        public open fun <set-m31>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public open override /*1*/ var m32: kotlin.Double
        public open override /*1*/ fun <get-m32>(): kotlin.Double
        public open fun <set-m32>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public open override /*1*/ var m33: kotlin.Double
        public open override /*1*/ fun <get-m33>(): kotlin.Double
        public open fun <set-m33>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public open override /*1*/ var m34: kotlin.Double
        public open override /*1*/ fun <get-m34>(): kotlin.Double
        public open fun <set-m34>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public open override /*1*/ var m41: kotlin.Double
        public open override /*1*/ fun <get-m41>(): kotlin.Double
        public open fun <set-m41>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public open override /*1*/ var m42: kotlin.Double
        public open override /*1*/ fun <get-m42>(): kotlin.Double
        public open fun <set-m42>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public open override /*1*/ var m43: kotlin.Double
        public open override /*1*/ fun <get-m43>(): kotlin.Double
        public open fun <set-m43>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public open override /*1*/ var m44: kotlin.Double
        public open override /*1*/ fun <get-m44>(): kotlin.Double
        public open fun <set-m44>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public final fun invertSelf(): org.w3c.dom.DOMMatrix
    public final fun multiplySelf(/*0*/ other: org.w3c.dom.DOMMatrix): org.w3c.dom.DOMMatrix
    public final fun preMultiplySelf(/*0*/ other: org.w3c.dom.DOMMatrix): org.w3c.dom.DOMMatrix
    public final fun rotateAxisAngleSelf(/*0*/ x: kotlin.Double, /*1*/ y: kotlin.Double, /*2*/ z: kotlin.Double, /*3*/ angle: kotlin.Double): org.w3c.dom.DOMMatrix
    public final fun rotateFromVectorSelf(/*0*/ x: kotlin.Double, /*1*/ y: kotlin.Double): org.w3c.dom.DOMMatrix
    public final fun rotateSelf(/*0*/ angle: kotlin.Double, /*1*/ originX: kotlin.Double = ..., /*2*/ originY: kotlin.Double = ...): org.w3c.dom.DOMMatrix
    public final fun scale3dSelf(/*0*/ scale: kotlin.Double, /*1*/ originX: kotlin.Double = ..., /*2*/ originY: kotlin.Double = ..., /*3*/ originZ: kotlin.Double = ...): org.w3c.dom.DOMMatrix
    public final fun scaleNonUniformSelf(/*0*/ scaleX: kotlin.Double, /*1*/ scaleY: kotlin.Double = ..., /*2*/ scaleZ: kotlin.Double = ..., /*3*/ originX: kotlin.Double = ..., /*4*/ originY: kotlin.Double = ..., /*5*/ originZ: kotlin.Double = ...): org.w3c.dom.DOMMatrix
    public final fun scaleSelf(/*0*/ scale: kotlin.Double, /*1*/ originX: kotlin.Double = ..., /*2*/ originY: kotlin.Double = ...): org.w3c.dom.DOMMatrix
    public final fun setMatrixValue(/*0*/ transformList: kotlin.String): org.w3c.dom.DOMMatrix
    public final fun skewXSelf(/*0*/ sx: kotlin.Double): org.w3c.dom.DOMMatrix
    public final fun skewYSelf(/*0*/ sy: kotlin.Double): org.w3c.dom.DOMMatrix
    public final fun translateSelf(/*0*/ tx: kotlin.Double, /*1*/ ty: kotlin.Double, /*2*/ tz: kotlin.Double = ...): org.w3c.dom.DOMMatrix
}

public open external class DOMMatrixReadOnly {
    /*primary*/ public constructor DOMMatrixReadOnly(/*0*/ numberSequence: kotlin.Array<kotlin.Double>)
    public open val a: kotlin.Double
        public open fun <get-a>(): kotlin.Double
    public open val b: kotlin.Double
        public open fun <get-b>(): kotlin.Double
    public open val c: kotlin.Double
        public open fun <get-c>(): kotlin.Double
    public open val d: kotlin.Double
        public open fun <get-d>(): kotlin.Double
    public open val e: kotlin.Double
        public open fun <get-e>(): kotlin.Double
    public open val f: kotlin.Double
        public open fun <get-f>(): kotlin.Double
    public open val is2D: kotlin.Boolean
        public open fun <get-is2D>(): kotlin.Boolean
    public open val isIdentity: kotlin.Boolean
        public open fun <get-isIdentity>(): kotlin.Boolean
    public open val m11: kotlin.Double
        public open fun <get-m11>(): kotlin.Double
    public open val m12: kotlin.Double
        public open fun <get-m12>(): kotlin.Double
    public open val m13: kotlin.Double
        public open fun <get-m13>(): kotlin.Double
    public open val m14: kotlin.Double
        public open fun <get-m14>(): kotlin.Double
    public open val m21: kotlin.Double
        public open fun <get-m21>(): kotlin.Double
    public open val m22: kotlin.Double
        public open fun <get-m22>(): kotlin.Double
    public open val m23: kotlin.Double
        public open fun <get-m23>(): kotlin.Double
    public open val m24: kotlin.Double
        public open fun <get-m24>(): kotlin.Double
    public open val m31: kotlin.Double
        public open fun <get-m31>(): kotlin.Double
    public open val m32: kotlin.Double
        public open fun <get-m32>(): kotlin.Double
    public open val m33: kotlin.Double
        public open fun <get-m33>(): kotlin.Double
    public open val m34: kotlin.Double
        public open fun <get-m34>(): kotlin.Double
    public open val m41: kotlin.Double
        public open fun <get-m41>(): kotlin.Double
    public open val m42: kotlin.Double
        public open fun <get-m42>(): kotlin.Double
    public open val m43: kotlin.Double
        public open fun <get-m43>(): kotlin.Double
    public open val m44: kotlin.Double
        public open fun <get-m44>(): kotlin.Double
    public final fun flipX(): org.w3c.dom.DOMMatrix
    public final fun flipY(): org.w3c.dom.DOMMatrix
    public final fun inverse(): org.w3c.dom.DOMMatrix
    public final fun multiply(/*0*/ other: org.w3c.dom.DOMMatrix): org.w3c.dom.DOMMatrix
    public final fun rotate(/*0*/ angle: kotlin.Double, /*1*/ originX: kotlin.Double = ..., /*2*/ originY: kotlin.Double = ...): org.w3c.dom.DOMMatrix
    public final fun rotateAxisAngle(/*0*/ x: kotlin.Double, /*1*/ y: kotlin.Double, /*2*/ z: kotlin.Double, /*3*/ angle: kotlin.Double): org.w3c.dom.DOMMatrix
    public final fun rotateFromVector(/*0*/ x: kotlin.Double, /*1*/ y: kotlin.Double): org.w3c.dom.DOMMatrix
    public final fun scale(/*0*/ scale: kotlin.Double, /*1*/ originX: kotlin.Double = ..., /*2*/ originY: kotlin.Double = ...): org.w3c.dom.DOMMatrix
    public final fun scale3d(/*0*/ scale: kotlin.Double, /*1*/ originX: kotlin.Double = ..., /*2*/ originY: kotlin.Double = ..., /*3*/ originZ: kotlin.Double = ...): org.w3c.dom.DOMMatrix
    public final fun scaleNonUniform(/*0*/ scaleX: kotlin.Double, /*1*/ scaleY: kotlin.Double = ..., /*2*/ scaleZ: kotlin.Double = ..., /*3*/ originX: kotlin.Double = ..., /*4*/ originY: kotlin.Double = ..., /*5*/ originZ: kotlin.Double = ...): org.w3c.dom.DOMMatrix
    public final fun skewX(/*0*/ sx: kotlin.Double): org.w3c.dom.DOMMatrix
    public final fun skewY(/*0*/ sy: kotlin.Double): org.w3c.dom.DOMMatrix
    public final fun toFloat32Array(): org.khronos.webgl.Float32Array
    public final fun toFloat64Array(): org.khronos.webgl.Float64Array
    public final fun transformPoint(/*0*/ point: org.w3c.dom.DOMPointInit = ...): org.w3c.dom.DOMPoint
    public final fun translate(/*0*/ tx: kotlin.Double, /*1*/ ty: kotlin.Double, /*2*/ tz: kotlin.Double = ...): org.w3c.dom.DOMMatrix
}

public open external class DOMPoint : org.w3c.dom.DOMPointReadOnly {
    public constructor DOMPoint(/*0*/ x: kotlin.Double = ..., /*1*/ y: kotlin.Double = ..., /*2*/ z: kotlin.Double = ..., /*3*/ w: kotlin.Double = ...)
    public constructor DOMPoint(/*0*/ point: org.w3c.dom.DOMPointInit)
    public open override /*1*/ var w: kotlin.Double
        public open override /*1*/ fun <get-w>(): kotlin.Double
        public open fun <set-w>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public open override /*1*/ var x: kotlin.Double
        public open override /*1*/ fun <get-x>(): kotlin.Double
        public open fun <set-x>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public open override /*1*/ var y: kotlin.Double
        public open override /*1*/ fun <get-y>(): kotlin.Double
        public open fun <set-y>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public open override /*1*/ var z: kotlin.Double
        public open override /*1*/ fun <get-z>(): kotlin.Double
        public open fun <set-z>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
}

public external interface DOMPointInit {
    public open var w: kotlin.Double?
        public open fun <get-w>(): kotlin.Double?
        public open fun <set-w>(/*0*/ value: kotlin.Double?): kotlin.Unit
    public open var x: kotlin.Double?
        public open fun <get-x>(): kotlin.Double?
        public open fun <set-x>(/*0*/ value: kotlin.Double?): kotlin.Unit
    public open var y: kotlin.Double?
        public open fun <get-y>(): kotlin.Double?
        public open fun <set-y>(/*0*/ value: kotlin.Double?): kotlin.Unit
    public open var z: kotlin.Double?
        public open fun <get-z>(): kotlin.Double?
        public open fun <set-z>(/*0*/ value: kotlin.Double?): kotlin.Unit
}

public open external class DOMPointReadOnly {
    /*primary*/ public constructor DOMPointReadOnly(/*0*/ x: kotlin.Double, /*1*/ y: kotlin.Double, /*2*/ z: kotlin.Double, /*3*/ w: kotlin.Double)
    public open val w: kotlin.Double
        public open fun <get-w>(): kotlin.Double
    public open val x: kotlin.Double
        public open fun <get-x>(): kotlin.Double
    public open val y: kotlin.Double
        public open fun <get-y>(): kotlin.Double
    public open val z: kotlin.Double
        public open fun <get-z>(): kotlin.Double
    public final fun matrixTransform(/*0*/ matrix: org.w3c.dom.DOMMatrixReadOnly): org.w3c.dom.DOMPoint
}

public open external class DOMQuad {
    public constructor DOMQuad(/*0*/ p1: org.w3c.dom.DOMPointInit = ..., /*1*/ p2: org.w3c.dom.DOMPointInit = ..., /*2*/ p3: org.w3c.dom.DOMPointInit = ..., /*3*/ p4: org.w3c.dom.DOMPointInit = ...)
    public constructor DOMQuad(/*0*/ rect: org.w3c.dom.DOMRectInit)
    public open val bounds: org.w3c.dom.DOMRectReadOnly
        public open fun <get-bounds>(): org.w3c.dom.DOMRectReadOnly
    public open val p1: org.w3c.dom.DOMPoint
        public open fun <get-p1>(): org.w3c.dom.DOMPoint
    public open val p2: org.w3c.dom.DOMPoint
        public open fun <get-p2>(): org.w3c.dom.DOMPoint
    public open val p3: org.w3c.dom.DOMPoint
        public open fun <get-p3>(): org.w3c.dom.DOMPoint
    public open val p4: org.w3c.dom.DOMPoint
        public open fun <get-p4>(): org.w3c.dom.DOMPoint
}

public open external class DOMRect : org.w3c.dom.DOMRectReadOnly {
    /*primary*/ public constructor DOMRect(/*0*/ x: kotlin.Double = ..., /*1*/ y: kotlin.Double = ..., /*2*/ width: kotlin.Double = ..., /*3*/ height: kotlin.Double = ...)
    public open override /*1*/ var height: kotlin.Double
        public open override /*1*/ fun <get-height>(): kotlin.Double
        public open fun <set-height>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public open override /*1*/ var width: kotlin.Double
        public open override /*1*/ fun <get-width>(): kotlin.Double
        public open fun <set-width>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public open override /*1*/ var x: kotlin.Double
        public open override /*1*/ fun <get-x>(): kotlin.Double
        public open fun <set-x>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public open override /*1*/ var y: kotlin.Double
        public open override /*1*/ fun <get-y>(): kotlin.Double
        public open fun <set-y>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
}

public external interface DOMRectInit {
    public open var height: kotlin.Double?
        public open fun <get-height>(): kotlin.Double?
        public open fun <set-height>(/*0*/ value: kotlin.Double?): kotlin.Unit
    public open var width: kotlin.Double?
        public open fun <get-width>(): kotlin.Double?
        public open fun <set-width>(/*0*/ value: kotlin.Double?): kotlin.Unit
    public open var x: kotlin.Double?
        public open fun <get-x>(): kotlin.Double?
        public open fun <set-x>(/*0*/ value: kotlin.Double?): kotlin.Unit
    public open var y: kotlin.Double?
        public open fun <get-y>(): kotlin.Double?
        public open fun <set-y>(/*0*/ value: kotlin.Double?): kotlin.Unit
}

public external interface DOMRectList : org.w3c.dom.ItemArrayLike<org.w3c.dom.DOMRect> {
    public abstract override /*1*/ fun item(/*0*/ index: kotlin.Int): org.w3c.dom.DOMRect?
}

public open external class DOMRectReadOnly {
    /*primary*/ public constructor DOMRectReadOnly(/*0*/ x: kotlin.Double, /*1*/ y: kotlin.Double, /*2*/ width: kotlin.Double, /*3*/ height: kotlin.Double)
    public open val bottom: kotlin.Double
        public open fun <get-bottom>(): kotlin.Double
    public open val height: kotlin.Double
        public open fun <get-height>(): kotlin.Double
    public open val left: kotlin.Double
        public open fun <get-left>(): kotlin.Double
    public open val right: kotlin.Double
        public open fun <get-right>(): kotlin.Double
    public open val top: kotlin.Double
        public open fun <get-top>(): kotlin.Double
    public open val width: kotlin.Double
        public open fun <get-width>(): kotlin.Double
    public open val x: kotlin.Double
        public open fun <get-x>(): kotlin.Double
    public open val y: kotlin.Double
        public open fun <get-y>(): kotlin.Double
}

public abstract external class DOMStringMap {
    /*primary*/ public constructor DOMStringMap()
}

public abstract external class DOMTokenList : org.w3c.dom.ItemArrayLike<kotlin.String> {
    /*primary*/ public constructor DOMTokenList()
    public open var value: kotlin.String
        public open fun <get-value>(): kotlin.String
        public open fun <set-value>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public final fun add(/*0*/ vararg tokens: kotlin.String /*kotlin.Array<out kotlin.String>*/): kotlin.Unit
    public final fun contains(/*0*/ token: kotlin.String): kotlin.Boolean
    public open override /*1*/ fun item(/*0*/ index: kotlin.Int): kotlin.String?
    public final fun remove(/*0*/ vararg tokens: kotlin.String /*kotlin.Array<out kotlin.String>*/): kotlin.Unit
    public final fun replace(/*0*/ token: kotlin.String, /*1*/ newToken: kotlin.String): kotlin.Unit
    public final fun supports(/*0*/ token: kotlin.String): kotlin.Boolean
    public final fun toggle(/*0*/ token: kotlin.String, /*1*/ force: kotlin.Boolean = ...): kotlin.Boolean
}

public abstract external class DataTransfer {
    /*primary*/ public constructor DataTransfer()
    public open var dropEffect: kotlin.String
        public open fun <get-dropEffect>(): kotlin.String
        public open fun <set-dropEffect>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var effectAllowed: kotlin.String
        public open fun <get-effectAllowed>(): kotlin.String
        public open fun <set-effectAllowed>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open val files: org.w3c.files.FileList
        public open fun <get-files>(): org.w3c.files.FileList
    public open val items: org.w3c.dom.DataTransferItemList
        public open fun <get-items>(): org.w3c.dom.DataTransferItemList
    public open val types: kotlin.Array<out kotlin.String>
        public open fun <get-types>(): kotlin.Array<out kotlin.String>
    public final fun clearData(/*0*/ format: kotlin.String = ...): kotlin.Unit
    public final fun getData(/*0*/ format: kotlin.String): kotlin.String
    public final fun setData(/*0*/ format: kotlin.String, /*1*/ data: kotlin.String): kotlin.Unit
    public final fun setDragImage(/*0*/ image: org.w3c.dom.Element, /*1*/ x: kotlin.Int, /*2*/ y: kotlin.Int): kotlin.Unit
}

public abstract external class DataTransferItem {
    /*primary*/ public constructor DataTransferItem()
    public open val kind: kotlin.String
        public open fun <get-kind>(): kotlin.String
    public open val type: kotlin.String
        public open fun <get-type>(): kotlin.String
    public final fun getAsFile(): org.w3c.files.File?
    public final fun getAsString(/*0*/ _callback: ((kotlin.String) -> kotlin.Unit)?): kotlin.Unit
}

public abstract external class DataTransferItemList {
    /*primary*/ public constructor DataTransferItemList()
    public open val length: kotlin.Int
        public open fun <get-length>(): kotlin.Int
    public final fun add(/*0*/ data: kotlin.String, /*1*/ type: kotlin.String): org.w3c.dom.DataTransferItem?
    public final fun add(/*0*/ data: org.w3c.files.File): org.w3c.dom.DataTransferItem?
    public final fun clear(): kotlin.Unit
    public final fun remove(/*0*/ index: kotlin.Int): kotlin.Unit
}

public abstract external class DedicatedWorkerGlobalScope : org.w3c.dom.WorkerGlobalScope {
    /*primary*/ public constructor DedicatedWorkerGlobalScope()
    public open var onmessage: ((org.w3c.dom.MessageEvent) -> dynamic)?
        public open fun <get-onmessage>(): ((org.w3c.dom.MessageEvent) -> dynamic)?
        public open fun <set-onmessage>(/*0*/ <set-?>: ((org.w3c.dom.MessageEvent) -> dynamic)?): kotlin.Unit
    public final fun close(): kotlin.Unit
    public final fun postMessage(/*0*/ message: kotlin.Any?, /*1*/ transfer: kotlin.Array<dynamic> = ...): kotlin.Unit
}

public open external class Document : org.w3c.dom.Node, org.w3c.dom.GlobalEventHandlers, org.w3c.dom.DocumentAndElementEventHandlers, org.w3c.dom.NonElementParentNode, org.w3c.dom.DocumentOrShadowRoot, org.w3c.dom.ParentNode, org.w3c.dom.GeometryUtils {
    /*primary*/ public constructor Document()
    public open val URL: kotlin.String
        public open fun <get-URL>(): kotlin.String
    public open val activeElement: org.w3c.dom.Element?
        public open fun <get-activeElement>(): org.w3c.dom.Element?
    public final var alinkColor: kotlin.String
        public final fun <get-alinkColor>(): kotlin.String
        public final fun <set-alinkColor>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open val all: org.w3c.dom.HTMLAllCollection
        public open fun <get-all>(): org.w3c.dom.HTMLAllCollection
    public open val anchors: org.w3c.dom.HTMLCollection
        public open fun <get-anchors>(): org.w3c.dom.HTMLCollection
    public open val applets: org.w3c.dom.HTMLCollection
        public open fun <get-applets>(): org.w3c.dom.HTMLCollection
    public final var bgColor: kotlin.String
        public final fun <get-bgColor>(): kotlin.String
        public final fun <set-bgColor>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public final var body: org.w3c.dom.HTMLElement?
        public final fun <get-body>(): org.w3c.dom.HTMLElement?
        public final fun <set-body>(/*0*/ <set-?>: org.w3c.dom.HTMLElement?): kotlin.Unit
    public open val characterSet: kotlin.String
        public open fun <get-characterSet>(): kotlin.String
    public open val charset: kotlin.String
        public open fun <get-charset>(): kotlin.String
    public open override /*1*/ val childElementCount: kotlin.Int
        public open override /*1*/ fun <get-childElementCount>(): kotlin.Int
    public open override /*1*/ val children: org.w3c.dom.HTMLCollection
        public open override /*1*/ fun <get-children>(): org.w3c.dom.HTMLCollection
    public open val compatMode: kotlin.String
        public open fun <get-compatMode>(): kotlin.String
    public open val contentType: kotlin.String
        public open fun <get-contentType>(): kotlin.String
    public final var cookie: kotlin.String
        public final fun <get-cookie>(): kotlin.String
        public final fun <set-cookie>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open val currentScript: org.w3c.dom.HTMLOrSVGScriptElement?
        public open fun <get-currentScript>(): org.w3c.dom.HTMLOrSVGScriptElement?
    public open val defaultView: org.w3c.dom.Window?
        public open fun <get-defaultView>(): org.w3c.dom.Window?
    public final var designMode: kotlin.String
        public final fun <get-designMode>(): kotlin.String
        public final fun <set-designMode>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public final var dir: kotlin.String
        public final fun <get-dir>(): kotlin.String
        public final fun <set-dir>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open val doctype: org.w3c.dom.DocumentType?
        public open fun <get-doctype>(): org.w3c.dom.DocumentType?
    public open val documentElement: org.w3c.dom.Element?
        public open fun <get-documentElement>(): org.w3c.dom.Element?
    public open val documentURI: kotlin.String
        public open fun <get-documentURI>(): kotlin.String
    public final var domain: kotlin.String
        public final fun <get-domain>(): kotlin.String
        public final fun <set-domain>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open val embeds: org.w3c.dom.HTMLCollection
        public open fun <get-embeds>(): org.w3c.dom.HTMLCollection
    public final var fgColor: kotlin.String
        public final fun <get-fgColor>(): kotlin.String
        public final fun <set-fgColor>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open override /*1*/ val firstElementChild: org.w3c.dom.Element?
        public open override /*1*/ fun <get-firstElementChild>(): org.w3c.dom.Element?
    public open val forms: org.w3c.dom.HTMLCollection
        public open fun <get-forms>(): org.w3c.dom.HTMLCollection
    public open val fullscreen: kotlin.Boolean
        public open fun <get-fullscreen>(): kotlin.Boolean
    public open override /*1*/ val fullscreenElement: org.w3c.dom.Element?
        public open override /*1*/ fun <get-fullscreenElement>(): org.w3c.dom.Element?
    public open val fullscreenEnabled: kotlin.Boolean
        public open fun <get-fullscreenEnabled>(): kotlin.Boolean
    public open val head: org.w3c.dom.HTMLHeadElement?
        public open fun <get-head>(): org.w3c.dom.HTMLHeadElement?
    public open val images: org.w3c.dom.HTMLCollection
        public open fun <get-images>(): org.w3c.dom.HTMLCollection
    public open val implementation: org.w3c.dom.DOMImplementation
        public open fun <get-implementation>(): org.w3c.dom.DOMImplementation
    public open val inputEncoding: kotlin.String
        public open fun <get-inputEncoding>(): kotlin.String
    public open override /*1*/ val lastElementChild: org.w3c.dom.Element?
        public open override /*1*/ fun <get-lastElementChild>(): org.w3c.dom.Element?
    public open val lastModified: kotlin.String
        public open fun <get-lastModified>(): kotlin.String
    public final var linkColor: kotlin.String
        public final fun <get-linkColor>(): kotlin.String
        public final fun <set-linkColor>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open val links: org.w3c.dom.HTMLCollection
        public open fun <get-links>(): org.w3c.dom.HTMLCollection
    public open val location: org.w3c.dom.Location?
        public open fun <get-location>(): org.w3c.dom.Location?
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
    public final var onfullscreenchange: ((org.w3c.dom.events.Event) -> dynamic)?
        public final fun <get-onfullscreenchange>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public final fun <set-onfullscreenchange>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public final var onfullscreenerror: ((org.w3c.dom.events.Event) -> dynamic)?
        public final fun <get-onfullscreenerror>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public final fun <set-onfullscreenerror>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
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
    public final var onreadystatechange: ((org.w3c.dom.events.Event) -> dynamic)?
        public final fun <get-onreadystatechange>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public final fun <set-onreadystatechange>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
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
    public open val origin: kotlin.String
        public open fun <get-origin>(): kotlin.String
    public open val plugins: org.w3c.dom.HTMLCollection
        public open fun <get-plugins>(): org.w3c.dom.HTMLCollection
    public open val readyState: org.w3c.dom.DocumentReadyState
        public open fun <get-readyState>(): org.w3c.dom.DocumentReadyState
    public open val referrer: kotlin.String
        public open fun <get-referrer>(): kotlin.String
    public open val rootElement: org.w3c.dom.svg.SVGSVGElement?
        public open fun <get-rootElement>(): org.w3c.dom.svg.SVGSVGElement?
    public open val scripts: org.w3c.dom.HTMLCollection
        public open fun <get-scripts>(): org.w3c.dom.HTMLCollection
    public open val scrollingElement: org.w3c.dom.Element?
        public open fun <get-scrollingElement>(): org.w3c.dom.Element?
    public open val styleSheets: org.w3c.dom.css.StyleSheetList
        public open fun <get-styleSheets>(): org.w3c.dom.css.StyleSheetList
    public final var title: kotlin.String
        public final fun <get-title>(): kotlin.String
        public final fun <set-title>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public final var vlinkColor: kotlin.String
        public final fun <get-vlinkColor>(): kotlin.String
        public final fun <set-vlinkColor>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public final fun adoptNode(/*0*/ node: org.w3c.dom.Node): org.w3c.dom.Node
    public open override /*1*/ fun append(/*0*/ vararg nodes: dynamic /*kotlin.Array<out dynamic>*/): kotlin.Unit
    public final fun captureEvents(): kotlin.Unit
    public final fun caretPositionFromPoint(/*0*/ x: kotlin.Double, /*1*/ y: kotlin.Double): org.w3c.dom.CaretPosition?
    public final fun clear(): kotlin.Unit
    public final fun close(): kotlin.Unit
    public open override /*1*/ fun convertPointFromNode(/*0*/ point: org.w3c.dom.DOMPointInit, /*1*/ from: dynamic, /*2*/ options: org.w3c.dom.ConvertCoordinateOptions = ...): org.w3c.dom.DOMPoint
    public open override /*1*/ fun convertQuadFromNode(/*0*/ quad: dynamic, /*1*/ from: dynamic, /*2*/ options: org.w3c.dom.ConvertCoordinateOptions = ...): org.w3c.dom.DOMQuad
    public open override /*1*/ fun convertRectFromNode(/*0*/ rect: org.w3c.dom.DOMRectReadOnly, /*1*/ from: dynamic, /*2*/ options: org.w3c.dom.ConvertCoordinateOptions = ...): org.w3c.dom.DOMQuad
    public final fun createAttribute(/*0*/ localName: kotlin.String): org.w3c.dom.Attr
    public final fun createAttributeNS(/*0*/ namespace: kotlin.String?, /*1*/ qualifiedName: kotlin.String): org.w3c.dom.Attr
    public final fun createCDATASection(/*0*/ data: kotlin.String): org.w3c.dom.CDATASection
    public final fun createComment(/*0*/ data: kotlin.String): org.w3c.dom.Comment
    public final fun createDocumentFragment(): org.w3c.dom.DocumentFragment
    public final fun createElement(/*0*/ localName: kotlin.String, /*1*/ options: org.w3c.dom.ElementCreationOptions = ...): org.w3c.dom.Element
    public final fun createElementNS(/*0*/ namespace: kotlin.String?, /*1*/ qualifiedName: kotlin.String, /*2*/ options: org.w3c.dom.ElementCreationOptions = ...): org.w3c.dom.Element
    public final fun createEvent(/*0*/ `interface`: kotlin.String): org.w3c.dom.events.Event
    public final fun createNodeIterator(/*0*/ root: org.w3c.dom.Node, /*1*/ whatToShow: kotlin.Int = ..., /*2*/ filter: ((org.w3c.dom.Node) -> kotlin.Short)? = ...): org.w3c.dom.NodeIterator
    public final fun createNodeIterator(/*0*/ root: org.w3c.dom.Node, /*1*/ whatToShow: kotlin.Int = ..., /*2*/ filter: org.w3c.dom.NodeFilter? = ...): org.w3c.dom.NodeIterator
    public final fun createProcessingInstruction(/*0*/ target: kotlin.String, /*1*/ data: kotlin.String): org.w3c.dom.ProcessingInstruction
    public final fun createRange(): org.w3c.dom.Range
    public final fun createTextNode(/*0*/ data: kotlin.String): org.w3c.dom.Text
    public final fun createTouch(/*0*/ view: org.w3c.dom.Window, /*1*/ target: org.w3c.dom.events.EventTarget, /*2*/ identifier: kotlin.Int, /*3*/ pageX: kotlin.Int, /*4*/ pageY: kotlin.Int, /*5*/ screenX: kotlin.Int, /*6*/ screenY: kotlin.Int): org.w3c.dom.Touch
    public final fun createTouchList(/*0*/ vararg touches: org.w3c.dom.Touch /*kotlin.Array<out org.w3c.dom.Touch>*/): org.w3c.dom.TouchList
    public final fun createTreeWalker(/*0*/ root: org.w3c.dom.Node, /*1*/ whatToShow: kotlin.Int = ..., /*2*/ filter: ((org.w3c.dom.Node) -> kotlin.Short)? = ...): org.w3c.dom.TreeWalker
    public final fun createTreeWalker(/*0*/ root: org.w3c.dom.Node, /*1*/ whatToShow: kotlin.Int = ..., /*2*/ filter: org.w3c.dom.NodeFilter? = ...): org.w3c.dom.TreeWalker
    public final fun elementFromPoint(/*0*/ x: kotlin.Double, /*1*/ y: kotlin.Double): org.w3c.dom.Element?
    public final fun elementsFromPoint(/*0*/ x: kotlin.Double, /*1*/ y: kotlin.Double): kotlin.Array<org.w3c.dom.Element>
    public final fun execCommand(/*0*/ commandId: kotlin.String, /*1*/ showUI: kotlin.Boolean = ..., /*2*/ value: kotlin.String = ...): kotlin.Boolean
    public final fun exitFullscreen(): kotlin.js.Promise<kotlin.Unit>
    public open override /*1*/ fun getBoxQuads(/*0*/ options: org.w3c.dom.BoxQuadOptions = ...): kotlin.Array<org.w3c.dom.DOMQuad>
    public open override /*1*/ fun getElementById(/*0*/ elementId: kotlin.String): org.w3c.dom.Element?
    public final fun getElementsByClassName(/*0*/ classNames: kotlin.String): org.w3c.dom.HTMLCollection
    public final fun getElementsByName(/*0*/ elementName: kotlin.String): org.w3c.dom.NodeList
    public final fun getElementsByTagName(/*0*/ qualifiedName: kotlin.String): org.w3c.dom.HTMLCollection
    public final fun getElementsByTagNameNS(/*0*/ namespace: kotlin.String?, /*1*/ localName: kotlin.String): org.w3c.dom.HTMLCollection
    public final fun hasFocus(): kotlin.Boolean
    public final fun importNode(/*0*/ node: org.w3c.dom.Node, /*1*/ deep: kotlin.Boolean = ...): org.w3c.dom.Node
    public final fun open(/*0*/ type: kotlin.String = ..., /*1*/ replace: kotlin.String = ...): org.w3c.dom.Document
    public final fun open(/*0*/ url: kotlin.String, /*1*/ name: kotlin.String, /*2*/ features: kotlin.String): org.w3c.dom.Window
    public open override /*1*/ fun prepend(/*0*/ vararg nodes: dynamic /*kotlin.Array<out dynamic>*/): kotlin.Unit
    public final fun queryCommandEnabled(/*0*/ commandId: kotlin.String): kotlin.Boolean
    public final fun queryCommandIndeterm(/*0*/ commandId: kotlin.String): kotlin.Boolean
    public final fun queryCommandState(/*0*/ commandId: kotlin.String): kotlin.Boolean
    public final fun queryCommandSupported(/*0*/ commandId: kotlin.String): kotlin.Boolean
    public final fun queryCommandValue(/*0*/ commandId: kotlin.String): kotlin.String
    public open override /*1*/ fun querySelector(/*0*/ selectors: kotlin.String): org.w3c.dom.Element?
    public open override /*1*/ fun querySelectorAll(/*0*/ selectors: kotlin.String): org.w3c.dom.NodeList
    public final fun releaseEvents(): kotlin.Unit
    public final fun write(/*0*/ vararg text: kotlin.String /*kotlin.Array<out kotlin.String>*/): kotlin.Unit
    public final fun writeln(/*0*/ vararg text: kotlin.String /*kotlin.Array<out kotlin.String>*/): kotlin.Unit

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
