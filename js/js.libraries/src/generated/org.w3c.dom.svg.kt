/*
 * Generated file
 * DO NOT EDIT
 * 
 * See libraries/tools/idl2k for details
 */

@file:Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
package org.w3c.dom.svg

import kotlin.js.*
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

public external abstract class SVGElement : Element, ElementCSSInlineStyle, GlobalEventHandlers, SVGElementInstance {
    open val dataset: DOMStringMap
    open val ownerSVGElement: SVGSVGElement?
    open val viewportElement: SVGElement?
    open var tabIndex: Int
    fun focus(): Unit
    fun blur(): Unit
}

public external interface SVGBoundingBoxOptions {
    var fill: Boolean? /* = true */
        get() = definedExternally
        set(value) = definedExternally
    var stroke: Boolean? /* = false */
        get() = definedExternally
        set(value) = definedExternally
    var markers: Boolean? /* = false */
        get() = definedExternally
        set(value) = definedExternally
    var clipped: Boolean? /* = false */
        get() = definedExternally
        set(value) = definedExternally
}

@kotlin.internal.InlineOnly
public inline fun SVGBoundingBoxOptions(fill: Boolean? = true, stroke: Boolean? = false, markers: Boolean? = false, clipped: Boolean? = false): SVGBoundingBoxOptions {
    val o = js("({})")

    o["fill"] = fill
    o["stroke"] = stroke
    o["markers"] = markers
    o["clipped"] = clipped

    return o
}

public external abstract class SVGGraphicsElement : SVGElement, SVGTests {
    open val transform: SVGAnimatedTransformList
    fun getBBox(options: SVGBoundingBoxOptions = definedExternally): DOMRect
    fun getCTM(): DOMMatrix?
    fun getScreenCTM(): DOMMatrix?
}

public external abstract class SVGGeometryElement : SVGGraphicsElement {
    open val pathLength: SVGAnimatedNumber
    fun isPointInFill(point: DOMPoint): Boolean
    fun isPointInStroke(point: DOMPoint): Boolean
    fun getTotalLength(): Float
    fun getPointAtLength(distance: Float): DOMPoint
}

public external abstract class SVGNumber {
    open var value: Float
}

public external abstract class SVGLength {
    open val unitType: Short
    open var value: Float
    open var valueInSpecifiedUnits: Float
    open var valueAsString: String
    fun newValueSpecifiedUnits(unitType: Short, valueInSpecifiedUnits: Float): Unit
    fun convertToSpecifiedUnits(unitType: Short): Unit

    companion object {
        val SVG_LENGTHTYPE_UNKNOWN: Short
        val SVG_LENGTHTYPE_NUMBER: Short
        val SVG_LENGTHTYPE_PERCENTAGE: Short
        val SVG_LENGTHTYPE_EMS: Short
        val SVG_LENGTHTYPE_EXS: Short
        val SVG_LENGTHTYPE_PX: Short
        val SVG_LENGTHTYPE_CM: Short
        val SVG_LENGTHTYPE_MM: Short
        val SVG_LENGTHTYPE_IN: Short
        val SVG_LENGTHTYPE_PT: Short
        val SVG_LENGTHTYPE_PC: Short
    }
}

public external abstract class SVGAngle {
    open val unitType: Short
    open var value: Float
    open var valueInSpecifiedUnits: Float
    open var valueAsString: String
    fun newValueSpecifiedUnits(unitType: Short, valueInSpecifiedUnits: Float): Unit
    fun convertToSpecifiedUnits(unitType: Short): Unit

    companion object {
        val SVG_ANGLETYPE_UNKNOWN: Short
        val SVG_ANGLETYPE_UNSPECIFIED: Short
        val SVG_ANGLETYPE_DEG: Short
        val SVG_ANGLETYPE_RAD: Short
        val SVG_ANGLETYPE_GRAD: Short
    }
}

public external abstract class SVGNameList {
    open val length: Int
    open val numberOfItems: Int
    fun clear(): Unit
    fun initialize(newItem: dynamic): dynamic
    fun getItem(index: Int): dynamic
    fun insertItemBefore(newItem: dynamic, index: Int): dynamic
    fun replaceItem(newItem: dynamic, index: Int): dynamic
    fun removeItem(index: Int): dynamic
    fun appendItem(newItem: dynamic): dynamic
}
@kotlin.internal.InlineOnly inline operator fun SVGNameList.get(index: Int): dynamic = asDynamic()[index]
@kotlin.internal.InlineOnly inline operator fun SVGNameList.set(index: Int, newItem: dynamic): Unit { asDynamic()[index] = newItem; }

public external abstract class SVGNumberList {
    open val length: Int
    open val numberOfItems: Int
    fun clear(): Unit
    fun initialize(newItem: SVGNumber): SVGNumber
    fun getItem(index: Int): SVGNumber
    fun insertItemBefore(newItem: SVGNumber, index: Int): SVGNumber
    fun replaceItem(newItem: SVGNumber, index: Int): SVGNumber
    fun removeItem(index: Int): SVGNumber
    fun appendItem(newItem: SVGNumber): SVGNumber
}
@kotlin.internal.InlineOnly inline operator fun SVGNumberList.get(index: Int): SVGNumber? = asDynamic()[index]
@kotlin.internal.InlineOnly inline operator fun SVGNumberList.set(index: Int, newItem: SVGNumber): Unit { asDynamic()[index] = newItem; }

public external abstract class SVGLengthList {
    open val length: Int
    open val numberOfItems: Int
    fun clear(): Unit
    fun initialize(newItem: SVGLength): SVGLength
    fun getItem(index: Int): SVGLength
    fun insertItemBefore(newItem: SVGLength, index: Int): SVGLength
    fun replaceItem(newItem: SVGLength, index: Int): SVGLength
    fun removeItem(index: Int): SVGLength
    fun appendItem(newItem: SVGLength): SVGLength
}
@kotlin.internal.InlineOnly inline operator fun SVGLengthList.get(index: Int): SVGLength? = asDynamic()[index]
@kotlin.internal.InlineOnly inline operator fun SVGLengthList.set(index: Int, newItem: SVGLength): Unit { asDynamic()[index] = newItem; }

public external abstract class SVGAnimatedBoolean {
    open var baseVal: Boolean
    open val animVal: Boolean
}

public external abstract class SVGAnimatedEnumeration {
    open var baseVal: Short
    open val animVal: Short
}

public external abstract class SVGAnimatedInteger {
    open var baseVal: Int
    open val animVal: Int
}

public external abstract class SVGAnimatedNumber {
    open var baseVal: Float
    open val animVal: Float
}

public external abstract class SVGAnimatedLength {
    open val baseVal: SVGLength
    open val animVal: SVGLength
}

public external abstract class SVGAnimatedAngle {
    open val baseVal: SVGAngle
    open val animVal: SVGAngle
}

public external abstract class SVGAnimatedString {
    open var baseVal: String
    open val animVal: String
}

public external abstract class SVGAnimatedRect {
    open val baseVal: DOMRect
    open val animVal: DOMRectReadOnly
}

public external abstract class SVGAnimatedNumberList {
    open val baseVal: SVGNumberList
    open val animVal: SVGNumberList
}

public external abstract class SVGAnimatedLengthList {
    open val baseVal: SVGLengthList
    open val animVal: SVGLengthList
}

public external abstract class SVGStringList {
    open val length: Int
    open val numberOfItems: Int
    fun clear(): Unit
    fun initialize(newItem: String): String
    fun getItem(index: Int): String
    fun insertItemBefore(newItem: String, index: Int): String
    fun replaceItem(newItem: String, index: Int): String
    fun removeItem(index: Int): String
    fun appendItem(newItem: String): String
}
@kotlin.internal.InlineOnly inline operator fun SVGStringList.get(index: Int): String? = asDynamic()[index]
@kotlin.internal.InlineOnly inline operator fun SVGStringList.set(index: Int, newItem: String): Unit { asDynamic()[index] = newItem; }

public external interface SVGUnitTypes {

    companion object {
        val SVG_UNIT_TYPE_UNKNOWN: Short
        val SVG_UNIT_TYPE_USERSPACEONUSE: Short
        val SVG_UNIT_TYPE_OBJECTBOUNDINGBOX: Short
    }
}

public external interface SVGTests {
    val requiredExtensions: SVGStringList
    val systemLanguage: SVGStringList
}

public external interface SVGFitToViewBox {
    val viewBox: SVGAnimatedRect
    val preserveAspectRatio: SVGAnimatedPreserveAspectRatio
}

public external interface SVGZoomAndPan {
    var zoomAndPan: Short

    companion object {
        val SVG_ZOOMANDPAN_UNKNOWN: Short
        val SVG_ZOOMANDPAN_DISABLE: Short
        val SVG_ZOOMANDPAN_MAGNIFY: Short
    }
}

public external interface SVGURIReference {
    val href: SVGAnimatedString
}

public external abstract class SVGSVGElement : SVGGraphicsElement, SVGFitToViewBox, SVGZoomAndPan, WindowEventHandlers {
    open val x: SVGAnimatedLength
    open val y: SVGAnimatedLength
    open val width: SVGAnimatedLength
    open val height: SVGAnimatedLength
    open var currentScale: Float
    open val currentTranslate: DOMPointReadOnly
    fun getIntersectionList(rect: DOMRectReadOnly, referenceElement: SVGElement?): NodeList
    fun getEnclosureList(rect: DOMRectReadOnly, referenceElement: SVGElement?): NodeList
    fun checkIntersection(element: SVGElement, rect: DOMRectReadOnly): Boolean
    fun checkEnclosure(element: SVGElement, rect: DOMRectReadOnly): Boolean
    fun deselectAll(): Unit
    fun createSVGNumber(): SVGNumber
    fun createSVGLength(): SVGLength
    fun createSVGAngle(): SVGAngle
    fun createSVGPoint(): DOMPoint
    fun createSVGMatrix(): DOMMatrix
    fun createSVGRect(): DOMRect
    fun createSVGTransform(): SVGTransform
    fun createSVGTransformFromMatrix(matrix: DOMMatrixReadOnly): SVGTransform
    fun getElementById(elementId: String): Element
    fun suspendRedraw(maxWaitMilliseconds: Int): Int
    fun unsuspendRedraw(suspendHandleID: Int): Unit
    fun unsuspendRedrawAll(): Unit
    fun forceRedraw(): Unit

    companion object {
        val SVG_ZOOMANDPAN_UNKNOWN: Short
        val SVG_ZOOMANDPAN_DISABLE: Short
        val SVG_ZOOMANDPAN_MAGNIFY: Short
    }
}

public external abstract class SVGGElement : SVGGraphicsElement {
}

public external abstract class SVGUnknownElement : SVGGraphicsElement {
}

public external abstract class SVGDefsElement : SVGGraphicsElement {
}

public external abstract class SVGDescElement : SVGElement {
}

public external abstract class SVGMetadataElement : SVGElement {
}

public external abstract class SVGTitleElement : SVGElement {
}

public external abstract class SVGSymbolElement : SVGGraphicsElement, SVGFitToViewBox {
}

public external abstract class SVGUseElement : SVGGraphicsElement, SVGURIReference {
    open val x: SVGAnimatedLength
    open val y: SVGAnimatedLength
    open val width: SVGAnimatedLength
    open val height: SVGAnimatedLength
    open val instanceRoot: SVGElement?
    open val animatedInstanceRoot: SVGElement?
}

public external open class SVGUseElementShadowRoot : ShadowRoot {
    override fun getElementById(elementId: String): Element?
    override fun prepend(vararg nodes: dynamic): Unit
    override fun append(vararg nodes: dynamic): Unit
    override fun querySelector(selectors: String): Element?
    override fun querySelectorAll(selectors: String): NodeList
}

public external interface SVGElementInstance {
    val correspondingElement: SVGElement?
    val correspondingUseElement: SVGUseElement?
}

public external open class ShadowAnimation(source: dynamic, newTarget: dynamic) {
    open val sourceAnimation: dynamic
}

public external abstract class SVGSwitchElement : SVGGraphicsElement {
}

public external interface GetSVGDocument {
    fun getSVGDocument(): Document
}

public external abstract class SVGStyleElement : SVGElement, LinkStyle {
    open var type: String
    open var media: String
    open var title: String
}

public external abstract class SVGTransform {
    open val type: Short
    open val matrix: DOMMatrix
    open val angle: Float
    fun setMatrix(matrix: DOMMatrixReadOnly): Unit
    fun setTranslate(tx: Float, ty: Float): Unit
    fun setScale(sx: Float, sy: Float): Unit
    fun setRotate(angle: Float, cx: Float, cy: Float): Unit
    fun setSkewX(angle: Float): Unit
    fun setSkewY(angle: Float): Unit

    companion object {
        val SVG_TRANSFORM_UNKNOWN: Short
        val SVG_TRANSFORM_MATRIX: Short
        val SVG_TRANSFORM_TRANSLATE: Short
        val SVG_TRANSFORM_SCALE: Short
        val SVG_TRANSFORM_ROTATE: Short
        val SVG_TRANSFORM_SKEWX: Short
        val SVG_TRANSFORM_SKEWY: Short
    }
}

public external abstract class SVGTransformList {
    open val length: Int
    open val numberOfItems: Int
    fun clear(): Unit
    fun initialize(newItem: SVGTransform): SVGTransform
    fun getItem(index: Int): SVGTransform
    fun insertItemBefore(newItem: SVGTransform, index: Int): SVGTransform
    fun replaceItem(newItem: SVGTransform, index: Int): SVGTransform
    fun removeItem(index: Int): SVGTransform
    fun appendItem(newItem: SVGTransform): SVGTransform
    fun createSVGTransformFromMatrix(matrix: DOMMatrixReadOnly): SVGTransform
    fun consolidate(): SVGTransform?
}
@kotlin.internal.InlineOnly inline operator fun SVGTransformList.get(index: Int): SVGTransform? = asDynamic()[index]
@kotlin.internal.InlineOnly inline operator fun SVGTransformList.set(index: Int, newItem: SVGTransform): Unit { asDynamic()[index] = newItem; }

public external abstract class SVGAnimatedTransformList {
    open val baseVal: SVGTransformList
    open val animVal: SVGTransformList
}

public external abstract class SVGPreserveAspectRatio {
    open var align: Short
    open var meetOrSlice: Short

    companion object {
        val SVG_PRESERVEASPECTRATIO_UNKNOWN: Short
        val SVG_PRESERVEASPECTRATIO_NONE: Short
        val SVG_PRESERVEASPECTRATIO_XMINYMIN: Short
        val SVG_PRESERVEASPECTRATIO_XMIDYMIN: Short
        val SVG_PRESERVEASPECTRATIO_XMAXYMIN: Short
        val SVG_PRESERVEASPECTRATIO_XMINYMID: Short
        val SVG_PRESERVEASPECTRATIO_XMIDYMID: Short
        val SVG_PRESERVEASPECTRATIO_XMAXYMID: Short
        val SVG_PRESERVEASPECTRATIO_XMINYMAX: Short
        val SVG_PRESERVEASPECTRATIO_XMIDYMAX: Short
        val SVG_PRESERVEASPECTRATIO_XMAXYMAX: Short
        val SVG_MEETORSLICE_UNKNOWN: Short
        val SVG_MEETORSLICE_MEET: Short
        val SVG_MEETORSLICE_SLICE: Short
    }
}

public external abstract class SVGAnimatedPreserveAspectRatio {
    open val baseVal: SVGPreserveAspectRatio
    open val animVal: SVGPreserveAspectRatio
}

public external abstract class SVGPathElement : SVGGeometryElement {
}

public external abstract class SVGRectElement : SVGGeometryElement {
    open val x: SVGAnimatedLength
    open val y: SVGAnimatedLength
    open val width: SVGAnimatedLength
    open val height: SVGAnimatedLength
    open val rx: SVGAnimatedLength
    open val ry: SVGAnimatedLength
}

public external abstract class SVGCircleElement : SVGGeometryElement {
    open val cx: SVGAnimatedLength
    open val cy: SVGAnimatedLength
    open val r: SVGAnimatedLength
}

public external abstract class SVGEllipseElement : SVGGeometryElement {
    open val cx: SVGAnimatedLength
    open val cy: SVGAnimatedLength
    open val rx: SVGAnimatedLength
    open val ry: SVGAnimatedLength
}

public external abstract class SVGLineElement : SVGGeometryElement {
    open val x1: SVGAnimatedLength
    open val y1: SVGAnimatedLength
    open val x2: SVGAnimatedLength
    open val y2: SVGAnimatedLength
}

public external abstract class SVGMeshElement : SVGGeometryElement, SVGURIReference {
}

public external interface SVGAnimatedPoints {
    val points: SVGPointList
    val animatedPoints: SVGPointList
}

public external abstract class SVGPointList {
    open val length: Int
    open val numberOfItems: Int
    fun clear(): Unit
    fun initialize(newItem: DOMPoint): DOMPoint
    fun getItem(index: Int): DOMPoint
    fun insertItemBefore(newItem: DOMPoint, index: Int): DOMPoint
    fun replaceItem(newItem: DOMPoint, index: Int): DOMPoint
    fun removeItem(index: Int): DOMPoint
    fun appendItem(newItem: DOMPoint): DOMPoint
}
@kotlin.internal.InlineOnly inline operator fun SVGPointList.get(index: Int): DOMPoint? = asDynamic()[index]
@kotlin.internal.InlineOnly inline operator fun SVGPointList.set(index: Int, newItem: DOMPoint): Unit { asDynamic()[index] = newItem; }

public external abstract class SVGPolylineElement : SVGGeometryElement, SVGAnimatedPoints {
}

public external abstract class SVGPolygonElement : SVGGeometryElement, SVGAnimatedPoints {
}

public external abstract class SVGTextContentElement : SVGGraphicsElement {
    open val textLength: SVGAnimatedLength
    open val lengthAdjust: SVGAnimatedEnumeration
    fun getNumberOfChars(): Int
    fun getComputedTextLength(): Float
    fun getSubStringLength(charnum: Int, nchars: Int): Float
    fun getStartPositionOfChar(charnum: Int): DOMPoint
    fun getEndPositionOfChar(charnum: Int): DOMPoint
    fun getExtentOfChar(charnum: Int): DOMRect
    fun getRotationOfChar(charnum: Int): Float
    fun getCharNumAtPosition(point: DOMPoint): Int
    fun selectSubString(charnum: Int, nchars: Int): Unit

    companion object {
        val LENGTHADJUST_UNKNOWN: Short
        val LENGTHADJUST_SPACING: Short
        val LENGTHADJUST_SPACINGANDGLYPHS: Short
    }
}

public external abstract class SVGTextPositioningElement : SVGTextContentElement {
    open val x: SVGAnimatedLengthList
    open val y: SVGAnimatedLengthList
    open val dx: SVGAnimatedLengthList
    open val dy: SVGAnimatedLengthList
    open val rotate: SVGAnimatedNumberList
}

public external abstract class SVGTextElement : SVGTextPositioningElement {
}

public external abstract class SVGTSpanElement : SVGTextPositioningElement {
}

public external abstract class SVGTextPathElement : SVGTextContentElement, SVGURIReference {
    open val startOffset: SVGAnimatedLength
    open val method: SVGAnimatedEnumeration
    open val spacing: SVGAnimatedEnumeration

    companion object {
        val TEXTPATH_METHODTYPE_UNKNOWN: Short
        val TEXTPATH_METHODTYPE_ALIGN: Short
        val TEXTPATH_METHODTYPE_STRETCH: Short
        val TEXTPATH_SPACINGTYPE_UNKNOWN: Short
        val TEXTPATH_SPACINGTYPE_AUTO: Short
        val TEXTPATH_SPACINGTYPE_EXACT: Short
    }
}

public external abstract class SVGImageElement : SVGGraphicsElement, SVGURIReference, HTMLOrSVGImageElement {
    open val x: SVGAnimatedLength
    open val y: SVGAnimatedLength
    open val width: SVGAnimatedLength
    open val height: SVGAnimatedLength
    open val preserveAspectRatio: SVGAnimatedPreserveAspectRatio
    open var crossOrigin: String?
}

public external abstract class SVGForeignObjectElement : SVGGraphicsElement {
    open val x: SVGAnimatedLength
    open val y: SVGAnimatedLength
    open val width: SVGAnimatedLength
    open val height: SVGAnimatedLength
}

public external abstract class SVGMarkerElement : SVGElement, SVGFitToViewBox {
    open val refX: SVGAnimatedLength
    open val refY: SVGAnimatedLength
    open val markerUnits: SVGAnimatedEnumeration
    open val markerWidth: SVGAnimatedLength
    open val markerHeight: SVGAnimatedLength
    open val orientType: SVGAnimatedEnumeration
    open val orientAngle: SVGAnimatedAngle
    open var orient: String
    fun setOrientToAuto(): Unit
    fun setOrientToAngle(angle: SVGAngle): Unit

    companion object {
        val SVG_MARKERUNITS_UNKNOWN: Short
        val SVG_MARKERUNITS_USERSPACEONUSE: Short
        val SVG_MARKERUNITS_STROKEWIDTH: Short
        val SVG_MARKER_ORIENT_UNKNOWN: Short
        val SVG_MARKER_ORIENT_AUTO: Short
        val SVG_MARKER_ORIENT_ANGLE: Short
    }
}

public external abstract class SVGSolidcolorElement : SVGElement {
}

public external abstract class SVGGradientElement : SVGElement, SVGURIReference, SVGUnitTypes {
    open val gradientUnits: SVGAnimatedEnumeration
    open val gradientTransform: SVGAnimatedTransformList
    open val spreadMethod: SVGAnimatedEnumeration

    companion object {
        val SVG_SPREADMETHOD_UNKNOWN: Short
        val SVG_SPREADMETHOD_PAD: Short
        val SVG_SPREADMETHOD_REFLECT: Short
        val SVG_SPREADMETHOD_REPEAT: Short
        val SVG_UNIT_TYPE_UNKNOWN: Short
        val SVG_UNIT_TYPE_USERSPACEONUSE: Short
        val SVG_UNIT_TYPE_OBJECTBOUNDINGBOX: Short
    }
}

public external abstract class SVGLinearGradientElement : SVGGradientElement {
    open val x1: SVGAnimatedLength
    open val y1: SVGAnimatedLength
    open val x2: SVGAnimatedLength
    open val y2: SVGAnimatedLength
}

public external abstract class SVGRadialGradientElement : SVGGradientElement {
    open val cx: SVGAnimatedLength
    open val cy: SVGAnimatedLength
    open val r: SVGAnimatedLength
    open val fx: SVGAnimatedLength
    open val fy: SVGAnimatedLength
    open val fr: SVGAnimatedLength
}

public external abstract class SVGMeshGradientElement : SVGGradientElement {
}

public external abstract class SVGMeshrowElement : SVGElement {
}

public external abstract class SVGMeshpatchElement : SVGElement {
}

public external abstract class SVGStopElement : SVGElement {
    open val offset: SVGAnimatedNumber
}

public external abstract class SVGPatternElement : SVGElement, SVGFitToViewBox, SVGURIReference, SVGUnitTypes {
    open val patternUnits: SVGAnimatedEnumeration
    open val patternContentUnits: SVGAnimatedEnumeration
    open val patternTransform: SVGAnimatedTransformList
    open val x: SVGAnimatedLength
    open val y: SVGAnimatedLength
    open val width: SVGAnimatedLength
    open val height: SVGAnimatedLength

    companion object {
        val SVG_UNIT_TYPE_UNKNOWN: Short
        val SVG_UNIT_TYPE_USERSPACEONUSE: Short
        val SVG_UNIT_TYPE_OBJECTBOUNDINGBOX: Short
    }
}

public external abstract class SVGHatchElement : SVGElement {
}

public external abstract class SVGHatchpathElement : SVGElement {
}

public external abstract class SVGCursorElement : SVGElement, SVGURIReference {
    open val x: SVGAnimatedLength
    open val y: SVGAnimatedLength
}

public external abstract class SVGScriptElement : SVGElement, SVGURIReference, HTMLOrSVGScriptElement {
    open var type: String
    open var crossOrigin: String?
}

public external abstract class SVGAElement : SVGGraphicsElement, SVGURIReference {
    open val target: SVGAnimatedString
    open val download: SVGAnimatedString
    open val rel: SVGAnimatedString
    open val relList: SVGAnimatedString
    open val hreflang: SVGAnimatedString
    open val type: SVGAnimatedString
}

public external abstract class SVGViewElement : SVGElement, SVGFitToViewBox, SVGZoomAndPan {

    companion object {
        val SVG_ZOOMANDPAN_UNKNOWN: Short
        val SVG_ZOOMANDPAN_DISABLE: Short
        val SVG_ZOOMANDPAN_MAGNIFY: Short
    }
}

