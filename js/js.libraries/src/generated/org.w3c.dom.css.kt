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
    var cssText: String
        get() = noImpl
        set(value) = noImpl
    var length: Int
        get() = noImpl
        set(value) = noImpl
    var parentRule: CSSRule?
        get() = noImpl
        set(value) = noImpl
    var cssFloat: String
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

