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

@native public open class SVGDocument : Document() {
    open val rootElement: SVGSVGElement
        get() = noImpl
}

@native public interface SVGSVGElement : SVGElement, SVGTests, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGLocatable, SVGFitToViewBox, SVGZoomAndPan {
    val x: SVGAnimatedLength
        get() = noImpl
    val y: SVGAnimatedLength
        get() = noImpl
    val width: SVGAnimatedLength
        get() = noImpl
    val height: SVGAnimatedLength
        get() = noImpl
    var contentScriptType: dynamic
        get() = noImpl
        set(value) = noImpl
    var contentStyleType: dynamic
        get() = noImpl
        set(value) = noImpl
    val viewport: SVGRect
        get() = noImpl
    val pixelUnitToMillimeterX: Float
        get() = noImpl
    val pixelUnitToMillimeterY: Float
        get() = noImpl
    val screenPixelToMillimeterX: Float
        get() = noImpl
    val screenPixelToMillimeterY: Float
        get() = noImpl
    val useCurrentView: Boolean
        get() = noImpl
    val currentView: SVGViewSpec
        get() = noImpl
    var currentScale: Float
        get() = noImpl
        set(value) = noImpl
    val currentTranslate: SVGPoint
        get() = noImpl
    fun suspendRedraw(maxWaitMilliseconds: Int): Int = noImpl
    fun unsuspendRedraw(suspendHandleID: Int): Unit = noImpl
    fun unsuspendRedrawAll(): Unit = noImpl
    fun forceRedraw(): Unit = noImpl
    fun pauseAnimations(): Unit = noImpl
    fun unpauseAnimations(): Unit = noImpl
    fun animationsPaused(): Boolean = noImpl
    fun getCurrentTime(): Float = noImpl
    fun setCurrentTime(seconds: Float): Unit = noImpl
    fun getIntersectionList(rect: SVGRect, referenceElement: SVGElement): NodeList = noImpl
    fun getEnclosureList(rect: SVGRect, referenceElement: SVGElement): NodeList = noImpl
    fun checkIntersection(element: SVGElement, rect: SVGRect): Boolean = noImpl
    fun checkEnclosure(element: SVGElement, rect: SVGRect): Boolean = noImpl
    fun deselectAll(): Unit = noImpl
    fun createSVGNumber(): SVGNumber = noImpl
    fun createSVGLength(): SVGLength = noImpl
    fun createSVGAngle(): SVGAngle = noImpl
    fun createSVGPoint(): SVGPoint = noImpl
    fun createSVGMatrix(): SVGMatrix = noImpl
    fun createSVGRect(): SVGRect = noImpl
    fun createSVGTransform(): SVGTransform = noImpl
    fun createSVGTransformFromMatrix(matrix: SVGMatrix): SVGTransform = noImpl
    fun getElementById(elementId: String): Element = noImpl
}

@native public interface SVGGElement : SVGElement, SVGTests, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGTransformable {
}

@native public interface SVGDefsElement : SVGElement, SVGTests, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGTransformable {
}

@native public interface SVGDescElement : SVGElement, SVGLangSpace, SVGStylable {
}

@native public interface SVGTitleElement : SVGElement, SVGLangSpace, SVGStylable {
}

@native public interface SVGSymbolElement : SVGElement, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGFitToViewBox {
}

@native public interface SVGUseElement : SVGElement, SVGURIReference, SVGTests, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGTransformable {
    val x: SVGAnimatedLength
        get() = noImpl
    val y: SVGAnimatedLength
        get() = noImpl
    val width: SVGAnimatedLength
        get() = noImpl
    val height: SVGAnimatedLength
        get() = noImpl
    val instanceRoot: SVGElementInstance
        get() = noImpl
    val animatedInstanceRoot: SVGElementInstance
        get() = noImpl
}

@native public interface SVGElementInstance : EventTarget {
    val correspondingElement: SVGElement
        get() = noImpl
    val correspondingUseElement: SVGUseElement
        get() = noImpl
    val parentNode: SVGElementInstance
        get() = noImpl
    val childNodes: SVGElementInstanceList
        get() = noImpl
    val firstChild: SVGElementInstance
        get() = noImpl
    val lastChild: SVGElementInstance
        get() = noImpl
    val previousSibling: SVGElementInstance
        get() = noImpl
    val nextSibling: SVGElementInstance
        get() = noImpl
}

@native public interface SVGElementInstanceList {
    val length: Int
        get() = noImpl
    fun item(index: Int): SVGElementInstance = noImpl
}

@native public interface SVGImageElement : SVGElement, SVGURIReference, SVGTests, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGTransformable {
    val x: SVGAnimatedLength
        get() = noImpl
    val y: SVGAnimatedLength
        get() = noImpl
    val width: SVGAnimatedLength
        get() = noImpl
    val height: SVGAnimatedLength
        get() = noImpl
    val preserveAspectRatio: SVGAnimatedPreserveAspectRatio
        get() = noImpl
}

@native public interface SVGSwitchElement : SVGElement, SVGTests, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGTransformable {
}

@native public interface GetSVGDocument {
    fun getSVGDocument(): SVGDocument = noImpl
}

@native public interface SVGElement : Element {
//    var id: dynamic
//        get() = noImpl
//        set(value) = noImpl
    var xmlbase: dynamic
        get() = noImpl
        set(value) = noImpl
    val ownerSVGElement: SVGSVGElement
        get() = noImpl
    val viewportElement: SVGElement
        get() = noImpl
    val style: CSSStyleDeclaration
        get() = noImpl
}

@native public interface SVGAnimatedBoolean {
    var baseVal: dynamic
        get() = noImpl
        set(value) = noImpl
    val animVal: Boolean
        get() = noImpl
}

@native public interface SVGAnimatedString {
    var baseVal: dynamic
        get() = noImpl
        set(value) = noImpl
    val animVal: String
        get() = noImpl
}

@native public interface SVGStringList {
    val numberOfItems: Int
        get() = noImpl
    fun clear(): Unit = noImpl
    fun initialize(newItem: String): String = noImpl
    fun getItem(index: Int): String = noImpl
    fun insertItemBefore(newItem: String, index: Int): String = noImpl
    fun replaceItem(newItem: String, index: Int): String = noImpl
    fun removeItem(index: Int): String = noImpl
    fun appendItem(newItem: String): String = noImpl
}

@native public interface SVGAnimatedEnumeration {
    var baseVal: dynamic
        get() = noImpl
        set(value) = noImpl
    val animVal: Short
        get() = noImpl
}

@native public interface SVGAnimatedInteger {
    var baseVal: dynamic
        get() = noImpl
        set(value) = noImpl
    val animVal: Int
        get() = noImpl
}

@native public interface SVGNumber {
    var value: dynamic
        get() = noImpl
        set(value) = noImpl
}

@native public interface SVGAnimatedNumber {
    var baseVal: dynamic
        get() = noImpl
        set(value) = noImpl
    val animVal: Float
        get() = noImpl
}

@native public interface SVGNumberList {
    val numberOfItems: Int
        get() = noImpl
    fun clear(): Unit = noImpl
    fun initialize(newItem: SVGNumber): SVGNumber = noImpl
    fun getItem(index: Int): SVGNumber = noImpl
    fun insertItemBefore(newItem: SVGNumber, index: Int): SVGNumber = noImpl
    fun replaceItem(newItem: SVGNumber, index: Int): SVGNumber = noImpl
    fun removeItem(index: Int): SVGNumber = noImpl
    fun appendItem(newItem: SVGNumber): SVGNumber = noImpl
}

@native public interface SVGAnimatedNumberList {
    val baseVal: SVGNumberList
        get() = noImpl
    val animVal: SVGNumberList
        get() = noImpl
}

@native public interface SVGLength {
    val unitType: Short
        get() = noImpl
    var value: dynamic
        get() = noImpl
        set(value) = noImpl
    var valueInSpecifiedUnits: dynamic
        get() = noImpl
        set(value) = noImpl
    var valueAsString: dynamic
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

@native public interface SVGAnimatedLength {
    val baseVal: SVGLength
        get() = noImpl
    val animVal: SVGLength
        get() = noImpl
}

@native public interface SVGLengthList {
    val numberOfItems: Int
        get() = noImpl
    fun clear(): Unit = noImpl
    fun initialize(newItem: SVGLength): SVGLength = noImpl
    fun getItem(index: Int): SVGLength = noImpl
    fun insertItemBefore(newItem: SVGLength, index: Int): SVGLength = noImpl
    fun replaceItem(newItem: SVGLength, index: Int): SVGLength = noImpl
    fun removeItem(index: Int): SVGLength = noImpl
    fun appendItem(newItem: SVGLength): SVGLength = noImpl
}

@native public interface SVGAnimatedLengthList {
    val baseVal: SVGLengthList
        get() = noImpl
    val animVal: SVGLengthList
        get() = noImpl
}

@native public interface SVGAngle {
    val unitType: Short
        get() = noImpl
    var value: dynamic
        get() = noImpl
        set(value) = noImpl
    var valueInSpecifiedUnits: dynamic
        get() = noImpl
        set(value) = noImpl
    var valueAsString: dynamic
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

@native public interface SVGAnimatedAngle {
    val baseVal: SVGAngle
        get() = noImpl
    val animVal: SVGAngle
        get() = noImpl
}

@native public interface SVGColor {
    val colorType: Short
        get() = noImpl
    val rgbColor: dynamic
        get() = noImpl
    val iccColor: SVGICCColor
        get() = noImpl
    fun setRGBColor(rgbColor: String): Unit = noImpl
    fun setRGBColorICCColor(rgbColor: String, iccColor: String): Unit = noImpl
    fun setColor(colorType: Short, rgbColor: String, iccColor: String): Unit = noImpl

    companion object {
        val SVG_COLORTYPE_UNKNOWN: Short = 0
        val SVG_COLORTYPE_RGBCOLOR: Short = 1
        val SVG_COLORTYPE_RGBCOLOR_ICCCOLOR: Short = 2
        val SVG_COLORTYPE_CURRENTCOLOR: Short = 3
    }
}

@native public interface SVGICCColor {
    var colorProfile: dynamic
        get() = noImpl
        set(value) = noImpl
    val colors: SVGNumberList
        get() = noImpl
}

@native public interface SVGRect {
    var x: dynamic
        get() = noImpl
        set(value) = noImpl
    var y: dynamic
        get() = noImpl
        set(value) = noImpl
    var width: dynamic
        get() = noImpl
        set(value) = noImpl
    var height: dynamic
        get() = noImpl
        set(value) = noImpl
}

@native public interface SVGAnimatedRect {
    val baseVal: SVGRect
        get() = noImpl
    val animVal: SVGRect
        get() = noImpl
}

@native public interface SVGUnitTypes {

    companion object {
        val SVG_UNIT_TYPE_UNKNOWN: Short = 0
        val SVG_UNIT_TYPE_USERSPACEONUSE: Short = 1
        val SVG_UNIT_TYPE_OBJECTBOUNDINGBOX: Short = 2
    }
}

@native public interface SVGStylable {
    fun getPresentationAttribute(name: String): dynamic = noImpl
}

@native public interface SVGLocatable {
    val nearestViewportElement: SVGElement
        get() = noImpl
    val farthestViewportElement: SVGElement
        get() = noImpl
    fun getBBox(): SVGRect = noImpl
    fun getCTM(): SVGMatrix = noImpl
    fun getScreenCTM(): SVGMatrix = noImpl
    fun getTransformToElement(element: SVGElement): SVGMatrix = noImpl
}

@native public interface SVGTransformable : SVGLocatable {
    val transform: SVGAnimatedTransformList
        get() = noImpl
}

@native public interface SVGTests {
    val requiredFeatures: SVGStringList
        get() = noImpl
    val requiredExtensions: SVGStringList
        get() = noImpl
    val systemLanguage: SVGStringList
        get() = noImpl
    fun hasExtension(extension: String): Boolean = noImpl
}

@native public interface SVGLangSpace {
    var xmllang: dynamic
        get() = noImpl
        set(value) = noImpl
    var xmlspace: dynamic
        get() = noImpl
        set(value) = noImpl
}

@native public interface SVGExternalResourcesRequired {
    val externalResourcesRequired: SVGAnimatedBoolean
        get() = noImpl
}

@native public interface SVGFitToViewBox {
    val viewBox: SVGAnimatedRect
        get() = noImpl
    val preserveAspectRatio: SVGAnimatedPreserveAspectRatio
        get() = noImpl
}

@native public interface SVGZoomAndPan {
    var zoomAndPan: dynamic
        get() = noImpl
        set(value) = noImpl

    companion object {
        val SVG_ZOOMANDPAN_UNKNOWN: Short = 0
        val SVG_ZOOMANDPAN_DISABLE: Short = 1
        val SVG_ZOOMANDPAN_MAGNIFY: Short = 2
    }
}

@native public interface SVGViewSpec : SVGZoomAndPan, SVGFitToViewBox {
    val transform: SVGTransformList
        get() = noImpl
    val viewTarget: SVGElement
        get() = noImpl
    val viewBoxString: String
        get() = noImpl
    val preserveAspectRatioString: String
        get() = noImpl
    val transformString: String
        get() = noImpl
    val viewTargetString: String
        get() = noImpl
}

@native public interface SVGURIReference {
    val href: SVGAnimatedString
        get() = noImpl
}

@native public interface SVGCSSRule : CSSRule {

    companion object {
        val COLOR_PROFILE_RULE: Short = 7
    }
}

@native public interface SVGRenderingIntent {

    companion object {
        val RENDERING_INTENT_UNKNOWN: Short = 0
        val RENDERING_INTENT_AUTO: Short = 1
        val RENDERING_INTENT_PERCEPTUAL: Short = 2
        val RENDERING_INTENT_RELATIVE_COLORIMETRIC: Short = 3
        val RENDERING_INTENT_SATURATION: Short = 4
        val RENDERING_INTENT_ABSOLUTE_COLORIMETRIC: Short = 5
    }
}

@native public interface SVGStyleElement : SVGElement, SVGLangSpace {
    var type: dynamic
        get() = noImpl
        set(value) = noImpl
    var media: dynamic
        get() = noImpl
        set(value) = noImpl
    var title: dynamic
        get() = noImpl
        set(value) = noImpl
}

@native public interface SVGPoint {
    var x: dynamic
        get() = noImpl
        set(value) = noImpl
    var y: dynamic
        get() = noImpl
        set(value) = noImpl
    fun matrixTransform(matrix: SVGMatrix): SVGPoint = noImpl
}

@native public interface SVGPointList {
    val numberOfItems: Int
        get() = noImpl
    fun clear(): Unit = noImpl
    fun initialize(newItem: SVGPoint): SVGPoint = noImpl
    fun getItem(index: Int): SVGPoint = noImpl
    fun insertItemBefore(newItem: SVGPoint, index: Int): SVGPoint = noImpl
    fun replaceItem(newItem: SVGPoint, index: Int): SVGPoint = noImpl
    fun removeItem(index: Int): SVGPoint = noImpl
    fun appendItem(newItem: SVGPoint): SVGPoint = noImpl
}

@native public interface SVGMatrix {
    var a: dynamic
        get() = noImpl
        set(value) = noImpl
    var b: dynamic
        get() = noImpl
        set(value) = noImpl
    var c: dynamic
        get() = noImpl
        set(value) = noImpl
    var d: dynamic
        get() = noImpl
        set(value) = noImpl
    var e: dynamic
        get() = noImpl
        set(value) = noImpl
    var f: dynamic
        get() = noImpl
        set(value) = noImpl
    fun multiply(secondMatrix: SVGMatrix): SVGMatrix = noImpl
    fun inverse(): SVGMatrix = noImpl
    fun translate(x: Float, y: Float): SVGMatrix = noImpl
    fun scale(scaleFactor: Float): SVGMatrix = noImpl
    fun scaleNonUniform(scaleFactorX: Float, scaleFactorY: Float): SVGMatrix = noImpl
    fun rotate(angle: Float): SVGMatrix = noImpl
    fun rotateFromVector(x: Float, y: Float): SVGMatrix = noImpl
    fun flipX(): SVGMatrix = noImpl
    fun flipY(): SVGMatrix = noImpl
    fun skewX(angle: Float): SVGMatrix = noImpl
    fun skewY(angle: Float): SVGMatrix = noImpl
}

@native public interface SVGTransform {
    val type: Short
        get() = noImpl
    val matrix: SVGMatrix
        get() = noImpl
    val angle: Float
        get() = noImpl
    fun setMatrix(matrix: SVGMatrix): Unit = noImpl
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

@native public interface SVGTransformList {
    val numberOfItems: Int
        get() = noImpl
    fun clear(): Unit = noImpl
    fun initialize(newItem: SVGTransform): SVGTransform = noImpl
    fun getItem(index: Int): SVGTransform = noImpl
    fun insertItemBefore(newItem: SVGTransform, index: Int): SVGTransform = noImpl
    fun replaceItem(newItem: SVGTransform, index: Int): SVGTransform = noImpl
    fun removeItem(index: Int): SVGTransform = noImpl
    fun appendItem(newItem: SVGTransform): SVGTransform = noImpl
    fun createSVGTransformFromMatrix(matrix: SVGMatrix): SVGTransform = noImpl
    fun consolidate(): SVGTransform = noImpl
}

@native public interface SVGAnimatedTransformList {
    val baseVal: SVGTransformList
        get() = noImpl
    val animVal: SVGTransformList
        get() = noImpl
}

@native public interface SVGPreserveAspectRatio {
    var align: dynamic
        get() = noImpl
        set(value) = noImpl
    var meetOrSlice: dynamic
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

@native public interface SVGAnimatedPreserveAspectRatio {
    val baseVal: SVGPreserveAspectRatio
        get() = noImpl
    val animVal: SVGPreserveAspectRatio
        get() = noImpl
}

@native public interface SVGPathSeg {
    val pathSegType: Short
        get() = noImpl
    val pathSegTypeAsLetter: String
        get() = noImpl

    companion object {
        val PATHSEG_UNKNOWN: Short = 0
        val PATHSEG_CLOSEPATH: Short = 1
        val PATHSEG_MOVETO_ABS: Short = 2
        val PATHSEG_MOVETO_REL: Short = 3
        val PATHSEG_LINETO_ABS: Short = 4
        val PATHSEG_LINETO_REL: Short = 5
        val PATHSEG_CURVETO_CUBIC_ABS: Short = 6
        val PATHSEG_CURVETO_CUBIC_REL: Short = 7
        val PATHSEG_CURVETO_QUADRATIC_ABS: Short = 8
        val PATHSEG_CURVETO_QUADRATIC_REL: Short = 9
        val PATHSEG_ARC_ABS: Short = 10
        val PATHSEG_ARC_REL: Short = 11
        val PATHSEG_LINETO_HORIZONTAL_ABS: Short = 12
        val PATHSEG_LINETO_HORIZONTAL_REL: Short = 13
        val PATHSEG_LINETO_VERTICAL_ABS: Short = 14
        val PATHSEG_LINETO_VERTICAL_REL: Short = 15
        val PATHSEG_CURVETO_CUBIC_SMOOTH_ABS: Short = 16
        val PATHSEG_CURVETO_CUBIC_SMOOTH_REL: Short = 17
        val PATHSEG_CURVETO_QUADRATIC_SMOOTH_ABS: Short = 18
        val PATHSEG_CURVETO_QUADRATIC_SMOOTH_REL: Short = 19
    }
}

@native public interface SVGPathSegClosePath : SVGPathSeg {
}

@native public interface SVGPathSegMovetoAbs : SVGPathSeg {
    var x: dynamic
        get() = noImpl
        set(value) = noImpl
    var y: dynamic
        get() = noImpl
        set(value) = noImpl
}

@native public interface SVGPathSegMovetoRel : SVGPathSeg {
    var x: dynamic
        get() = noImpl
        set(value) = noImpl
    var y: dynamic
        get() = noImpl
        set(value) = noImpl
}

@native public interface SVGPathSegLinetoAbs : SVGPathSeg {
    var x: dynamic
        get() = noImpl
        set(value) = noImpl
    var y: dynamic
        get() = noImpl
        set(value) = noImpl
}

@native public interface SVGPathSegLinetoRel : SVGPathSeg {
    var x: dynamic
        get() = noImpl
        set(value) = noImpl
    var y: dynamic
        get() = noImpl
        set(value) = noImpl
}

@native public interface SVGPathSegCurvetoCubicAbs : SVGPathSeg {
    var x: dynamic
        get() = noImpl
        set(value) = noImpl
    var y: dynamic
        get() = noImpl
        set(value) = noImpl
    var x1: dynamic
        get() = noImpl
        set(value) = noImpl
    var y1: dynamic
        get() = noImpl
        set(value) = noImpl
    var x2: dynamic
        get() = noImpl
        set(value) = noImpl
    var y2: dynamic
        get() = noImpl
        set(value) = noImpl
}

@native public interface SVGPathSegCurvetoCubicRel : SVGPathSeg {
    var x: dynamic
        get() = noImpl
        set(value) = noImpl
    var y: dynamic
        get() = noImpl
        set(value) = noImpl
    var x1: dynamic
        get() = noImpl
        set(value) = noImpl
    var y1: dynamic
        get() = noImpl
        set(value) = noImpl
    var x2: dynamic
        get() = noImpl
        set(value) = noImpl
    var y2: dynamic
        get() = noImpl
        set(value) = noImpl
}

@native public interface SVGPathSegCurvetoQuadraticAbs : SVGPathSeg {
    var x: dynamic
        get() = noImpl
        set(value) = noImpl
    var y: dynamic
        get() = noImpl
        set(value) = noImpl
    var x1: dynamic
        get() = noImpl
        set(value) = noImpl
    var y1: dynamic
        get() = noImpl
        set(value) = noImpl
}

@native public interface SVGPathSegCurvetoQuadraticRel : SVGPathSeg {
    var x: dynamic
        get() = noImpl
        set(value) = noImpl
    var y: dynamic
        get() = noImpl
        set(value) = noImpl
    var x1: dynamic
        get() = noImpl
        set(value) = noImpl
    var y1: dynamic
        get() = noImpl
        set(value) = noImpl
}

@native public interface SVGPathSegArcAbs : SVGPathSeg {
    var x: dynamic
        get() = noImpl
        set(value) = noImpl
    var y: dynamic
        get() = noImpl
        set(value) = noImpl
    var r1: dynamic
        get() = noImpl
        set(value) = noImpl
    var r2: dynamic
        get() = noImpl
        set(value) = noImpl
    var angle: dynamic
        get() = noImpl
        set(value) = noImpl
    var largeArcFlag: dynamic
        get() = noImpl
        set(value) = noImpl
    var sweepFlag: dynamic
        get() = noImpl
        set(value) = noImpl
}

@native public interface SVGPathSegArcRel : SVGPathSeg {
    var x: dynamic
        get() = noImpl
        set(value) = noImpl
    var y: dynamic
        get() = noImpl
        set(value) = noImpl
    var r1: dynamic
        get() = noImpl
        set(value) = noImpl
    var r2: dynamic
        get() = noImpl
        set(value) = noImpl
    var angle: dynamic
        get() = noImpl
        set(value) = noImpl
    var largeArcFlag: dynamic
        get() = noImpl
        set(value) = noImpl
    var sweepFlag: dynamic
        get() = noImpl
        set(value) = noImpl
}

@native public interface SVGPathSegLinetoHorizontalAbs : SVGPathSeg {
    var x: dynamic
        get() = noImpl
        set(value) = noImpl
}

@native public interface SVGPathSegLinetoHorizontalRel : SVGPathSeg {
    var x: dynamic
        get() = noImpl
        set(value) = noImpl
}

@native public interface SVGPathSegLinetoVerticalAbs : SVGPathSeg {
    var y: dynamic
        get() = noImpl
        set(value) = noImpl
}

@native public interface SVGPathSegLinetoVerticalRel : SVGPathSeg {
    var y: dynamic
        get() = noImpl
        set(value) = noImpl
}

@native public interface SVGPathSegCurvetoCubicSmoothAbs : SVGPathSeg {
    var x: dynamic
        get() = noImpl
        set(value) = noImpl
    var y: dynamic
        get() = noImpl
        set(value) = noImpl
    var x2: dynamic
        get() = noImpl
        set(value) = noImpl
    var y2: dynamic
        get() = noImpl
        set(value) = noImpl
}

@native public interface SVGPathSegCurvetoCubicSmoothRel : SVGPathSeg {
    var x: dynamic
        get() = noImpl
        set(value) = noImpl
    var y: dynamic
        get() = noImpl
        set(value) = noImpl
    var x2: dynamic
        get() = noImpl
        set(value) = noImpl
    var y2: dynamic
        get() = noImpl
        set(value) = noImpl
}

@native public interface SVGPathSegCurvetoQuadraticSmoothAbs : SVGPathSeg {
    var x: dynamic
        get() = noImpl
        set(value) = noImpl
    var y: dynamic
        get() = noImpl
        set(value) = noImpl
}

@native public interface SVGPathSegCurvetoQuadraticSmoothRel : SVGPathSeg {
    var x: dynamic
        get() = noImpl
        set(value) = noImpl
    var y: dynamic
        get() = noImpl
        set(value) = noImpl
}

@native public interface SVGPathSegList {
    val numberOfItems: Int
        get() = noImpl
    fun clear(): Unit = noImpl
    fun initialize(newItem: SVGPathSeg): SVGPathSeg = noImpl
    fun getItem(index: Int): SVGPathSeg = noImpl
    fun insertItemBefore(newItem: SVGPathSeg, index: Int): SVGPathSeg = noImpl
    fun replaceItem(newItem: SVGPathSeg, index: Int): SVGPathSeg = noImpl
    fun removeItem(index: Int): SVGPathSeg = noImpl
    fun appendItem(newItem: SVGPathSeg): SVGPathSeg = noImpl
}

@native public interface SVGAnimatedPathData {
    val pathSegList: SVGPathSegList
        get() = noImpl
    val normalizedPathSegList: SVGPathSegList
        get() = noImpl
    val animatedPathSegList: SVGPathSegList
        get() = noImpl
    val animatedNormalizedPathSegList: SVGPathSegList
        get() = noImpl
}

@native public interface SVGPathElement : SVGElement, SVGTests, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGTransformable, SVGAnimatedPathData {
    val pathLength: SVGAnimatedNumber
        get() = noImpl
    fun getTotalLength(): Float = noImpl
    fun getPointAtLength(distance: Float): SVGPoint = noImpl
    fun getPathSegAtLength(distance: Float): Int = noImpl
    fun createSVGPathSegClosePath(): SVGPathSegClosePath = noImpl
    fun createSVGPathSegMovetoAbs(x: Float, y: Float): SVGPathSegMovetoAbs = noImpl
    fun createSVGPathSegMovetoRel(x: Float, y: Float): SVGPathSegMovetoRel = noImpl
    fun createSVGPathSegLinetoAbs(x: Float, y: Float): SVGPathSegLinetoAbs = noImpl
    fun createSVGPathSegLinetoRel(x: Float, y: Float): SVGPathSegLinetoRel = noImpl
    fun createSVGPathSegCurvetoCubicAbs(x: Float, y: Float, x1: Float, y1: Float, x2: Float, y2: Float): SVGPathSegCurvetoCubicAbs = noImpl
    fun createSVGPathSegCurvetoCubicRel(x: Float, y: Float, x1: Float, y1: Float, x2: Float, y2: Float): SVGPathSegCurvetoCubicRel = noImpl
    fun createSVGPathSegCurvetoQuadraticAbs(x: Float, y: Float, x1: Float, y1: Float): SVGPathSegCurvetoQuadraticAbs = noImpl
    fun createSVGPathSegCurvetoQuadraticRel(x: Float, y: Float, x1: Float, y1: Float): SVGPathSegCurvetoQuadraticRel = noImpl
    fun createSVGPathSegArcAbs(x: Float, y: Float, r1: Float, r2: Float, angle: Float, largeArcFlag: Boolean, sweepFlag: Boolean): SVGPathSegArcAbs = noImpl
    fun createSVGPathSegArcRel(x: Float, y: Float, r1: Float, r2: Float, angle: Float, largeArcFlag: Boolean, sweepFlag: Boolean): SVGPathSegArcRel = noImpl
    fun createSVGPathSegLinetoHorizontalAbs(x: Float): SVGPathSegLinetoHorizontalAbs = noImpl
    fun createSVGPathSegLinetoHorizontalRel(x: Float): SVGPathSegLinetoHorizontalRel = noImpl
    fun createSVGPathSegLinetoVerticalAbs(y: Float): SVGPathSegLinetoVerticalAbs = noImpl
    fun createSVGPathSegLinetoVerticalRel(y: Float): SVGPathSegLinetoVerticalRel = noImpl
    fun createSVGPathSegCurvetoCubicSmoothAbs(x: Float, y: Float, x2: Float, y2: Float): SVGPathSegCurvetoCubicSmoothAbs = noImpl
    fun createSVGPathSegCurvetoCubicSmoothRel(x: Float, y: Float, x2: Float, y2: Float): SVGPathSegCurvetoCubicSmoothRel = noImpl
    fun createSVGPathSegCurvetoQuadraticSmoothAbs(x: Float, y: Float): SVGPathSegCurvetoQuadraticSmoothAbs = noImpl
    fun createSVGPathSegCurvetoQuadraticSmoothRel(x: Float, y: Float): SVGPathSegCurvetoQuadraticSmoothRel = noImpl
}

@native public interface SVGRectElement : SVGElement, SVGTests, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGTransformable {
    val x: SVGAnimatedLength
        get() = noImpl
    val y: SVGAnimatedLength
        get() = noImpl
    val width: SVGAnimatedLength
        get() = noImpl
    val height: SVGAnimatedLength
        get() = noImpl
    val rx: SVGAnimatedLength
        get() = noImpl
    val ry: SVGAnimatedLength
        get() = noImpl
}

@native public interface SVGCircleElement : SVGElement, SVGTests, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGTransformable {
    val cx: SVGAnimatedLength
        get() = noImpl
    val cy: SVGAnimatedLength
        get() = noImpl
    val r: SVGAnimatedLength
        get() = noImpl
}

@native public interface SVGEllipseElement : SVGElement, SVGTests, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGTransformable {
    val cx: SVGAnimatedLength
        get() = noImpl
    val cy: SVGAnimatedLength
        get() = noImpl
    val rx: SVGAnimatedLength
        get() = noImpl
    val ry: SVGAnimatedLength
        get() = noImpl
}

@native public interface SVGLineElement : SVGElement, SVGTests, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGTransformable {
    val x1: SVGAnimatedLength
        get() = noImpl
    val y1: SVGAnimatedLength
        get() = noImpl
    val x2: SVGAnimatedLength
        get() = noImpl
    val y2: SVGAnimatedLength
        get() = noImpl
}

@native public interface SVGAnimatedPoints {
    val points: SVGPointList
        get() = noImpl
    val animatedPoints: SVGPointList
        get() = noImpl
}

@native public interface SVGPolylineElement : SVGElement, SVGTests, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGTransformable, SVGAnimatedPoints {
}

@native public interface SVGPolygonElement : SVGElement, SVGTests, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGTransformable, SVGAnimatedPoints {
}

@native public interface SVGTextContentElement : SVGElement, SVGTests, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable {
    val textLength: SVGAnimatedLength
        get() = noImpl
    val lengthAdjust: SVGAnimatedEnumeration
        get() = noImpl
    fun getNumberOfChars(): Int = noImpl
    fun getComputedTextLength(): Float = noImpl
    fun getSubStringLength(charnum: Int, nchars: Int): Float = noImpl
    fun getStartPositionOfChar(charnum: Int): SVGPoint = noImpl
    fun getEndPositionOfChar(charnum: Int): SVGPoint = noImpl
    fun getExtentOfChar(charnum: Int): SVGRect = noImpl
    fun getRotationOfChar(charnum: Int): Float = noImpl
    fun getCharNumAtPosition(point: SVGPoint): Int = noImpl
    fun selectSubString(charnum: Int, nchars: Int): Unit = noImpl

    companion object {
        val LENGTHADJUST_UNKNOWN: Short = 0
        val LENGTHADJUST_SPACING: Short = 1
        val LENGTHADJUST_SPACINGANDGLYPHS: Short = 2
    }
}

@native public interface SVGTextPositioningElement : SVGTextContentElement {
    val x: SVGAnimatedLengthList
        get() = noImpl
    val y: SVGAnimatedLengthList
        get() = noImpl
    val dx: SVGAnimatedLengthList
        get() = noImpl
    val dy: SVGAnimatedLengthList
        get() = noImpl
    val rotate: SVGAnimatedNumberList
        get() = noImpl
}

@native public interface SVGTextElement : SVGTextPositioningElement, SVGTransformable {
}

@native public interface SVGTSpanElement : SVGTextPositioningElement {
}

@native public interface SVGTRefElement : SVGTextPositioningElement, SVGURIReference {
}

@native public interface SVGTextPathElement : SVGTextContentElement, SVGURIReference {
    val startOffset: SVGAnimatedLength
        get() = noImpl
    val method: SVGAnimatedEnumeration
        get() = noImpl
    val spacing: SVGAnimatedEnumeration
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

@native public interface SVGAltGlyphElement : SVGTextPositioningElement, SVGURIReference {
    var glyphRef: dynamic
        get() = noImpl
        set(value) = noImpl
    var format: dynamic
        get() = noImpl
        set(value) = noImpl
}

@native public interface SVGAltGlyphDefElement : SVGElement {
}

@native public interface SVGAltGlyphItemElement : SVGElement {
}

@native public interface SVGGlyphRefElement : SVGElement, SVGURIReference, SVGStylable {
    var glyphRef: dynamic
        get() = noImpl
        set(value) = noImpl
    var format: dynamic
        get() = noImpl
        set(value) = noImpl
    var x: dynamic
        get() = noImpl
        set(value) = noImpl
    var y: dynamic
        get() = noImpl
        set(value) = noImpl
    var dx: dynamic
        get() = noImpl
        set(value) = noImpl
    var dy: dynamic
        get() = noImpl
        set(value) = noImpl
}

@native public interface SVGPaint : SVGColor {
    val paintType: Short
        get() = noImpl
    val uri: String
        get() = noImpl
    fun setUri(uri: String): Unit = noImpl
    fun setPaint(paintType: Short, uri: String, rgbColor: String, iccColor: String): Unit = noImpl

    companion object {
        val SVG_PAINTTYPE_UNKNOWN: Short = 0
        val SVG_PAINTTYPE_RGBCOLOR: Short = 1
        val SVG_PAINTTYPE_RGBCOLOR_ICCCOLOR: Short = 2
        val SVG_PAINTTYPE_NONE: Short = 101
        val SVG_PAINTTYPE_CURRENTCOLOR: Short = 102
        val SVG_PAINTTYPE_URI_NONE: Short = 103
        val SVG_PAINTTYPE_URI_CURRENTCOLOR: Short = 104
        val SVG_PAINTTYPE_URI_RGBCOLOR: Short = 105
        val SVG_PAINTTYPE_URI_RGBCOLOR_ICCCOLOR: Short = 106
        val SVG_PAINTTYPE_URI: Short = 107
    }
}

@native public interface SVGMarkerElement : SVGElement, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGFitToViewBox {
    val refX: SVGAnimatedLength
        get() = noImpl
    val refY: SVGAnimatedLength
        get() = noImpl
    val markerUnits: SVGAnimatedEnumeration
        get() = noImpl
    val markerWidth: SVGAnimatedLength
        get() = noImpl
    val markerHeight: SVGAnimatedLength
        get() = noImpl
    val orientType: SVGAnimatedEnumeration
        get() = noImpl
    val orientAngle: SVGAnimatedAngle
        get() = noImpl
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

@native public interface SVGColorProfileElement : SVGElement, SVGURIReference, SVGRenderingIntent {
    var local: String
        get() = noImpl
        set(value) = noImpl
    var name: String
        get() = noImpl
        set(value) = noImpl
    var renderingIntent: Short
        get() = noImpl
        set(value) = noImpl
}

@native public interface SVGColorProfileRule : SVGCSSRule, SVGRenderingIntent {
    var src: dynamic
        get() = noImpl
        set(value) = noImpl
    var name: dynamic
        get() = noImpl
        set(value) = noImpl
    var renderingIntent: dynamic
        get() = noImpl
        set(value) = noImpl
}

@native public interface SVGGradientElement : SVGElement, SVGURIReference, SVGExternalResourcesRequired, SVGStylable, SVGUnitTypes {
    val gradientUnits: SVGAnimatedEnumeration
        get() = noImpl
    val gradientTransform: SVGAnimatedTransformList
        get() = noImpl
    val spreadMethod: SVGAnimatedEnumeration
        get() = noImpl

    companion object {
        val SVG_SPREADMETHOD_UNKNOWN: Short = 0
        val SVG_SPREADMETHOD_PAD: Short = 1
        val SVG_SPREADMETHOD_REFLECT: Short = 2
        val SVG_SPREADMETHOD_REPEAT: Short = 3
    }
}

@native public interface SVGLinearGradientElement : SVGGradientElement {
    val x1: SVGAnimatedLength
        get() = noImpl
    val y1: SVGAnimatedLength
        get() = noImpl
    val x2: SVGAnimatedLength
        get() = noImpl
    val y2: SVGAnimatedLength
        get() = noImpl
}

@native public interface SVGRadialGradientElement : SVGGradientElement {
    val cx: SVGAnimatedLength
        get() = noImpl
    val cy: SVGAnimatedLength
        get() = noImpl
    val r: SVGAnimatedLength
        get() = noImpl
    val fx: SVGAnimatedLength
        get() = noImpl
    val fy: SVGAnimatedLength
        get() = noImpl
}

@native public interface SVGStopElement : SVGElement, SVGStylable {
    val offset: SVGAnimatedNumber
        get() = noImpl
}

@native public interface SVGPatternElement : SVGElement, SVGURIReference, SVGTests, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGFitToViewBox, SVGUnitTypes {
    val patternUnits: SVGAnimatedEnumeration
        get() = noImpl
    val patternContentUnits: SVGAnimatedEnumeration
        get() = noImpl
    val patternTransform: SVGAnimatedTransformList
        get() = noImpl
    val x: SVGAnimatedLength
        get() = noImpl
    val y: SVGAnimatedLength
        get() = noImpl
    val width: SVGAnimatedLength
        get() = noImpl
    val height: SVGAnimatedLength
        get() = noImpl
}

@native public interface SVGClipPathElement : SVGElement, SVGTests, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGTransformable, SVGUnitTypes {
    val clipPathUnits: SVGAnimatedEnumeration
        get() = noImpl
}

@native public interface SVGMaskElement : SVGElement, SVGTests, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGUnitTypes {
    val maskUnits: SVGAnimatedEnumeration
        get() = noImpl
    val maskContentUnits: SVGAnimatedEnumeration
        get() = noImpl
    val x: SVGAnimatedLength
        get() = noImpl
    val y: SVGAnimatedLength
        get() = noImpl
    val width: SVGAnimatedLength
        get() = noImpl
    val height: SVGAnimatedLength
        get() = noImpl
}

@native public interface SVGFilterElement : SVGElement, SVGURIReference, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGUnitTypes {
    val filterUnits: SVGAnimatedEnumeration
        get() = noImpl
    val primitiveUnits: SVGAnimatedEnumeration
        get() = noImpl
    val x: SVGAnimatedLength
        get() = noImpl
    val y: SVGAnimatedLength
        get() = noImpl
    val width: SVGAnimatedLength
        get() = noImpl
    val height: SVGAnimatedLength
        get() = noImpl
    val filterResX: SVGAnimatedInteger
        get() = noImpl
    val filterResY: SVGAnimatedInteger
        get() = noImpl
    fun setFilterRes(filterResX: Int, filterResY: Int): Unit = noImpl
}

@native public interface SVGFilterPrimitiveStandardAttributes : SVGStylable {
    val x: SVGAnimatedLength
        get() = noImpl
    val y: SVGAnimatedLength
        get() = noImpl
    val width: SVGAnimatedLength
        get() = noImpl
    val height: SVGAnimatedLength
        get() = noImpl
    val result: SVGAnimatedString
        get() = noImpl
}

@native public interface SVGFEBlendElement : SVGElement, SVGFilterPrimitiveStandardAttributes {
    val in1: SVGAnimatedString
        get() = noImpl
    val in2: SVGAnimatedString
        get() = noImpl
    val mode: SVGAnimatedEnumeration
        get() = noImpl

    companion object {
        val SVG_FEBLEND_MODE_UNKNOWN: Short = 0
        val SVG_FEBLEND_MODE_NORMAL: Short = 1
        val SVG_FEBLEND_MODE_MULTIPLY: Short = 2
        val SVG_FEBLEND_MODE_SCREEN: Short = 3
        val SVG_FEBLEND_MODE_DARKEN: Short = 4
        val SVG_FEBLEND_MODE_LIGHTEN: Short = 5
    }
}

@native public interface SVGFEColorMatrixElement : SVGElement, SVGFilterPrimitiveStandardAttributes {
    val in1: SVGAnimatedString
        get() = noImpl
    val type: SVGAnimatedEnumeration
        get() = noImpl
    val values: SVGAnimatedNumberList
        get() = noImpl

    companion object {
        val SVG_FECOLORMATRIX_TYPE_UNKNOWN: Short = 0
        val SVG_FECOLORMATRIX_TYPE_MATRIX: Short = 1
        val SVG_FECOLORMATRIX_TYPE_SATURATE: Short = 2
        val SVG_FECOLORMATRIX_TYPE_HUEROTATE: Short = 3
        val SVG_FECOLORMATRIX_TYPE_LUMINANCETOALPHA: Short = 4
    }
}

@native public interface SVGFEComponentTransferElement : SVGElement, SVGFilterPrimitiveStandardAttributes {
    val in1: SVGAnimatedString
        get() = noImpl
}

@native public interface SVGComponentTransferFunctionElement : SVGElement {
    val type: SVGAnimatedEnumeration
        get() = noImpl
    val tableValues: SVGAnimatedNumberList
        get() = noImpl
    val slope: SVGAnimatedNumber
        get() = noImpl
    val intercept: SVGAnimatedNumber
        get() = noImpl
    val amplitude: SVGAnimatedNumber
        get() = noImpl
    val exponent: SVGAnimatedNumber
        get() = noImpl
    val offset: SVGAnimatedNumber
        get() = noImpl

    companion object {
        val SVG_FECOMPONENTTRANSFER_TYPE_UNKNOWN: Short = 0
        val SVG_FECOMPONENTTRANSFER_TYPE_IDENTITY: Short = 1
        val SVG_FECOMPONENTTRANSFER_TYPE_TABLE: Short = 2
        val SVG_FECOMPONENTTRANSFER_TYPE_DISCRETE: Short = 3
        val SVG_FECOMPONENTTRANSFER_TYPE_LINEAR: Short = 4
        val SVG_FECOMPONENTTRANSFER_TYPE_GAMMA: Short = 5
    }
}

@native public interface SVGFEFuncRElement : SVGComponentTransferFunctionElement {
}

@native public interface SVGFEFuncGElement : SVGComponentTransferFunctionElement {
}

@native public interface SVGFEFuncBElement : SVGComponentTransferFunctionElement {
}

@native public interface SVGFEFuncAElement : SVGComponentTransferFunctionElement {
}

@native public interface SVGFECompositeElement : SVGElement, SVGFilterPrimitiveStandardAttributes {
    val in1: SVGAnimatedString
        get() = noImpl
    val in2: SVGAnimatedString
        get() = noImpl
    val operator: SVGAnimatedEnumeration
        get() = noImpl
    val k1: SVGAnimatedNumber
        get() = noImpl
    val k2: SVGAnimatedNumber
        get() = noImpl
    val k3: SVGAnimatedNumber
        get() = noImpl
    val k4: SVGAnimatedNumber
        get() = noImpl

    companion object {
        val SVG_FECOMPOSITE_OPERATOR_UNKNOWN: Short = 0
        val SVG_FECOMPOSITE_OPERATOR_OVER: Short = 1
        val SVG_FECOMPOSITE_OPERATOR_IN: Short = 2
        val SVG_FECOMPOSITE_OPERATOR_OUT: Short = 3
        val SVG_FECOMPOSITE_OPERATOR_ATOP: Short = 4
        val SVG_FECOMPOSITE_OPERATOR_XOR: Short = 5
        val SVG_FECOMPOSITE_OPERATOR_ARITHMETIC: Short = 6
    }
}

@native public interface SVGFEConvolveMatrixElement : SVGElement, SVGFilterPrimitiveStandardAttributes {
    val in1: SVGAnimatedString
        get() = noImpl
    val orderX: SVGAnimatedInteger
        get() = noImpl
    val orderY: SVGAnimatedInteger
        get() = noImpl
    val kernelMatrix: SVGAnimatedNumberList
        get() = noImpl
    val divisor: SVGAnimatedNumber
        get() = noImpl
    val bias: SVGAnimatedNumber
        get() = noImpl
    val targetX: SVGAnimatedInteger
        get() = noImpl
    val targetY: SVGAnimatedInteger
        get() = noImpl
    val edgeMode: SVGAnimatedEnumeration
        get() = noImpl
    val kernelUnitLengthX: SVGAnimatedNumber
        get() = noImpl
    val kernelUnitLengthY: SVGAnimatedNumber
        get() = noImpl
    val preserveAlpha: SVGAnimatedBoolean
        get() = noImpl

    companion object {
        val SVG_EDGEMODE_UNKNOWN: Short = 0
        val SVG_EDGEMODE_DUPLICATE: Short = 1
        val SVG_EDGEMODE_WRAP: Short = 2
        val SVG_EDGEMODE_NONE: Short = 3
    }
}

@native public interface SVGFEDiffuseLightingElement : SVGElement, SVGFilterPrimitiveStandardAttributes {
    val in1: SVGAnimatedString
        get() = noImpl
    val surfaceScale: SVGAnimatedNumber
        get() = noImpl
    val diffuseConstant: SVGAnimatedNumber
        get() = noImpl
    val kernelUnitLengthX: SVGAnimatedNumber
        get() = noImpl
    val kernelUnitLengthY: SVGAnimatedNumber
        get() = noImpl
}

@native public interface SVGFEDistantLightElement : SVGElement {
    val azimuth: SVGAnimatedNumber
        get() = noImpl
    val elevation: SVGAnimatedNumber
        get() = noImpl
}

@native public interface SVGFEPointLightElement : SVGElement {
    val x: SVGAnimatedNumber
        get() = noImpl
    val y: SVGAnimatedNumber
        get() = noImpl
    val z: SVGAnimatedNumber
        get() = noImpl
}

@native public interface SVGFESpotLightElement : SVGElement {
    val x: SVGAnimatedNumber
        get() = noImpl
    val y: SVGAnimatedNumber
        get() = noImpl
    val z: SVGAnimatedNumber
        get() = noImpl
    val pointsAtX: SVGAnimatedNumber
        get() = noImpl
    val pointsAtY: SVGAnimatedNumber
        get() = noImpl
    val pointsAtZ: SVGAnimatedNumber
        get() = noImpl
    val specularExponent: SVGAnimatedNumber
        get() = noImpl
    val limitingConeAngle: SVGAnimatedNumber
        get() = noImpl
}

@native public interface SVGFEDisplacementMapElement : SVGElement, SVGFilterPrimitiveStandardAttributes {
    val in1: SVGAnimatedString
        get() = noImpl
    val in2: SVGAnimatedString
        get() = noImpl
    val scale: SVGAnimatedNumber
        get() = noImpl
    val xChannelSelector: SVGAnimatedEnumeration
        get() = noImpl
    val yChannelSelector: SVGAnimatedEnumeration
        get() = noImpl

    companion object {
        val SVG_CHANNEL_UNKNOWN: Short = 0
        val SVG_CHANNEL_R: Short = 1
        val SVG_CHANNEL_G: Short = 2
        val SVG_CHANNEL_B: Short = 3
        val SVG_CHANNEL_A: Short = 4
    }
}

@native public interface SVGFEFloodElement : SVGElement, SVGFilterPrimitiveStandardAttributes {
}

@native public interface SVGFEGaussianBlurElement : SVGElement, SVGFilterPrimitiveStandardAttributes {
    val in1: SVGAnimatedString
        get() = noImpl
    val stdDeviationX: SVGAnimatedNumber
        get() = noImpl
    val stdDeviationY: SVGAnimatedNumber
        get() = noImpl
    fun setStdDeviation(stdDeviationX: Float, stdDeviationY: Float): Unit = noImpl
}

@native public interface SVGFEImageElement : SVGElement, SVGURIReference, SVGLangSpace, SVGExternalResourcesRequired, SVGFilterPrimitiveStandardAttributes {
    val preserveAspectRatio: SVGAnimatedPreserveAspectRatio
        get() = noImpl
}

@native public interface SVGFEMergeElement : SVGElement, SVGFilterPrimitiveStandardAttributes {
}

@native public interface SVGFEMergeNodeElement : SVGElement {
    val in1: SVGAnimatedString
        get() = noImpl
}

@native public interface SVGFEMorphologyElement : SVGElement, SVGFilterPrimitiveStandardAttributes {
    val in1: SVGAnimatedString
        get() = noImpl
    val operator: SVGAnimatedEnumeration
        get() = noImpl
    val radiusX: SVGAnimatedNumber
        get() = noImpl
    val radiusY: SVGAnimatedNumber
        get() = noImpl

    companion object {
        val SVG_MORPHOLOGY_OPERATOR_UNKNOWN: Short = 0
        val SVG_MORPHOLOGY_OPERATOR_ERODE: Short = 1
        val SVG_MORPHOLOGY_OPERATOR_DILATE: Short = 2
    }
}

@native public interface SVGFEOffsetElement : SVGElement, SVGFilterPrimitiveStandardAttributes {
    val in1: SVGAnimatedString
        get() = noImpl
    val dx: SVGAnimatedNumber
        get() = noImpl
    val dy: SVGAnimatedNumber
        get() = noImpl
}

@native public interface SVGFESpecularLightingElement : SVGElement, SVGFilterPrimitiveStandardAttributes {
    val in1: SVGAnimatedString
        get() = noImpl
    val surfaceScale: SVGAnimatedNumber
        get() = noImpl
    val specularConstant: SVGAnimatedNumber
        get() = noImpl
    val specularExponent: SVGAnimatedNumber
        get() = noImpl
    val kernelUnitLengthX: SVGAnimatedNumber
        get() = noImpl
    val kernelUnitLengthY: SVGAnimatedNumber
        get() = noImpl
}

@native public interface SVGFETileElement : SVGElement, SVGFilterPrimitiveStandardAttributes {
    val in1: SVGAnimatedString
        get() = noImpl
}

@native public interface SVGFETurbulenceElement : SVGElement, SVGFilterPrimitiveStandardAttributes {
    val baseFrequencyX: SVGAnimatedNumber
        get() = noImpl
    val baseFrequencyY: SVGAnimatedNumber
        get() = noImpl
    val numOctaves: SVGAnimatedInteger
        get() = noImpl
    val seed: SVGAnimatedNumber
        get() = noImpl
    val stitchTiles: SVGAnimatedEnumeration
        get() = noImpl
    val type: SVGAnimatedEnumeration
        get() = noImpl

    companion object {
        val SVG_TURBULENCE_TYPE_UNKNOWN: Short = 0
        val SVG_TURBULENCE_TYPE_FRACTALNOISE: Short = 1
        val SVG_TURBULENCE_TYPE_TURBULENCE: Short = 2
        val SVG_STITCHTYPE_UNKNOWN: Short = 0
        val SVG_STITCHTYPE_STITCH: Short = 1
        val SVG_STITCHTYPE_NOSTITCH: Short = 2
    }
}

@native public interface SVGCursorElement : SVGElement, SVGURIReference, SVGTests, SVGExternalResourcesRequired {
    val x: SVGAnimatedLength
        get() = noImpl
    val y: SVGAnimatedLength
        get() = noImpl
}

@native public interface SVGAElement : SVGElement, SVGURIReference, SVGTests, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGTransformable {
    val target: SVGAnimatedString
        get() = noImpl
}

@native public interface SVGViewElement : SVGElement, SVGExternalResourcesRequired, SVGFitToViewBox, SVGZoomAndPan {
    val viewTarget: SVGStringList
        get() = noImpl
}

@native public interface SVGScriptElement : SVGElement, SVGURIReference, SVGExternalResourcesRequired {
    var type: dynamic
        get() = noImpl
        set(value) = noImpl
}

@native public open class SVGZoomEvent : UIEvent(noImpl, noImpl) {
    open val zoomRectScreen: SVGRect
        get() = noImpl
    open val previousScale: Float
        get() = noImpl
    open val previousTranslate: SVGPoint
        get() = noImpl
    open val newScale: Float
        get() = noImpl
    open val newTranslate: SVGPoint
        get() = noImpl
}

@native public interface ElementTimeControl {
    fun beginElement(): Unit = noImpl
    fun beginElementAt(offset: Float): Unit = noImpl
    fun endElement(): Unit = noImpl
    fun endElementAt(offset: Float): Unit = noImpl
}

@native public open class TimeEvent : Event(noImpl, noImpl) {
    open val view: dynamic
        get() = noImpl
    open val detail: Int
        get() = noImpl
    fun initTimeEvent(typeArg: String, viewArg: dynamic, detailArg: Int): Unit = noImpl
}

@native public interface SVGAnimationElement : SVGElement, SVGTests, SVGExternalResourcesRequired, ElementTimeControl {
    val targetElement: SVGElement
        get() = noImpl
    fun getStartTime(): Float = noImpl
    fun getCurrentTime(): Float = noImpl
    fun getSimpleDuration(): Float = noImpl
}

@native public interface SVGAnimateElement : SVGAnimationElement, SVGStylable {
}

@native public interface SVGSetElement : SVGAnimationElement {
}

@native public interface SVGAnimateMotionElement : SVGAnimationElement {
}

@native public interface SVGMPathElement : SVGElement, SVGURIReference, SVGExternalResourcesRequired {
}

@native public interface SVGAnimateColorElement : SVGAnimationElement, SVGStylable {
}

@native public interface SVGAnimateTransformElement : SVGAnimationElement {
}

@native public interface SVGFontElement : SVGElement, SVGExternalResourcesRequired, SVGStylable {
}

@native public interface SVGGlyphElement : SVGElement, SVGStylable {
}

@native public interface SVGMissingGlyphElement : SVGElement, SVGStylable {
}

@native public interface SVGHKernElement : SVGElement {
}

@native public interface SVGVKernElement : SVGElement {
}

@native public interface SVGFontFaceElement : SVGElement {
}

@native public interface SVGFontFaceSrcElement : SVGElement {
}

@native public interface SVGFontFaceUriElement : SVGElement {
}

@native public interface SVGFontFaceFormatElement : SVGElement {
}

@native public interface SVGFontFaceNameElement : SVGElement {
}

@native public interface SVGMetadataElement : SVGElement {
}

@native public interface SVGForeignObjectElement : SVGElement, SVGTests, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGTransformable {
    val x: SVGAnimatedLength
        get() = noImpl
    val y: SVGAnimatedLength
        get() = noImpl
    val width: SVGAnimatedLength
        get() = noImpl
    val height: SVGAnimatedLength
        get() = noImpl
}

