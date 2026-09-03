/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// NOTE: THIS FILE IS AUTO-GENERATED, DO NOT EDIT!
// See github.com/kotlin/dukat for details

@file:Suppress(
    "NO_EXPLICIT_VISIBILITY_IN_API_MODE",
    "NO_EXPLICIT_RETURN_TYPE_IN_API_MODE",
    "DEPRECATION"
) // TODO: Fix in dukat: https://github.com/Kotlin/dukat/issues/124

package org.w3c.dom.svg

import kotlinx.browser.PLEASE_USE_KOTLINX_BROWSER_INSTEAD
import kotlin.js.*
import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.dom.css.*
import kotlin.internal.InlineOnly

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGElement : Element, ElementCSSInlineStyle, GlobalEventHandlers, SVGElementInstance {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val dataset: DOMStringMap

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val ownerSVGElement: SVGSVGElement?

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val viewportElement: SVGElement?

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var tabIndex: Int

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun focus()

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun blur()

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public external interface SVGBoundingBoxOptions {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    var fill: Boolean?
        get() = definedExternally
        set(value) = definedExternally

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    var stroke: Boolean?
        get() = definedExternally
        set(value) = definedExternally

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    var markers: Boolean?
        get() = definedExternally
        set(value) = definedExternally

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    var clipped: Boolean?
        get() = definedExternally
        set(value) = definedExternally
}

@InlineOnly
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public inline fun SVGBoundingBoxOptions(
    fill: Boolean? = true,
    stroke: Boolean? = false,
    markers: Boolean? = false,
    clipped: Boolean? = false
): SVGBoundingBoxOptions {
    val o = js("({})")
    o["fill"] = fill
    o["stroke"] = stroke
    o["markers"] = markers
    o["clipped"] = clipped
    return o
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGGraphicsElement : SVGElement, SVGTests {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val transform: SVGAnimatedTransformList

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun getBBox(options: SVGBoundingBoxOptions = definedExternally): DOMRect

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun getCTM(): DOMMatrix?

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun getScreenCTM(): DOMMatrix?

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGGeometryElement : SVGGraphicsElement {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val pathLength: SVGAnimatedNumber

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun isPointInFill(point: DOMPoint): Boolean

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun isPointInStroke(point: DOMPoint): Boolean

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun getTotalLength(): Float

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun getPointAtLength(distance: Float): DOMPoint

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGNumber {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var value: Float
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGLength {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val unitType: Short

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var value: Float

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var valueInSpecifiedUnits: Float

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var valueAsString: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun newValueSpecifiedUnits(unitType: Short, valueInSpecifiedUnits: Float)

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun convertToSpecifiedUnits(unitType: Short)

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_LENGTHTYPE_UNKNOWN: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_LENGTHTYPE_NUMBER: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_LENGTHTYPE_PERCENTAGE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_LENGTHTYPE_EMS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_LENGTHTYPE_EXS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_LENGTHTYPE_PX: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_LENGTHTYPE_CM: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_LENGTHTYPE_MM: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_LENGTHTYPE_IN: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_LENGTHTYPE_PT: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_LENGTHTYPE_PC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGAngle {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val unitType: Short

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var value: Float

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var valueInSpecifiedUnits: Float

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var valueAsString: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun newValueSpecifiedUnits(unitType: Short, valueInSpecifiedUnits: Float)

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun convertToSpecifiedUnits(unitType: Short)

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_ANGLETYPE_UNKNOWN: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_ANGLETYPE_UNSPECIFIED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_ANGLETYPE_DEG: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_ANGLETYPE_RAD: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_ANGLETYPE_GRAD: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGNameList {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val length: Int

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val numberOfItems: Int

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun clear()

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun initialize(newItem: dynamic): dynamic

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun insertItemBefore(newItem: dynamic, index: Int): dynamic

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun replaceItem(newItem: dynamic, index: Int): dynamic

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun removeItem(index: Int): dynamic

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun appendItem(newItem: dynamic): dynamic

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun getItem(index: Int): dynamic
}

@InlineOnly
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public inline operator fun SVGNameList.get(index: Int): dynamic = asDynamic()[index]

@InlineOnly
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public inline operator fun SVGNameList.set(index: Int, newItem: dynamic) {
    asDynamic()[index] = newItem
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGNumberList {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val length: Int

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val numberOfItems: Int

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun clear()

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun initialize(newItem: SVGNumber): SVGNumber

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun insertItemBefore(newItem: SVGNumber, index: Int): SVGNumber

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun replaceItem(newItem: SVGNumber, index: Int): SVGNumber

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun removeItem(index: Int): SVGNumber

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun appendItem(newItem: SVGNumber): SVGNumber

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun getItem(index: Int): SVGNumber
}

@InlineOnly
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public inline operator fun SVGNumberList.get(index: Int): SVGNumber? = asDynamic()[index]

@InlineOnly
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public inline operator fun SVGNumberList.set(index: Int, newItem: SVGNumber) {
    asDynamic()[index] = newItem
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGLengthList {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val length: Int

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val numberOfItems: Int

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun clear()

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun initialize(newItem: SVGLength): SVGLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun insertItemBefore(newItem: SVGLength, index: Int): SVGLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun replaceItem(newItem: SVGLength, index: Int): SVGLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun removeItem(index: Int): SVGLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun appendItem(newItem: SVGLength): SVGLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun getItem(index: Int): SVGLength
}

@InlineOnly
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public inline operator fun SVGLengthList.get(index: Int): SVGLength? = asDynamic()[index]

@InlineOnly
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public inline operator fun SVGLengthList.set(index: Int, newItem: SVGLength) {
    asDynamic()[index] = newItem
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGAnimatedBoolean {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var baseVal: Boolean

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val animVal: Boolean
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGAnimatedEnumeration {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var baseVal: Short

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val animVal: Short
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGAnimatedInteger {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var baseVal: Int

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val animVal: Int
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGAnimatedNumber {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var baseVal: Float

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val animVal: Float
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGAnimatedLength {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val baseVal: SVGLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val animVal: SVGLength
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGAnimatedAngle {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val baseVal: SVGAngle

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val animVal: SVGAngle
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGAnimatedString {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var baseVal: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val animVal: String
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGAnimatedRect {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val baseVal: DOMRect

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val animVal: DOMRectReadOnly
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGAnimatedNumberList {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val baseVal: SVGNumberList

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val animVal: SVGNumberList
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGAnimatedLengthList {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val baseVal: SVGLengthList

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val animVal: SVGLengthList
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGStringList {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val length: Int

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val numberOfItems: Int

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun clear()

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun initialize(newItem: String): String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun insertItemBefore(newItem: String, index: Int): String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun replaceItem(newItem: String, index: Int): String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun removeItem(index: Int): String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun appendItem(newItem: String): String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun getItem(index: Int): String
}

@InlineOnly
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public inline operator fun SVGStringList.get(index: Int): String? = asDynamic()[index]

@InlineOnly
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public inline operator fun SVGStringList.set(index: Int, newItem: String) {
    asDynamic()[index] = newItem
}

@Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public external interface SVGUnitTypes {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_UNIT_TYPE_UNKNOWN: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_UNIT_TYPE_USERSPACEONUSE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_UNIT_TYPE_OBJECTBOUNDINGBOX: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public external interface SVGTests {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    val requiredExtensions: SVGStringList

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    val systemLanguage: SVGStringList
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public external interface SVGFitToViewBox {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    val viewBox: SVGAnimatedRect

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    val preserveAspectRatio: SVGAnimatedPreserveAspectRatio
}

@Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public external interface SVGZoomAndPan {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    var zoomAndPan: Short

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_ZOOMANDPAN_UNKNOWN: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_ZOOMANDPAN_DISABLE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_ZOOMANDPAN_MAGNIFY: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public external interface SVGURIReference {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    val href: SVGAnimatedString
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGSVGElement : SVGGraphicsElement, SVGFitToViewBox, SVGZoomAndPan, WindowEventHandlers {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val x: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val y: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val width: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val height: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var currentScale: Float

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val currentTranslate: DOMPointReadOnly

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun getIntersectionList(rect: DOMRectReadOnly, referenceElement: SVGElement?): NodeList

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun getEnclosureList(rect: DOMRectReadOnly, referenceElement: SVGElement?): NodeList

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun checkIntersection(element: SVGElement, rect: DOMRectReadOnly): Boolean

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun checkEnclosure(element: SVGElement, rect: DOMRectReadOnly): Boolean

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun deselectAll()

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun createSVGNumber(): SVGNumber

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun createSVGLength(): SVGLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun createSVGAngle(): SVGAngle

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun createSVGPoint(): DOMPoint

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun createSVGMatrix(): DOMMatrix

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun createSVGRect(): DOMRect

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun createSVGTransform(): SVGTransform

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun createSVGTransformFromMatrix(matrix: DOMMatrixReadOnly): SVGTransform

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun getElementById(elementId: String): Element

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun suspendRedraw(maxWaitMilliseconds: Int): Int

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun unsuspendRedraw(suspendHandleID: Int)

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun unsuspendRedrawAll()

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun forceRedraw()

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_ZOOMANDPAN_UNKNOWN: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_ZOOMANDPAN_DISABLE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_ZOOMANDPAN_MAGNIFY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGGElement : SVGGraphicsElement {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGUnknownElement : SVGGraphicsElement {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGDefsElement : SVGGraphicsElement {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGDescElement : SVGElement {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGMetadataElement : SVGElement {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGTitleElement : SVGElement {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGSymbolElement : SVGGraphicsElement, SVGFitToViewBox {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGUseElement : SVGGraphicsElement, SVGURIReference {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val x: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val y: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val width: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val height: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val instanceRoot: SVGElement?

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val animatedInstanceRoot: SVGElement?

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public open external class SVGUseElementShadowRoot : ShadowRoot {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public external interface SVGElementInstance {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    val correspondingElement: SVGElement?
        get() = definedExternally

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    val correspondingUseElement: SVGUseElement?
        get() = definedExternally
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public open external class ShadowAnimation(source: dynamic, newTarget: dynamic) {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val sourceAnimation: dynamic
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGSwitchElement : SVGGraphicsElement {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public external interface GetSVGDocument {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun getSVGDocument(): Document
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGStyleElement : SVGElement, LinkStyle {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var type: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var media: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var title: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGTransform {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val type: Short

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val matrix: DOMMatrix

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val angle: Float

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun setMatrix(matrix: DOMMatrixReadOnly)

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun setTranslate(tx: Float, ty: Float)

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun setScale(sx: Float, sy: Float)

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun setRotate(angle: Float, cx: Float, cy: Float)

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun setSkewX(angle: Float)

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun setSkewY(angle: Float)

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_TRANSFORM_UNKNOWN: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_TRANSFORM_MATRIX: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_TRANSFORM_TRANSLATE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_TRANSFORM_SCALE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_TRANSFORM_ROTATE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_TRANSFORM_SKEWX: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_TRANSFORM_SKEWY: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGTransformList {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val length: Int

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val numberOfItems: Int

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun clear()

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun initialize(newItem: SVGTransform): SVGTransform

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun insertItemBefore(newItem: SVGTransform, index: Int): SVGTransform

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun replaceItem(newItem: SVGTransform, index: Int): SVGTransform

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun removeItem(index: Int): SVGTransform

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun appendItem(newItem: SVGTransform): SVGTransform

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun createSVGTransformFromMatrix(matrix: DOMMatrixReadOnly): SVGTransform

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun consolidate(): SVGTransform?

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun getItem(index: Int): SVGTransform
}

@InlineOnly
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public inline operator fun SVGTransformList.get(index: Int): SVGTransform? = asDynamic()[index]

@InlineOnly
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public inline operator fun SVGTransformList.set(index: Int, newItem: SVGTransform) {
    asDynamic()[index] = newItem
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGAnimatedTransformList {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val baseVal: SVGTransformList

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val animVal: SVGTransformList
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGPreserveAspectRatio {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var align: Short

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var meetOrSlice: Short

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_PRESERVEASPECTRATIO_UNKNOWN: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_PRESERVEASPECTRATIO_NONE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_PRESERVEASPECTRATIO_XMINYMIN: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_PRESERVEASPECTRATIO_XMIDYMIN: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_PRESERVEASPECTRATIO_XMAXYMIN: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_PRESERVEASPECTRATIO_XMINYMID: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_PRESERVEASPECTRATIO_XMIDYMID: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_PRESERVEASPECTRATIO_XMAXYMID: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_PRESERVEASPECTRATIO_XMINYMAX: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_PRESERVEASPECTRATIO_XMIDYMAX: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_PRESERVEASPECTRATIO_XMAXYMAX: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_MEETORSLICE_UNKNOWN: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_MEETORSLICE_MEET: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_MEETORSLICE_SLICE: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGAnimatedPreserveAspectRatio {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val baseVal: SVGPreserveAspectRatio

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val animVal: SVGPreserveAspectRatio
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGPathElement : SVGGeometryElement {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGRectElement : SVGGeometryElement {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val x: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val y: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val width: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val height: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val rx: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val ry: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGCircleElement : SVGGeometryElement {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val cx: SVGAnimatedLength
    open val cy: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val r: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGEllipseElement : SVGGeometryElement {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val cx: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val cy: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val rx: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val ry: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGLineElement : SVGGeometryElement {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val x1: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val y1: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val x2: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val y2: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGMeshElement : SVGGeometryElement, SVGURIReference {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public external interface SVGAnimatedPoints {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    val points: SVGPointList

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    val animatedPoints: SVGPointList
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGPointList {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val length: Int

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val numberOfItems: Int

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun clear()

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun initialize(newItem: DOMPoint): DOMPoint

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun insertItemBefore(newItem: DOMPoint, index: Int): DOMPoint

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun replaceItem(newItem: DOMPoint, index: Int): DOMPoint

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun removeItem(index: Int): DOMPoint

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun appendItem(newItem: DOMPoint): DOMPoint

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun getItem(index: Int): DOMPoint
}

@InlineOnly
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public inline operator fun SVGPointList.get(index: Int): DOMPoint? = asDynamic()[index]

@InlineOnly
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public inline operator fun SVGPointList.set(index: Int, newItem: DOMPoint) {
    asDynamic()[index] = newItem
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGPolylineElement : SVGGeometryElement, SVGAnimatedPoints {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGPolygonElement : SVGGeometryElement, SVGAnimatedPoints {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGTextContentElement : SVGGraphicsElement {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val textLength: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val lengthAdjust: SVGAnimatedEnumeration

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun getNumberOfChars(): Int

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun getComputedTextLength(): Float

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun getSubStringLength(charnum: Int, nchars: Int): Float

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun getStartPositionOfChar(charnum: Int): DOMPoint

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun getEndPositionOfChar(charnum: Int): DOMPoint

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun getExtentOfChar(charnum: Int): DOMRect

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun getRotationOfChar(charnum: Int): Float

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun getCharNumAtPosition(point: DOMPoint): Int

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun selectSubString(charnum: Int, nchars: Int)

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val LENGTHADJUST_UNKNOWN: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val LENGTHADJUST_SPACING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val LENGTHADJUST_SPACINGANDGLYPHS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGTextPositioningElement : SVGTextContentElement {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val x: SVGAnimatedLengthList

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val y: SVGAnimatedLengthList

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val dx: SVGAnimatedLengthList

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val dy: SVGAnimatedLengthList

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val rotate: SVGAnimatedNumberList

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val LENGTHADJUST_UNKNOWN: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val LENGTHADJUST_SPACING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val LENGTHADJUST_SPACINGANDGLYPHS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGTextElement : SVGTextPositioningElement {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val LENGTHADJUST_UNKNOWN: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val LENGTHADJUST_SPACING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val LENGTHADJUST_SPACINGANDGLYPHS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGTSpanElement : SVGTextPositioningElement {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val LENGTHADJUST_UNKNOWN: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val LENGTHADJUST_SPACING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val LENGTHADJUST_SPACINGANDGLYPHS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGTextPathElement : SVGTextContentElement, SVGURIReference {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val startOffset: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val method: SVGAnimatedEnumeration

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val spacing: SVGAnimatedEnumeration

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXTPATH_METHODTYPE_UNKNOWN: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXTPATH_METHODTYPE_ALIGN: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXTPATH_METHODTYPE_STRETCH: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXTPATH_SPACINGTYPE_UNKNOWN: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXTPATH_SPACINGTYPE_AUTO: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXTPATH_SPACINGTYPE_EXACT: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val LENGTHADJUST_UNKNOWN: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val LENGTHADJUST_SPACING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val LENGTHADJUST_SPACINGANDGLYPHS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGImageElement : SVGGraphicsElement, SVGURIReference, HTMLOrSVGImageElement {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val x: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val y: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val width: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val height: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val preserveAspectRatio: SVGAnimatedPreserveAspectRatio

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var crossOrigin: String?

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGForeignObjectElement : SVGGraphicsElement {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val x: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val y: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val width: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val height: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGMarkerElement : SVGElement, SVGFitToViewBox {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val refX: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val refY: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val markerUnits: SVGAnimatedEnumeration

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val markerWidth: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val markerHeight: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val orientType: SVGAnimatedEnumeration

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val orientAngle: SVGAnimatedAngle

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var orient: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun setOrientToAuto()

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun setOrientToAngle(angle: SVGAngle)

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_MARKERUNITS_UNKNOWN: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_MARKERUNITS_USERSPACEONUSE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_MARKERUNITS_STROKEWIDTH: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_MARKER_ORIENT_UNKNOWN: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_MARKER_ORIENT_AUTO: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_MARKER_ORIENT_ANGLE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGSolidcolorElement : SVGElement {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGGradientElement : SVGElement, SVGURIReference, SVGUnitTypes {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val gradientUnits: SVGAnimatedEnumeration

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val gradientTransform: SVGAnimatedTransformList

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val spreadMethod: SVGAnimatedEnumeration

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_SPREADMETHOD_UNKNOWN: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_SPREADMETHOD_PAD: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_SPREADMETHOD_REFLECT: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_SPREADMETHOD_REPEAT: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_UNIT_TYPE_UNKNOWN: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_UNIT_TYPE_USERSPACEONUSE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_UNIT_TYPE_OBJECTBOUNDINGBOX: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGLinearGradientElement : SVGGradientElement {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val x1: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val y1: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val x2: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val y2: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_SPREADMETHOD_UNKNOWN: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_SPREADMETHOD_PAD: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_SPREADMETHOD_REFLECT: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_SPREADMETHOD_REPEAT: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_UNIT_TYPE_UNKNOWN: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_UNIT_TYPE_USERSPACEONUSE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_UNIT_TYPE_OBJECTBOUNDINGBOX: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGRadialGradientElement : SVGGradientElement {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val cx: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val cy: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val r: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val fx: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val fy: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val fr: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_SPREADMETHOD_UNKNOWN: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_SPREADMETHOD_PAD: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_SPREADMETHOD_REFLECT: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_SPREADMETHOD_REPEAT: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_UNIT_TYPE_UNKNOWN: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_UNIT_TYPE_USERSPACEONUSE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_UNIT_TYPE_OBJECTBOUNDINGBOX: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGMeshGradientElement : SVGGradientElement {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_SPREADMETHOD_UNKNOWN: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_SPREADMETHOD_PAD: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_SPREADMETHOD_REFLECT: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_SPREADMETHOD_REPEAT: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_UNIT_TYPE_UNKNOWN: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_UNIT_TYPE_USERSPACEONUSE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_UNIT_TYPE_OBJECTBOUNDINGBOX: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGMeshrowElement : SVGElement {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGMeshpatchElement : SVGElement {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGStopElement : SVGElement {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val offset: SVGAnimatedNumber

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGPatternElement : SVGElement, SVGFitToViewBox, SVGURIReference, SVGUnitTypes {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val patternUnits: SVGAnimatedEnumeration

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val patternContentUnits: SVGAnimatedEnumeration

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val patternTransform: SVGAnimatedTransformList

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val x: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val y: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val width: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val height: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_UNIT_TYPE_UNKNOWN: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_UNIT_TYPE_USERSPACEONUSE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_UNIT_TYPE_OBJECTBOUNDINGBOX: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGHatchElement : SVGElement {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGHatchpathElement : SVGElement {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGCursorElement : SVGElement, SVGURIReference {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val x: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val y: SVGAnimatedLength

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGScriptElement : SVGElement, SVGURIReference, HTMLOrSVGScriptElement {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var type: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var crossOrigin: String?

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGAElement : SVGGraphicsElement, SVGURIReference {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val target: SVGAnimatedString

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val download: SVGAnimatedString

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val rel: SVGAnimatedString

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val relList: SVGAnimatedString

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val hreflang: SVGAnimatedString

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val type: SVGAnimatedString

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SVGViewElement : SVGElement, SVGFitToViewBox, SVGZoomAndPan {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_ZOOMANDPAN_UNKNOWN: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_ZOOMANDPAN_DISABLE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val SVG_ZOOMANDPAN_MAGNIFY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ELEMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ATTRIBUTE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val TEXT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CDATA_SECTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_REFERENCE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val ENTITY_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PROCESSING_INSTRUCTION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val COMMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_TYPE_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_FRAGMENT_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NOTATION_NODE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_DISCONNECTED: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_PRECEDING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_FOLLOWING: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINS: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_CONTAINED_BY: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: Short
    }
}
