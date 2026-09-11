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

package org.w3c.dom.css

import kotlinx.browser.PLEASE_USE_KOTLINX_BROWSER_INSTEAD
import kotlin.js.*
import org.khronos.webgl.*
import org.w3c.dom.*
import kotlin.internal.InlineOnly

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class MediaList : ItemArrayLike<String> {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var mediaText: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun appendMedium(medium: String)

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun deleteMedium(medium: String)

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    override fun item(index: Int): String?
}

@InlineOnly
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public inline operator fun MediaList.get(index: Int): String? = asDynamic()[index]

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class StyleSheet {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val type: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val href: String?

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val ownerNode: UnionElementOrProcessingInstruction?

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val parentStyleSheet: StyleSheet?

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val title: String?

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val media: MediaList

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var disabled: Boolean
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class CSSStyleSheet : StyleSheet {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val ownerRule: CSSRule?

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val cssRules: CSSRuleList

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun insertRule(rule: String, index: Int): Int

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun deleteRule(index: Int)
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class StyleSheetList : ItemArrayLike<StyleSheet> {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    override fun item(index: Int): StyleSheet?
}

@InlineOnly
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public inline operator fun StyleSheetList.get(index: Int): StyleSheet? = asDynamic()[index]

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public external interface LinkStyle {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    val sheet: StyleSheet?
        get() = definedExternally
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class CSSRuleList : ItemArrayLike<CSSRule> {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    override fun item(index: Int): CSSRule?
}

@InlineOnly
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public inline operator fun CSSRuleList.get(index: Int): CSSRule? = asDynamic()[index]

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class CSSRule {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val type: Short

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var cssText: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val parentRule: CSSRule?

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val parentStyleSheet: CSSStyleSheet?

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val STYLE_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CHARSET_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val IMPORT_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val MEDIA_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val FONT_FACE_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PAGE_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val MARGIN_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NAMESPACE_RULE: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class CSSStyleRule : CSSRule {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var selectorText: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val style: CSSStyleDeclaration

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val STYLE_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CHARSET_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val IMPORT_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val MEDIA_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val FONT_FACE_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PAGE_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val MARGIN_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NAMESPACE_RULE: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class CSSImportRule : CSSRule {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val href: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val media: MediaList

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val styleSheet: CSSStyleSheet

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val STYLE_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CHARSET_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val IMPORT_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val MEDIA_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val FONT_FACE_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PAGE_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val MARGIN_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NAMESPACE_RULE: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class CSSGroupingRule : CSSRule {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val cssRules: CSSRuleList

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun insertRule(rule: String, index: Int): Int

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun deleteRule(index: Int)

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val STYLE_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CHARSET_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val IMPORT_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val MEDIA_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val FONT_FACE_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PAGE_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val MARGIN_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NAMESPACE_RULE: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class CSSMediaRule : CSSGroupingRule {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val media: MediaList

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val STYLE_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CHARSET_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val IMPORT_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val MEDIA_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val FONT_FACE_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PAGE_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val MARGIN_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NAMESPACE_RULE: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class CSSPageRule : CSSGroupingRule {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var selectorText: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val style: CSSStyleDeclaration

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val STYLE_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CHARSET_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val IMPORT_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val MEDIA_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val FONT_FACE_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PAGE_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val MARGIN_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NAMESPACE_RULE: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class CSSMarginRule : CSSRule {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val name: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val style: CSSStyleDeclaration

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val STYLE_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CHARSET_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val IMPORT_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val MEDIA_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val FONT_FACE_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PAGE_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val MARGIN_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NAMESPACE_RULE: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class CSSNamespaceRule : CSSRule {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val namespaceURI: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val prefix: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val STYLE_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CHARSET_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val IMPORT_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val MEDIA_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val FONT_FACE_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val PAGE_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val MARGIN_RULE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NAMESPACE_RULE: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class CSSStyleDeclaration : ItemArrayLike<String> {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var cssText: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val parentRule: CSSRule?

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var cssFloat: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var alignContent: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var alignItems: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var alignSelf: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var animation: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var animationDelay: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var animationDirection: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var animationDuration: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var animationFillMode: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var animationIterationCount: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var animationName: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var animationPlayState: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var animationTimingFunction: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var backfaceVisibility: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var background: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var backgroundAttachment: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var backgroundClip: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var backgroundColor: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var backgroundImage: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var backgroundOrigin: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var backgroundPosition: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var backgroundRepeat: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var backgroundSize: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var border: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var borderBottom: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var borderBottomColor: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var borderBottomLeftRadius: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var borderBottomRightRadius: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var borderBottomStyle: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var borderBottomWidth: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var borderCollapse: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var borderColor: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var borderImage: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var borderImageOutset: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var borderImageRepeat: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var borderImageSlice: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var borderImageSource: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var borderImageWidth: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var borderLeft: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var borderLeftColor: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var borderLeftStyle: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var borderLeftWidth: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var borderRadius: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var borderRight: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var borderRightColor: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var borderRightStyle: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var borderRightWidth: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var borderSpacing: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var borderStyle: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var borderTop: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var borderTopColor: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var borderTopLeftRadius: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var borderTopRightRadius: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var borderTopStyle: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var borderTopWidth: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var borderWidth: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var bottom: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var boxDecorationBreak: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var boxShadow: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var boxSizing: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var breakAfter: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var breakBefore: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var breakInside: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var captionSide: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var clear: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var clip: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var color: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var columnCount: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var columnFill: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var columnGap: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var columnRule: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var columnRuleColor: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var columnRuleStyle: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var columnRuleWidth: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var columnSpan: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var columnWidth: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var columns: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var content: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var counterIncrement: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var counterReset: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var cursor: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var direction: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var display: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var emptyCells: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var filter: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var flex: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var flexBasis: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var flexDirection: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var flexFlow: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var flexGrow: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var flexShrink: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var flexWrap: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var font: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var fontFamily: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var fontFeatureSettings: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var fontKerning: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var fontLanguageOverride: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var fontSize: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var fontSizeAdjust: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var fontStretch: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var fontStyle: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var fontSynthesis: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var fontVariant: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var fontVariantAlternates: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var fontVariantCaps: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var fontVariantEastAsian: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var fontVariantLigatures: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var fontVariantNumeric: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var fontVariantPosition: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var fontWeight: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var hangingPunctuation: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var height: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var hyphens: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var imageOrientation: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var imageRendering: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var imageResolution: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var imeMode: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var justifyContent: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var left: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var letterSpacing: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var lineBreak: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var lineHeight: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var listStyle: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var listStyleImage: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var listStylePosition: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var listStyleType: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var margin: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var marginBottom: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var marginLeft: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var marginRight: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var marginTop: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var mark: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var markAfter: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var markBefore: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var marks: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var marqueeDirection: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var marqueePlayCount: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var marqueeSpeed: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var marqueeStyle: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var mask: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var maskType: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var maxHeight: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var maxWidth: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var minHeight: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var minWidth: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var navDown: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var navIndex: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var navLeft: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var navRight: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var navUp: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var objectFit: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var objectPosition: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var opacity: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var order: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var orphans: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var outline: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var outlineColor: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var outlineOffset: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var outlineStyle: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var outlineWidth: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var overflowWrap: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var overflowX: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var overflowY: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var padding: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var paddingBottom: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var paddingLeft: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var paddingRight: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var paddingTop: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var pageBreakAfter: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var pageBreakBefore: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var pageBreakInside: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var perspective: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var perspectiveOrigin: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var phonemes: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var position: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var quotes: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var resize: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var rest: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var restAfter: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var restBefore: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var right: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var tabSize: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var tableLayout: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var textAlign: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var textAlignLast: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var textCombineUpright: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var textDecoration: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var textDecorationColor: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var textDecorationLine: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var textDecorationStyle: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var textIndent: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var textJustify: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var textOrientation: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var textOverflow: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var textShadow: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var textTransform: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var textUnderlinePosition: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var top: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var transform: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var transformOrigin: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var transformStyle: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var transition: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var transitionDelay: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var transitionDuration: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var transitionProperty: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var transitionTimingFunction: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var unicodeBidi: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var verticalAlign: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var visibility: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var voiceBalance: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var voiceDuration: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var voicePitch: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var voicePitchRange: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var voiceRate: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var voiceStress: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var voiceVolume: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var whiteSpace: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var widows: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var width: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var wordBreak: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var wordSpacing: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var wordWrap: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var writingMode: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var zIndex: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var _dashed_attribute: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var _camel_cased_attribute: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var _webkit_cased_attribute: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun getPropertyValue(property: String): String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun getPropertyPriority(property: String): String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun setProperty(property: String, value: String, priority: String = definedExternally)

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun setPropertyValue(property: String, value: String)

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun setPropertyPriority(property: String, priority: String)

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun removeProperty(property: String): String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    override fun item(index: Int): String
}

@InlineOnly
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public inline operator fun CSSStyleDeclaration.get(index: Int): String? = asDynamic()[index]

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public external interface ElementCSSInlineStyle {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    val style: CSSStyleDeclaration
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class CSS {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        fun escape(ident: String): String
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public external interface UnionElementOrProcessingInstruction
