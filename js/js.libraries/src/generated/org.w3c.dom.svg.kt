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
import org.w3c.fetch.*
import org.w3c.files.*
import org.w3c.notifications.*
import org.w3c.performance.*
import org.w3c.workers.*
import org.w3c.xhr.*

native public open class SVGDocument : Document() {
    var rootElement: SVGSVGElement
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGSVGElement : SVGElement, SVGTests, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGLocatable, SVGFitToViewBox, SVGZoomAndPan {
    var x: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var y: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var width: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var height: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var contentScriptType: dynamic
        get() = noImpl
        set(value) = noImpl
    var contentStyleType: dynamic
        get() = noImpl
        set(value) = noImpl
    var viewport: SVGRect
        get() = noImpl
        set(value) = noImpl
    var pixelUnitToMillimeterX: Float
        get() = noImpl
        set(value) = noImpl
    var pixelUnitToMillimeterY: Float
        get() = noImpl
        set(value) = noImpl
    var screenPixelToMillimeterX: Float
        get() = noImpl
        set(value) = noImpl
    var screenPixelToMillimeterY: Float
        get() = noImpl
        set(value) = noImpl
    var useCurrentView: Boolean
        get() = noImpl
        set(value) = noImpl
    var currentView: SVGViewSpec
        get() = noImpl
        set(value) = noImpl
    var currentScale: Float
        get() = noImpl
        set(value) = noImpl
    var currentTranslate: SVGPoint
        get() = noImpl
        set(value) = noImpl
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

native public trait SVGGElement : SVGElement, SVGTests, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGTransformable {
}

native public trait SVGDefsElement : SVGElement, SVGTests, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGTransformable {
}

native public trait SVGDescElement : SVGElement, SVGLangSpace, SVGStylable {
}

native public trait SVGTitleElement : SVGElement, SVGLangSpace, SVGStylable {
}

native public trait SVGSymbolElement : SVGElement, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGFitToViewBox {
}

native public trait SVGUseElement : SVGElement, SVGURIReference, SVGTests, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGTransformable {
    var x: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var y: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var width: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var height: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var instanceRoot: SVGElementInstance
        get() = noImpl
        set(value) = noImpl
    var animatedInstanceRoot: SVGElementInstance
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGElementInstance : EventTarget {
    var correspondingElement: SVGElement
        get() = noImpl
        set(value) = noImpl
    var correspondingUseElement: SVGUseElement
        get() = noImpl
        set(value) = noImpl
    var parentNode: SVGElementInstance
        get() = noImpl
        set(value) = noImpl
    var childNodes: SVGElementInstanceList
        get() = noImpl
        set(value) = noImpl
    var firstChild: SVGElementInstance
        get() = noImpl
        set(value) = noImpl
    var lastChild: SVGElementInstance
        get() = noImpl
        set(value) = noImpl
    var previousSibling: SVGElementInstance
        get() = noImpl
        set(value) = noImpl
    var nextSibling: SVGElementInstance
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGElementInstanceList {
    var length: Int
        get() = noImpl
        set(value) = noImpl
    fun item(index: Int): SVGElementInstance = noImpl
}

native public trait SVGImageElement : SVGElement, SVGURIReference, SVGTests, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGTransformable {
    var x: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var y: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var width: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var height: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var preserveAspectRatio: SVGAnimatedPreserveAspectRatio
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGSwitchElement : SVGElement, SVGTests, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGTransformable {
}

native public trait GetSVGDocument {
    fun getSVGDocument(): SVGDocument = noImpl
}

native public trait SVGElement : Element {
//    var id: dynamic
//        get() = noImpl
//        set(value) = noImpl
    var xmlbase: dynamic
        get() = noImpl
        set(value) = noImpl
    var ownerSVGElement: SVGSVGElement
        get() = noImpl
        set(value) = noImpl
    var viewportElement: SVGElement
        get() = noImpl
        set(value) = noImpl
    var style: CSSStyleDeclaration
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGAnimatedBoolean {
    var baseVal: dynamic
        get() = noImpl
        set(value) = noImpl
    var animVal: Boolean
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGAnimatedString {
    var baseVal: dynamic
        get() = noImpl
        set(value) = noImpl
    var animVal: String
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGStringList {
    var numberOfItems: Int
        get() = noImpl
        set(value) = noImpl
    fun clear(): Unit = noImpl
    fun initialize(newItem: String): String = noImpl
    fun getItem(index: Int): String = noImpl
    fun insertItemBefore(newItem: String, index: Int): String = noImpl
    fun replaceItem(newItem: String, index: Int): String = noImpl
    fun removeItem(index: Int): String = noImpl
    fun appendItem(newItem: String): String = noImpl
}

native public trait SVGAnimatedEnumeration {
    var baseVal: dynamic
        get() = noImpl
        set(value) = noImpl
    var animVal: Short
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGAnimatedInteger {
    var baseVal: dynamic
        get() = noImpl
        set(value) = noImpl
    var animVal: Int
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGNumber {
    var value: dynamic
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGAnimatedNumber {
    var baseVal: dynamic
        get() = noImpl
        set(value) = noImpl
    var animVal: Float
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGNumberList {
    var numberOfItems: Int
        get() = noImpl
        set(value) = noImpl
    fun clear(): Unit = noImpl
    fun initialize(newItem: SVGNumber): SVGNumber = noImpl
    fun getItem(index: Int): SVGNumber = noImpl
    fun insertItemBefore(newItem: SVGNumber, index: Int): SVGNumber = noImpl
    fun replaceItem(newItem: SVGNumber, index: Int): SVGNumber = noImpl
    fun removeItem(index: Int): SVGNumber = noImpl
    fun appendItem(newItem: SVGNumber): SVGNumber = noImpl
}

native public trait SVGAnimatedNumberList {
    var baseVal: SVGNumberList
        get() = noImpl
        set(value) = noImpl
    var animVal: SVGNumberList
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGLength {
    var unitType: Short
        get() = noImpl
        set(value) = noImpl
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

native public trait SVGAnimatedLength {
    var baseVal: SVGLength
        get() = noImpl
        set(value) = noImpl
    var animVal: SVGLength
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGLengthList {
    var numberOfItems: Int
        get() = noImpl
        set(value) = noImpl
    fun clear(): Unit = noImpl
    fun initialize(newItem: SVGLength): SVGLength = noImpl
    fun getItem(index: Int): SVGLength = noImpl
    fun insertItemBefore(newItem: SVGLength, index: Int): SVGLength = noImpl
    fun replaceItem(newItem: SVGLength, index: Int): SVGLength = noImpl
    fun removeItem(index: Int): SVGLength = noImpl
    fun appendItem(newItem: SVGLength): SVGLength = noImpl
}

native public trait SVGAnimatedLengthList {
    var baseVal: SVGLengthList
        get() = noImpl
        set(value) = noImpl
    var animVal: SVGLengthList
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGAngle {
    var unitType: Short
        get() = noImpl
        set(value) = noImpl
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

native public trait SVGAnimatedAngle {
    var baseVal: SVGAngle
        get() = noImpl
        set(value) = noImpl
    var animVal: SVGAngle
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGColor {
    var colorType: Short
        get() = noImpl
        set(value) = noImpl
    var rgbColor: dynamic
        get() = noImpl
        set(value) = noImpl
    var iccColor: SVGICCColor
        get() = noImpl
        set(value) = noImpl
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

native public trait SVGICCColor {
    var colorProfile: dynamic
        get() = noImpl
        set(value) = noImpl
    var colors: SVGNumberList
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGRect {
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

native public trait SVGAnimatedRect {
    var baseVal: SVGRect
        get() = noImpl
        set(value) = noImpl
    var animVal: SVGRect
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGUnitTypes {

    companion object {
        val SVG_UNIT_TYPE_UNKNOWN: Short = 0
        val SVG_UNIT_TYPE_USERSPACEONUSE: Short = 1
        val SVG_UNIT_TYPE_OBJECTBOUNDINGBOX: Short = 2
    }
}

native public trait SVGStylable {
    fun getPresentationAttribute(name: String): dynamic = noImpl
}

native public trait SVGLocatable {
    var nearestViewportElement: SVGElement
        get() = noImpl
        set(value) = noImpl
    var farthestViewportElement: SVGElement
        get() = noImpl
        set(value) = noImpl
    fun getBBox(): SVGRect = noImpl
    fun getCTM(): SVGMatrix = noImpl
    fun getScreenCTM(): SVGMatrix = noImpl
    fun getTransformToElement(element: SVGElement): SVGMatrix = noImpl
}

native public trait SVGTransformable : SVGLocatable {
    var transform: SVGAnimatedTransformList
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGTests {
    var requiredFeatures: SVGStringList
        get() = noImpl
        set(value) = noImpl
    var requiredExtensions: SVGStringList
        get() = noImpl
        set(value) = noImpl
    var systemLanguage: SVGStringList
        get() = noImpl
        set(value) = noImpl
    fun hasExtension(extension: String): Boolean = noImpl
}

native public trait SVGLangSpace {
    var xmllang: dynamic
        get() = noImpl
        set(value) = noImpl
    var xmlspace: dynamic
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGExternalResourcesRequired {
    var externalResourcesRequired: SVGAnimatedBoolean
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGFitToViewBox {
    var viewBox: SVGAnimatedRect
        get() = noImpl
        set(value) = noImpl
    var preserveAspectRatio: SVGAnimatedPreserveAspectRatio
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGZoomAndPan {
    var zoomAndPan: dynamic
        get() = noImpl
        set(value) = noImpl

    companion object {
        val SVG_ZOOMANDPAN_UNKNOWN: Short = 0
        val SVG_ZOOMANDPAN_DISABLE: Short = 1
        val SVG_ZOOMANDPAN_MAGNIFY: Short = 2
    }
}

native public trait SVGViewSpec : SVGZoomAndPan, SVGFitToViewBox {
    var transform: SVGTransformList
        get() = noImpl
        set(value) = noImpl
    var viewTarget: SVGElement
        get() = noImpl
        set(value) = noImpl
    var viewBoxString: String
        get() = noImpl
        set(value) = noImpl
    var preserveAspectRatioString: String
        get() = noImpl
        set(value) = noImpl
    var transformString: String
        get() = noImpl
        set(value) = noImpl
    var viewTargetString: String
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGURIReference {
    var href: SVGAnimatedString
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGCSSRule : CSSRule {

    companion object {
        val COLOR_PROFILE_RULE: Short = 7
    }
}

native public trait SVGRenderingIntent {

    companion object {
        val RENDERING_INTENT_UNKNOWN: Short = 0
        val RENDERING_INTENT_AUTO: Short = 1
        val RENDERING_INTENT_PERCEPTUAL: Short = 2
        val RENDERING_INTENT_RELATIVE_COLORIMETRIC: Short = 3
        val RENDERING_INTENT_SATURATION: Short = 4
        val RENDERING_INTENT_ABSOLUTE_COLORIMETRIC: Short = 5
    }
}

native public trait SVGStyleElement : SVGElement, SVGLangSpace {
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

native public trait SVGPoint {
    var x: dynamic
        get() = noImpl
        set(value) = noImpl
    var y: dynamic
        get() = noImpl
        set(value) = noImpl
    fun matrixTransform(matrix: SVGMatrix): SVGPoint = noImpl
}

native public trait SVGPointList {
    var numberOfItems: Int
        get() = noImpl
        set(value) = noImpl
    fun clear(): Unit = noImpl
    fun initialize(newItem: SVGPoint): SVGPoint = noImpl
    fun getItem(index: Int): SVGPoint = noImpl
    fun insertItemBefore(newItem: SVGPoint, index: Int): SVGPoint = noImpl
    fun replaceItem(newItem: SVGPoint, index: Int): SVGPoint = noImpl
    fun removeItem(index: Int): SVGPoint = noImpl
    fun appendItem(newItem: SVGPoint): SVGPoint = noImpl
}

native public trait SVGMatrix {
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

native public trait SVGTransform {
    var type: Short
        get() = noImpl
        set(value) = noImpl
    var matrix: SVGMatrix
        get() = noImpl
        set(value) = noImpl
    var angle: Float
        get() = noImpl
        set(value) = noImpl
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

native public trait SVGTransformList {
    var numberOfItems: Int
        get() = noImpl
        set(value) = noImpl
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

native public trait SVGAnimatedTransformList {
    var baseVal: SVGTransformList
        get() = noImpl
        set(value) = noImpl
    var animVal: SVGTransformList
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGPreserveAspectRatio {
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

native public trait SVGAnimatedPreserveAspectRatio {
    var baseVal: SVGPreserveAspectRatio
        get() = noImpl
        set(value) = noImpl
    var animVal: SVGPreserveAspectRatio
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGPathSeg {
    var pathSegType: Short
        get() = noImpl
        set(value) = noImpl
    var pathSegTypeAsLetter: String
        get() = noImpl
        set(value) = noImpl

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

native public trait SVGPathSegClosePath : SVGPathSeg {
}

native public trait SVGPathSegMovetoAbs : SVGPathSeg {
    var x: dynamic
        get() = noImpl
        set(value) = noImpl
    var y: dynamic
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGPathSegMovetoRel : SVGPathSeg {
    var x: dynamic
        get() = noImpl
        set(value) = noImpl
    var y: dynamic
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGPathSegLinetoAbs : SVGPathSeg {
    var x: dynamic
        get() = noImpl
        set(value) = noImpl
    var y: dynamic
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGPathSegLinetoRel : SVGPathSeg {
    var x: dynamic
        get() = noImpl
        set(value) = noImpl
    var y: dynamic
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGPathSegCurvetoCubicAbs : SVGPathSeg {
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

native public trait SVGPathSegCurvetoCubicRel : SVGPathSeg {
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

native public trait SVGPathSegCurvetoQuadraticAbs : SVGPathSeg {
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

native public trait SVGPathSegCurvetoQuadraticRel : SVGPathSeg {
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

native public trait SVGPathSegArcAbs : SVGPathSeg {
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

native public trait SVGPathSegArcRel : SVGPathSeg {
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

native public trait SVGPathSegLinetoHorizontalAbs : SVGPathSeg {
    var x: dynamic
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGPathSegLinetoHorizontalRel : SVGPathSeg {
    var x: dynamic
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGPathSegLinetoVerticalAbs : SVGPathSeg {
    var y: dynamic
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGPathSegLinetoVerticalRel : SVGPathSeg {
    var y: dynamic
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGPathSegCurvetoCubicSmoothAbs : SVGPathSeg {
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

native public trait SVGPathSegCurvetoCubicSmoothRel : SVGPathSeg {
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

native public trait SVGPathSegCurvetoQuadraticSmoothAbs : SVGPathSeg {
    var x: dynamic
        get() = noImpl
        set(value) = noImpl
    var y: dynamic
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGPathSegCurvetoQuadraticSmoothRel : SVGPathSeg {
    var x: dynamic
        get() = noImpl
        set(value) = noImpl
    var y: dynamic
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGPathSegList {
    var numberOfItems: Int
        get() = noImpl
        set(value) = noImpl
    fun clear(): Unit = noImpl
    fun initialize(newItem: SVGPathSeg): SVGPathSeg = noImpl
    fun getItem(index: Int): SVGPathSeg = noImpl
    fun insertItemBefore(newItem: SVGPathSeg, index: Int): SVGPathSeg = noImpl
    fun replaceItem(newItem: SVGPathSeg, index: Int): SVGPathSeg = noImpl
    fun removeItem(index: Int): SVGPathSeg = noImpl
    fun appendItem(newItem: SVGPathSeg): SVGPathSeg = noImpl
}

native public trait SVGAnimatedPathData {
    var pathSegList: SVGPathSegList
        get() = noImpl
        set(value) = noImpl
    var normalizedPathSegList: SVGPathSegList
        get() = noImpl
        set(value) = noImpl
    var animatedPathSegList: SVGPathSegList
        get() = noImpl
        set(value) = noImpl
    var animatedNormalizedPathSegList: SVGPathSegList
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGPathElement : SVGElement, SVGTests, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGTransformable, SVGAnimatedPathData {
    var pathLength: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl
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

native public trait SVGRectElement : SVGElement, SVGTests, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGTransformable {
    var x: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var y: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var width: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var height: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var rx: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var ry: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGCircleElement : SVGElement, SVGTests, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGTransformable {
    var cx: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var cy: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var r: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGEllipseElement : SVGElement, SVGTests, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGTransformable {
    var cx: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var cy: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var rx: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var ry: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGLineElement : SVGElement, SVGTests, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGTransformable {
    var x1: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var y1: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var x2: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var y2: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGAnimatedPoints {
    var points: SVGPointList
        get() = noImpl
        set(value) = noImpl
    var animatedPoints: SVGPointList
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGPolylineElement : SVGElement, SVGTests, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGTransformable, SVGAnimatedPoints {
}

native public trait SVGPolygonElement : SVGElement, SVGTests, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGTransformable, SVGAnimatedPoints {
}

native public trait SVGTextContentElement : SVGElement, SVGTests, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable {
    var textLength: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var lengthAdjust: SVGAnimatedEnumeration
        get() = noImpl
        set(value) = noImpl
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

native public trait SVGTextPositioningElement : SVGTextContentElement {
    var x: SVGAnimatedLengthList
        get() = noImpl
        set(value) = noImpl
    var y: SVGAnimatedLengthList
        get() = noImpl
        set(value) = noImpl
    var dx: SVGAnimatedLengthList
        get() = noImpl
        set(value) = noImpl
    var dy: SVGAnimatedLengthList
        get() = noImpl
        set(value) = noImpl
    var rotate: SVGAnimatedNumberList
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGTextElement : SVGTextPositioningElement, SVGTransformable {
}

native public trait SVGTSpanElement : SVGTextPositioningElement {
}

native public trait SVGTRefElement : SVGTextPositioningElement, SVGURIReference {
}

native public trait SVGTextPathElement : SVGTextContentElement, SVGURIReference {
    var startOffset: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var method: SVGAnimatedEnumeration
        get() = noImpl
        set(value) = noImpl
    var spacing: SVGAnimatedEnumeration
        get() = noImpl
        set(value) = noImpl

    companion object {
        val TEXTPATH_METHODTYPE_UNKNOWN: Short = 0
        val TEXTPATH_METHODTYPE_ALIGN: Short = 1
        val TEXTPATH_METHODTYPE_STRETCH: Short = 2
        val TEXTPATH_SPACINGTYPE_UNKNOWN: Short = 0
        val TEXTPATH_SPACINGTYPE_AUTO: Short = 1
        val TEXTPATH_SPACINGTYPE_EXACT: Short = 2
    }
}

native public trait SVGAltGlyphElement : SVGTextPositioningElement, SVGURIReference {
    var glyphRef: dynamic
        get() = noImpl
        set(value) = noImpl
    var format: dynamic
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGAltGlyphDefElement : SVGElement {
}

native public trait SVGAltGlyphItemElement : SVGElement {
}

native public trait SVGGlyphRefElement : SVGElement, SVGURIReference, SVGStylable {
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

native public trait SVGPaint : SVGColor {
    var paintType: Short
        get() = noImpl
        set(value) = noImpl
    var uri: String
        get() = noImpl
        set(value) = noImpl
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

native public trait SVGMarkerElement : SVGElement, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGFitToViewBox {
    var refX: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var refY: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var markerUnits: SVGAnimatedEnumeration
        get() = noImpl
        set(value) = noImpl
    var markerWidth: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var markerHeight: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var orientType: SVGAnimatedEnumeration
        get() = noImpl
        set(value) = noImpl
    var orientAngle: SVGAnimatedAngle
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

native public trait SVGColorProfileElement : SVGElement, SVGURIReference, SVGRenderingIntent {
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

native public trait SVGColorProfileRule : SVGCSSRule, SVGRenderingIntent {
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

native public trait SVGGradientElement : SVGElement, SVGURIReference, SVGExternalResourcesRequired, SVGStylable, SVGUnitTypes {
    var gradientUnits: SVGAnimatedEnumeration
        get() = noImpl
        set(value) = noImpl
    var gradientTransform: SVGAnimatedTransformList
        get() = noImpl
        set(value) = noImpl
    var spreadMethod: SVGAnimatedEnumeration
        get() = noImpl
        set(value) = noImpl

    companion object {
        val SVG_SPREADMETHOD_UNKNOWN: Short = 0
        val SVG_SPREADMETHOD_PAD: Short = 1
        val SVG_SPREADMETHOD_REFLECT: Short = 2
        val SVG_SPREADMETHOD_REPEAT: Short = 3
    }
}

native public trait SVGLinearGradientElement : SVGGradientElement {
    var x1: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var y1: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var x2: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var y2: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGRadialGradientElement : SVGGradientElement {
    var cx: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var cy: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var r: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var fx: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var fy: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGStopElement : SVGElement, SVGStylable {
    var offset: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGPatternElement : SVGElement, SVGURIReference, SVGTests, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGFitToViewBox, SVGUnitTypes {
    var patternUnits: SVGAnimatedEnumeration
        get() = noImpl
        set(value) = noImpl
    var patternContentUnits: SVGAnimatedEnumeration
        get() = noImpl
        set(value) = noImpl
    var patternTransform: SVGAnimatedTransformList
        get() = noImpl
        set(value) = noImpl
    var x: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var y: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var width: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var height: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGClipPathElement : SVGElement, SVGTests, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGTransformable, SVGUnitTypes {
    var clipPathUnits: SVGAnimatedEnumeration
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGMaskElement : SVGElement, SVGTests, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGUnitTypes {
    var maskUnits: SVGAnimatedEnumeration
        get() = noImpl
        set(value) = noImpl
    var maskContentUnits: SVGAnimatedEnumeration
        get() = noImpl
        set(value) = noImpl
    var x: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var y: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var width: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var height: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGFilterElement : SVGElement, SVGURIReference, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGUnitTypes {
    var filterUnits: SVGAnimatedEnumeration
        get() = noImpl
        set(value) = noImpl
    var primitiveUnits: SVGAnimatedEnumeration
        get() = noImpl
        set(value) = noImpl
    var x: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var y: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var width: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var height: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var filterResX: SVGAnimatedInteger
        get() = noImpl
        set(value) = noImpl
    var filterResY: SVGAnimatedInteger
        get() = noImpl
        set(value) = noImpl
    fun setFilterRes(filterResX: Int, filterResY: Int): Unit = noImpl
}

native public trait SVGFilterPrimitiveStandardAttributes : SVGStylable {
    var x: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var y: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var width: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var height: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var result: SVGAnimatedString
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGFEBlendElement : SVGElement, SVGFilterPrimitiveStandardAttributes {
    var in1: SVGAnimatedString
        get() = noImpl
        set(value) = noImpl
    var in2: SVGAnimatedString
        get() = noImpl
        set(value) = noImpl
    var mode: SVGAnimatedEnumeration
        get() = noImpl
        set(value) = noImpl

    companion object {
        val SVG_FEBLEND_MODE_UNKNOWN: Short = 0
        val SVG_FEBLEND_MODE_NORMAL: Short = 1
        val SVG_FEBLEND_MODE_MULTIPLY: Short = 2
        val SVG_FEBLEND_MODE_SCREEN: Short = 3
        val SVG_FEBLEND_MODE_DARKEN: Short = 4
        val SVG_FEBLEND_MODE_LIGHTEN: Short = 5
    }
}

native public trait SVGFEColorMatrixElement : SVGElement, SVGFilterPrimitiveStandardAttributes {
    var in1: SVGAnimatedString
        get() = noImpl
        set(value) = noImpl
    var type: SVGAnimatedEnumeration
        get() = noImpl
        set(value) = noImpl
    var values: SVGAnimatedNumberList
        get() = noImpl
        set(value) = noImpl

    companion object {
        val SVG_FECOLORMATRIX_TYPE_UNKNOWN: Short = 0
        val SVG_FECOLORMATRIX_TYPE_MATRIX: Short = 1
        val SVG_FECOLORMATRIX_TYPE_SATURATE: Short = 2
        val SVG_FECOLORMATRIX_TYPE_HUEROTATE: Short = 3
        val SVG_FECOLORMATRIX_TYPE_LUMINANCETOALPHA: Short = 4
    }
}

native public trait SVGFEComponentTransferElement : SVGElement, SVGFilterPrimitiveStandardAttributes {
    var in1: SVGAnimatedString
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGComponentTransferFunctionElement : SVGElement {
    var type: SVGAnimatedEnumeration
        get() = noImpl
        set(value) = noImpl
    var tableValues: SVGAnimatedNumberList
        get() = noImpl
        set(value) = noImpl
    var slope: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl
    var intercept: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl
    var amplitude: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl
    var exponent: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl
    var offset: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl

    companion object {
        val SVG_FECOMPONENTTRANSFER_TYPE_UNKNOWN: Short = 0
        val SVG_FECOMPONENTTRANSFER_TYPE_IDENTITY: Short = 1
        val SVG_FECOMPONENTTRANSFER_TYPE_TABLE: Short = 2
        val SVG_FECOMPONENTTRANSFER_TYPE_DISCRETE: Short = 3
        val SVG_FECOMPONENTTRANSFER_TYPE_LINEAR: Short = 4
        val SVG_FECOMPONENTTRANSFER_TYPE_GAMMA: Short = 5
    }
}

native public trait SVGFEFuncRElement : SVGComponentTransferFunctionElement {
}

native public trait SVGFEFuncGElement : SVGComponentTransferFunctionElement {
}

native public trait SVGFEFuncBElement : SVGComponentTransferFunctionElement {
}

native public trait SVGFEFuncAElement : SVGComponentTransferFunctionElement {
}

native public trait SVGFECompositeElement : SVGElement, SVGFilterPrimitiveStandardAttributes {
    var in1: SVGAnimatedString
        get() = noImpl
        set(value) = noImpl
    var in2: SVGAnimatedString
        get() = noImpl
        set(value) = noImpl
    var operator: SVGAnimatedEnumeration
        get() = noImpl
        set(value) = noImpl
    var k1: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl
    var k2: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl
    var k3: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl
    var k4: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl

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

native public trait SVGFEConvolveMatrixElement : SVGElement, SVGFilterPrimitiveStandardAttributes {
    var in1: SVGAnimatedString
        get() = noImpl
        set(value) = noImpl
    var orderX: SVGAnimatedInteger
        get() = noImpl
        set(value) = noImpl
    var orderY: SVGAnimatedInteger
        get() = noImpl
        set(value) = noImpl
    var kernelMatrix: SVGAnimatedNumberList
        get() = noImpl
        set(value) = noImpl
    var divisor: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl
    var bias: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl
    var targetX: SVGAnimatedInteger
        get() = noImpl
        set(value) = noImpl
    var targetY: SVGAnimatedInteger
        get() = noImpl
        set(value) = noImpl
    var edgeMode: SVGAnimatedEnumeration
        get() = noImpl
        set(value) = noImpl
    var kernelUnitLengthX: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl
    var kernelUnitLengthY: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl
    var preserveAlpha: SVGAnimatedBoolean
        get() = noImpl
        set(value) = noImpl

    companion object {
        val SVG_EDGEMODE_UNKNOWN: Short = 0
        val SVG_EDGEMODE_DUPLICATE: Short = 1
        val SVG_EDGEMODE_WRAP: Short = 2
        val SVG_EDGEMODE_NONE: Short = 3
    }
}

native public trait SVGFEDiffuseLightingElement : SVGElement, SVGFilterPrimitiveStandardAttributes {
    var in1: SVGAnimatedString
        get() = noImpl
        set(value) = noImpl
    var surfaceScale: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl
    var diffuseConstant: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl
    var kernelUnitLengthX: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl
    var kernelUnitLengthY: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGFEDistantLightElement : SVGElement {
    var azimuth: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl
    var elevation: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGFEPointLightElement : SVGElement {
    var x: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl
    var y: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl
    var z: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGFESpotLightElement : SVGElement {
    var x: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl
    var y: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl
    var z: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl
    var pointsAtX: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl
    var pointsAtY: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl
    var pointsAtZ: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl
    var specularExponent: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl
    var limitingConeAngle: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGFEDisplacementMapElement : SVGElement, SVGFilterPrimitiveStandardAttributes {
    var in1: SVGAnimatedString
        get() = noImpl
        set(value) = noImpl
    var in2: SVGAnimatedString
        get() = noImpl
        set(value) = noImpl
    var scale: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl
    var xChannelSelector: SVGAnimatedEnumeration
        get() = noImpl
        set(value) = noImpl
    var yChannelSelector: SVGAnimatedEnumeration
        get() = noImpl
        set(value) = noImpl

    companion object {
        val SVG_CHANNEL_UNKNOWN: Short = 0
        val SVG_CHANNEL_R: Short = 1
        val SVG_CHANNEL_G: Short = 2
        val SVG_CHANNEL_B: Short = 3
        val SVG_CHANNEL_A: Short = 4
    }
}

native public trait SVGFEFloodElement : SVGElement, SVGFilterPrimitiveStandardAttributes {
}

native public trait SVGFEGaussianBlurElement : SVGElement, SVGFilterPrimitiveStandardAttributes {
    var in1: SVGAnimatedString
        get() = noImpl
        set(value) = noImpl
    var stdDeviationX: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl
    var stdDeviationY: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl
    fun setStdDeviation(stdDeviationX: Float, stdDeviationY: Float): Unit = noImpl
}

native public trait SVGFEImageElement : SVGElement, SVGURIReference, SVGLangSpace, SVGExternalResourcesRequired, SVGFilterPrimitiveStandardAttributes {
    var preserveAspectRatio: SVGAnimatedPreserveAspectRatio
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGFEMergeElement : SVGElement, SVGFilterPrimitiveStandardAttributes {
}

native public trait SVGFEMergeNodeElement : SVGElement {
    var in1: SVGAnimatedString
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGFEMorphologyElement : SVGElement, SVGFilterPrimitiveStandardAttributes {
    var in1: SVGAnimatedString
        get() = noImpl
        set(value) = noImpl
    var operator: SVGAnimatedEnumeration
        get() = noImpl
        set(value) = noImpl
    var radiusX: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl
    var radiusY: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl

    companion object {
        val SVG_MORPHOLOGY_OPERATOR_UNKNOWN: Short = 0
        val SVG_MORPHOLOGY_OPERATOR_ERODE: Short = 1
        val SVG_MORPHOLOGY_OPERATOR_DILATE: Short = 2
    }
}

native public trait SVGFEOffsetElement : SVGElement, SVGFilterPrimitiveStandardAttributes {
    var in1: SVGAnimatedString
        get() = noImpl
        set(value) = noImpl
    var dx: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl
    var dy: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGFESpecularLightingElement : SVGElement, SVGFilterPrimitiveStandardAttributes {
    var in1: SVGAnimatedString
        get() = noImpl
        set(value) = noImpl
    var surfaceScale: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl
    var specularConstant: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl
    var specularExponent: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl
    var kernelUnitLengthX: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl
    var kernelUnitLengthY: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGFETileElement : SVGElement, SVGFilterPrimitiveStandardAttributes {
    var in1: SVGAnimatedString
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGFETurbulenceElement : SVGElement, SVGFilterPrimitiveStandardAttributes {
    var baseFrequencyX: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl
    var baseFrequencyY: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl
    var numOctaves: SVGAnimatedInteger
        get() = noImpl
        set(value) = noImpl
    var seed: SVGAnimatedNumber
        get() = noImpl
        set(value) = noImpl
    var stitchTiles: SVGAnimatedEnumeration
        get() = noImpl
        set(value) = noImpl
    var type: SVGAnimatedEnumeration
        get() = noImpl
        set(value) = noImpl

    companion object {
        val SVG_TURBULENCE_TYPE_UNKNOWN: Short = 0
        val SVG_TURBULENCE_TYPE_FRACTALNOISE: Short = 1
        val SVG_TURBULENCE_TYPE_TURBULENCE: Short = 2
        val SVG_STITCHTYPE_UNKNOWN: Short = 0
        val SVG_STITCHTYPE_STITCH: Short = 1
        val SVG_STITCHTYPE_NOSTITCH: Short = 2
    }
}

native public trait SVGCursorElement : SVGElement, SVGURIReference, SVGTests, SVGExternalResourcesRequired {
    var x: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var y: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGAElement : SVGElement, SVGURIReference, SVGTests, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGTransformable {
    var target: SVGAnimatedString
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGViewElement : SVGElement, SVGExternalResourcesRequired, SVGFitToViewBox, SVGZoomAndPan {
    var viewTarget: SVGStringList
        get() = noImpl
        set(value) = noImpl
}

native public trait SVGScriptElement : SVGElement, SVGURIReference, SVGExternalResourcesRequired {
    var type: dynamic
        get() = noImpl
        set(value) = noImpl
}

native public open class SVGZoomEvent : UIEvent(noImpl, noImpl) {
    var zoomRectScreen: SVGRect
        get() = noImpl
        set(value) = noImpl
    var previousScale: Float
        get() = noImpl
        set(value) = noImpl
    var previousTranslate: SVGPoint
        get() = noImpl
        set(value) = noImpl
    var newScale: Float
        get() = noImpl
        set(value) = noImpl
    var newTranslate: SVGPoint
        get() = noImpl
        set(value) = noImpl
}

native public trait ElementTimeControl {
    fun beginElement(): Unit = noImpl
    fun beginElementAt(offset: Float): Unit = noImpl
    fun endElement(): Unit = noImpl
    fun endElementAt(offset: Float): Unit = noImpl
}

native public open class TimeEvent : Event(noImpl, noImpl) {
    var view: dynamic
        get() = noImpl
        set(value) = noImpl
    var detail: Int
        get() = noImpl
        set(value) = noImpl
    fun initTimeEvent(typeArg: String, viewArg: dynamic, detailArg: Int): Unit = noImpl
}

native public trait SVGAnimationElement : SVGElement, SVGTests, SVGExternalResourcesRequired, ElementTimeControl {
    var targetElement: SVGElement
        get() = noImpl
        set(value) = noImpl
    fun getStartTime(): Float = noImpl
    fun getCurrentTime(): Float = noImpl
    fun getSimpleDuration(): Float = noImpl
}

native public trait SVGAnimateElement : SVGAnimationElement, SVGStylable {
}

native public trait SVGSetElement : SVGAnimationElement {
}

native public trait SVGAnimateMotionElement : SVGAnimationElement {
}

native public trait SVGMPathElement : SVGElement, SVGURIReference, SVGExternalResourcesRequired {
}

native public trait SVGAnimateColorElement : SVGAnimationElement, SVGStylable {
}

native public trait SVGAnimateTransformElement : SVGAnimationElement {
}

native public trait SVGFontElement : SVGElement, SVGExternalResourcesRequired, SVGStylable {
}

native public trait SVGGlyphElement : SVGElement, SVGStylable {
}

native public trait SVGMissingGlyphElement : SVGElement, SVGStylable {
}

native public trait SVGHKernElement : SVGElement {
}

native public trait SVGVKernElement : SVGElement {
}

native public trait SVGFontFaceElement : SVGElement {
}

native public trait SVGFontFaceSrcElement : SVGElement {
}

native public trait SVGFontFaceUriElement : SVGElement {
}

native public trait SVGFontFaceFormatElement : SVGElement {
}

native public trait SVGFontFaceNameElement : SVGElement {
}

native public trait SVGMetadataElement : SVGElement {
}

native public trait SVGForeignObjectElement : SVGElement, SVGTests, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGTransformable {
    var x: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var y: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var width: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
    var height: SVGAnimatedLength
        get() = noImpl
        set(value) = noImpl
}

