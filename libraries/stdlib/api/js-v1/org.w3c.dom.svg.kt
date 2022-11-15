/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun SVGBoundingBoxOptions(fill: kotlin.Boolean? = ..., stroke: kotlin.Boolean? = ..., markers: kotlin.Boolean? = ..., clipped: kotlin.Boolean? = ...): org.w3c.dom.svg.SVGBoundingBoxOptions
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline operator fun org.w3c.dom.svg.SVGLengthList.get(index: kotlin.Int): org.w3c.dom.svg.SVGLength?
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline operator fun org.w3c.dom.svg.SVGNameList.get(index: kotlin.Int): dynamic
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline operator fun org.w3c.dom.svg.SVGNumberList.get(index: kotlin.Int): org.w3c.dom.svg.SVGNumber?
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline operator fun org.w3c.dom.svg.SVGPointList.get(index: kotlin.Int): org.w3c.dom.DOMPoint?
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline operator fun org.w3c.dom.svg.SVGStringList.get(index: kotlin.Int): kotlin.String?
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline operator fun org.w3c.dom.svg.SVGTransformList.get(index: kotlin.Int): org.w3c.dom.svg.SVGTransform?
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline operator fun org.w3c.dom.svg.SVGLengthList.set(index: kotlin.Int, newItem: org.w3c.dom.svg.SVGLength): kotlin.Unit
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline operator fun org.w3c.dom.svg.SVGNameList.set(index: kotlin.Int, newItem: dynamic): kotlin.Unit
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline operator fun org.w3c.dom.svg.SVGNumberList.set(index: kotlin.Int, newItem: org.w3c.dom.svg.SVGNumber): kotlin.Unit
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline operator fun org.w3c.dom.svg.SVGPointList.set(index: kotlin.Int, newItem: org.w3c.dom.DOMPoint): kotlin.Unit
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline operator fun org.w3c.dom.svg.SVGStringList.set(index: kotlin.Int, newItem: kotlin.String): kotlin.Unit
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline operator fun org.w3c.dom.svg.SVGTransformList.set(index: kotlin.Int, newItem: org.w3c.dom.svg.SVGTransform): kotlin.Unit
/*∆*/ 
/*∆*/ public external interface GetSVGDocument {
/*∆*/     public abstract fun getSVGDocument(): org.w3c.dom.Document
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGAElement : org.w3c.dom.svg.SVGGraphicsElement, org.w3c.dom.svg.SVGURIReference {
/*∆*/     public constructor SVGAElement()
/*∆*/ 
/*∆*/     public open val download: org.w3c.dom.svg.SVGAnimatedString { get; }
/*∆*/ 
/*∆*/     public open val hreflang: org.w3c.dom.svg.SVGAnimatedString { get; }
/*∆*/ 
/*∆*/     public open val rel: org.w3c.dom.svg.SVGAnimatedString { get; }
/*∆*/ 
/*∆*/     public open val relList: org.w3c.dom.svg.SVGAnimatedString { get; }
/*∆*/ 
/*∆*/     public open val target: org.w3c.dom.svg.SVGAnimatedString { get; }
/*∆*/ 
/*∆*/     public open val type: org.w3c.dom.svg.SVGAnimatedString { get; }
/*∆*/ 
/*∆*/     public companion object of SVGAElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGAngle {
/*∆*/     public constructor SVGAngle()
/*∆*/ 
/*∆*/     public open val unitType: kotlin.Short { get; }
/*∆*/ 
/*∆*/     public open var value: kotlin.Float { get; set; }
/*∆*/ 
/*∆*/     public open var valueAsString: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var valueInSpecifiedUnits: kotlin.Float { get; set; }
/*∆*/ 
/*∆*/     public final fun convertToSpecifiedUnits(unitType: kotlin.Short): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun newValueSpecifiedUnits(unitType: kotlin.Short, valueInSpecifiedUnits: kotlin.Float): kotlin.Unit
/*∆*/ 
/*∆*/     public companion object of SVGAngle {
/*∆*/         public final val SVG_ANGLETYPE_DEG: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_ANGLETYPE_GRAD: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_ANGLETYPE_RAD: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_ANGLETYPE_UNKNOWN: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_ANGLETYPE_UNSPECIFIED: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGAnimatedAngle {
/*∆*/     public constructor SVGAnimatedAngle()
/*∆*/ 
/*∆*/     public open val animVal: org.w3c.dom.svg.SVGAngle { get; }
/*∆*/ 
/*∆*/     public open val baseVal: org.w3c.dom.svg.SVGAngle { get; }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGAnimatedBoolean {
/*∆*/     public constructor SVGAnimatedBoolean()
/*∆*/ 
/*∆*/     public open val animVal: kotlin.Boolean { get; }
/*∆*/ 
/*∆*/     public open var baseVal: kotlin.Boolean { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGAnimatedEnumeration {
/*∆*/     public constructor SVGAnimatedEnumeration()
/*∆*/ 
/*∆*/     public open val animVal: kotlin.Short { get; }
/*∆*/ 
/*∆*/     public open var baseVal: kotlin.Short { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGAnimatedInteger {
/*∆*/     public constructor SVGAnimatedInteger()
/*∆*/ 
/*∆*/     public open val animVal: kotlin.Int { get; }
/*∆*/ 
/*∆*/     public open var baseVal: kotlin.Int { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGAnimatedLength {
/*∆*/     public constructor SVGAnimatedLength()
/*∆*/ 
/*∆*/     public open val animVal: org.w3c.dom.svg.SVGLength { get; }
/*∆*/ 
/*∆*/     public open val baseVal: org.w3c.dom.svg.SVGLength { get; }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGAnimatedLengthList {
/*∆*/     public constructor SVGAnimatedLengthList()
/*∆*/ 
/*∆*/     public open val animVal: org.w3c.dom.svg.SVGLengthList { get; }
/*∆*/ 
/*∆*/     public open val baseVal: org.w3c.dom.svg.SVGLengthList { get; }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGAnimatedNumber {
/*∆*/     public constructor SVGAnimatedNumber()
/*∆*/ 
/*∆*/     public open val animVal: kotlin.Float { get; }
/*∆*/ 
/*∆*/     public open var baseVal: kotlin.Float { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGAnimatedNumberList {
/*∆*/     public constructor SVGAnimatedNumberList()
/*∆*/ 
/*∆*/     public open val animVal: org.w3c.dom.svg.SVGNumberList { get; }
/*∆*/ 
/*∆*/     public open val baseVal: org.w3c.dom.svg.SVGNumberList { get; }
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface SVGAnimatedPoints {
/*∆*/     public abstract val animatedPoints: org.w3c.dom.svg.SVGPointList { get; }
/*∆*/ 
/*∆*/     public abstract val points: org.w3c.dom.svg.SVGPointList { get; }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGAnimatedPreserveAspectRatio {
/*∆*/     public constructor SVGAnimatedPreserveAspectRatio()
/*∆*/ 
/*∆*/     public open val animVal: org.w3c.dom.svg.SVGPreserveAspectRatio { get; }
/*∆*/ 
/*∆*/     public open val baseVal: org.w3c.dom.svg.SVGPreserveAspectRatio { get; }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGAnimatedRect {
/*∆*/     public constructor SVGAnimatedRect()
/*∆*/ 
/*∆*/     public open val animVal: org.w3c.dom.DOMRectReadOnly { get; }
/*∆*/ 
/*∆*/     public open val baseVal: org.w3c.dom.DOMRect { get; }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGAnimatedString {
/*∆*/     public constructor SVGAnimatedString()
/*∆*/ 
/*∆*/     public open val animVal: kotlin.String { get; }
/*∆*/ 
/*∆*/     public open var baseVal: kotlin.String { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGAnimatedTransformList {
/*∆*/     public constructor SVGAnimatedTransformList()
/*∆*/ 
/*∆*/     public open val animVal: org.w3c.dom.svg.SVGTransformList { get; }
/*∆*/ 
/*∆*/     public open val baseVal: org.w3c.dom.svg.SVGTransformList { get; }
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface SVGBoundingBoxOptions {
/*∆*/     public open var clipped: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var fill: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var markers: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var stroke: kotlin.Boolean? { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGCircleElement : org.w3c.dom.svg.SVGGeometryElement {
/*∆*/     public constructor SVGCircleElement()
/*∆*/ 
/*∆*/     public open val cx: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public open val cy: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public open val r: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public companion object of SVGCircleElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGCursorElement : org.w3c.dom.svg.SVGElement, org.w3c.dom.svg.SVGURIReference {
/*∆*/     public constructor SVGCursorElement()
/*∆*/ 
/*∆*/     public open val x: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public open val y: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public companion object of SVGCursorElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGDefsElement : org.w3c.dom.svg.SVGGraphicsElement {
/*∆*/     public constructor SVGDefsElement()
/*∆*/ 
/*∆*/     public companion object of SVGDefsElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGDescElement : org.w3c.dom.svg.SVGElement {
/*∆*/     public constructor SVGDescElement()
/*∆*/ 
/*∆*/     public companion object of SVGDescElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGElement : org.w3c.dom.Element, org.w3c.dom.css.ElementCSSInlineStyle, org.w3c.dom.GlobalEventHandlers, org.w3c.dom.svg.SVGElementInstance {
/*∆*/     public constructor SVGElement()
/*∆*/ 
/*∆*/     public open val dataset: org.w3c.dom.DOMStringMap { get; }
/*∆*/ 
/*∆*/     public open val ownerSVGElement: org.w3c.dom.svg.SVGSVGElement? { get; }
/*∆*/ 
/*∆*/     public open var tabIndex: kotlin.Int { get; set; }
/*∆*/ 
/*∆*/     public open val viewportElement: org.w3c.dom.svg.SVGElement? { get; }
/*∆*/ 
/*∆*/     public final fun blur(): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun focus(): kotlin.Unit
/*∆*/ 
/*∆*/     public companion object of SVGElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface SVGElementInstance {
/*∆*/     public open val correspondingElement: org.w3c.dom.svg.SVGElement? { get; }
/*∆*/ 
/*∆*/     public open val correspondingUseElement: org.w3c.dom.svg.SVGUseElement? { get; }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGEllipseElement : org.w3c.dom.svg.SVGGeometryElement {
/*∆*/     public constructor SVGEllipseElement()
/*∆*/ 
/*∆*/     public open val cx: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public open val cy: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public open val rx: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public open val ry: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public companion object of SVGEllipseElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface SVGFitToViewBox {
/*∆*/     public abstract val preserveAspectRatio: org.w3c.dom.svg.SVGAnimatedPreserveAspectRatio { get; }
/*∆*/ 
/*∆*/     public abstract val viewBox: org.w3c.dom.svg.SVGAnimatedRect { get; }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGForeignObjectElement : org.w3c.dom.svg.SVGGraphicsElement {
/*∆*/     public constructor SVGForeignObjectElement()
/*∆*/ 
/*∆*/     public open val height: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public open val width: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public open val x: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public open val y: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public companion object of SVGForeignObjectElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGGElement : org.w3c.dom.svg.SVGGraphicsElement {
/*∆*/     public constructor SVGGElement()
/*∆*/ 
/*∆*/     public companion object of SVGGElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGGeometryElement : org.w3c.dom.svg.SVGGraphicsElement {
/*∆*/     public constructor SVGGeometryElement()
/*∆*/ 
/*∆*/     public open val pathLength: org.w3c.dom.svg.SVGAnimatedNumber { get; }
/*∆*/ 
/*∆*/     public final fun getPointAtLength(distance: kotlin.Float): org.w3c.dom.DOMPoint
/*∆*/ 
/*∆*/     public final fun getTotalLength(): kotlin.Float
/*∆*/ 
/*∆*/     public final fun isPointInFill(point: org.w3c.dom.DOMPoint): kotlin.Boolean
/*∆*/ 
/*∆*/     public final fun isPointInStroke(point: org.w3c.dom.DOMPoint): kotlin.Boolean
/*∆*/ 
/*∆*/     public companion object of SVGGeometryElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGGradientElement : org.w3c.dom.svg.SVGElement, org.w3c.dom.svg.SVGURIReference, org.w3c.dom.svg.SVGUnitTypes {
/*∆*/     public constructor SVGGradientElement()
/*∆*/ 
/*∆*/     public open val gradientTransform: org.w3c.dom.svg.SVGAnimatedTransformList { get; }
/*∆*/ 
/*∆*/     public open val gradientUnits: org.w3c.dom.svg.SVGAnimatedEnumeration { get; }
/*∆*/ 
/*∆*/     public open val spreadMethod: org.w3c.dom.svg.SVGAnimatedEnumeration { get; }
/*∆*/ 
/*∆*/     public companion object of SVGGradientElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_SPREADMETHOD_PAD: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_SPREADMETHOD_REFLECT: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_SPREADMETHOD_REPEAT: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_SPREADMETHOD_UNKNOWN: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_UNIT_TYPE_OBJECTBOUNDINGBOX: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_UNIT_TYPE_UNKNOWN: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_UNIT_TYPE_USERSPACEONUSE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGGraphicsElement : org.w3c.dom.svg.SVGElement, org.w3c.dom.svg.SVGTests {
/*∆*/     public constructor SVGGraphicsElement()
/*∆*/ 
/*∆*/     public open val transform: org.w3c.dom.svg.SVGAnimatedTransformList { get; }
/*∆*/ 
/*∆*/     public final fun getBBox(options: org.w3c.dom.svg.SVGBoundingBoxOptions = ...): org.w3c.dom.DOMRect
/*∆*/ 
/*∆*/     public final fun getCTM(): org.w3c.dom.DOMMatrix?
/*∆*/ 
/*∆*/     public final fun getScreenCTM(): org.w3c.dom.DOMMatrix?
/*∆*/ 
/*∆*/     public companion object of SVGGraphicsElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGHatchElement : org.w3c.dom.svg.SVGElement {
/*∆*/     public constructor SVGHatchElement()
/*∆*/ 
/*∆*/     public companion object of SVGHatchElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGHatchpathElement : org.w3c.dom.svg.SVGElement {
/*∆*/     public constructor SVGHatchpathElement()
/*∆*/ 
/*∆*/     public companion object of SVGHatchpathElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGImageElement : org.w3c.dom.svg.SVGGraphicsElement, org.w3c.dom.svg.SVGURIReference, org.w3c.dom.HTMLOrSVGImageElement {
/*∆*/     public constructor SVGImageElement()
/*∆*/ 
/*∆*/     public open var crossOrigin: kotlin.String? { get; set; }
/*∆*/ 
/*∆*/     public open val height: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public open val preserveAspectRatio: org.w3c.dom.svg.SVGAnimatedPreserveAspectRatio { get; }
/*∆*/ 
/*∆*/     public open val width: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public open val x: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public open val y: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public companion object of SVGImageElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGLength {
/*∆*/     public constructor SVGLength()
/*∆*/ 
/*∆*/     public open val unitType: kotlin.Short { get; }
/*∆*/ 
/*∆*/     public open var value: kotlin.Float { get; set; }
/*∆*/ 
/*∆*/     public open var valueAsString: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var valueInSpecifiedUnits: kotlin.Float { get; set; }
/*∆*/ 
/*∆*/     public final fun convertToSpecifiedUnits(unitType: kotlin.Short): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun newValueSpecifiedUnits(unitType: kotlin.Short, valueInSpecifiedUnits: kotlin.Float): kotlin.Unit
/*∆*/ 
/*∆*/     public companion object of SVGLength {
/*∆*/         public final val SVG_LENGTHTYPE_CM: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_LENGTHTYPE_EMS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_LENGTHTYPE_EXS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_LENGTHTYPE_IN: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_LENGTHTYPE_MM: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_LENGTHTYPE_NUMBER: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_LENGTHTYPE_PC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_LENGTHTYPE_PERCENTAGE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_LENGTHTYPE_PT: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_LENGTHTYPE_PX: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_LENGTHTYPE_UNKNOWN: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGLengthList {
/*∆*/     public constructor SVGLengthList()
/*∆*/ 
/*∆*/     public open val length: kotlin.Int { get; }
/*∆*/ 
/*∆*/     public open val numberOfItems: kotlin.Int { get; }
/*∆*/ 
/*∆*/     public final fun appendItem(newItem: org.w3c.dom.svg.SVGLength): org.w3c.dom.svg.SVGLength
/*∆*/ 
/*∆*/     public final fun clear(): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun getItem(index: kotlin.Int): org.w3c.dom.svg.SVGLength
/*∆*/ 
/*∆*/     public final fun initialize(newItem: org.w3c.dom.svg.SVGLength): org.w3c.dom.svg.SVGLength
/*∆*/ 
/*∆*/     public final fun insertItemBefore(newItem: org.w3c.dom.svg.SVGLength, index: kotlin.Int): org.w3c.dom.svg.SVGLength
/*∆*/ 
/*∆*/     public final fun removeItem(index: kotlin.Int): org.w3c.dom.svg.SVGLength
/*∆*/ 
/*∆*/     public final fun replaceItem(newItem: org.w3c.dom.svg.SVGLength, index: kotlin.Int): org.w3c.dom.svg.SVGLength
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGLineElement : org.w3c.dom.svg.SVGGeometryElement {
/*∆*/     public constructor SVGLineElement()
/*∆*/ 
/*∆*/     public open val x1: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public open val x2: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public open val y1: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public open val y2: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public companion object of SVGLineElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGLinearGradientElement : org.w3c.dom.svg.SVGGradientElement {
/*∆*/     public constructor SVGLinearGradientElement()
/*∆*/ 
/*∆*/     public open val x1: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public open val x2: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public open val y1: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public open val y2: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public companion object of SVGLinearGradientElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_SPREADMETHOD_PAD: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_SPREADMETHOD_REFLECT: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_SPREADMETHOD_REPEAT: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_SPREADMETHOD_UNKNOWN: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_UNIT_TYPE_OBJECTBOUNDINGBOX: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_UNIT_TYPE_UNKNOWN: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_UNIT_TYPE_USERSPACEONUSE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGMarkerElement : org.w3c.dom.svg.SVGElement, org.w3c.dom.svg.SVGFitToViewBox {
/*∆*/     public constructor SVGMarkerElement()
/*∆*/ 
/*∆*/     public open val markerHeight: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public open val markerUnits: org.w3c.dom.svg.SVGAnimatedEnumeration { get; }
/*∆*/ 
/*∆*/     public open val markerWidth: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public open var orient: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open val orientAngle: org.w3c.dom.svg.SVGAnimatedAngle { get; }
/*∆*/ 
/*∆*/     public open val orientType: org.w3c.dom.svg.SVGAnimatedEnumeration { get; }
/*∆*/ 
/*∆*/     public open val refX: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public open val refY: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public final fun setOrientToAngle(angle: org.w3c.dom.svg.SVGAngle): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun setOrientToAuto(): kotlin.Unit
/*∆*/ 
/*∆*/     public companion object of SVGMarkerElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_MARKERUNITS_STROKEWIDTH: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_MARKERUNITS_UNKNOWN: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_MARKERUNITS_USERSPACEONUSE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_MARKER_ORIENT_ANGLE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_MARKER_ORIENT_AUTO: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_MARKER_ORIENT_UNKNOWN: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGMeshElement : org.w3c.dom.svg.SVGGeometryElement, org.w3c.dom.svg.SVGURIReference {
/*∆*/     public constructor SVGMeshElement()
/*∆*/ 
/*∆*/     public companion object of SVGMeshElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGMeshGradientElement : org.w3c.dom.svg.SVGGradientElement {
/*∆*/     public constructor SVGMeshGradientElement()
/*∆*/ 
/*∆*/     public companion object of SVGMeshGradientElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_SPREADMETHOD_PAD: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_SPREADMETHOD_REFLECT: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_SPREADMETHOD_REPEAT: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_SPREADMETHOD_UNKNOWN: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_UNIT_TYPE_OBJECTBOUNDINGBOX: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_UNIT_TYPE_UNKNOWN: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_UNIT_TYPE_USERSPACEONUSE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGMeshpatchElement : org.w3c.dom.svg.SVGElement {
/*∆*/     public constructor SVGMeshpatchElement()
/*∆*/ 
/*∆*/     public companion object of SVGMeshpatchElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGMeshrowElement : org.w3c.dom.svg.SVGElement {
/*∆*/     public constructor SVGMeshrowElement()
/*∆*/ 
/*∆*/     public companion object of SVGMeshrowElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGMetadataElement : org.w3c.dom.svg.SVGElement {
/*∆*/     public constructor SVGMetadataElement()
/*∆*/ 
/*∆*/     public companion object of SVGMetadataElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGNameList {
/*∆*/     public constructor SVGNameList()
/*∆*/ 
/*∆*/     public open val length: kotlin.Int { get; }
/*∆*/ 
/*∆*/     public open val numberOfItems: kotlin.Int { get; }
/*∆*/ 
/*∆*/     public final fun appendItem(newItem: dynamic): dynamic
/*∆*/ 
/*∆*/     public final fun clear(): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun getItem(index: kotlin.Int): dynamic
/*∆*/ 
/*∆*/     public final fun initialize(newItem: dynamic): dynamic
/*∆*/ 
/*∆*/     public final fun insertItemBefore(newItem: dynamic, index: kotlin.Int): dynamic
/*∆*/ 
/*∆*/     public final fun removeItem(index: kotlin.Int): dynamic
/*∆*/ 
/*∆*/     public final fun replaceItem(newItem: dynamic, index: kotlin.Int): dynamic
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGNumber {
/*∆*/     public constructor SVGNumber()
/*∆*/ 
/*∆*/     public open var value: kotlin.Float { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGNumberList {
/*∆*/     public constructor SVGNumberList()
/*∆*/ 
/*∆*/     public open val length: kotlin.Int { get; }
/*∆*/ 
/*∆*/     public open val numberOfItems: kotlin.Int { get; }
/*∆*/ 
/*∆*/     public final fun appendItem(newItem: org.w3c.dom.svg.SVGNumber): org.w3c.dom.svg.SVGNumber
/*∆*/ 
/*∆*/     public final fun clear(): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun getItem(index: kotlin.Int): org.w3c.dom.svg.SVGNumber
/*∆*/ 
/*∆*/     public final fun initialize(newItem: org.w3c.dom.svg.SVGNumber): org.w3c.dom.svg.SVGNumber
/*∆*/ 
/*∆*/     public final fun insertItemBefore(newItem: org.w3c.dom.svg.SVGNumber, index: kotlin.Int): org.w3c.dom.svg.SVGNumber
/*∆*/ 
/*∆*/     public final fun removeItem(index: kotlin.Int): org.w3c.dom.svg.SVGNumber
/*∆*/ 
/*∆*/     public final fun replaceItem(newItem: org.w3c.dom.svg.SVGNumber, index: kotlin.Int): org.w3c.dom.svg.SVGNumber
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGPathElement : org.w3c.dom.svg.SVGGeometryElement {
/*∆*/     public constructor SVGPathElement()
/*∆*/ 
/*∆*/     public companion object of SVGPathElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGPatternElement : org.w3c.dom.svg.SVGElement, org.w3c.dom.svg.SVGFitToViewBox, org.w3c.dom.svg.SVGURIReference, org.w3c.dom.svg.SVGUnitTypes {
/*∆*/     public constructor SVGPatternElement()
/*∆*/ 
/*∆*/     public open val height: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public open val patternContentUnits: org.w3c.dom.svg.SVGAnimatedEnumeration { get; }
/*∆*/ 
/*∆*/     public open val patternTransform: org.w3c.dom.svg.SVGAnimatedTransformList { get; }
/*∆*/ 
/*∆*/     public open val patternUnits: org.w3c.dom.svg.SVGAnimatedEnumeration { get; }
/*∆*/ 
/*∆*/     public open val width: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public open val x: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public open val y: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public companion object of SVGPatternElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_UNIT_TYPE_OBJECTBOUNDINGBOX: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_UNIT_TYPE_UNKNOWN: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_UNIT_TYPE_USERSPACEONUSE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGPointList {
/*∆*/     public constructor SVGPointList()
/*∆*/ 
/*∆*/     public open val length: kotlin.Int { get; }
/*∆*/ 
/*∆*/     public open val numberOfItems: kotlin.Int { get; }
/*∆*/ 
/*∆*/     public final fun appendItem(newItem: org.w3c.dom.DOMPoint): org.w3c.dom.DOMPoint
/*∆*/ 
/*∆*/     public final fun clear(): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun getItem(index: kotlin.Int): org.w3c.dom.DOMPoint
/*∆*/ 
/*∆*/     public final fun initialize(newItem: org.w3c.dom.DOMPoint): org.w3c.dom.DOMPoint
/*∆*/ 
/*∆*/     public final fun insertItemBefore(newItem: org.w3c.dom.DOMPoint, index: kotlin.Int): org.w3c.dom.DOMPoint
/*∆*/ 
/*∆*/     public final fun removeItem(index: kotlin.Int): org.w3c.dom.DOMPoint
/*∆*/ 
/*∆*/     public final fun replaceItem(newItem: org.w3c.dom.DOMPoint, index: kotlin.Int): org.w3c.dom.DOMPoint
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGPolygonElement : org.w3c.dom.svg.SVGGeometryElement, org.w3c.dom.svg.SVGAnimatedPoints {
/*∆*/     public constructor SVGPolygonElement()
/*∆*/ 
/*∆*/     public companion object of SVGPolygonElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGPolylineElement : org.w3c.dom.svg.SVGGeometryElement, org.w3c.dom.svg.SVGAnimatedPoints {
/*∆*/     public constructor SVGPolylineElement()
/*∆*/ 
/*∆*/     public companion object of SVGPolylineElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGPreserveAspectRatio {
/*∆*/     public constructor SVGPreserveAspectRatio()
/*∆*/ 
/*∆*/     public open var align: kotlin.Short { get; set; }
/*∆*/ 
/*∆*/     public open var meetOrSlice: kotlin.Short { get; set; }
/*∆*/ 
/*∆*/     public companion object of SVGPreserveAspectRatio {
/*∆*/         public final val SVG_MEETORSLICE_MEET: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_MEETORSLICE_SLICE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_MEETORSLICE_UNKNOWN: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_PRESERVEASPECTRATIO_NONE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_PRESERVEASPECTRATIO_UNKNOWN: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_PRESERVEASPECTRATIO_XMAXYMAX: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_PRESERVEASPECTRATIO_XMAXYMID: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_PRESERVEASPECTRATIO_XMAXYMIN: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_PRESERVEASPECTRATIO_XMIDYMAX: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_PRESERVEASPECTRATIO_XMIDYMID: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_PRESERVEASPECTRATIO_XMIDYMIN: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_PRESERVEASPECTRATIO_XMINYMAX: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_PRESERVEASPECTRATIO_XMINYMID: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_PRESERVEASPECTRATIO_XMINYMIN: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGRadialGradientElement : org.w3c.dom.svg.SVGGradientElement {
/*∆*/     public constructor SVGRadialGradientElement()
/*∆*/ 
/*∆*/     public open val cx: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public open val cy: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public open val fr: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public open val fx: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public open val fy: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public open val r: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public companion object of SVGRadialGradientElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_SPREADMETHOD_PAD: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_SPREADMETHOD_REFLECT: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_SPREADMETHOD_REPEAT: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_SPREADMETHOD_UNKNOWN: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_UNIT_TYPE_OBJECTBOUNDINGBOX: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_UNIT_TYPE_UNKNOWN: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_UNIT_TYPE_USERSPACEONUSE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGRectElement : org.w3c.dom.svg.SVGGeometryElement {
/*∆*/     public constructor SVGRectElement()
/*∆*/ 
/*∆*/     public open val height: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public open val rx: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public open val ry: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public open val width: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public open val x: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public open val y: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public companion object of SVGRectElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGSVGElement : org.w3c.dom.svg.SVGGraphicsElement, org.w3c.dom.svg.SVGFitToViewBox, org.w3c.dom.svg.SVGZoomAndPan, org.w3c.dom.WindowEventHandlers {
/*∆*/     public constructor SVGSVGElement()
/*∆*/ 
/*∆*/     public open var currentScale: kotlin.Float { get; set; }
/*∆*/ 
/*∆*/     public open val currentTranslate: org.w3c.dom.DOMPointReadOnly { get; }
/*∆*/ 
/*∆*/     public open val height: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public open val width: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public open val x: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public open val y: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public final fun checkEnclosure(element: org.w3c.dom.svg.SVGElement, rect: org.w3c.dom.DOMRectReadOnly): kotlin.Boolean
/*∆*/ 
/*∆*/     public final fun checkIntersection(element: org.w3c.dom.svg.SVGElement, rect: org.w3c.dom.DOMRectReadOnly): kotlin.Boolean
/*∆*/ 
/*∆*/     public final fun createSVGAngle(): org.w3c.dom.svg.SVGAngle
/*∆*/ 
/*∆*/     public final fun createSVGLength(): org.w3c.dom.svg.SVGLength
/*∆*/ 
/*∆*/     public final fun createSVGMatrix(): org.w3c.dom.DOMMatrix
/*∆*/ 
/*∆*/     public final fun createSVGNumber(): org.w3c.dom.svg.SVGNumber
/*∆*/ 
/*∆*/     public final fun createSVGPoint(): org.w3c.dom.DOMPoint
/*∆*/ 
/*∆*/     public final fun createSVGRect(): org.w3c.dom.DOMRect
/*∆*/ 
/*∆*/     public final fun createSVGTransform(): org.w3c.dom.svg.SVGTransform
/*∆*/ 
/*∆*/     public final fun createSVGTransformFromMatrix(matrix: org.w3c.dom.DOMMatrixReadOnly): org.w3c.dom.svg.SVGTransform
/*∆*/ 
/*∆*/     public final fun deselectAll(): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun forceRedraw(): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun getElementById(elementId: kotlin.String): org.w3c.dom.Element
/*∆*/ 
/*∆*/     public final fun getEnclosureList(rect: org.w3c.dom.DOMRectReadOnly, referenceElement: org.w3c.dom.svg.SVGElement?): org.w3c.dom.NodeList
/*∆*/ 
/*∆*/     public final fun getIntersectionList(rect: org.w3c.dom.DOMRectReadOnly, referenceElement: org.w3c.dom.svg.SVGElement?): org.w3c.dom.NodeList
/*∆*/ 
/*∆*/     public final fun suspendRedraw(maxWaitMilliseconds: kotlin.Int): kotlin.Int
/*∆*/ 
/*∆*/     public final fun unsuspendRedraw(suspendHandleID: kotlin.Int): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun unsuspendRedrawAll(): kotlin.Unit
/*∆*/ 
/*∆*/     public companion object of SVGSVGElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_ZOOMANDPAN_DISABLE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_ZOOMANDPAN_MAGNIFY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_ZOOMANDPAN_UNKNOWN: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGScriptElement : org.w3c.dom.svg.SVGElement, org.w3c.dom.svg.SVGURIReference, org.w3c.dom.HTMLOrSVGScriptElement {
/*∆*/     public constructor SVGScriptElement()
/*∆*/ 
/*∆*/     public open var crossOrigin: kotlin.String? { get; set; }
/*∆*/ 
/*∆*/     public open var type: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public companion object of SVGScriptElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGSolidcolorElement : org.w3c.dom.svg.SVGElement {
/*∆*/     public constructor SVGSolidcolorElement()
/*∆*/ 
/*∆*/     public companion object of SVGSolidcolorElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGStopElement : org.w3c.dom.svg.SVGElement {
/*∆*/     public constructor SVGStopElement()
/*∆*/ 
/*∆*/     public open val offset: org.w3c.dom.svg.SVGAnimatedNumber { get; }
/*∆*/ 
/*∆*/     public companion object of SVGStopElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGStringList {
/*∆*/     public constructor SVGStringList()
/*∆*/ 
/*∆*/     public open val length: kotlin.Int { get; }
/*∆*/ 
/*∆*/     public open val numberOfItems: kotlin.Int { get; }
/*∆*/ 
/*∆*/     public final fun appendItem(newItem: kotlin.String): kotlin.String
/*∆*/ 
/*∆*/     public final fun clear(): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun getItem(index: kotlin.Int): kotlin.String
/*∆*/ 
/*∆*/     public final fun initialize(newItem: kotlin.String): kotlin.String
/*∆*/ 
/*∆*/     public final fun insertItemBefore(newItem: kotlin.String, index: kotlin.Int): kotlin.String
/*∆*/ 
/*∆*/     public final fun removeItem(index: kotlin.Int): kotlin.String
/*∆*/ 
/*∆*/     public final fun replaceItem(newItem: kotlin.String, index: kotlin.Int): kotlin.String
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGStyleElement : org.w3c.dom.svg.SVGElement, org.w3c.dom.css.LinkStyle {
/*∆*/     public constructor SVGStyleElement()
/*∆*/ 
/*∆*/     public open var media: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var title: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var type: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public companion object of SVGStyleElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGSwitchElement : org.w3c.dom.svg.SVGGraphicsElement {
/*∆*/     public constructor SVGSwitchElement()
/*∆*/ 
/*∆*/     public companion object of SVGSwitchElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGSymbolElement : org.w3c.dom.svg.SVGGraphicsElement, org.w3c.dom.svg.SVGFitToViewBox {
/*∆*/     public constructor SVGSymbolElement()
/*∆*/ 
/*∆*/     public companion object of SVGSymbolElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGTSpanElement : org.w3c.dom.svg.SVGTextPositioningElement {
/*∆*/     public constructor SVGTSpanElement()
/*∆*/ 
/*∆*/     public companion object of SVGTSpanElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val LENGTHADJUST_SPACING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val LENGTHADJUST_SPACINGANDGLYPHS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val LENGTHADJUST_UNKNOWN: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface SVGTests {
/*∆*/     public abstract val requiredExtensions: org.w3c.dom.svg.SVGStringList { get; }
/*∆*/ 
/*∆*/     public abstract val systemLanguage: org.w3c.dom.svg.SVGStringList { get; }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGTextContentElement : org.w3c.dom.svg.SVGGraphicsElement {
/*∆*/     public constructor SVGTextContentElement()
/*∆*/ 
/*∆*/     public open val lengthAdjust: org.w3c.dom.svg.SVGAnimatedEnumeration { get; }
/*∆*/ 
/*∆*/     public open val textLength: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public final fun getCharNumAtPosition(point: org.w3c.dom.DOMPoint): kotlin.Int
/*∆*/ 
/*∆*/     public final fun getComputedTextLength(): kotlin.Float
/*∆*/ 
/*∆*/     public final fun getEndPositionOfChar(charnum: kotlin.Int): org.w3c.dom.DOMPoint
/*∆*/ 
/*∆*/     public final fun getExtentOfChar(charnum: kotlin.Int): org.w3c.dom.DOMRect
/*∆*/ 
/*∆*/     public final fun getNumberOfChars(): kotlin.Int
/*∆*/ 
/*∆*/     public final fun getRotationOfChar(charnum: kotlin.Int): kotlin.Float
/*∆*/ 
/*∆*/     public final fun getStartPositionOfChar(charnum: kotlin.Int): org.w3c.dom.DOMPoint
/*∆*/ 
/*∆*/     public final fun getSubStringLength(charnum: kotlin.Int, nchars: kotlin.Int): kotlin.Float
/*∆*/ 
/*∆*/     public final fun selectSubString(charnum: kotlin.Int, nchars: kotlin.Int): kotlin.Unit
/*∆*/ 
/*∆*/     public companion object of SVGTextContentElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val LENGTHADJUST_SPACING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val LENGTHADJUST_SPACINGANDGLYPHS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val LENGTHADJUST_UNKNOWN: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGTextElement : org.w3c.dom.svg.SVGTextPositioningElement {
/*∆*/     public constructor SVGTextElement()
/*∆*/ 
/*∆*/     public companion object of SVGTextElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val LENGTHADJUST_SPACING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val LENGTHADJUST_SPACINGANDGLYPHS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val LENGTHADJUST_UNKNOWN: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGTextPathElement : org.w3c.dom.svg.SVGTextContentElement, org.w3c.dom.svg.SVGURIReference {
/*∆*/     public constructor SVGTextPathElement()
/*∆*/ 
/*∆*/     public open val method: org.w3c.dom.svg.SVGAnimatedEnumeration { get; }
/*∆*/ 
/*∆*/     public open val spacing: org.w3c.dom.svg.SVGAnimatedEnumeration { get; }
/*∆*/ 
/*∆*/     public open val startOffset: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public companion object of SVGTextPathElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val LENGTHADJUST_SPACING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val LENGTHADJUST_SPACINGANDGLYPHS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val LENGTHADJUST_UNKNOWN: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXTPATH_METHODTYPE_ALIGN: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXTPATH_METHODTYPE_STRETCH: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXTPATH_METHODTYPE_UNKNOWN: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXTPATH_SPACINGTYPE_AUTO: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXTPATH_SPACINGTYPE_EXACT: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXTPATH_SPACINGTYPE_UNKNOWN: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGTextPositioningElement : org.w3c.dom.svg.SVGTextContentElement {
/*∆*/     public constructor SVGTextPositioningElement()
/*∆*/ 
/*∆*/     public open val dx: org.w3c.dom.svg.SVGAnimatedLengthList { get; }
/*∆*/ 
/*∆*/     public open val dy: org.w3c.dom.svg.SVGAnimatedLengthList { get; }
/*∆*/ 
/*∆*/     public open val rotate: org.w3c.dom.svg.SVGAnimatedNumberList { get; }
/*∆*/ 
/*∆*/     public open val x: org.w3c.dom.svg.SVGAnimatedLengthList { get; }
/*∆*/ 
/*∆*/     public open val y: org.w3c.dom.svg.SVGAnimatedLengthList { get; }
/*∆*/ 
/*∆*/     public companion object of SVGTextPositioningElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val LENGTHADJUST_SPACING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val LENGTHADJUST_SPACINGANDGLYPHS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val LENGTHADJUST_UNKNOWN: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGTitleElement : org.w3c.dom.svg.SVGElement {
/*∆*/     public constructor SVGTitleElement()
/*∆*/ 
/*∆*/     public companion object of SVGTitleElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGTransform {
/*∆*/     public constructor SVGTransform()
/*∆*/ 
/*∆*/     public open val angle: kotlin.Float { get; }
/*∆*/ 
/*∆*/     public open val matrix: org.w3c.dom.DOMMatrix { get; }
/*∆*/ 
/*∆*/     public open val type: kotlin.Short { get; }
/*∆*/ 
/*∆*/     public final fun setMatrix(matrix: org.w3c.dom.DOMMatrixReadOnly): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun setRotate(angle: kotlin.Float, cx: kotlin.Float, cy: kotlin.Float): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun setScale(sx: kotlin.Float, sy: kotlin.Float): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun setSkewX(angle: kotlin.Float): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun setSkewY(angle: kotlin.Float): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun setTranslate(tx: kotlin.Float, ty: kotlin.Float): kotlin.Unit
/*∆*/ 
/*∆*/     public companion object of SVGTransform {
/*∆*/         public final val SVG_TRANSFORM_MATRIX: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_TRANSFORM_ROTATE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_TRANSFORM_SCALE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_TRANSFORM_SKEWX: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_TRANSFORM_SKEWY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_TRANSFORM_TRANSLATE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_TRANSFORM_UNKNOWN: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGTransformList {
/*∆*/     public constructor SVGTransformList()
/*∆*/ 
/*∆*/     public open val length: kotlin.Int { get; }
/*∆*/ 
/*∆*/     public open val numberOfItems: kotlin.Int { get; }
/*∆*/ 
/*∆*/     public final fun appendItem(newItem: org.w3c.dom.svg.SVGTransform): org.w3c.dom.svg.SVGTransform
/*∆*/ 
/*∆*/     public final fun clear(): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun consolidate(): org.w3c.dom.svg.SVGTransform?
/*∆*/ 
/*∆*/     public final fun createSVGTransformFromMatrix(matrix: org.w3c.dom.DOMMatrixReadOnly): org.w3c.dom.svg.SVGTransform
/*∆*/ 
/*∆*/     public final fun getItem(index: kotlin.Int): org.w3c.dom.svg.SVGTransform
/*∆*/ 
/*∆*/     public final fun initialize(newItem: org.w3c.dom.svg.SVGTransform): org.w3c.dom.svg.SVGTransform
/*∆*/ 
/*∆*/     public final fun insertItemBefore(newItem: org.w3c.dom.svg.SVGTransform, index: kotlin.Int): org.w3c.dom.svg.SVGTransform
/*∆*/ 
/*∆*/     public final fun removeItem(index: kotlin.Int): org.w3c.dom.svg.SVGTransform
/*∆*/ 
/*∆*/     public final fun replaceItem(newItem: org.w3c.dom.svg.SVGTransform, index: kotlin.Int): org.w3c.dom.svg.SVGTransform
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface SVGURIReference {
/*∆*/     public abstract val href: org.w3c.dom.svg.SVGAnimatedString { get; }
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface SVGUnitTypes {
/*∆*/     public companion object of SVGUnitTypes {
/*∆*/         public final val SVG_UNIT_TYPE_OBJECTBOUNDINGBOX: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_UNIT_TYPE_UNKNOWN: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_UNIT_TYPE_USERSPACEONUSE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGUnknownElement : org.w3c.dom.svg.SVGGraphicsElement {
/*∆*/     public constructor SVGUnknownElement()
/*∆*/ 
/*∆*/     public companion object of SVGUnknownElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGUseElement : org.w3c.dom.svg.SVGGraphicsElement, org.w3c.dom.svg.SVGURIReference {
/*∆*/     public constructor SVGUseElement()
/*∆*/ 
/*∆*/     public open val animatedInstanceRoot: org.w3c.dom.svg.SVGElement? { get; }
/*∆*/ 
/*∆*/     public open val height: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public open val instanceRoot: org.w3c.dom.svg.SVGElement? { get; }
/*∆*/ 
/*∆*/     public open val width: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public open val x: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public open val y: org.w3c.dom.svg.SVGAnimatedLength { get; }
/*∆*/ 
/*∆*/     public companion object of SVGUseElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public open external class SVGUseElementShadowRoot : org.w3c.dom.ShadowRoot {
/*∆*/     public constructor SVGUseElementShadowRoot()
/*∆*/ 
/*∆*/     public companion object of SVGUseElementShadowRoot {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SVGViewElement : org.w3c.dom.svg.SVGElement, org.w3c.dom.svg.SVGFitToViewBox, org.w3c.dom.svg.SVGZoomAndPan {
/*∆*/     public constructor SVGViewElement()
/*∆*/ 
/*∆*/     public companion object of SVGViewElement {
/*∆*/         public final val ATTRIBUTE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CDATA_SECTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val COMMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOCUMENT_TYPE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ELEMENT_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val ENTITY_REFERENCE_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NOTATION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_ZOOMANDPAN_DISABLE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_ZOOMANDPAN_MAGNIFY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_ZOOMANDPAN_UNKNOWN: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TEXT_NODE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface SVGZoomAndPan {
/*∆*/     public abstract var zoomAndPan: kotlin.Short { get; set; }
/*∆*/ 
/*∆*/     public companion object of SVGZoomAndPan {
/*∆*/         public final val SVG_ZOOMANDPAN_DISABLE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_ZOOMANDPAN_MAGNIFY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val SVG_ZOOMANDPAN_UNKNOWN: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public open external class ShadowAnimation {
/*∆*/     public constructor ShadowAnimation(source: dynamic, newTarget: dynamic)
/*∆*/ 
/*∆*/     public open val sourceAnimation: dynamic { get; }
/*∆*/ }