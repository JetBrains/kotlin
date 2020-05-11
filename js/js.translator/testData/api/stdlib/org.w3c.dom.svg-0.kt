package org.w3c.dom.svg

@kotlin.internal.InlineOnly public inline fun SVGBoundingBoxOptions(/*0*/ fill: kotlin.Boolean? = ..., /*1*/ stroke: kotlin.Boolean? = ..., /*2*/ markers: kotlin.Boolean? = ..., /*3*/ clipped: kotlin.Boolean? = ...): org.w3c.dom.svg.SVGBoundingBoxOptions
@kotlin.internal.InlineOnly public inline operator fun org.w3c.dom.svg.SVGLengthList.get(/*0*/ index: kotlin.Int): org.w3c.dom.svg.SVGLength?
@kotlin.internal.InlineOnly public inline operator fun org.w3c.dom.svg.SVGNameList.get(/*0*/ index: kotlin.Int): dynamic
@kotlin.internal.InlineOnly public inline operator fun org.w3c.dom.svg.SVGNumberList.get(/*0*/ index: kotlin.Int): org.w3c.dom.svg.SVGNumber?
@kotlin.internal.InlineOnly public inline operator fun org.w3c.dom.svg.SVGPointList.get(/*0*/ index: kotlin.Int): org.w3c.dom.DOMPoint?
@kotlin.internal.InlineOnly public inline operator fun org.w3c.dom.svg.SVGStringList.get(/*0*/ index: kotlin.Int): kotlin.String?
@kotlin.internal.InlineOnly public inline operator fun org.w3c.dom.svg.SVGTransformList.get(/*0*/ index: kotlin.Int): org.w3c.dom.svg.SVGTransform?
@kotlin.internal.InlineOnly public inline operator fun org.w3c.dom.svg.SVGLengthList.set(/*0*/ index: kotlin.Int, /*1*/ newItem: org.w3c.dom.svg.SVGLength): kotlin.Unit
@kotlin.internal.InlineOnly public inline operator fun org.w3c.dom.svg.SVGNameList.set(/*0*/ index: kotlin.Int, /*1*/ newItem: dynamic): kotlin.Unit
@kotlin.internal.InlineOnly public inline operator fun org.w3c.dom.svg.SVGNumberList.set(/*0*/ index: kotlin.Int, /*1*/ newItem: org.w3c.dom.svg.SVGNumber): kotlin.Unit
@kotlin.internal.InlineOnly public inline operator fun org.w3c.dom.svg.SVGPointList.set(/*0*/ index: kotlin.Int, /*1*/ newItem: org.w3c.dom.DOMPoint): kotlin.Unit
@kotlin.internal.InlineOnly public inline operator fun org.w3c.dom.svg.SVGStringList.set(/*0*/ index: kotlin.Int, /*1*/ newItem: kotlin.String): kotlin.Unit
@kotlin.internal.InlineOnly public inline operator fun org.w3c.dom.svg.SVGTransformList.set(/*0*/ index: kotlin.Int, /*1*/ newItem: org.w3c.dom.svg.SVGTransform): kotlin.Unit

public external interface GetSVGDocument {
    public abstract fun getSVGDocument(): org.w3c.dom.Document
}

public abstract external class SVGAElement : org.w3c.dom.svg.SVGGraphicsElement, org.w3c.dom.svg.SVGURIReference {
    /*primary*/ public constructor SVGAElement()
    public open val download: org.w3c.dom.svg.SVGAnimatedString
        public open fun <get-download>(): org.w3c.dom.svg.SVGAnimatedString
    public open val hreflang: org.w3c.dom.svg.SVGAnimatedString
        public open fun <get-hreflang>(): org.w3c.dom.svg.SVGAnimatedString
    public open val rel: org.w3c.dom.svg.SVGAnimatedString
        public open fun <get-rel>(): org.w3c.dom.svg.SVGAnimatedString
    public open val relList: org.w3c.dom.svg.SVGAnimatedString
        public open fun <get-relList>(): org.w3c.dom.svg.SVGAnimatedString
    public open val target: org.w3c.dom.svg.SVGAnimatedString
        public open fun <get-target>(): org.w3c.dom.svg.SVGAnimatedString
    public open val type: org.w3c.dom.svg.SVGAnimatedString
        public open fun <get-type>(): org.w3c.dom.svg.SVGAnimatedString

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

public abstract external class SVGAngle {
    /*primary*/ public constructor SVGAngle()
    public open val unitType: kotlin.Short
        public open fun <get-unitType>(): kotlin.Short
    public open var value: kotlin.Float
        public open fun <get-value>(): kotlin.Float
        public open fun <set-value>(/*0*/ <set-?>: kotlin.Float): kotlin.Unit
    public open var valueAsString: kotlin.String
        public open fun <get-valueAsString>(): kotlin.String
        public open fun <set-valueAsString>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var valueInSpecifiedUnits: kotlin.Float
        public open fun <get-valueInSpecifiedUnits>(): kotlin.Float
        public open fun <set-valueInSpecifiedUnits>(/*0*/ <set-?>: kotlin.Float): kotlin.Unit
    public final fun convertToSpecifiedUnits(/*0*/ unitType: kotlin.Short): kotlin.Unit
    public final fun newValueSpecifiedUnits(/*0*/ unitType: kotlin.Short, /*1*/ valueInSpecifiedUnits: kotlin.Float): kotlin.Unit

    public companion object Companion {
        public final val SVG_ANGLETYPE_DEG: kotlin.Short
            public final fun <get-SVG_ANGLETYPE_DEG>(): kotlin.Short
        public final val SVG_ANGLETYPE_GRAD: kotlin.Short
            public final fun <get-SVG_ANGLETYPE_GRAD>(): kotlin.Short
        public final val SVG_ANGLETYPE_RAD: kotlin.Short
            public final fun <get-SVG_ANGLETYPE_RAD>(): kotlin.Short
        public final val SVG_ANGLETYPE_UNKNOWN: kotlin.Short
            public final fun <get-SVG_ANGLETYPE_UNKNOWN>(): kotlin.Short
        public final val SVG_ANGLETYPE_UNSPECIFIED: kotlin.Short
            public final fun <get-SVG_ANGLETYPE_UNSPECIFIED>(): kotlin.Short
    }
}

public abstract external class SVGAnimatedAngle {
    /*primary*/ public constructor SVGAnimatedAngle()
    public open val animVal: org.w3c.dom.svg.SVGAngle
        public open fun <get-animVal>(): org.w3c.dom.svg.SVGAngle
    public open val baseVal: org.w3c.dom.svg.SVGAngle
        public open fun <get-baseVal>(): org.w3c.dom.svg.SVGAngle
}

public abstract external class SVGAnimatedBoolean {
    /*primary*/ public constructor SVGAnimatedBoolean()
    public open val animVal: kotlin.Boolean
        public open fun <get-animVal>(): kotlin.Boolean
    public open var baseVal: kotlin.Boolean
        public open fun <get-baseVal>(): kotlin.Boolean
        public open fun <set-baseVal>(/*0*/ <set-?>: kotlin.Boolean): kotlin.Unit
}

public abstract external class SVGAnimatedEnumeration {
    /*primary*/ public constructor SVGAnimatedEnumeration()
    public open val animVal: kotlin.Short
        public open fun <get-animVal>(): kotlin.Short
    public open var baseVal: kotlin.Short
        public open fun <get-baseVal>(): kotlin.Short
        public open fun <set-baseVal>(/*0*/ <set-?>: kotlin.Short): kotlin.Unit
}

public abstract external class SVGAnimatedInteger {
    /*primary*/ public constructor SVGAnimatedInteger()
    public open val animVal: kotlin.Int
        public open fun <get-animVal>(): kotlin.Int
    public open var baseVal: kotlin.Int
        public open fun <get-baseVal>(): kotlin.Int
        public open fun <set-baseVal>(/*0*/ <set-?>: kotlin.Int): kotlin.Unit
}

public abstract external class SVGAnimatedLength {
    /*primary*/ public constructor SVGAnimatedLength()
    public open val animVal: org.w3c.dom.svg.SVGLength
        public open fun <get-animVal>(): org.w3c.dom.svg.SVGLength
    public open val baseVal: org.w3c.dom.svg.SVGLength
        public open fun <get-baseVal>(): org.w3c.dom.svg.SVGLength
}

public abstract external class SVGAnimatedLengthList {
    /*primary*/ public constructor SVGAnimatedLengthList()
    public open val animVal: org.w3c.dom.svg.SVGLengthList
        public open fun <get-animVal>(): org.w3c.dom.svg.SVGLengthList
    public open val baseVal: org.w3c.dom.svg.SVGLengthList
        public open fun <get-baseVal>(): org.w3c.dom.svg.SVGLengthList
}

public abstract external class SVGAnimatedNumber {
    /*primary*/ public constructor SVGAnimatedNumber()
    public open val animVal: kotlin.Float
        public open fun <get-animVal>(): kotlin.Float
    public open var baseVal: kotlin.Float
        public open fun <get-baseVal>(): kotlin.Float
        public open fun <set-baseVal>(/*0*/ <set-?>: kotlin.Float): kotlin.Unit
}

public abstract external class SVGAnimatedNumberList {
    /*primary*/ public constructor SVGAnimatedNumberList()
    public open val animVal: org.w3c.dom.svg.SVGNumberList
        public open fun <get-animVal>(): org.w3c.dom.svg.SVGNumberList
    public open val baseVal: org.w3c.dom.svg.SVGNumberList
        public open fun <get-baseVal>(): org.w3c.dom.svg.SVGNumberList
}

public external interface SVGAnimatedPoints {
    public abstract val animatedPoints: org.w3c.dom.svg.SVGPointList
        public abstract fun <get-animatedPoints>(): org.w3c.dom.svg.SVGPointList
    public abstract val points: org.w3c.dom.svg.SVGPointList
        public abstract fun <get-points>(): org.w3c.dom.svg.SVGPointList
}

public abstract external class SVGAnimatedPreserveAspectRatio {
    /*primary*/ public constructor SVGAnimatedPreserveAspectRatio()
    public open val animVal: org.w3c.dom.svg.SVGPreserveAspectRatio
        public open fun <get-animVal>(): org.w3c.dom.svg.SVGPreserveAspectRatio
    public open val baseVal: org.w3c.dom.svg.SVGPreserveAspectRatio
        public open fun <get-baseVal>(): org.w3c.dom.svg.SVGPreserveAspectRatio
}

public abstract external class SVGAnimatedRect {
    /*primary*/ public constructor SVGAnimatedRect()
    public open val animVal: org.w3c.dom.DOMRectReadOnly
        public open fun <get-animVal>(): org.w3c.dom.DOMRectReadOnly
    public open val baseVal: org.w3c.dom.DOMRect
        public open fun <get-baseVal>(): org.w3c.dom.DOMRect
}

public abstract external class SVGAnimatedString {
    /*primary*/ public constructor SVGAnimatedString()
    public open val animVal: kotlin.String
        public open fun <get-animVal>(): kotlin.String
    public open var baseVal: kotlin.String
        public open fun <get-baseVal>(): kotlin.String
        public open fun <set-baseVal>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
}

public abstract external class SVGAnimatedTransformList {
    /*primary*/ public constructor SVGAnimatedTransformList()
    public open val animVal: org.w3c.dom.svg.SVGTransformList
        public open fun <get-animVal>(): org.w3c.dom.svg.SVGTransformList
    public open val baseVal: org.w3c.dom.svg.SVGTransformList
        public open fun <get-baseVal>(): org.w3c.dom.svg.SVGTransformList
}

public external interface SVGBoundingBoxOptions {
    public open var clipped: kotlin.Boolean?
        public open fun <get-clipped>(): kotlin.Boolean?
        public open fun <set-clipped>(/*0*/ value: kotlin.Boolean?): kotlin.Unit
    public open var fill: kotlin.Boolean?
        public open fun <get-fill>(): kotlin.Boolean?
        public open fun <set-fill>(/*0*/ value: kotlin.Boolean?): kotlin.Unit
    public open var markers: kotlin.Boolean?
        public open fun <get-markers>(): kotlin.Boolean?
        public open fun <set-markers>(/*0*/ value: kotlin.Boolean?): kotlin.Unit
    public open var stroke: kotlin.Boolean?
        public open fun <get-stroke>(): kotlin.Boolean?
        public open fun <set-stroke>(/*0*/ value: kotlin.Boolean?): kotlin.Unit
}

public abstract external class SVGCircleElement : org.w3c.dom.svg.SVGGeometryElement {
    /*primary*/ public constructor SVGCircleElement()
    public open val cx: org.w3c.dom.svg.SVGAnimatedLength
        public open fun <get-cx>(): org.w3c.dom.svg.SVGAnimatedLength
    public open val cy: org.w3c.dom.svg.SVGAnimatedLength
        public open fun <get-cy>(): org.w3c.dom.svg.SVGAnimatedLength
    public open val r: org.w3c.dom.svg.SVGAnimatedLength
        public open fun <get-r>(): org.w3c.dom.svg.SVGAnimatedLength

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

public abstract external class SVGCursorElement : org.w3c.dom.svg.SVGElement, org.w3c.dom.svg.SVGURIReference {
    /*primary*/ public constructor SVGCursorElement()
    public open val x: org.w3c.dom.svg.SVGAnimatedLength
        public open fun <get-x>(): org.w3c.dom.svg.SVGAnimatedLength
    public open val y: org.w3c.dom.svg.SVGAnimatedLength
        public open fun <get-y>(): org.w3c.dom.svg.SVGAnimatedLength

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

public abstract external class SVGDefsElement : org.w3c.dom.svg.SVGGraphicsElement {
    /*primary*/ public constructor SVGDefsElement()

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

public abstract external class SVGDescElement : org.w3c.dom.svg.SVGElement {
    /*primary*/ public constructor SVGDescElement()

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

public abstract external class SVGElement : org.w3c.dom.Element, org.w3c.dom.css.ElementCSSInlineStyle, org.w3c.dom.GlobalEventHandlers, org.w3c.dom.svg.SVGElementInstance {
    /*primary*/ public constructor SVGElement()
    public open val dataset: org.w3c.dom.DOMStringMap
        public open fun <get-dataset>(): org.w3c.dom.DOMStringMap
    public open val ownerSVGElement: org.w3c.dom.svg.SVGSVGElement?
        public open fun <get-ownerSVGElement>(): org.w3c.dom.svg.SVGSVGElement?
    public open var tabIndex: kotlin.Int
        public open fun <get-tabIndex>(): kotlin.Int
        public open fun <set-tabIndex>(/*0*/ <set-?>: kotlin.Int): kotlin.Unit
    public open val viewportElement: org.w3c.dom.svg.SVGElement?
        public open fun <get-viewportElement>(): org.w3c.dom.svg.SVGElement?
    public final fun blur(): kotlin.Unit
    public final fun focus(): kotlin.Unit

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

public external interface SVGElementInstance {
    public open val correspondingElement: org.w3c.dom.svg.SVGElement?
        public open fun <get-correspondingElement>(): org.w3c.dom.svg.SVGElement?
    public open val correspondingUseElement: org.w3c.dom.svg.SVGUseElement?
        public open fun <get-correspondingUseElement>(): org.w3c.dom.svg.SVGUseElement?
}

public abstract external class SVGEllipseElement : org.w3c.dom.svg.SVGGeometryElement {
    /*primary*/ public constructor SVGEllipseElement()
    public open val cx: org.w3c.dom.svg.SVGAnimatedLength
        public open fun <get-cx>(): org.w3c.dom.svg.SVGAnimatedLength
    public open val cy: org.w3c.dom.svg.SVGAnimatedLength
        public open fun <get-cy>(): org.w3c.dom.svg.SVGAnimatedLength
    public open val rx: org.w3c.dom.svg.SVGAnimatedLength
        public open fun <get-rx>(): org.w3c.dom.svg.SVGAnimatedLength
    public open val ry: org.w3c.dom.svg.SVGAnimatedLength
        public open fun <get-ry>(): org.w3c.dom.svg.SVGAnimatedLength

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

public external interface SVGFitToViewBox {
    public abstract val preserveAspectRatio: org.w3c.dom.svg.SVGAnimatedPreserveAspectRatio
        public abstract fun <get-preserveAspectRatio>(): org.w3c.dom.svg.SVGAnimatedPreserveAspectRatio
    public abstract val viewBox: org.w3c.dom.svg.SVGAnimatedRect
        public abstract fun <get-viewBox>(): org.w3c.dom.svg.SVGAnimatedRect
}

public abstract external class SVGForeignObjectElement : org.w3c.dom.svg.SVGGraphicsElement {
    /*primary*/ public constructor SVGForeignObjectElement()
    public open val height: org.w3c.dom.svg.SVGAnimatedLength
        public open fun <get-height>(): org.w3c.dom.svg.SVGAnimatedLength
    public open val width: org.w3c.dom.svg.SVGAnimatedLength
        public open fun <get-width>(): org.w3c.dom.svg.SVGAnimatedLength
    public open val x: org.w3c.dom.svg.SVGAnimatedLength
        public open fun <get-x>(): org.w3c.dom.svg.SVGAnimatedLength
    public open val y: org.w3c.dom.svg.SVGAnimatedLength
        public open fun <get-y>(): org.w3c.dom.svg.SVGAnimatedLength

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

public abstract external class SVGGElement : org.w3c.dom.svg.SVGGraphicsElement {
    /*primary*/ public constructor SVGGElement()

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

public abstract external class SVGGeometryElement : org.w3c.dom.svg.SVGGraphicsElement {
    /*primary*/ public constructor SVGGeometryElement()
    public open val pathLength: org.w3c.dom.svg.SVGAnimatedNumber
        public open fun <get-pathLength>(): org.w3c.dom.svg.SVGAnimatedNumber
    public final fun getPointAtLength(/*0*/ distance: kotlin.Float): org.w3c.dom.DOMPoint
    public final fun getTotalLength(): kotlin.Float
    public final fun isPointInFill(/*0*/ point: org.w3c.dom.DOMPoint): kotlin.Boolean
    public final fun isPointInStroke(/*0*/ point: org.w3c.dom.DOMPoint): kotlin.Boolean

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

public abstract external class SVGGradientElement : org.w3c.dom.svg.SVGElement, org.w3c.dom.svg.SVGURIReference, org.w3c.dom.svg.SVGUnitTypes {
    /*primary*/ public constructor SVGGradientElement()
    public open val gradientTransform: org.w3c.dom.svg.SVGAnimatedTransformList
        public open fun <get-gradientTransform>(): org.w3c.dom.svg.SVGAnimatedTransformList
    public open val gradientUnits: org.w3c.dom.svg.SVGAnimatedEnumeration
        public open fun <get-gradientUnits>(): org.w3c.dom.svg.SVGAnimatedEnumeration
    public open val spreadMethod: org.w3c.dom.svg.SVGAnimatedEnumeration
        public open fun <get-spreadMethod>(): org.w3c.dom.svg.SVGAnimatedEnumeration

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
        public final val SVG_SPREADMETHOD_PAD: kotlin.Short
            public final fun <get-SVG_SPREADMETHOD_PAD>(): kotlin.Short
        public final val SVG_SPREADMETHOD_REFLECT: kotlin.Short
            public final fun <get-SVG_SPREADMETHOD_REFLECT>(): kotlin.Short
        public final val SVG_SPREADMETHOD_REPEAT: kotlin.Short
            public final fun <get-SVG_SPREADMETHOD_REPEAT>(): kotlin.Short
        public final val SVG_SPREADMETHOD_UNKNOWN: kotlin.Short
            public final fun <get-SVG_SPREADMETHOD_UNKNOWN>(): kotlin.Short
        public final val SVG_UNIT_TYPE_OBJECTBOUNDINGBOX: kotlin.Short
            public final fun <get-SVG_UNIT_TYPE_OBJECTBOUNDINGBOX>(): kotlin.Short
        public final val SVG_UNIT_TYPE_UNKNOWN: kotlin.Short
            public final fun <get-SVG_UNIT_TYPE_UNKNOWN>(): kotlin.Short
        public final val SVG_UNIT_TYPE_USERSPACEONUSE: kotlin.Short
            public final fun <get-SVG_UNIT_TYPE_USERSPACEONUSE>(): kotlin.Short
        public final val TEXT_NODE: kotlin.Short
            public final fun <get-TEXT_NODE>(): kotlin.Short
    }
}

public abstract external class SVGGraphicsElement : org.w3c.dom.svg.SVGElement, org.w3c.dom.svg.SVGTests {
    /*primary*/ public constructor SVGGraphicsElement()
    public open val transform: org.w3c.dom.svg.SVGAnimatedTransformList
        public open fun <get-transform>(): org.w3c.dom.svg.SVGAnimatedTransformList
    public final fun getBBox(/*0*/ options: org.w3c.dom.svg.SVGBoundingBoxOptions = ...): org.w3c.dom.DOMRect
    public final fun getCTM(): org.w3c.dom.DOMMatrix?
    public final fun getScreenCTM(): org.w3c.dom.DOMMatrix?

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

public abstract external class SVGHatchElement : org.w3c.dom.svg.SVGElement {
    /*primary*/ public constructor SVGHatchElement()

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

public abstract external class SVGHatchpathElement : org.w3c.dom.svg.SVGElement {
    /*primary*/ public constructor SVGHatchpathElement()

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

public abstract external class SVGImageElement : org.w3c.dom.svg.SVGGraphicsElement, org.w3c.dom.svg.SVGURIReference, org.w3c.dom.HTMLOrSVGImageElement {
    /*primary*/ public constructor SVGImageElement()
    public open var crossOrigin: kotlin.String?
        public open fun <get-crossOrigin>(): kotlin.String?
        public open fun <set-crossOrigin>(/*0*/ <set-?>: kotlin.String?): kotlin.Unit
    public open val height: org.w3c.dom.svg.SVGAnimatedLength
        public open fun <get-height>(): org.w3c.dom.svg.SVGAnimatedLength
    public open val preserveAspectRatio: org.w3c.dom.svg.SVGAnimatedPreserveAspectRatio
        public open fun <get-preserveAspectRatio>(): org.w3c.dom.svg.SVGAnimatedPreserveAspectRatio
    public open val width: org.w3c.dom.svg.SVGAnimatedLength
        public open fun <get-width>(): org.w3c.dom.svg.SVGAnimatedLength
    public open val x: org.w3c.dom.svg.SVGAnimatedLength
        public open fun <get-x>(): org.w3c.dom.svg.SVGAnimatedLength
    public open val y: org.w3c.dom.svg.SVGAnimatedLength
        public open fun <get-y>(): org.w3c.dom.svg.SVGAnimatedLength

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

public abstract external class SVGLength {
    /*primary*/ public constructor SVGLength()
    public open val unitType: kotlin.Short
        public open fun <get-unitType>(): kotlin.Short
    public open var value: kotlin.Float
        public open fun <get-value>(): kotlin.Float
        public open fun <set-value>(/*0*/ <set-?>: kotlin.Float): kotlin.Unit
    public open var valueAsString: kotlin.String
        public open fun <get-valueAsString>(): kotlin.String
        public open fun <set-valueAsString>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var valueInSpecifiedUnits: kotlin.Float
        public open fun <get-valueInSpecifiedUnits>(): kotlin.Float
        public open fun <set-valueInSpecifiedUnits>(/*0*/ <set-?>: kotlin.Float): kotlin.Unit
    public final fun convertToSpecifiedUnits(/*0*/ unitType: kotlin.Short): kotlin.Unit
    public final fun newValueSpecifiedUnits(/*0*/ unitType: kotlin.Short, /*1*/ valueInSpecifiedUnits: kotlin.Float): kotlin.Unit

    public companion object Companion {
        public final val SVG_LENGTHTYPE_CM: kotlin.Short
            public final fun <get-SVG_LENGTHTYPE_CM>(): kotlin.Short
        public final val SVG_LENGTHTYPE_EMS: kotlin.Short
            public final fun <get-SVG_LENGTHTYPE_EMS>(): kotlin.Short
        public final val SVG_LENGTHTYPE_EXS: kotlin.Short
            public final fun <get-SVG_LENGTHTYPE_EXS>(): kotlin.Short
        public final val SVG_LENGTHTYPE_IN: kotlin.Short
            public final fun <get-SVG_LENGTHTYPE_IN>(): kotlin.Short
        public final val SVG_LENGTHTYPE_MM: kotlin.Short
            public final fun <get-SVG_LENGTHTYPE_MM>(): kotlin.Short
        public final val SVG_LENGTHTYPE_NUMBER: kotlin.Short
            public final fun <get-SVG_LENGTHTYPE_NUMBER>(): kotlin.Short
        public final val SVG_LENGTHTYPE_PC: kotlin.Short
            public final fun <get-SVG_LENGTHTYPE_PC>(): kotlin.Short
        public final val SVG_LENGTHTYPE_PERCENTAGE: kotlin.Short
            public final fun <get-SVG_LENGTHTYPE_PERCENTAGE>(): kotlin.Short
        public final val SVG_LENGTHTYPE_PT: kotlin.Short
            public final fun <get-SVG_LENGTHTYPE_PT>(): kotlin.Short
        public final val SVG_LENGTHTYPE_PX: kotlin.Short
            public final fun <get-SVG_LENGTHTYPE_PX>(): kotlin.Short
        public final val SVG_LENGTHTYPE_UNKNOWN: kotlin.Short
            public final fun <get-SVG_LENGTHTYPE_UNKNOWN>(): kotlin.Short
    }
}

public abstract external class SVGLengthList {
    /*primary*/ public constructor SVGLengthList()
    public open val length: kotlin.Int
        public open fun <get-length>(): kotlin.Int
    public open val numberOfItems: kotlin.Int
        public open fun <get-numberOfItems>(): kotlin.Int
    public final fun appendItem(/*0*/ newItem: org.w3c.dom.svg.SVGLength): org.w3c.dom.svg.SVGLength
    public final fun clear(): kotlin.Unit
    public final fun getItem(/*0*/ index: kotlin.Int): org.w3c.dom.svg.SVGLength
    public final fun initialize(/*0*/ newItem: org.w3c.dom.svg.SVGLength): org.w3c.dom.svg.SVGLength
    public final fun insertItemBefore(/*0*/ newItem: org.w3c.dom.svg.SVGLength, /*1*/ index: kotlin.Int): org.w3c.dom.svg.SVGLength
    public final fun removeItem(/*0*/ index: kotlin.Int): org.w3c.dom.svg.SVGLength
    public final fun replaceItem(/*0*/ newItem: org.w3c.dom.svg.SVGLength, /*1*/ index: kotlin.Int): org.w3c.dom.svg.SVGLength
}

public abstract external class SVGLineElement : org.w3c.dom.svg.SVGGeometryElement {
    /*primary*/ public constructor SVGLineElement()
    public open val x1: org.w3c.dom.svg.SVGAnimatedLength
        public open fun <get-x1>(): org.w3c.dom.svg.SVGAnimatedLength
    public open val x2: org.w3c.dom.svg.SVGAnimatedLength
        public open fun <get-x2>(): org.w3c.dom.svg.SVGAnimatedLength
    public open val y1: org.w3c.dom.svg.SVGAnimatedLength
        public open fun <get-y1>(): org.w3c.dom.svg.SVGAnimatedLength
    public open val y2: org.w3c.dom.svg.SVGAnimatedLength
        public open fun <get-y2>(): org.w3c.dom.svg.SVGAnimatedLength

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
