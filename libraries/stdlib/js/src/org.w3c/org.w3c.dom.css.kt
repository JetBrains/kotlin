/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license 
 * that can be found in the license/LICENSE.txt file.
 */

// NOTE: THIS FILE IS AUTO-GENERATED, DO NOT EDIT!
// See libraries/tools/idl2k for details

@file:Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
package org.w3c.dom.css

import kotlin.js.*
import org.khronos.webgl.*
import org.w3c.css.masking.*
import org.w3c.dom.*
import org.w3c.dom.clipboard.*
import org.w3c.dom.events.*
import org.w3c.dom.mediacapture.*
import org.w3c.dom.parsing.*
import org.w3c.dom.pointerevents.*
import org.w3c.dom.svg.*
import org.w3c.dom.url.*
import org.w3c.fetch.*
import org.w3c.files.*
import org.w3c.notifications.*
import org.w3c.performance.*
import org.w3c.workers.*
import org.w3c.xhr.*

/**
 * Exposes the JavaScript [CSSStyleDeclaration](https://developer.mozilla.org/en/docs/Web/API/CSSStyleDeclaration) to Kotlin
 */
public external abstract class CSSStyleDeclaration : ItemArrayLike<String> {
    open var cssText: String
    override val length: Int
    open val parentRule: CSSRule?
    open var cssFloat: String
    open var _dashed_attribute: String
    open var _camel_cased_attribute: String
    open var _webkit_cased_attribute: String
    open var alignContent: String
    open var alignItems: String
    open var alignSelf: String
    open var animation: String
    open var animationDelay: String
    open var animationDirection: String
    open var animationDuration: String
    open var animationFillMode: String
    open var animationIterationCount: String
    open var animationName: String
    open var animationPlayState: String
    open var animationTimingFunction: String
    open var backfaceVisibility: String
    open var background: String
    open var backgroundAttachment: String
    open var backgroundClip: String
    open var backgroundColor: String
    open var backgroundImage: String
    open var backgroundOrigin: String
    open var backgroundPosition: String
    open var backgroundRepeat: String
    open var backgroundSize: String
    open var border: String
    open var borderBottom: String
    open var borderBottomColor: String
    open var borderBottomLeftRadius: String
    open var borderBottomRightRadius: String
    open var borderBottomStyle: String
    open var borderBottomWidth: String
    open var borderCollapse: String
    open var borderColor: String
    open var borderImage: String
    open var borderImageOutset: String
    open var borderImageRepeat: String
    open var borderImageSlice: String
    open var borderImageSource: String
    open var borderImageWidth: String
    open var borderLeft: String
    open var borderLeftColor: String
    open var borderLeftStyle: String
    open var borderLeftWidth: String
    open var borderRadius: String
    open var borderRight: String
    open var borderRightColor: String
    open var borderRightStyle: String
    open var borderRightWidth: String
    open var borderSpacing: String
    open var borderStyle: String
    open var borderTop: String
    open var borderTopColor: String
    open var borderTopLeftRadius: String
    open var borderTopRightRadius: String
    open var borderTopStyle: String
    open var borderTopWidth: String
    open var borderWidth: String
    open var bottom: String
    open var boxDecorationBreak: String
    open var boxShadow: String
    open var boxSizing: String
    open var breakAfter: String
    open var breakBefore: String
    open var breakInside: String
    open var captionSide: String
    open var clear: String
    open var clip: String
    open var color: String
    open var columnCount: String
    open var columnFill: String
    open var columnGap: String
    open var columnRule: String
    open var columnRuleColor: String
    open var columnRuleStyle: String
    open var columnRuleWidth: String
    open var columnSpan: String
    open var columnWidth: String
    open var columns: String
    open var content: String
    open var counterIncrement: String
    open var counterReset: String
    open var cursor: String
    open var direction: String
    open var display: String
    open var emptyCells: String
    open var filter: String
    open var flex: String
    open var flexBasis: String
    open var flexDirection: String
    open var flexFlow: String
    open var flexGrow: String
    open var flexShrink: String
    open var flexWrap: String
    open var font: String
    open var fontFamily: String
    open var fontFeatureSettings: String
    open var fontKerning: String
    open var fontLanguageOverride: String
    open var fontSize: String
    open var fontSizeAdjust: String
    open var fontStretch: String
    open var fontStyle: String
    open var fontSynthesis: String
    open var fontVariant: String
    open var fontVariantAlternates: String
    open var fontVariantCaps: String
    open var fontVariantEastAsian: String
    open var fontVariantLigatures: String
    open var fontVariantNumeric: String
    open var fontVariantPosition: String
    open var fontWeight: String
    open var hangingPunctuation: String
    open var height: String
    open var hyphens: String
    open var imageOrientation: String
    open var imageRendering: String
    open var imageResolution: String
    open var imeMode: String
    open var justifyContent: String
    open var left: String
    open var letterSpacing: String
    open var lineBreak: String
    open var lineHeight: String
    open var listStyle: String
    open var listStyleImage: String
    open var listStylePosition: String
    open var listStyleType: String
    open var margin: String
    open var marginBottom: String
    open var marginLeft: String
    open var marginRight: String
    open var marginTop: String
    open var mark: String
    open var markAfter: String
    open var markBefore: String
    open var marks: String
    open var marqueeDirection: String
    open var marqueePlayCount: String
    open var marqueeSpeed: String
    open var marqueeStyle: String
    open var mask: String
    open var maskType: String
    open var maxHeight: String
    open var maxWidth: String
    open var minHeight: String
    open var minWidth: String
    open var navDown: String
    open var navIndex: String
    open var navLeft: String
    open var navRight: String
    open var navUp: String
    open var objectFit: String
    open var objectPosition: String
    open var opacity: String
    open var order: String
    open var orphans: String
    open var outline: String
    open var outlineColor: String
    open var outlineOffset: String
    open var outlineStyle: String
    open var outlineWidth: String
    open var overflowWrap: String
    open var overflowX: String
    open var overflowY: String
    open var padding: String
    open var paddingBottom: String
    open var paddingLeft: String
    open var paddingRight: String
    open var paddingTop: String
    open var pageBreakAfter: String
    open var pageBreakBefore: String
    open var pageBreakInside: String
    open var perspective: String
    open var perspectiveOrigin: String
    open var phonemes: String
    open var position: String
    open var quotes: String
    open var resize: String
    open var rest: String
    open var restAfter: String
    open var restBefore: String
    open var right: String
    open var tabSize: String
    open var tableLayout: String
    open var textAlign: String
    open var textAlignLast: String
    open var textCombineUpright: String
    open var textDecoration: String
    open var textDecorationColor: String
    open var textDecorationLine: String
    open var textDecorationStyle: String
    open var textIndent: String
    open var textJustify: String
    open var textOrientation: String
    open var textOverflow: String
    open var textShadow: String
    open var textTransform: String
    open var textUnderlinePosition: String
    open var top: String
    open var transform: String
    open var transformOrigin: String
    open var transformStyle: String
    open var transition: String
    open var transitionDelay: String
    open var transitionDuration: String
    open var transitionProperty: String
    open var transitionTimingFunction: String
    open var unicodeBidi: String
    open var verticalAlign: String
    open var visibility: String
    open var voiceBalance: String
    open var voiceDuration: String
    open var voicePitch: String
    open var voicePitchRange: String
    open var voiceRate: String
    open var voiceStress: String
    open var voiceVolume: String
    open var whiteSpace: String
    open var widows: String
    open var width: String
    open var wordBreak: String
    open var wordSpacing: String
    open var wordWrap: String
    open var writingMode: String
    open var zIndex: String
    override fun item(index: Int): String
    fun getPropertyValue(property: String): String
    fun getPropertyPriority(property: String): String
    fun setProperty(property: String, value: String, priority: String = definedExternally): Unit
    fun setPropertyValue(property: String, value: String): Unit
    fun setPropertyPriority(property: String, priority: String): Unit
    fun removeProperty(property: String): String
}
@kotlin.internal.InlineOnly inline operator fun CSSStyleDeclaration.get(index: Int): String? = asDynamic()[index]

public external abstract class MediaList : ItemArrayLike<String> {
    open var mediaText: String
    override val length: Int
    override fun item(index: Int): String?
    fun appendMedium(medium: String): Unit
    fun deleteMedium(medium: String): Unit
}
@kotlin.internal.InlineOnly inline operator fun MediaList.get(index: Int): String? = asDynamic()[index]

/**
 * Exposes the JavaScript [StyleSheet](https://developer.mozilla.org/en/docs/Web/API/StyleSheet) to Kotlin
 */
public external abstract class StyleSheet {
    open val type: String
    open val href: String?
    open val ownerNode: UnionElementOrProcessingInstruction?
    open val parentStyleSheet: StyleSheet?
    open val title: String?
    open val media: MediaList
    open var disabled: Boolean
}

/**
 * Exposes the JavaScript [CSSStyleSheet](https://developer.mozilla.org/en/docs/Web/API/CSSStyleSheet) to Kotlin
 */
public external abstract class CSSStyleSheet : StyleSheet {
    open val ownerRule: CSSRule?
    open val cssRules: CSSRuleList
    fun insertRule(rule: String, index: Int): Int
    fun deleteRule(index: Int): Unit
}

/**
 * Exposes the JavaScript [StyleSheetList](https://developer.mozilla.org/en/docs/Web/API/StyleSheetList) to Kotlin
 */
public external abstract class StyleSheetList : ItemArrayLike<StyleSheet> {
    override val length: Int
    override fun item(index: Int): StyleSheet?
}
@kotlin.internal.InlineOnly inline operator fun StyleSheetList.get(index: Int): StyleSheet? = asDynamic()[index]

/**
 * Exposes the JavaScript [LinkStyle](https://developer.mozilla.org/en/docs/Web/API/LinkStyle) to Kotlin
 */
public external interface LinkStyle {
    val sheet: StyleSheet?
}

/**
 * Exposes the JavaScript [CSSRuleList](https://developer.mozilla.org/en/docs/Web/API/CSSRuleList) to Kotlin
 */
public external abstract class CSSRuleList : ItemArrayLike<CSSRule> {
    override val length: Int
    override fun item(index: Int): CSSRule?
}
@kotlin.internal.InlineOnly inline operator fun CSSRuleList.get(index: Int): CSSRule? = asDynamic()[index]

/**
 * Exposes the JavaScript [CSSRule](https://developer.mozilla.org/en/docs/Web/API/CSSRule) to Kotlin
 */
public external abstract class CSSRule {
    open val type: Short
    open var cssText: String
    open val parentRule: CSSRule?
    open val parentStyleSheet: CSSStyleSheet?

    companion object {
        val STYLE_RULE: Short
        val CHARSET_RULE: Short
        val IMPORT_RULE: Short
        val MEDIA_RULE: Short
        val FONT_FACE_RULE: Short
        val PAGE_RULE: Short
        val MARGIN_RULE: Short
        val NAMESPACE_RULE: Short
    }
}

/**
 * Exposes the JavaScript [CSSStyleRule](https://developer.mozilla.org/en/docs/Web/API/CSSStyleRule) to Kotlin
 */
public external abstract class CSSStyleRule : CSSRule {
    open var selectorText: String
    open val style: CSSStyleDeclaration
}

public external abstract class CSSImportRule : CSSRule {
    open val href: String
    open val media: MediaList
    open val styleSheet: CSSStyleSheet
}

/**
 * Exposes the JavaScript [CSSGroupingRule](https://developer.mozilla.org/en/docs/Web/API/CSSGroupingRule) to Kotlin
 */
public external abstract class CSSGroupingRule : CSSRule {
    open val cssRules: CSSRuleList
    fun insertRule(rule: String, index: Int): Int
    fun deleteRule(index: Int): Unit
}

/**
 * Exposes the JavaScript [CSSMediaRule](https://developer.mozilla.org/en/docs/Web/API/CSSMediaRule) to Kotlin
 */
public external abstract class CSSMediaRule : CSSGroupingRule {
    open val media: MediaList
}

/**
 * Exposes the JavaScript [CSSPageRule](https://developer.mozilla.org/en/docs/Web/API/CSSPageRule) to Kotlin
 */
public external abstract class CSSPageRule : CSSGroupingRule {
    open var selectorText: String
    open val style: CSSStyleDeclaration
}

public external abstract class CSSMarginRule : CSSRule {
    open val name: String
    open val style: CSSStyleDeclaration
}

/**
 * Exposes the JavaScript [CSSNamespaceRule](https://developer.mozilla.org/en/docs/Web/API/CSSNamespaceRule) to Kotlin
 */
public external abstract class CSSNamespaceRule : CSSRule {
    open val namespaceURI: String
    open val prefix: String
}

public external interface ElementCSSInlineStyle {
    val style: CSSStyleDeclaration
}

/**
 * Exposes the JavaScript [CSS](https://developer.mozilla.org/en/docs/Web/API/CSS) to Kotlin
 */
public external abstract class CSS {

    companion object {
        fun escape(ident: String): String
    }
}

