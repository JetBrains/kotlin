/*
 * Generated file
 * DO NOT EDIT
 * 
 * See libraries/tools/idl2k for details
 */

package org.w3c.dom.css

import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.dom.events.*
import org.w3c.dom.parsing.*
import org.w3c.dom.svg.*
import org.w3c.fetch.*
import org.w3c.files.*
import org.w3c.notifications.*
import org.w3c.performance.*
import org.w3c.workers.*
import org.w3c.xhr.*

native public trait MediaList {
    var mediaText: String
        get() = noImpl
        set(value) = noImpl
    var length: Int
        get() = noImpl
        set(value) = noImpl
    fun item(index: Int): String? = noImpl
    nativeGetter fun get(index: Int): String? = noImpl
    fun appendMedium(medium: String): Unit = noImpl
    fun deleteMedium(medium: String): Unit = noImpl
}

native public trait StyleSheet {
    var type: String
        get() = noImpl
        set(value) = noImpl
    var href: String?
        get() = noImpl
        set(value) = noImpl
    var ownerNode: UnionElementOrProcessingInstruction?
        get() = noImpl
        set(value) = noImpl
    var parentStyleSheet: StyleSheet?
        get() = noImpl
        set(value) = noImpl
    var title: String?
        get() = noImpl
        set(value) = noImpl
    var media: MediaList
        get() = noImpl
        set(value) = noImpl
    var disabled: Boolean
        get() = noImpl
        set(value) = noImpl
}

native public trait CSSStyleSheet : StyleSheet {
    var ownerRule: CSSRule?
        get() = noImpl
        set(value) = noImpl
    var cssRules: CSSRuleList
        get() = noImpl
        set(value) = noImpl
    fun insertRule(rule: String, index: Int): Int = noImpl
    fun deleteRule(index: Int): Unit = noImpl
}

native public trait StyleSheetList {
    var length: Int
        get() = noImpl
        set(value) = noImpl
    fun item(index: Int): StyleSheet? = noImpl
    nativeGetter fun get(index: Int): StyleSheet? = noImpl
}

native public trait CSSRuleList {
    var length: Int
        get() = noImpl
        set(value) = noImpl
    fun item(index: Int): CSSRule? = noImpl
    nativeGetter fun get(index: Int): CSSRule? = noImpl
}

native public trait CSSRule {
    var type: Short
        get() = noImpl
        set(value) = noImpl
    var cssText: String
        get() = noImpl
        set(value) = noImpl
    var parentRule: CSSRule?
        get() = noImpl
        set(value) = noImpl
    var parentStyleSheet: CSSStyleSheet?
        get() = noImpl
        set(value) = noImpl

    companion object {
        val STYLE_RULE: Short = 1
        val CHARSET_RULE: Short = 2
        val IMPORT_RULE: Short = 3
        val MEDIA_RULE: Short = 4
        val FONT_FACE_RULE: Short = 5
        val PAGE_RULE: Short = 6
        val MARGIN_RULE: Short = 9
        val NAMESPACE_RULE: Short = 10
    }
}

native public trait CSSStyleRule : CSSRule {
    var selectorText: String
        get() = noImpl
        set(value) = noImpl
    var style: CSSStyleDeclaration
        get() = noImpl
        set(value) = noImpl
}

native public trait CSSImportRule : CSSRule {
    var href: String
        get() = noImpl
        set(value) = noImpl
    var media: MediaList
        get() = noImpl
        set(value) = noImpl
    var styleSheet: CSSStyleSheet
        get() = noImpl
        set(value) = noImpl
}

native public trait CSSGroupingRule : CSSRule {
    var cssRules: CSSRuleList
        get() = noImpl
        set(value) = noImpl
    fun insertRule(rule: String, index: Int): Int = noImpl
    fun deleteRule(index: Int): Unit = noImpl
}

native public trait CSSMediaRule : CSSGroupingRule {
    var media: MediaList
        get() = noImpl
        set(value) = noImpl
}

native public trait CSSPageRule : CSSGroupingRule {
    var selectorText: String
        get() = noImpl
        set(value) = noImpl
    var style: CSSStyleDeclaration
        get() = noImpl
        set(value) = noImpl
}

native public trait CSSMarginRule : CSSRule {
    var name: String
        get() = noImpl
        set(value) = noImpl
    var style: CSSStyleDeclaration
        get() = noImpl
        set(value) = noImpl
}

native public trait CSSNamespaceRule : CSSRule {
    var namespaceURI: String
        get() = noImpl
        set(value) = noImpl
    var prefix: String
        get() = noImpl
        set(value) = noImpl
}

native public trait CSSStyleDeclaration {
    var alignContent: String
        get() = noImpl
        set(value) = noImpl
    var alignItems: String
        get() = noImpl
        set(value) = noImpl
    var alignSelf: String
        get() = noImpl
        set(value) = noImpl
    var animation: String
        get() = noImpl
        set(value) = noImpl
    var animationDelay: String
        get() = noImpl
        set(value) = noImpl
    var animationDirection: String
        get() = noImpl
        set(value) = noImpl
    var animationDuration: String
        get() = noImpl
        set(value) = noImpl
    var animationFillMode: String
        get() = noImpl
        set(value) = noImpl
    var animationIterationCount: String
        get() = noImpl
        set(value) = noImpl
    var animationName: String
        get() = noImpl
        set(value) = noImpl
    var animationPlayState: String
        get() = noImpl
        set(value) = noImpl
    var animationTimingFunction: String
        get() = noImpl
        set(value) = noImpl
    var backfaceVisibility: String
        get() = noImpl
        set(value) = noImpl
    var background: String
        get() = noImpl
        set(value) = noImpl
    var backgroundAttachment: String
        get() = noImpl
        set(value) = noImpl
    var backgroundClip: String
        get() = noImpl
        set(value) = noImpl
    var backgroundColor: String
        get() = noImpl
        set(value) = noImpl
    var backgroundImage: String
        get() = noImpl
        set(value) = noImpl
    var backgroundOrigin: String
        get() = noImpl
        set(value) = noImpl
    var backgroundPosition: String
        get() = noImpl
        set(value) = noImpl
    var backgroundRepeat: String
        get() = noImpl
        set(value) = noImpl
    var backgroundSize: String
        get() = noImpl
        set(value) = noImpl
    var border: String
        get() = noImpl
        set(value) = noImpl
    var borderBottom: String
        get() = noImpl
        set(value) = noImpl
    var borderBottomColor: String
        get() = noImpl
        set(value) = noImpl
    var borderBottomLeftRadius: String
        get() = noImpl
        set(value) = noImpl
    var borderBottomRightRadius: String
        get() = noImpl
        set(value) = noImpl
    var borderBottomStyle: String
        get() = noImpl
        set(value) = noImpl
    var borderBottomWidth: String
        get() = noImpl
        set(value) = noImpl
    var borderCollapse: String
        get() = noImpl
        set(value) = noImpl
    var borderColor: String
        get() = noImpl
        set(value) = noImpl
    var borderImage: String
        get() = noImpl
        set(value) = noImpl
    var borderImageOutset: String
        get() = noImpl
        set(value) = noImpl
    var borderImageRepeat: String
        get() = noImpl
        set(value) = noImpl
    var borderImageSlice: String
        get() = noImpl
        set(value) = noImpl
    var borderImageSource: String
        get() = noImpl
        set(value) = noImpl
    var borderImageWidth: String
        get() = noImpl
        set(value) = noImpl
    var borderLeft: String
        get() = noImpl
        set(value) = noImpl
    var borderLeftColor: String
        get() = noImpl
        set(value) = noImpl
    var borderLeftStyle: String
        get() = noImpl
        set(value) = noImpl
    var borderLeftWidth: String
        get() = noImpl
        set(value) = noImpl
    var borderRadius: String
        get() = noImpl
        set(value) = noImpl
    var borderRight: String
        get() = noImpl
        set(value) = noImpl
    var borderRightColor: String
        get() = noImpl
        set(value) = noImpl
    var borderRightStyle: String
        get() = noImpl
        set(value) = noImpl
    var borderRightWidth: String
        get() = noImpl
        set(value) = noImpl
    var borderSpacing: String
        get() = noImpl
        set(value) = noImpl
    var borderStyle: String
        get() = noImpl
        set(value) = noImpl
    var borderTop: String
        get() = noImpl
        set(value) = noImpl
    var borderTopColor: String
        get() = noImpl
        set(value) = noImpl
    var borderTopLeftRadius: String
        get() = noImpl
        set(value) = noImpl
    var borderTopRightRadius: String
        get() = noImpl
        set(value) = noImpl
    var borderTopStyle: String
        get() = noImpl
        set(value) = noImpl
    var borderTopWidth: String
        get() = noImpl
        set(value) = noImpl
    var borderWidth: String
        get() = noImpl
        set(value) = noImpl
    var bottom: String
        get() = noImpl
        set(value) = noImpl
    var boxDecorationBreak: String
        get() = noImpl
        set(value) = noImpl
    var boxShadow: String
        get() = noImpl
        set(value) = noImpl
    var boxSizing: String
        get() = noImpl
        set(value) = noImpl
    var breakAfter: String
        get() = noImpl
        set(value) = noImpl
    var breakBefore: String
        get() = noImpl
        set(value) = noImpl
    var breakInside: String
        get() = noImpl
        set(value) = noImpl
    var captionSide: String
        get() = noImpl
        set(value) = noImpl
    var clear: String
        get() = noImpl
        set(value) = noImpl
    var clip: String
        get() = noImpl
        set(value) = noImpl
    var color: String
        get() = noImpl
        set(value) = noImpl
    var columnCount: String
        get() = noImpl
        set(value) = noImpl
    var columnFill: String
        get() = noImpl
        set(value) = noImpl
    var columnGap: String
        get() = noImpl
        set(value) = noImpl
    var columnRule: String
        get() = noImpl
        set(value) = noImpl
    var columnRuleColor: String
        get() = noImpl
        set(value) = noImpl
    var columnRuleStyle: String
        get() = noImpl
        set(value) = noImpl
    var columnRuleWidth: String
        get() = noImpl
        set(value) = noImpl
    var columnSpan: String
        get() = noImpl
        set(value) = noImpl
    var columnWidth: String
        get() = noImpl
        set(value) = noImpl
    var columns: String
        get() = noImpl
        set(value) = noImpl
    var content: String
        get() = noImpl
        set(value) = noImpl
    var counterIncrement: String
        get() = noImpl
        set(value) = noImpl
    var counterReset: String
        get() = noImpl
        set(value) = noImpl
    var cursor: String
        get() = noImpl
        set(value) = noImpl
    var direction: String
        get() = noImpl
        set(value) = noImpl
    var display: String
        get() = noImpl
        set(value) = noImpl
    var emptyCells: String
        get() = noImpl
        set(value) = noImpl
    var filter: String
        get() = noImpl
        set(value) = noImpl
    var flex: String
        get() = noImpl
        set(value) = noImpl
    var flexBasis: String
        get() = noImpl
        set(value) = noImpl
    var flexDirection: String
        get() = noImpl
        set(value) = noImpl
    var flexFlow: String
        get() = noImpl
        set(value) = noImpl
    var flexGrow: String
        get() = noImpl
        set(value) = noImpl
    var flexShrink: String
        get() = noImpl
        set(value) = noImpl
    var flexWrap: String
        get() = noImpl
        set(value) = noImpl
    var cssFloat: String
        get() = noImpl
        set(value) = noImpl
    var font: String
        get() = noImpl
        set(value) = noImpl
    var fontFamily: String
        get() = noImpl
        set(value) = noImpl
    var fontFeatureSettings: String
        get() = noImpl
        set(value) = noImpl
    var fontKerning: String
        get() = noImpl
        set(value) = noImpl
    var fontLanguageOverride: String
        get() = noImpl
        set(value) = noImpl
    var fontSize: String
        get() = noImpl
        set(value) = noImpl
    var fontSizeAdjust: String
        get() = noImpl
        set(value) = noImpl
    var fontStretch: String
        get() = noImpl
        set(value) = noImpl
    var fontStyle: String
        get() = noImpl
        set(value) = noImpl
    var fontSynthesis: String
        get() = noImpl
        set(value) = noImpl
    var fontVariant: String
        get() = noImpl
        set(value) = noImpl
    var fontVariantAlternates: String
        get() = noImpl
        set(value) = noImpl
    var fontVariantCaps: String
        get() = noImpl
        set(value) = noImpl
    var fontVariantEastAsian: String
        get() = noImpl
        set(value) = noImpl
    var fontVariantLigatures: String
        get() = noImpl
        set(value) = noImpl
    var fontVariantNumeric: String
        get() = noImpl
        set(value) = noImpl
    var fontVariantPosition: String
        get() = noImpl
        set(value) = noImpl
    var fontWeight: String
        get() = noImpl
        set(value) = noImpl
    var hangingPunctuation: String
        get() = noImpl
        set(value) = noImpl
    var height: String
        get() = noImpl
        set(value) = noImpl
    var hyphens: String
        get() = noImpl
        set(value) = noImpl
    var imageOrientation: String
        get() = noImpl
        set(value) = noImpl
    var imageRendering: String
        get() = noImpl
        set(value) = noImpl
    var imageResolution: String
        get() = noImpl
        set(value) = noImpl
    var imeMode: String
        get() = noImpl
        set(value) = noImpl
    var justifyContent: String
        get() = noImpl
        set(value) = noImpl
    var left: String
        get() = noImpl
        set(value) = noImpl
    var letterSpacing: String
        get() = noImpl
        set(value) = noImpl
    var lineBreak: String
        get() = noImpl
        set(value) = noImpl
    var lineHeight: String
        get() = noImpl
        set(value) = noImpl
    var listStyle: String
        get() = noImpl
        set(value) = noImpl
    var listStyleImage: String
        get() = noImpl
        set(value) = noImpl
    var listStylePosition: String
        get() = noImpl
        set(value) = noImpl
    var listStyleType: String
        get() = noImpl
        set(value) = noImpl
    var margin: String
        get() = noImpl
        set(value) = noImpl
    var marginBottom: String
        get() = noImpl
        set(value) = noImpl
    var marginLeft: String
        get() = noImpl
        set(value) = noImpl
    var marginRight: String
        get() = noImpl
        set(value) = noImpl
    var marginTop: String
        get() = noImpl
        set(value) = noImpl
    var mark: String
        get() = noImpl
        set(value) = noImpl
    var markAfter: String
        get() = noImpl
        set(value) = noImpl
    var markBefore: String
        get() = noImpl
        set(value) = noImpl
    var marks: String
        get() = noImpl
        set(value) = noImpl
    var marqueeDirection: String
        get() = noImpl
        set(value) = noImpl
    var marqueePlayCount: String
        get() = noImpl
        set(value) = noImpl
    var marqueeSpeed: String
        get() = noImpl
        set(value) = noImpl
    var marqueeStyle: String
        get() = noImpl
        set(value) = noImpl
    var mask: String
        get() = noImpl
        set(value) = noImpl
    var maskType: String
        get() = noImpl
        set(value) = noImpl
    var maxHeight: String
        get() = noImpl
        set(value) = noImpl
    var maxWidth: String
        get() = noImpl
        set(value) = noImpl
    var minHeight: String
        get() = noImpl
        set(value) = noImpl
    var minWidth: String
        get() = noImpl
        set(value) = noImpl
    var navDown: String
        get() = noImpl
        set(value) = noImpl
    var navIndex: String
        get() = noImpl
        set(value) = noImpl
    var navLeft: String
        get() = noImpl
        set(value) = noImpl
    var navRight: String
        get() = noImpl
        set(value) = noImpl
    var navUp: String
        get() = noImpl
        set(value) = noImpl
    var objectFit: String
        get() = noImpl
        set(value) = noImpl
    var objectPosition: String
        get() = noImpl
        set(value) = noImpl
    var opacity: String
        get() = noImpl
        set(value) = noImpl
    var order: String
        get() = noImpl
        set(value) = noImpl
    var orphans: String
        get() = noImpl
        set(value) = noImpl
    var outline: String
        get() = noImpl
        set(value) = noImpl
    var outlineColor: String
        get() = noImpl
        set(value) = noImpl
    var outlineOffset: String
        get() = noImpl
        set(value) = noImpl
    var outlineStyle: String
        get() = noImpl
        set(value) = noImpl
    var outlineWidth: String
        get() = noImpl
        set(value) = noImpl
    var overflowWrap: String
        get() = noImpl
        set(value) = noImpl
    var overflowX: String
        get() = noImpl
        set(value) = noImpl
    var overflowY: String
        get() = noImpl
        set(value) = noImpl
    var padding: String
        get() = noImpl
        set(value) = noImpl
    var paddingBottom: String
        get() = noImpl
        set(value) = noImpl
    var paddingLeft: String
        get() = noImpl
        set(value) = noImpl
    var paddingRight: String
        get() = noImpl
        set(value) = noImpl
    var paddingTop: String
        get() = noImpl
        set(value) = noImpl
    var pageBreakAfter: String
        get() = noImpl
        set(value) = noImpl
    var pageBreakBefore: String
        get() = noImpl
        set(value) = noImpl
    var pageBreakInside: String
        get() = noImpl
        set(value) = noImpl
    var perspective: String
        get() = noImpl
        set(value) = noImpl
    var perspectiveOrigin: String
        get() = noImpl
        set(value) = noImpl
    var phonemes: String
        get() = noImpl
        set(value) = noImpl
    var position: String
        get() = noImpl
        set(value) = noImpl
    var quotes: String
        get() = noImpl
        set(value) = noImpl
    var resize: String
        get() = noImpl
        set(value) = noImpl
    var rest: String
        get() = noImpl
        set(value) = noImpl
    var restAfter: String
        get() = noImpl
        set(value) = noImpl
    var restBefore: String
        get() = noImpl
        set(value) = noImpl
    var right: String
        get() = noImpl
        set(value) = noImpl
    var tabSize: String
        get() = noImpl
        set(value) = noImpl
    var tableLayout: String
        get() = noImpl
        set(value) = noImpl
    var textAlign: String
        get() = noImpl
        set(value) = noImpl
    var textAlignLast: String
        get() = noImpl
        set(value) = noImpl
    var textCombineUpright: String
        get() = noImpl
        set(value) = noImpl
    var textDecoration: String
        get() = noImpl
        set(value) = noImpl
    var textDecorationColor: String
        get() = noImpl
        set(value) = noImpl
    var textDecorationLine: String
        get() = noImpl
        set(value) = noImpl
    var textDecorationStyle: String
        get() = noImpl
        set(value) = noImpl
    var textIndent: String
        get() = noImpl
        set(value) = noImpl
    var textJustify: String
        get() = noImpl
        set(value) = noImpl
    var textOrientation: String
        get() = noImpl
        set(value) = noImpl
    var textOverflow: String
        get() = noImpl
        set(value) = noImpl
    var textShadow: String
        get() = noImpl
        set(value) = noImpl
    var textTransform: String
        get() = noImpl
        set(value) = noImpl
    var textUnderlinePosition: String
        get() = noImpl
        set(value) = noImpl
    var top: String
        get() = noImpl
        set(value) = noImpl
    var transform: String
        get() = noImpl
        set(value) = noImpl
    var transformOrigin: String
        get() = noImpl
        set(value) = noImpl
    var transformStyle: String
        get() = noImpl
        set(value) = noImpl
    var transition: String
        get() = noImpl
        set(value) = noImpl
    var transitionDelay: String
        get() = noImpl
        set(value) = noImpl
    var transitionDuration: String
        get() = noImpl
        set(value) = noImpl
    var transitionProperty: String
        get() = noImpl
        set(value) = noImpl
    var transitionTimingFunction: String
        get() = noImpl
        set(value) = noImpl
    var unicodeBidi: String
        get() = noImpl
        set(value) = noImpl
    var verticalAlign: String
        get() = noImpl
        set(value) = noImpl
    var visibility: String
        get() = noImpl
        set(value) = noImpl
    var voiceBalance: String
        get() = noImpl
        set(value) = noImpl
    var voiceDuration: String
        get() = noImpl
        set(value) = noImpl
    var voicePitch: String
        get() = noImpl
        set(value) = noImpl
    var voicePitchRange: String
        get() = noImpl
        set(value) = noImpl
    var voiceRate: String
        get() = noImpl
        set(value) = noImpl
    var voiceStress: String
        get() = noImpl
        set(value) = noImpl
    var voiceVolume: String
        get() = noImpl
        set(value) = noImpl
    var whiteSpace: String
        get() = noImpl
        set(value) = noImpl
    var widows: String
        get() = noImpl
        set(value) = noImpl
    var width: String
        get() = noImpl
        set(value) = noImpl
    var wordBreak: String
        get() = noImpl
        set(value) = noImpl
    var wordSpacing: String
        get() = noImpl
        set(value) = noImpl
    var wordWrap: String
        get() = noImpl
        set(value) = noImpl
    var writingMode: String
        get() = noImpl
        set(value) = noImpl
    var zIndex: String
        get() = noImpl
        set(value) = noImpl
    var cssText: String
        get() = noImpl
        set(value) = noImpl
    var length: Int
        get() = noImpl
        set(value) = noImpl
    var parentRule: CSSRule?
        get() = noImpl
        set(value) = noImpl
    var _dashed_attribute: String
        get() = noImpl
        set(value) = noImpl
    var _camel_cased_attribute: String
        get() = noImpl
        set(value) = noImpl
    fun item(index: Int): String = noImpl
    nativeGetter fun get(index: Int): String? = noImpl
    fun getPropertyValue(property: String): String = noImpl
    fun getPropertyPriority(property: String): String = noImpl
    fun setProperty(property: String, value: String, priority: String = ""): Unit = noImpl
    fun setPropertyValue(property: String, value: String): Unit = noImpl
    fun setPropertyPriority(property: String, priority: String): Unit = noImpl
    fun removeProperty(property: String): String = noImpl
}

native public trait PseudoElement {
    var cascadedStyle: CSSStyleDeclaration
        get() = noImpl
        set(value) = noImpl
    var defaultStyle: CSSStyleDeclaration
        get() = noImpl
        set(value) = noImpl
    var rawComputedStyle: CSSStyleDeclaration
        get() = noImpl
        set(value) = noImpl
    var usedStyle: CSSStyleDeclaration
        get() = noImpl
        set(value) = noImpl
}

native public trait CSS {
}

