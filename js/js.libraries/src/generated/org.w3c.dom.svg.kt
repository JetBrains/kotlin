/*
 * Generated file
 * DO NOT EDIT
 * 
 * See libraries/tools/idl2k for details
 */

package org.w3c.dom.svg

import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.dom.css.*
import org.w3c.dom.events.*
import org.w3c.dom.parsing.*
import org.w3c.dom.url.*
import org.w3c.fetch.*
import org.w3c.files.*
import org.w3c.notifications.*
import org.w3c.performance.*
import org.w3c.workers.*
import org.w3c.xhr.*

@native public abstract class SVGElement : Element(), ElementCSSInlineStyle, GlobalEventHandlers, SVGElementInstance {
    open val dataset: DOMStringMap
        get() = noImpl
    open val ownerSVGElement: SVGSVGElement?
        get() = noImpl
    open val viewportElement: SVGElement?
        get() = noImpl
    open var tabIndex: Int
        get() = noImpl
        set(value) = noImpl
    fun focus(): Unit = noImpl
    fun blur(): Unit = noImpl
}

@native public interface SVGBoundingBoxOptions {
    var fill: Boolean? /* = true */
    var stroke: Boolean? /* = false */
    var markers: Boolean? /* = false */
    var clipped: Boolean? /* = false */
}

@Suppress("NOTHING_TO_INLINE")
public inline fun SVGBoundingBoxOptions(fill: Boolean? = true, stroke: Boolean? = false, markers: Boolean? = false, clipped: Boolean? = false): SVGBoundingBoxOptions {
    val o = js("({})")

    o["fill"] = fill
    o["stroke"] = stroke
    o["markers"] = markers
    o["clipped"] = clipped

    return o
}

@native public abstract class SVGGraphicsElement : SVGElement(), SVGTests {
    open val transform: SVGAnimatedTransformList
        get() = noImpl
    fun getBBox(options: SVGBoundingBoxOptions = noImpl): DOMRect = noImpl
    fun getCTM(): DOMMatrix? = noImpl
    fun getScreenCTM(): DOMMatrix? = noImpl
}

@native public abstract class SVGGeometryElement : SVGGraphicsElement() {
    open val pathLength: SVGAnimatedNumber
        get() = noImpl
    fun isPointInFill(point: DOMPoint): Boolean = noImpl
    fun isPointInStroke(point: DOMPoint): Boolean = noImpl
    fun getTotalLength(): Float = noImpl
    fun getPointAtLength(distance: Float): DOMPoint = noImpl
}

@native public abstract class SVGNumber {
    open var value: Float
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class SVGLength {
    open val unitType: Short
        get() = noImpl
    open var value: Float
        get() = noImpl
        set(value) = noImpl
    open var valueInSpecifiedUnits: Float
        get() = noImpl
        set(value) = noImpl
    open var valueAsString: String
        get() = noImpl
        set(value) = noImpl
    fun newValueSpecifiedUnits(unitType: Short, valueInSpecifiedUnits: Float): Unit = noImpl
    fun convertToSpecifiedUnits(unitType: Short): Unit = noImpl

    companion object {
        val SVG_LENGTHTYPE_UNKNOWN: Short = 0
        val SVG_LENGTHTYPE_NUMBER: Short = 1
        val SVG_LENGTHTYPE_PERCENTAGE: Short = 2
        val SVG_LENGTHTYPE_EMS: Short = 3
        val SVG_LENGTHTYPE_EXS: Short = 4
        val SVG_LENGTHTYPE_PX: Short = 5
        val SVG_LENGTHTYPE_CM: Short = 6
        val SVG_LENGTHTYPE_MM: Short = 7
        val SVG_LENGTHTYPE_IN: Short = 8
        val SVG_LENGTHTYPE_PT: Short = 9
        val SVG_LENGTHTYPE_PC: Short = 10
    }
}

@native public abstract class SVGAngle {
    open val unitType: Short
        get() = noImpl
    open var value: Float
        get() = noImpl
        set(value) = noImpl
    open var valueInSpecifiedUnits: Float
        get() = noImpl
        set(value) = noImpl
    open var valueAsString: String
        get() = noImpl
        set(value) = noImpl
    fun newValueSpecifiedUnits(unitType: Short, valueInSpecifiedUnits: Float): Unit = noImpl
    fun convertToSpecifiedUnits(unitType: Short): Unit = noImpl

    companion object {
        val SVG_ANGLETYPE_UNKNOWN: Short = 0
        val SVG_ANGLETYPE_UNSPECIFIED: Short = 1
        val SVG_ANGLETYPE_DEG: Short = 2
        val SVG_ANGLETYPE_RAD: Short = 3
        val SVG_ANGLETYPE_GRAD: Short = 4
    }
}

@native public abstract class SVGNameList {
    open val length: Int
        get() = noImpl
    open val numberOfItems: Int
        get() = noImpl
    fun clear(): Unit = noImpl
    fun initialize(newItem: dynamic): dynamic = noImpl
    fun getItem(index: Int): dynamic = noImpl
    @nativeGetter
    operator fun get(index: Int): dynamic = noImpl
    fun insertItemBefore(newItem: dynamic, index: Int): dynamic = noImpl
    fun replaceItem(newItem: dynamic, index: Int): dynamic = noImpl
    fun removeItem(index: Int): dynamic = noImpl
    fun appendItem(newItem: dynamic): dynamic = noImpl
    @nativeSetter
    operator fun set(index: Int, newItem: dynamic): Unit = noImpl
}

@native public abstract class SVGNumberList {
    open val length: Int
        get() = noImpl
    open val numberOfItems: Int
        get() = noImpl
    fun clear(): Unit = noImpl
    fun initialize(newItem: SVGNumber): SVGNumber = noImpl
    fun getItem(index: Int): SVGNumber = noImpl
    @nativeGetter
    operator fun get(index: Int): SVGNumber? = noImpl
    fun insertItemBefore(newItem: SVGNumber, index: Int): SVGNumber = noImpl
    fun replaceItem(newItem: SVGNumber, index: Int): SVGNumber = noImpl
    fun removeItem(index: Int): SVGNumber = noImpl
    fun appendItem(newItem: SVGNumber): SVGNumber = noImpl
    @nativeSetter
    operator fun set(index: Int, newItem: SVGNumber): Unit = noImpl
}

@native public abstract class SVGLengthList {
    open val length: Int
        get() = noImpl
    open val numberOfItems: Int
        get() = noImpl
    fun clear(): Unit = noImpl
    fun initialize(newItem: SVGLength): SVGLength = noImpl
    fun getItem(index: Int): SVGLength = noImpl
    @nativeGetter
    operator fun get(index: Int): SVGLength? = noImpl
    fun insertItemBefore(newItem: SVGLength, index: Int): SVGLength = noImpl
    fun replaceItem(newItem: SVGLength, index: Int): SVGLength = noImpl
    fun removeItem(index: Int): SVGLength = noImpl
    fun appendItem(newItem: SVGLength): SVGLength = noImpl
    @nativeSetter
    operator fun set(index: Int, newItem: SVGLength): Unit = noImpl
}

@native public abstract class SVGAnimatedBoolean {
    open var baseVal: Boolean
        get() = noImpl
        set(value) = noImpl
    open val animVal: Boolean
        get() = noImpl
}

@native public abstract class SVGAnimatedEnumeration {
    open var baseVal: Short
        get() = noImpl
        set(value) = noImpl
    open val animVal: Short
        get() = noImpl
}

@native public abstract class SVGAnimatedInteger {
    open var baseVal: Int
        get() = noImpl
        set(value) = noImpl
    open val animVal: Int
        get() = noImpl
}

@native public abstract class SVGAnimatedNumber {
    open var baseVal: Float
        get() = noImpl
        set(value) = noImpl
    open val animVal: Float
        get() = noImpl
}

@native public abstract class SVGAnimatedLength {
    open val baseVal: SVGLength
        get() = noImpl
    open val animVal: SVGLength
        get() = noImpl
}

@native public abstract class SVGAnimatedAngle {
    open val baseVal: SVGAngle
        get() = noImpl
    open val animVal: SVGAngle
        get() = noImpl
}

@native public abstract class SVGAnimatedString {
    open var baseVal: String
        get() = noImpl
        set(value) = noImpl
    open val animVal: String
        get() = noImpl
}

@native public abstract class SVGAnimatedRect {
    open val baseVal: DOMRect
        get() = noImpl
    open val animVal: DOMRectReadOnly
        get() = noImpl
}

@native public abstract class SVGAnimatedNumberList {
    open val baseVal: SVGNumberList
        get() = noImpl
    open val animVal: SVGNumberList
        get() = noImpl
}

@native public abstract class SVGAnimatedLengthList {
    open val baseVal: SVGLengthList
        get() = noImpl
    open val animVal: SVGLengthList
        get() = noImpl
}

@native public abstract class SVGStringList {
    open val length: Int
        get() = noImpl
    open val numberOfItems: Int
        get() = noImpl
    fun clear(): Unit = noImpl
    fun initialize(newItem: String): String = noImpl
    fun getItem(index: Int): String = noImpl
    @nativeGetter
    operator fun get(index: Int): String? = noImpl
    fun insertItemBefore(newItem: String, index: Int): String = noImpl
    fun replaceItem(newItem: String, index: Int): String = noImpl
    fun removeItem(index: Int): String = noImpl
    fun appendItem(newItem: String): String = noImpl
    @nativeSetter
    operator fun set(index: Int, newItem: String): Unit = noImpl
}

@native public interface SVGUnitTypes {

    companion object {
        val SVG_UNIT_TYPE_UNKNOWN: Short = 0
        val SVG_UNIT_TYPE_USERSPACEONUSE: Short = 1
        val SVG_UNIT_TYPE_OBJECTBOUNDINGBOX: Short = 2
    }
}

@native public interface SVGTests {
    val requiredExtensions: SVGStringList
        get() = noImpl
    val systemLanguage: SVGStringList
        get() = noImpl
}

@native public interface SVGFitToViewBox {
    val viewBox: SVGAnimatedRect
        get() = noImpl
    val preserveAspectRatio: SVGAnimatedPreserveAspectRatio
        get() = noImpl
}

@native public interface SVGZoomAndPan {
    var zoomAndPan: Short
        get() = noImpl
        set(value) = noImpl

    companion object {
        val SVG_ZOOMANDPAN_UNKNOWN: Short = 0
        val SVG_ZOOMANDPAN_DISABLE: Short = 1
        val SVG_ZOOMANDPAN_MAGNIFY: Short = 2
    }
}

@native public interface SVGURIReference {
    val href: SVGAnimatedString
        get() = noImpl
}

@native public abstract class SVGSVGElement : SVGGraphicsElement(), SVGFitToViewBox, SVGZoomAndPan, WindowEventHandlers {
    open val x: SVGAnimatedLength
        get() = noImpl
    open val y: SVGAnimatedLength
        get() = noImpl
    open val width: SVGAnimatedLength
        get() = noImpl
    open val height: SVGAnimatedLength
        get() = noImpl
    open var currentScale: Float
        get() = noImpl
        set(value) = noImpl
    open val currentTranslate: DOMPointReadOnly
        get() = noImpl
    fun getIntersectionList(rect: DOMRectReadOnly, referenceElement: SVGElement?): NodeList = noImpl
    fun getEnclosureList(rect: DOMRectReadOnly, referenceElement: SVGElement?): NodeList = noImpl
    fun checkIntersection(element: SVGElement, rect: DOMRectReadOnly): Boolean = noImpl
    fun checkEnclosure(element: SVGElement, rect: DOMRectReadOnly): Boolean = noImpl
    fun deselectAll(): Unit = noImpl
    fun createSVGNumber(): SVGNumber = noImpl
    fun createSVGLength(): SVGLength = noImpl
    fun createSVGAngle(): SVGAngle = noImpl
    fun createSVGPoint(): DOMPoint = noImpl
    fun createSVGMatrix(): DOMMatrix = noImpl
    fun createSVGRect(): DOMRect = noImpl
    fun createSVGTransform(): SVGTransform = noImpl
    fun createSVGTransformFromMatrix(matrix: DOMMatrixReadOnly): SVGTransform = noImpl
    fun getElementById(elementId: String): Element = noImpl
    fun suspendRedraw(maxWaitMilliseconds: Int): Int = noImpl
    fun unsuspendRedraw(suspendHandleID: Int): Unit = noImpl
    fun unsuspendRedrawAll(): Unit = noImpl
    fun forceRedraw(): Unit = noImpl

    companion object {
        val SVG_ZOOMANDPAN_UNKNOWN: Short = 0
        val SVG_ZOOMANDPAN_DISABLE: Short = 1
        val SVG_ZOOMANDPAN_MAGNIFY: Short = 2
    }
}

@native public abstract class SVGGElement : SVGGraphicsElement() {
}

@native public abstract class SVGUnknownElement : SVGGraphicsElement() {
}

@native public abstract class SVGDefsElement : SVGGraphicsElement() {
}

@native public abstract class SVGDescElement : SVGElement() {
}

@native public abstract class SVGMetadataElement : SVGElement() {
}

@native public abstract class SVGTitleElement : SVGElement() {
}

@native public abstract class SVGSymbolElement : SVGGraphicsElement(), SVGFitToViewBox {
}

@native public abstract class SVGUseElement : SVGGraphicsElement(), SVGURIReference {
    open val x: SVGAnimatedLength
        get() = noImpl
    open val y: SVGAnimatedLength
        get() = noImpl
    open val width: SVGAnimatedLength
        get() = noImpl
    open val height: SVGAnimatedLength
        get() = noImpl
    open val instanceRoot: SVGElement?
        get() = noImpl
    open val animatedInstanceRoot: SVGElement?
        get() = noImpl
}

@native public open class SVGUseElementShadowRoot : ShadowRoot() {
}

@native public interface SVGElementInstance {
    val correspondingElement: SVGElement?
        get() = noImpl
    val correspondingUseElement: SVGUseElement?
        get() = noImpl
}

@native public open class ShadowAnimation(source: dynamic, newTarget: dynamic) {
    open val sourceAnimation: dynamic
        get() = noImpl
}

@native public abstract class SVGSwitchElement : SVGGraphicsElement() {
}

@native public interface GetSVGDocument {
    fun getSVGDocument(): Document = noImpl
}

@native public abstract class SVGStyleElement : SVGElement(), LinkStyle {
    open var type: String
        get() = noImpl
        set(value) = noImpl
    open var media: String
        get() = noImpl
        set(value) = noImpl
    open var title: String
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class SVGTransform {
    open val type: Short
        get() = noImpl
    open val matrix: DOMMatrix
        get() = noImpl
    open val angle: Float
        get() = noImpl
    fun setMatrix(matrix: DOMMatrixReadOnly): Unit = noImpl
    fun setTranslate(tx: Float, ty: Float): Unit = noImpl
    fun setScale(sx: Float, sy: Float): Unit = noImpl
    fun setRotate(angle: Float, cx: Float, cy: Float): Unit = noImpl
    fun setSkewX(angle: Float): Unit = noImpl
    fun setSkewY(angle: Float): Unit = noImpl

    companion object {
        val SVG_TRANSFORM_UNKNOWN: Short = 0
        val SVG_TRANSFORM_MATRIX: Short = 1
        val SVG_TRANSFORM_TRANSLATE: Short = 2
        val SVG_TRANSFORM_SCALE: Short = 3
        val SVG_TRANSFORM_ROTATE: Short = 4
        val SVG_TRANSFORM_SKEWX: Short = 5
        val SVG_TRANSFORM_SKEWY: Short = 6
    }
}

@native public abstract class SVGTransformList {
    open val length: Int
        get() = noImpl
    open val numberOfItems: Int
        get() = noImpl
    fun clear(): Unit = noImpl
    fun initialize(newItem: SVGTransform): SVGTransform = noImpl
    fun getItem(index: Int): SVGTransform = noImpl
    @nativeGetter
    operator fun get(index: Int): SVGTransform? = noImpl
    fun insertItemBefore(newItem: SVGTransform, index: Int): SVGTransform = noImpl
    fun replaceItem(newItem: SVGTransform, index: Int): SVGTransform = noImpl
    fun removeItem(index: Int): SVGTransform = noImpl
    fun appendItem(newItem: SVGTransform): SVGTransform = noImpl
    @nativeSetter
    operator fun set(index: Int, newItem: SVGTransform): Unit = noImpl
    fun createSVGTransformFromMatrix(matrix: DOMMatrixReadOnly): SVGTransform = noImpl
    fun consolidate(): SVGTransform? = noImpl
}

@native public abstract class SVGAnimatedTransformList {
    open val baseVal: SVGTransformList
        get() = noImpl
    open val animVal: SVGTransformList
        get() = noImpl
}

@native public abstract class SVGPreserveAspectRatio {
    open var align: Short
        get() = noImpl
        set(value) = noImpl
    open var meetOrSlice: Short
        get() = noImpl
        set(value) = noImpl

    companion object {
        val SVG_PRESERVEASPECTRATIO_UNKNOWN: Short = 0
        val SVG_PRESERVEASPECTRATIO_NONE: Short = 1
        val SVG_PRESERVEASPECTRATIO_XMINYMIN: Short = 2
        val SVG_PRESERVEASPECTRATIO_XMIDYMIN: Short = 3
        val SVG_PRESERVEASPECTRATIO_XMAXYMIN: Short = 4
        val SVG_PRESERVEASPECTRATIO_XMINYMID: Short = 5
        val SVG_PRESERVEASPECTRATIO_XMIDYMID: Short = 6
        val SVG_PRESERVEASPECTRATIO_XMAXYMID: Short = 7
        val SVG_PRESERVEASPECTRATIO_XMINYMAX: Short = 8
        val SVG_PRESERVEASPECTRATIO_XMIDYMAX: Short = 9
        val SVG_PRESERVEASPECTRATIO_XMAXYMAX: Short = 10
        val SVG_MEETORSLICE_UNKNOWN: Short = 0
        val SVG_MEETORSLICE_MEET: Short = 1
        val SVG_MEETORSLICE_SLICE: Short = 2
    }
}

@native public abstract class SVGAnimatedPreserveAspectRatio {
    open val baseVal: SVGPreserveAspectRatio
        get() = noImpl
    open val animVal: SVGPreserveAspectRatio
        get() = noImpl
}

@native public abstract class SVGPathElement : SVGGeometryElement() {
}

@native public abstract class SVGRectElement : SVGGeometryElement() {
    open val x: SVGAnimatedLength
        get() = noImpl
    open val y: SVGAnimatedLength
        get() = noImpl
    open val width: SVGAnimatedLength
        get() = noImpl
    open val height: SVGAnimatedLength
        get() = noImpl
    open val rx: SVGAnimatedLength
        get() = noImpl
    open val ry: SVGAnimatedLength
        get() = noImpl
}

@native public abstract class SVGCircleElement : SVGGeometryElement() {
    open val cx: SVGAnimatedLength
        get() = noImpl
    open val cy: SVGAnimatedLength
        get() = noImpl
    open val r: SVGAnimatedLength
        get() = noImpl
}

@native public abstract class SVGEllipseElement : SVGGeometryElement() {
    open val cx: SVGAnimatedLength
        get() = noImpl
    open val cy: SVGAnimatedLength
        get() = noImpl
    open val rx: SVGAnimatedLength
        get() = noImpl
    open val ry: SVGAnimatedLength
        get() = noImpl
}

@native public abstract class SVGLineElement : SVGGeometryElement() {
    open val x1: SVGAnimatedLength
        get() = noImpl
    open val y1: SVGAnimatedLength
        get() = noImpl
    open val x2: SVGAnimatedLength
        get() = noImpl
    open val y2: SVGAnimatedLength
        get() = noImpl
}

@native public abstract class SVGMeshElement : SVGGeometryElement(), SVGURIReference {
}

@native public interface SVGAnimatedPoints {
    val points: SVGPointList
        get() = noImpl
    val animatedPoints: SVGPointList
        get() = noImpl
}

@native public abstract class SVGPointList {
    open val length: Int
        get() = noImpl
    open val numberOfItems: Int
        get() = noImpl
    fun clear(): Unit = noImpl
    fun initialize(newItem: DOMPoint): DOMPoint = noImpl
    fun getItem(index: Int): DOMPoint = noImpl
    @nativeGetter
    operator fun get(index: Int): DOMPoint? = noImpl
    fun insertItemBefore(newItem: DOMPoint, index: Int): DOMPoint = noImpl
    fun replaceItem(newItem: DOMPoint, index: Int): DOMPoint = noImpl
    fun removeItem(index: Int): DOMPoint = noImpl
    fun appendItem(newItem: DOMPoint): DOMPoint = noImpl
    @nativeSetter
    operator fun set(index: Int, newItem: DOMPoint): Unit = noImpl
}

@native public abstract class SVGPolylineElement : SVGGeometryElement(), SVGAnimatedPoints {
}

@native public abstract class SVGPolygonElement : SVGGeometryElement(), SVGAnimatedPoints {
}

@native public abstract class SVGTextContentElement : SVGGraphicsElement() {
    open val textLength: SVGAnimatedLength
        get() = noImpl
    open val lengthAdjust: SVGAnimatedEnumeration
        get() = noImpl
    fun getNumberOfChars(): Int = noImpl
    fun getComputedTextLength(): Float = noImpl
    fun getSubStringLength(charnum: Int, nchars: Int): Float = noImpl
    fun getStartPositionOfChar(charnum: Int): DOMPoint = noImpl
    fun getEndPositionOfChar(charnum: Int): DOMPoint = noImpl
    fun getExtentOfChar(charnum: Int): DOMRect = noImpl
    fun getRotationOfChar(charnum: Int): Float = noImpl
    fun getCharNumAtPosition(point: DOMPoint): Int = noImpl
    fun selectSubString(charnum: Int, nchars: Int): Unit = noImpl

    companion object {
        val LENGTHADJUST_UNKNOWN: Short = 0
        val LENGTHADJUST_SPACING: Short = 1
        val LENGTHADJUST_SPACINGANDGLYPHS: Short = 2
    }
}

@native public abstract class SVGTextPositioningElement : SVGTextContentElement() {
    open val x: SVGAnimatedLengthList
        get() = noImpl
    open val y: SVGAnimatedLengthList
        get() = noImpl
    open val dx: SVGAnimatedLengthList
        get() = noImpl
    open val dy: SVGAnimatedLengthList
        get() = noImpl
    open val rotate: SVGAnimatedNumberList
        get() = noImpl
}

@native public abstract class SVGTextElement : SVGTextPositioningElement() {
}

@native public abstract class SVGTSpanElement : SVGTextPositioningElement() {
}

@native public abstract class SVGTextPathElement : SVGTextContentElement(), SVGURIReference {
    open val startOffset: SVGAnimatedLength
        get() = noImpl
    open val method: SVGAnimatedEnumeration
        get() = noImpl
    open val spacing: SVGAnimatedEnumeration
        get() = noImpl

    companion object {
        val TEXTPATH_METHODTYPE_UNKNOWN: Short = 0
        val TEXTPATH_METHODTYPE_ALIGN: Short = 1
        val TEXTPATH_METHODTYPE_STRETCH: Short = 2
        val TEXTPATH_SPACINGTYPE_UNKNOWN: Short = 0
        val TEXTPATH_SPACINGTYPE_AUTO: Short = 1
        val TEXTPATH_SPACINGTYPE_EXACT: Short = 2
    }
}

@native public abstract class SVGImageElement : SVGGraphicsElement(), SVGURIReference, HTMLOrSVGImageElement {
    open val x: SVGAnimatedLength
        get() = noImpl
    open val y: SVGAnimatedLength
        get() = noImpl
    open val width: SVGAnimatedLength
        get() = noImpl
    open val height: SVGAnimatedLength
        get() = noImpl
    open val preserveAspectRatio: SVGAnimatedPreserveAspectRatio
        get() = noImpl
    open var crossOrigin: String?
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class SVGForeignObjectElement : SVGGraphicsElement() {
    open val x: SVGAnimatedLength
        get() = noImpl
    open val y: SVGAnimatedLength
        get() = noImpl
    open val width: SVGAnimatedLength
        get() = noImpl
    open val height: SVGAnimatedLength
        get() = noImpl
}

@native public abstract class SVGMarkerElement : SVGElement(), SVGFitToViewBox {
    open val refX: SVGAnimatedLength
        get() = noImpl
    open val refY: SVGAnimatedLength
        get() = noImpl
    open val markerUnits: SVGAnimatedEnumeration
        get() = noImpl
    open val markerWidth: SVGAnimatedLength
        get() = noImpl
    open val markerHeight: SVGAnimatedLength
        get() = noImpl
    open val orientType: SVGAnimatedEnumeration
        get() = noImpl
    open val orientAngle: SVGAnimatedAngle
        get() = noImpl
    open var orient: String
        get() = noImpl
        set(value) = noImpl
    fun setOrientToAuto(): Unit = noImpl
    fun setOrientToAngle(angle: SVGAngle): Unit = noImpl

    companion object {
        val SVG_MARKERUNITS_UNKNOWN: Short = 0
        val SVG_MARKERUNITS_USERSPACEONUSE: Short = 1
        val SVG_MARKERUNITS_STROKEWIDTH: Short = 2
        val SVG_MARKER_ORIENT_UNKNOWN: Short = 0
        val SVG_MARKER_ORIENT_AUTO: Short = 1
        val SVG_MARKER_ORIENT_ANGLE: Short = 2
    }
}

@native public abstract class SVGSolidcolorElement : SVGElement() {
}

@native public abstract class SVGGradientElement : SVGElement(), SVGURIReference, SVGUnitTypes {
    open val gradientUnits: SVGAnimatedEnumeration
        get() = noImpl
    open val gradientTransform: SVGAnimatedTransformList
        get() = noImpl
    open val spreadMethod: SVGAnimatedEnumeration
        get() = noImpl

    companion object {
        val SVG_SPREADMETHOD_UNKNOWN: Short = 0
        val SVG_SPREADMETHOD_PAD: Short = 1
        val SVG_SPREADMETHOD_REFLECT: Short = 2
        val SVG_SPREADMETHOD_REPEAT: Short = 3
        val SVG_UNIT_TYPE_UNKNOWN: Short = 0
        val SVG_UNIT_TYPE_USERSPACEONUSE: Short = 1
        val SVG_UNIT_TYPE_OBJECTBOUNDINGBOX: Short = 2
    }
}

@native public abstract class SVGLinearGradientElement : SVGGradientElement() {
    open val x1: SVGAnimatedLength
        get() = noImpl
    open val y1: SVGAnimatedLength
        get() = noImpl
    open val x2: SVGAnimatedLength
        get() = noImpl
    open val y2: SVGAnimatedLength
        get() = noImpl
}

@native public abstract class SVGRadialGradientElement : SVGGradientElement() {
    open val cx: SVGAnimatedLength
        get() = noImpl
    open val cy: SVGAnimatedLength
        get() = noImpl
    open val r: SVGAnimatedLength
        get() = noImpl
    open val fx: SVGAnimatedLength
        get() = noImpl
    open val fy: SVGAnimatedLength
        get() = noImpl
    open val fr: SVGAnimatedLength
        get() = noImpl
}

@native public abstract class SVGMeshGradientElement : SVGGradientElement() {
}

@native public abstract class SVGMeshrowElement : SVGElement() {
}

@native public abstract class SVGMeshpatchElement : SVGElement() {
}

@native public abstract class SVGStopElement : SVGElement() {
    open val offset: SVGAnimatedNumber
        get() = noImpl
}

@native public abstract class SVGPatternElement : SVGElement(), SVGFitToViewBox, SVGURIReference, SVGUnitTypes {
    open val patternUnits: SVGAnimatedEnumeration
        get() = noImpl
    open val patternContentUnits: SVGAnimatedEnumeration
        get() = noImpl
    open val patternTransform: SVGAnimatedTransformList
        get() = noImpl
    open val x: SVGAnimatedLength
        get() = noImpl
    open val y: SVGAnimatedLength
        get() = noImpl
    open val width: SVGAnimatedLength
        get() = noImpl
    open val height: SVGAnimatedLength
        get() = noImpl

    companion object {
        val SVG_UNIT_TYPE_UNKNOWN: Short = 0
        val SVG_UNIT_TYPE_USERSPACEONUSE: Short = 1
        val SVG_UNIT_TYPE_OBJECTBOUNDINGBOX: Short = 2
    }
}

@native public abstract class SVGHatchElement : SVGElement() {
}

@native public abstract class SVGHatchpathElement : SVGElement() {
}

@native public abstract class SVGCursorElement : SVGElement(), SVGURIReference {
    open val x: SVGAnimatedLength
        get() = noImpl
    open val y: SVGAnimatedLength
        get() = noImpl
}

@native public abstract class SVGScriptElement : SVGElement(), SVGURIReference, HTMLOrSVGScriptElement {
    open var type: String
        get() = noImpl
        set(value) = noImpl
    open var crossOrigin: String?
        get() = noImpl
        set(value) = noImpl
}

@native public abstract class SVGAElement : SVGGraphicsElement(), SVGURIReference {
    open val target: SVGAnimatedString
        get() = noImpl
    open val download: SVGAnimatedString
        get() = noImpl
    open val rel: SVGAnimatedString
        get() = noImpl
    open val relList: SVGAnimatedString
        get() = noImpl
    open val hreflang: SVGAnimatedString
        get() = noImpl
    open val type: SVGAnimatedString
        get() = noImpl
}

@native public abstract class SVGViewElement : SVGElement(), SVGFitToViewBox, SVGZoomAndPan {

    companion object {
        val SVG_ZOOMANDPAN_UNKNOWN: Short = 0
        val SVG_ZOOMANDPAN_DISABLE: Short = 1
        val SVG_ZOOMANDPAN_MAGNIFY: Short = 2
    }
}

