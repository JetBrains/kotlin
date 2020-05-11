package org.w3c.dom.css

@kotlin.internal.InlineOnly public inline operator fun org.w3c.dom.css.CSSRuleList.get(/*0*/ index: kotlin.Int): org.w3c.dom.css.CSSRule?
@kotlin.internal.InlineOnly public inline operator fun org.w3c.dom.css.CSSStyleDeclaration.get(/*0*/ index: kotlin.Int): kotlin.String?
@kotlin.internal.InlineOnly public inline operator fun org.w3c.dom.css.MediaList.get(/*0*/ index: kotlin.Int): kotlin.String?
@kotlin.internal.InlineOnly public inline operator fun org.w3c.dom.css.StyleSheetList.get(/*0*/ index: kotlin.Int): org.w3c.dom.css.StyleSheet?

public abstract external class CSS {
    /*primary*/ public constructor CSS()

    public companion object Companion {
        public final fun escape(/*0*/ ident: kotlin.String): kotlin.String
    }
}

public abstract external class CSSGroupingRule : org.w3c.dom.css.CSSRule {
    /*primary*/ public constructor CSSGroupingRule()
    public open val cssRules: org.w3c.dom.css.CSSRuleList
        public open fun <get-cssRules>(): org.w3c.dom.css.CSSRuleList
    public final fun deleteRule(/*0*/ index: kotlin.Int): kotlin.Unit
    public final fun insertRule(/*0*/ rule: kotlin.String, /*1*/ index: kotlin.Int): kotlin.Int

    public companion object Companion {
        public final val CHARSET_RULE: kotlin.Short
            public final fun <get-CHARSET_RULE>(): kotlin.Short
        public final val FONT_FACE_RULE: kotlin.Short
            public final fun <get-FONT_FACE_RULE>(): kotlin.Short
        public final val IMPORT_RULE: kotlin.Short
            public final fun <get-IMPORT_RULE>(): kotlin.Short
        public final val MARGIN_RULE: kotlin.Short
            public final fun <get-MARGIN_RULE>(): kotlin.Short
        public final val MEDIA_RULE: kotlin.Short
            public final fun <get-MEDIA_RULE>(): kotlin.Short
        public final val NAMESPACE_RULE: kotlin.Short
            public final fun <get-NAMESPACE_RULE>(): kotlin.Short
        public final val PAGE_RULE: kotlin.Short
            public final fun <get-PAGE_RULE>(): kotlin.Short
        public final val STYLE_RULE: kotlin.Short
            public final fun <get-STYLE_RULE>(): kotlin.Short
    }
}

public abstract external class CSSImportRule : org.w3c.dom.css.CSSRule {
    /*primary*/ public constructor CSSImportRule()
    public open val href: kotlin.String
        public open fun <get-href>(): kotlin.String
    public open val media: org.w3c.dom.css.MediaList
        public open fun <get-media>(): org.w3c.dom.css.MediaList
    public open val styleSheet: org.w3c.dom.css.CSSStyleSheet
        public open fun <get-styleSheet>(): org.w3c.dom.css.CSSStyleSheet

    public companion object Companion {
        public final val CHARSET_RULE: kotlin.Short
            public final fun <get-CHARSET_RULE>(): kotlin.Short
        public final val FONT_FACE_RULE: kotlin.Short
            public final fun <get-FONT_FACE_RULE>(): kotlin.Short
        public final val IMPORT_RULE: kotlin.Short
            public final fun <get-IMPORT_RULE>(): kotlin.Short
        public final val MARGIN_RULE: kotlin.Short
            public final fun <get-MARGIN_RULE>(): kotlin.Short
        public final val MEDIA_RULE: kotlin.Short
            public final fun <get-MEDIA_RULE>(): kotlin.Short
        public final val NAMESPACE_RULE: kotlin.Short
            public final fun <get-NAMESPACE_RULE>(): kotlin.Short
        public final val PAGE_RULE: kotlin.Short
            public final fun <get-PAGE_RULE>(): kotlin.Short
        public final val STYLE_RULE: kotlin.Short
            public final fun <get-STYLE_RULE>(): kotlin.Short
    }
}

public abstract external class CSSMarginRule : org.w3c.dom.css.CSSRule {
    /*primary*/ public constructor CSSMarginRule()
    public open val name: kotlin.String
        public open fun <get-name>(): kotlin.String
    public open val style: org.w3c.dom.css.CSSStyleDeclaration
        public open fun <get-style>(): org.w3c.dom.css.CSSStyleDeclaration

    public companion object Companion {
        public final val CHARSET_RULE: kotlin.Short
            public final fun <get-CHARSET_RULE>(): kotlin.Short
        public final val FONT_FACE_RULE: kotlin.Short
            public final fun <get-FONT_FACE_RULE>(): kotlin.Short
        public final val IMPORT_RULE: kotlin.Short
            public final fun <get-IMPORT_RULE>(): kotlin.Short
        public final val MARGIN_RULE: kotlin.Short
            public final fun <get-MARGIN_RULE>(): kotlin.Short
        public final val MEDIA_RULE: kotlin.Short
            public final fun <get-MEDIA_RULE>(): kotlin.Short
        public final val NAMESPACE_RULE: kotlin.Short
            public final fun <get-NAMESPACE_RULE>(): kotlin.Short
        public final val PAGE_RULE: kotlin.Short
            public final fun <get-PAGE_RULE>(): kotlin.Short
        public final val STYLE_RULE: kotlin.Short
            public final fun <get-STYLE_RULE>(): kotlin.Short
    }
}

public abstract external class CSSMediaRule : org.w3c.dom.css.CSSGroupingRule {
    /*primary*/ public constructor CSSMediaRule()
    public open val media: org.w3c.dom.css.MediaList
        public open fun <get-media>(): org.w3c.dom.css.MediaList

    public companion object Companion {
        public final val CHARSET_RULE: kotlin.Short
            public final fun <get-CHARSET_RULE>(): kotlin.Short
        public final val FONT_FACE_RULE: kotlin.Short
            public final fun <get-FONT_FACE_RULE>(): kotlin.Short
        public final val IMPORT_RULE: kotlin.Short
            public final fun <get-IMPORT_RULE>(): kotlin.Short
        public final val MARGIN_RULE: kotlin.Short
            public final fun <get-MARGIN_RULE>(): kotlin.Short
        public final val MEDIA_RULE: kotlin.Short
            public final fun <get-MEDIA_RULE>(): kotlin.Short
        public final val NAMESPACE_RULE: kotlin.Short
            public final fun <get-NAMESPACE_RULE>(): kotlin.Short
        public final val PAGE_RULE: kotlin.Short
            public final fun <get-PAGE_RULE>(): kotlin.Short
        public final val STYLE_RULE: kotlin.Short
            public final fun <get-STYLE_RULE>(): kotlin.Short
    }
}

public abstract external class CSSNamespaceRule : org.w3c.dom.css.CSSRule {
    /*primary*/ public constructor CSSNamespaceRule()
    public open val namespaceURI: kotlin.String
        public open fun <get-namespaceURI>(): kotlin.String
    public open val prefix: kotlin.String
        public open fun <get-prefix>(): kotlin.String

    public companion object Companion {
        public final val CHARSET_RULE: kotlin.Short
            public final fun <get-CHARSET_RULE>(): kotlin.Short
        public final val FONT_FACE_RULE: kotlin.Short
            public final fun <get-FONT_FACE_RULE>(): kotlin.Short
        public final val IMPORT_RULE: kotlin.Short
            public final fun <get-IMPORT_RULE>(): kotlin.Short
        public final val MARGIN_RULE: kotlin.Short
            public final fun <get-MARGIN_RULE>(): kotlin.Short
        public final val MEDIA_RULE: kotlin.Short
            public final fun <get-MEDIA_RULE>(): kotlin.Short
        public final val NAMESPACE_RULE: kotlin.Short
            public final fun <get-NAMESPACE_RULE>(): kotlin.Short
        public final val PAGE_RULE: kotlin.Short
            public final fun <get-PAGE_RULE>(): kotlin.Short
        public final val STYLE_RULE: kotlin.Short
            public final fun <get-STYLE_RULE>(): kotlin.Short
    }
}

public abstract external class CSSPageRule : org.w3c.dom.css.CSSGroupingRule {
    /*primary*/ public constructor CSSPageRule()
    public open var selectorText: kotlin.String
        public open fun <get-selectorText>(): kotlin.String
        public open fun <set-selectorText>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open val style: org.w3c.dom.css.CSSStyleDeclaration
        public open fun <get-style>(): org.w3c.dom.css.CSSStyleDeclaration

    public companion object Companion {
        public final val CHARSET_RULE: kotlin.Short
            public final fun <get-CHARSET_RULE>(): kotlin.Short
        public final val FONT_FACE_RULE: kotlin.Short
            public final fun <get-FONT_FACE_RULE>(): kotlin.Short
        public final val IMPORT_RULE: kotlin.Short
            public final fun <get-IMPORT_RULE>(): kotlin.Short
        public final val MARGIN_RULE: kotlin.Short
            public final fun <get-MARGIN_RULE>(): kotlin.Short
        public final val MEDIA_RULE: kotlin.Short
            public final fun <get-MEDIA_RULE>(): kotlin.Short
        public final val NAMESPACE_RULE: kotlin.Short
            public final fun <get-NAMESPACE_RULE>(): kotlin.Short
        public final val PAGE_RULE: kotlin.Short
            public final fun <get-PAGE_RULE>(): kotlin.Short
        public final val STYLE_RULE: kotlin.Short
            public final fun <get-STYLE_RULE>(): kotlin.Short
    }
}

public abstract external class CSSRule {
    /*primary*/ public constructor CSSRule()
    public open var cssText: kotlin.String
        public open fun <get-cssText>(): kotlin.String
        public open fun <set-cssText>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open val parentRule: org.w3c.dom.css.CSSRule?
        public open fun <get-parentRule>(): org.w3c.dom.css.CSSRule?
    public open val parentStyleSheet: org.w3c.dom.css.CSSStyleSheet?
        public open fun <get-parentStyleSheet>(): org.w3c.dom.css.CSSStyleSheet?
    public open val type: kotlin.Short
        public open fun <get-type>(): kotlin.Short

    public companion object Companion {
        public final val CHARSET_RULE: kotlin.Short
            public final fun <get-CHARSET_RULE>(): kotlin.Short
        public final val FONT_FACE_RULE: kotlin.Short
            public final fun <get-FONT_FACE_RULE>(): kotlin.Short
        public final val IMPORT_RULE: kotlin.Short
            public final fun <get-IMPORT_RULE>(): kotlin.Short
        public final val MARGIN_RULE: kotlin.Short
            public final fun <get-MARGIN_RULE>(): kotlin.Short
        public final val MEDIA_RULE: kotlin.Short
            public final fun <get-MEDIA_RULE>(): kotlin.Short
        public final val NAMESPACE_RULE: kotlin.Short
            public final fun <get-NAMESPACE_RULE>(): kotlin.Short
        public final val PAGE_RULE: kotlin.Short
            public final fun <get-PAGE_RULE>(): kotlin.Short
        public final val STYLE_RULE: kotlin.Short
            public final fun <get-STYLE_RULE>(): kotlin.Short
    }
}

public abstract external class CSSRuleList : org.w3c.dom.ItemArrayLike<org.w3c.dom.css.CSSRule> {
    /*primary*/ public constructor CSSRuleList()
    public open override /*1*/ fun item(/*0*/ index: kotlin.Int): org.w3c.dom.css.CSSRule?
}

public abstract external class CSSStyleDeclaration : org.w3c.dom.ItemArrayLike<kotlin.String> {
    /*primary*/ public constructor CSSStyleDeclaration()
    public open var _camel_cased_attribute: kotlin.String
        public open fun <get-_camel_cased_attribute>(): kotlin.String
        public open fun <set-_camel_cased_attribute>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var _dashed_attribute: kotlin.String
        public open fun <get-_dashed_attribute>(): kotlin.String
        public open fun <set-_dashed_attribute>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var _webkit_cased_attribute: kotlin.String
        public open fun <get-_webkit_cased_attribute>(): kotlin.String
        public open fun <set-_webkit_cased_attribute>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var alignContent: kotlin.String
        public open fun <get-alignContent>(): kotlin.String
        public open fun <set-alignContent>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var alignItems: kotlin.String
        public open fun <get-alignItems>(): kotlin.String
        public open fun <set-alignItems>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var alignSelf: kotlin.String
        public open fun <get-alignSelf>(): kotlin.String
        public open fun <set-alignSelf>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var animation: kotlin.String
        public open fun <get-animation>(): kotlin.String
        public open fun <set-animation>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var animationDelay: kotlin.String
        public open fun <get-animationDelay>(): kotlin.String
        public open fun <set-animationDelay>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var animationDirection: kotlin.String
        public open fun <get-animationDirection>(): kotlin.String
        public open fun <set-animationDirection>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var animationDuration: kotlin.String
        public open fun <get-animationDuration>(): kotlin.String
        public open fun <set-animationDuration>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var animationFillMode: kotlin.String
        public open fun <get-animationFillMode>(): kotlin.String
        public open fun <set-animationFillMode>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var animationIterationCount: kotlin.String
        public open fun <get-animationIterationCount>(): kotlin.String
        public open fun <set-animationIterationCount>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var animationName: kotlin.String
        public open fun <get-animationName>(): kotlin.String
        public open fun <set-animationName>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var animationPlayState: kotlin.String
        public open fun <get-animationPlayState>(): kotlin.String
        public open fun <set-animationPlayState>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var animationTimingFunction: kotlin.String
        public open fun <get-animationTimingFunction>(): kotlin.String
        public open fun <set-animationTimingFunction>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var backfaceVisibility: kotlin.String
        public open fun <get-backfaceVisibility>(): kotlin.String
        public open fun <set-backfaceVisibility>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var background: kotlin.String
        public open fun <get-background>(): kotlin.String
        public open fun <set-background>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var backgroundAttachment: kotlin.String
        public open fun <get-backgroundAttachment>(): kotlin.String
        public open fun <set-backgroundAttachment>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var backgroundClip: kotlin.String
        public open fun <get-backgroundClip>(): kotlin.String
        public open fun <set-backgroundClip>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var backgroundColor: kotlin.String
        public open fun <get-backgroundColor>(): kotlin.String
        public open fun <set-backgroundColor>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var backgroundImage: kotlin.String
        public open fun <get-backgroundImage>(): kotlin.String
        public open fun <set-backgroundImage>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var backgroundOrigin: kotlin.String
        public open fun <get-backgroundOrigin>(): kotlin.String
        public open fun <set-backgroundOrigin>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var backgroundPosition: kotlin.String
        public open fun <get-backgroundPosition>(): kotlin.String
        public open fun <set-backgroundPosition>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var backgroundRepeat: kotlin.String
        public open fun <get-backgroundRepeat>(): kotlin.String
        public open fun <set-backgroundRepeat>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var backgroundSize: kotlin.String
        public open fun <get-backgroundSize>(): kotlin.String
        public open fun <set-backgroundSize>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var border: kotlin.String
        public open fun <get-border>(): kotlin.String
        public open fun <set-border>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var borderBottom: kotlin.String
        public open fun <get-borderBottom>(): kotlin.String
        public open fun <set-borderBottom>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var borderBottomColor: kotlin.String
        public open fun <get-borderBottomColor>(): kotlin.String
        public open fun <set-borderBottomColor>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var borderBottomLeftRadius: kotlin.String
        public open fun <get-borderBottomLeftRadius>(): kotlin.String
        public open fun <set-borderBottomLeftRadius>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var borderBottomRightRadius: kotlin.String
        public open fun <get-borderBottomRightRadius>(): kotlin.String
        public open fun <set-borderBottomRightRadius>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var borderBottomStyle: kotlin.String
        public open fun <get-borderBottomStyle>(): kotlin.String
        public open fun <set-borderBottomStyle>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var borderBottomWidth: kotlin.String
        public open fun <get-borderBottomWidth>(): kotlin.String
        public open fun <set-borderBottomWidth>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var borderCollapse: kotlin.String
        public open fun <get-borderCollapse>(): kotlin.String
        public open fun <set-borderCollapse>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var borderColor: kotlin.String
        public open fun <get-borderColor>(): kotlin.String
        public open fun <set-borderColor>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var borderImage: kotlin.String
        public open fun <get-borderImage>(): kotlin.String
        public open fun <set-borderImage>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var borderImageOutset: kotlin.String
        public open fun <get-borderImageOutset>(): kotlin.String
        public open fun <set-borderImageOutset>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var borderImageRepeat: kotlin.String
        public open fun <get-borderImageRepeat>(): kotlin.String
        public open fun <set-borderImageRepeat>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var borderImageSlice: kotlin.String
        public open fun <get-borderImageSlice>(): kotlin.String
        public open fun <set-borderImageSlice>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var borderImageSource: kotlin.String
        public open fun <get-borderImageSource>(): kotlin.String
        public open fun <set-borderImageSource>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var borderImageWidth: kotlin.String
        public open fun <get-borderImageWidth>(): kotlin.String
        public open fun <set-borderImageWidth>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var borderLeft: kotlin.String
        public open fun <get-borderLeft>(): kotlin.String
        public open fun <set-borderLeft>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var borderLeftColor: kotlin.String
        public open fun <get-borderLeftColor>(): kotlin.String
        public open fun <set-borderLeftColor>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var borderLeftStyle: kotlin.String
        public open fun <get-borderLeftStyle>(): kotlin.String
        public open fun <set-borderLeftStyle>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var borderLeftWidth: kotlin.String
        public open fun <get-borderLeftWidth>(): kotlin.String
        public open fun <set-borderLeftWidth>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var borderRadius: kotlin.String
        public open fun <get-borderRadius>(): kotlin.String
        public open fun <set-borderRadius>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var borderRight: kotlin.String
        public open fun <get-borderRight>(): kotlin.String
        public open fun <set-borderRight>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var borderRightColor: kotlin.String
        public open fun <get-borderRightColor>(): kotlin.String
        public open fun <set-borderRightColor>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var borderRightStyle: kotlin.String
        public open fun <get-borderRightStyle>(): kotlin.String
        public open fun <set-borderRightStyle>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var borderRightWidth: kotlin.String
        public open fun <get-borderRightWidth>(): kotlin.String
        public open fun <set-borderRightWidth>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var borderSpacing: kotlin.String
        public open fun <get-borderSpacing>(): kotlin.String
        public open fun <set-borderSpacing>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var borderStyle: kotlin.String
        public open fun <get-borderStyle>(): kotlin.String
        public open fun <set-borderStyle>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var borderTop: kotlin.String
        public open fun <get-borderTop>(): kotlin.String
        public open fun <set-borderTop>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var borderTopColor: kotlin.String
        public open fun <get-borderTopColor>(): kotlin.String
        public open fun <set-borderTopColor>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var borderTopLeftRadius: kotlin.String
        public open fun <get-borderTopLeftRadius>(): kotlin.String
        public open fun <set-borderTopLeftRadius>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var borderTopRightRadius: kotlin.String
        public open fun <get-borderTopRightRadius>(): kotlin.String
        public open fun <set-borderTopRightRadius>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var borderTopStyle: kotlin.String
        public open fun <get-borderTopStyle>(): kotlin.String
        public open fun <set-borderTopStyle>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var borderTopWidth: kotlin.String
        public open fun <get-borderTopWidth>(): kotlin.String
        public open fun <set-borderTopWidth>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var borderWidth: kotlin.String
        public open fun <get-borderWidth>(): kotlin.String
        public open fun <set-borderWidth>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var bottom: kotlin.String
        public open fun <get-bottom>(): kotlin.String
        public open fun <set-bottom>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var boxDecorationBreak: kotlin.String
        public open fun <get-boxDecorationBreak>(): kotlin.String
        public open fun <set-boxDecorationBreak>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var boxShadow: kotlin.String
        public open fun <get-boxShadow>(): kotlin.String
        public open fun <set-boxShadow>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var boxSizing: kotlin.String
        public open fun <get-boxSizing>(): kotlin.String
        public open fun <set-boxSizing>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var breakAfter: kotlin.String
        public open fun <get-breakAfter>(): kotlin.String
        public open fun <set-breakAfter>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var breakBefore: kotlin.String
        public open fun <get-breakBefore>(): kotlin.String
        public open fun <set-breakBefore>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var breakInside: kotlin.String
        public open fun <get-breakInside>(): kotlin.String
        public open fun <set-breakInside>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var captionSide: kotlin.String
        public open fun <get-captionSide>(): kotlin.String
        public open fun <set-captionSide>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var clear: kotlin.String
        public open fun <get-clear>(): kotlin.String
        public open fun <set-clear>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var clip: kotlin.String
        public open fun <get-clip>(): kotlin.String
        public open fun <set-clip>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var color: kotlin.String
        public open fun <get-color>(): kotlin.String
        public open fun <set-color>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var columnCount: kotlin.String
        public open fun <get-columnCount>(): kotlin.String
        public open fun <set-columnCount>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var columnFill: kotlin.String
        public open fun <get-columnFill>(): kotlin.String
        public open fun <set-columnFill>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var columnGap: kotlin.String
        public open fun <get-columnGap>(): kotlin.String
        public open fun <set-columnGap>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var columnRule: kotlin.String
        public open fun <get-columnRule>(): kotlin.String
        public open fun <set-columnRule>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var columnRuleColor: kotlin.String
        public open fun <get-columnRuleColor>(): kotlin.String
        public open fun <set-columnRuleColor>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var columnRuleStyle: kotlin.String
        public open fun <get-columnRuleStyle>(): kotlin.String
        public open fun <set-columnRuleStyle>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var columnRuleWidth: kotlin.String
        public open fun <get-columnRuleWidth>(): kotlin.String
        public open fun <set-columnRuleWidth>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var columnSpan: kotlin.String
        public open fun <get-columnSpan>(): kotlin.String
        public open fun <set-columnSpan>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var columnWidth: kotlin.String
        public open fun <get-columnWidth>(): kotlin.String
        public open fun <set-columnWidth>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var columns: kotlin.String
        public open fun <get-columns>(): kotlin.String
        public open fun <set-columns>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var content: kotlin.String
        public open fun <get-content>(): kotlin.String
        public open fun <set-content>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var counterIncrement: kotlin.String
        public open fun <get-counterIncrement>(): kotlin.String
        public open fun <set-counterIncrement>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var counterReset: kotlin.String
        public open fun <get-counterReset>(): kotlin.String
        public open fun <set-counterReset>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var cssFloat: kotlin.String
        public open fun <get-cssFloat>(): kotlin.String
        public open fun <set-cssFloat>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var cssText: kotlin.String
        public open fun <get-cssText>(): kotlin.String
        public open fun <set-cssText>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var cursor: kotlin.String
        public open fun <get-cursor>(): kotlin.String
        public open fun <set-cursor>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var direction: kotlin.String
        public open fun <get-direction>(): kotlin.String
        public open fun <set-direction>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var display: kotlin.String
        public open fun <get-display>(): kotlin.String
        public open fun <set-display>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var emptyCells: kotlin.String
        public open fun <get-emptyCells>(): kotlin.String
        public open fun <set-emptyCells>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var filter: kotlin.String
        public open fun <get-filter>(): kotlin.String
        public open fun <set-filter>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var flex: kotlin.String
        public open fun <get-flex>(): kotlin.String
        public open fun <set-flex>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var flexBasis: kotlin.String
        public open fun <get-flexBasis>(): kotlin.String
        public open fun <set-flexBasis>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var flexDirection: kotlin.String
        public open fun <get-flexDirection>(): kotlin.String
        public open fun <set-flexDirection>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var flexFlow: kotlin.String
        public open fun <get-flexFlow>(): kotlin.String
        public open fun <set-flexFlow>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var flexGrow: kotlin.String
        public open fun <get-flexGrow>(): kotlin.String
        public open fun <set-flexGrow>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var flexShrink: kotlin.String
        public open fun <get-flexShrink>(): kotlin.String
        public open fun <set-flexShrink>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var flexWrap: kotlin.String
        public open fun <get-flexWrap>(): kotlin.String
        public open fun <set-flexWrap>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var font: kotlin.String
        public open fun <get-font>(): kotlin.String
        public open fun <set-font>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var fontFamily: kotlin.String
        public open fun <get-fontFamily>(): kotlin.String
        public open fun <set-fontFamily>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var fontFeatureSettings: kotlin.String
        public open fun <get-fontFeatureSettings>(): kotlin.String
        public open fun <set-fontFeatureSettings>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var fontKerning: kotlin.String
        public open fun <get-fontKerning>(): kotlin.String
        public open fun <set-fontKerning>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var fontLanguageOverride: kotlin.String
        public open fun <get-fontLanguageOverride>(): kotlin.String
        public open fun <set-fontLanguageOverride>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var fontSize: kotlin.String
        public open fun <get-fontSize>(): kotlin.String
        public open fun <set-fontSize>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var fontSizeAdjust: kotlin.String
        public open fun <get-fontSizeAdjust>(): kotlin.String
        public open fun <set-fontSizeAdjust>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var fontStretch: kotlin.String
        public open fun <get-fontStretch>(): kotlin.String
        public open fun <set-fontStretch>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var fontStyle: kotlin.String
        public open fun <get-fontStyle>(): kotlin.String
        public open fun <set-fontStyle>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var fontSynthesis: kotlin.String
        public open fun <get-fontSynthesis>(): kotlin.String
        public open fun <set-fontSynthesis>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var fontVariant: kotlin.String
        public open fun <get-fontVariant>(): kotlin.String
        public open fun <set-fontVariant>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var fontVariantAlternates: kotlin.String
        public open fun <get-fontVariantAlternates>(): kotlin.String
        public open fun <set-fontVariantAlternates>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var fontVariantCaps: kotlin.String
        public open fun <get-fontVariantCaps>(): kotlin.String
        public open fun <set-fontVariantCaps>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var fontVariantEastAsian: kotlin.String
        public open fun <get-fontVariantEastAsian>(): kotlin.String
        public open fun <set-fontVariantEastAsian>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var fontVariantLigatures: kotlin.String
        public open fun <get-fontVariantLigatures>(): kotlin.String
        public open fun <set-fontVariantLigatures>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var fontVariantNumeric: kotlin.String
        public open fun <get-fontVariantNumeric>(): kotlin.String
        public open fun <set-fontVariantNumeric>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var fontVariantPosition: kotlin.String
        public open fun <get-fontVariantPosition>(): kotlin.String
        public open fun <set-fontVariantPosition>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var fontWeight: kotlin.String
        public open fun <get-fontWeight>(): kotlin.String
        public open fun <set-fontWeight>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var hangingPunctuation: kotlin.String
        public open fun <get-hangingPunctuation>(): kotlin.String
        public open fun <set-hangingPunctuation>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var height: kotlin.String
        public open fun <get-height>(): kotlin.String
        public open fun <set-height>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var hyphens: kotlin.String
        public open fun <get-hyphens>(): kotlin.String
        public open fun <set-hyphens>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var imageOrientation: kotlin.String
        public open fun <get-imageOrientation>(): kotlin.String
        public open fun <set-imageOrientation>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var imageRendering: kotlin.String
        public open fun <get-imageRendering>(): kotlin.String
        public open fun <set-imageRendering>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var imageResolution: kotlin.String
        public open fun <get-imageResolution>(): kotlin.String
        public open fun <set-imageResolution>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var imeMode: kotlin.String
        public open fun <get-imeMode>(): kotlin.String
        public open fun <set-imeMode>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var justifyContent: kotlin.String
        public open fun <get-justifyContent>(): kotlin.String
        public open fun <set-justifyContent>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var left: kotlin.String
        public open fun <get-left>(): kotlin.String
        public open fun <set-left>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var letterSpacing: kotlin.String
        public open fun <get-letterSpacing>(): kotlin.String
        public open fun <set-letterSpacing>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var lineBreak: kotlin.String
        public open fun <get-lineBreak>(): kotlin.String
        public open fun <set-lineBreak>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var lineHeight: kotlin.String
        public open fun <get-lineHeight>(): kotlin.String
        public open fun <set-lineHeight>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var listStyle: kotlin.String
        public open fun <get-listStyle>(): kotlin.String
        public open fun <set-listStyle>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var listStyleImage: kotlin.String
        public open fun <get-listStyleImage>(): kotlin.String
        public open fun <set-listStyleImage>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var listStylePosition: kotlin.String
        public open fun <get-listStylePosition>(): kotlin.String
        public open fun <set-listStylePosition>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var listStyleType: kotlin.String
        public open fun <get-listStyleType>(): kotlin.String
        public open fun <set-listStyleType>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var margin: kotlin.String
        public open fun <get-margin>(): kotlin.String
        public open fun <set-margin>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var marginBottom: kotlin.String
        public open fun <get-marginBottom>(): kotlin.String
        public open fun <set-marginBottom>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var marginLeft: kotlin.String
        public open fun <get-marginLeft>(): kotlin.String
        public open fun <set-marginLeft>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var marginRight: kotlin.String
        public open fun <get-marginRight>(): kotlin.String
        public open fun <set-marginRight>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var marginTop: kotlin.String
        public open fun <get-marginTop>(): kotlin.String
        public open fun <set-marginTop>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var mark: kotlin.String
        public open fun <get-mark>(): kotlin.String
        public open fun <set-mark>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var markAfter: kotlin.String
        public open fun <get-markAfter>(): kotlin.String
        public open fun <set-markAfter>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var markBefore: kotlin.String
        public open fun <get-markBefore>(): kotlin.String
        public open fun <set-markBefore>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var marks: kotlin.String
        public open fun <get-marks>(): kotlin.String
        public open fun <set-marks>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var marqueeDirection: kotlin.String
        public open fun <get-marqueeDirection>(): kotlin.String
        public open fun <set-marqueeDirection>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var marqueePlayCount: kotlin.String
        public open fun <get-marqueePlayCount>(): kotlin.String
        public open fun <set-marqueePlayCount>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var marqueeSpeed: kotlin.String
        public open fun <get-marqueeSpeed>(): kotlin.String
        public open fun <set-marqueeSpeed>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var marqueeStyle: kotlin.String
        public open fun <get-marqueeStyle>(): kotlin.String
        public open fun <set-marqueeStyle>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var mask: kotlin.String
        public open fun <get-mask>(): kotlin.String
        public open fun <set-mask>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var maskType: kotlin.String
        public open fun <get-maskType>(): kotlin.String
        public open fun <set-maskType>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var maxHeight: kotlin.String
        public open fun <get-maxHeight>(): kotlin.String
        public open fun <set-maxHeight>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var maxWidth: kotlin.String
        public open fun <get-maxWidth>(): kotlin.String
        public open fun <set-maxWidth>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var minHeight: kotlin.String
        public open fun <get-minHeight>(): kotlin.String
        public open fun <set-minHeight>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var minWidth: kotlin.String
        public open fun <get-minWidth>(): kotlin.String
        public open fun <set-minWidth>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var navDown: kotlin.String
        public open fun <get-navDown>(): kotlin.String
        public open fun <set-navDown>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var navIndex: kotlin.String
        public open fun <get-navIndex>(): kotlin.String
        public open fun <set-navIndex>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var navLeft: kotlin.String
        public open fun <get-navLeft>(): kotlin.String
        public open fun <set-navLeft>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var navRight: kotlin.String
        public open fun <get-navRight>(): kotlin.String
        public open fun <set-navRight>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var navUp: kotlin.String
        public open fun <get-navUp>(): kotlin.String
        public open fun <set-navUp>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var objectFit: kotlin.String
        public open fun <get-objectFit>(): kotlin.String
        public open fun <set-objectFit>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var objectPosition: kotlin.String
        public open fun <get-objectPosition>(): kotlin.String
        public open fun <set-objectPosition>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var opacity: kotlin.String
        public open fun <get-opacity>(): kotlin.String
        public open fun <set-opacity>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var order: kotlin.String
        public open fun <get-order>(): kotlin.String
        public open fun <set-order>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var orphans: kotlin.String
        public open fun <get-orphans>(): kotlin.String
        public open fun <set-orphans>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var outline: kotlin.String
        public open fun <get-outline>(): kotlin.String
        public open fun <set-outline>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var outlineColor: kotlin.String
        public open fun <get-outlineColor>(): kotlin.String
        public open fun <set-outlineColor>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var outlineOffset: kotlin.String
        public open fun <get-outlineOffset>(): kotlin.String
        public open fun <set-outlineOffset>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var outlineStyle: kotlin.String
        public open fun <get-outlineStyle>(): kotlin.String
        public open fun <set-outlineStyle>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var outlineWidth: kotlin.String
        public open fun <get-outlineWidth>(): kotlin.String
        public open fun <set-outlineWidth>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var overflowWrap: kotlin.String
        public open fun <get-overflowWrap>(): kotlin.String
        public open fun <set-overflowWrap>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var overflowX: kotlin.String
        public open fun <get-overflowX>(): kotlin.String
        public open fun <set-overflowX>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var overflowY: kotlin.String
        public open fun <get-overflowY>(): kotlin.String
        public open fun <set-overflowY>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var padding: kotlin.String
        public open fun <get-padding>(): kotlin.String
        public open fun <set-padding>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var paddingBottom: kotlin.String
        public open fun <get-paddingBottom>(): kotlin.String
        public open fun <set-paddingBottom>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var paddingLeft: kotlin.String
        public open fun <get-paddingLeft>(): kotlin.String
        public open fun <set-paddingLeft>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var paddingRight: kotlin.String
        public open fun <get-paddingRight>(): kotlin.String
        public open fun <set-paddingRight>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var paddingTop: kotlin.String
        public open fun <get-paddingTop>(): kotlin.String
        public open fun <set-paddingTop>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var pageBreakAfter: kotlin.String
        public open fun <get-pageBreakAfter>(): kotlin.String
        public open fun <set-pageBreakAfter>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var pageBreakBefore: kotlin.String
        public open fun <get-pageBreakBefore>(): kotlin.String
        public open fun <set-pageBreakBefore>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var pageBreakInside: kotlin.String
        public open fun <get-pageBreakInside>(): kotlin.String
        public open fun <set-pageBreakInside>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open val parentRule: org.w3c.dom.css.CSSRule?
        public open fun <get-parentRule>(): org.w3c.dom.css.CSSRule?
    public open var perspective: kotlin.String
        public open fun <get-perspective>(): kotlin.String
        public open fun <set-perspective>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var perspectiveOrigin: kotlin.String
        public open fun <get-perspectiveOrigin>(): kotlin.String
        public open fun <set-perspectiveOrigin>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var phonemes: kotlin.String
        public open fun <get-phonemes>(): kotlin.String
        public open fun <set-phonemes>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var position: kotlin.String
        public open fun <get-position>(): kotlin.String
        public open fun <set-position>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var quotes: kotlin.String
        public open fun <get-quotes>(): kotlin.String
        public open fun <set-quotes>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var resize: kotlin.String
        public open fun <get-resize>(): kotlin.String
        public open fun <set-resize>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var rest: kotlin.String
        public open fun <get-rest>(): kotlin.String
        public open fun <set-rest>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var restAfter: kotlin.String
        public open fun <get-restAfter>(): kotlin.String
        public open fun <set-restAfter>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var restBefore: kotlin.String
        public open fun <get-restBefore>(): kotlin.String
        public open fun <set-restBefore>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var right: kotlin.String
        public open fun <get-right>(): kotlin.String
        public open fun <set-right>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var tabSize: kotlin.String
        public open fun <get-tabSize>(): kotlin.String
        public open fun <set-tabSize>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var tableLayout: kotlin.String
        public open fun <get-tableLayout>(): kotlin.String
        public open fun <set-tableLayout>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var textAlign: kotlin.String
        public open fun <get-textAlign>(): kotlin.String
        public open fun <set-textAlign>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var textAlignLast: kotlin.String
        public open fun <get-textAlignLast>(): kotlin.String
        public open fun <set-textAlignLast>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var textCombineUpright: kotlin.String
        public open fun <get-textCombineUpright>(): kotlin.String
        public open fun <set-textCombineUpright>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var textDecoration: kotlin.String
        public open fun <get-textDecoration>(): kotlin.String
        public open fun <set-textDecoration>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var textDecorationColor: kotlin.String
        public open fun <get-textDecorationColor>(): kotlin.String
        public open fun <set-textDecorationColor>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var textDecorationLine: kotlin.String
        public open fun <get-textDecorationLine>(): kotlin.String
        public open fun <set-textDecorationLine>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var textDecorationStyle: kotlin.String
        public open fun <get-textDecorationStyle>(): kotlin.String
        public open fun <set-textDecorationStyle>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var textIndent: kotlin.String
        public open fun <get-textIndent>(): kotlin.String
        public open fun <set-textIndent>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var textJustify: kotlin.String
        public open fun <get-textJustify>(): kotlin.String
        public open fun <set-textJustify>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var textOrientation: kotlin.String
        public open fun <get-textOrientation>(): kotlin.String
        public open fun <set-textOrientation>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var textOverflow: kotlin.String
        public open fun <get-textOverflow>(): kotlin.String
        public open fun <set-textOverflow>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var textShadow: kotlin.String
        public open fun <get-textShadow>(): kotlin.String
        public open fun <set-textShadow>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var textTransform: kotlin.String
        public open fun <get-textTransform>(): kotlin.String
        public open fun <set-textTransform>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var textUnderlinePosition: kotlin.String
        public open fun <get-textUnderlinePosition>(): kotlin.String
        public open fun <set-textUnderlinePosition>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var top: kotlin.String
        public open fun <get-top>(): kotlin.String
        public open fun <set-top>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var transform: kotlin.String
        public open fun <get-transform>(): kotlin.String
        public open fun <set-transform>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var transformOrigin: kotlin.String
        public open fun <get-transformOrigin>(): kotlin.String
        public open fun <set-transformOrigin>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var transformStyle: kotlin.String
        public open fun <get-transformStyle>(): kotlin.String
        public open fun <set-transformStyle>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var transition: kotlin.String
        public open fun <get-transition>(): kotlin.String
        public open fun <set-transition>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var transitionDelay: kotlin.String
        public open fun <get-transitionDelay>(): kotlin.String
        public open fun <set-transitionDelay>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var transitionDuration: kotlin.String
        public open fun <get-transitionDuration>(): kotlin.String
        public open fun <set-transitionDuration>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var transitionProperty: kotlin.String
        public open fun <get-transitionProperty>(): kotlin.String
        public open fun <set-transitionProperty>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var transitionTimingFunction: kotlin.String
        public open fun <get-transitionTimingFunction>(): kotlin.String
        public open fun <set-transitionTimingFunction>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var unicodeBidi: kotlin.String
        public open fun <get-unicodeBidi>(): kotlin.String
        public open fun <set-unicodeBidi>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var verticalAlign: kotlin.String
        public open fun <get-verticalAlign>(): kotlin.String
        public open fun <set-verticalAlign>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var visibility: kotlin.String
        public open fun <get-visibility>(): kotlin.String
        public open fun <set-visibility>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var voiceBalance: kotlin.String
        public open fun <get-voiceBalance>(): kotlin.String
        public open fun <set-voiceBalance>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var voiceDuration: kotlin.String
        public open fun <get-voiceDuration>(): kotlin.String
        public open fun <set-voiceDuration>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var voicePitch: kotlin.String
        public open fun <get-voicePitch>(): kotlin.String
        public open fun <set-voicePitch>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var voicePitchRange: kotlin.String
        public open fun <get-voicePitchRange>(): kotlin.String
        public open fun <set-voicePitchRange>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var voiceRate: kotlin.String
        public open fun <get-voiceRate>(): kotlin.String
        public open fun <set-voiceRate>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var voiceStress: kotlin.String
        public open fun <get-voiceStress>(): kotlin.String
        public open fun <set-voiceStress>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var voiceVolume: kotlin.String
        public open fun <get-voiceVolume>(): kotlin.String
        public open fun <set-voiceVolume>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var whiteSpace: kotlin.String
        public open fun <get-whiteSpace>(): kotlin.String
        public open fun <set-whiteSpace>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var widows: kotlin.String
        public open fun <get-widows>(): kotlin.String
        public open fun <set-widows>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var width: kotlin.String
        public open fun <get-width>(): kotlin.String
        public open fun <set-width>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var wordBreak: kotlin.String
        public open fun <get-wordBreak>(): kotlin.String
        public open fun <set-wordBreak>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var wordSpacing: kotlin.String
        public open fun <get-wordSpacing>(): kotlin.String
        public open fun <set-wordSpacing>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var wordWrap: kotlin.String
        public open fun <get-wordWrap>(): kotlin.String
        public open fun <set-wordWrap>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var writingMode: kotlin.String
        public open fun <get-writingMode>(): kotlin.String
        public open fun <set-writingMode>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var zIndex: kotlin.String
        public open fun <get-zIndex>(): kotlin.String
        public open fun <set-zIndex>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public final fun getPropertyPriority(/*0*/ property: kotlin.String): kotlin.String
    public final fun getPropertyValue(/*0*/ property: kotlin.String): kotlin.String
    public open override /*1*/ fun item(/*0*/ index: kotlin.Int): kotlin.String
    public final fun removeProperty(/*0*/ property: kotlin.String): kotlin.String
    public final fun setProperty(/*0*/ property: kotlin.String, /*1*/ value: kotlin.String, /*2*/ priority: kotlin.String = ...): kotlin.Unit
    public final fun setPropertyPriority(/*0*/ property: kotlin.String, /*1*/ priority: kotlin.String): kotlin.Unit
    public final fun setPropertyValue(/*0*/ property: kotlin.String, /*1*/ value: kotlin.String): kotlin.Unit
}

public abstract external class CSSStyleRule : org.w3c.dom.css.CSSRule {
    /*primary*/ public constructor CSSStyleRule()
    public open var selectorText: kotlin.String
        public open fun <get-selectorText>(): kotlin.String
        public open fun <set-selectorText>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open val style: org.w3c.dom.css.CSSStyleDeclaration
        public open fun <get-style>(): org.w3c.dom.css.CSSStyleDeclaration

    public companion object Companion {
        public final val CHARSET_RULE: kotlin.Short
            public final fun <get-CHARSET_RULE>(): kotlin.Short
        public final val FONT_FACE_RULE: kotlin.Short
            public final fun <get-FONT_FACE_RULE>(): kotlin.Short
        public final val IMPORT_RULE: kotlin.Short
            public final fun <get-IMPORT_RULE>(): kotlin.Short
        public final val MARGIN_RULE: kotlin.Short
            public final fun <get-MARGIN_RULE>(): kotlin.Short
        public final val MEDIA_RULE: kotlin.Short
            public final fun <get-MEDIA_RULE>(): kotlin.Short
        public final val NAMESPACE_RULE: kotlin.Short
            public final fun <get-NAMESPACE_RULE>(): kotlin.Short
        public final val PAGE_RULE: kotlin.Short
            public final fun <get-PAGE_RULE>(): kotlin.Short
        public final val STYLE_RULE: kotlin.Short
            public final fun <get-STYLE_RULE>(): kotlin.Short
    }
}

public abstract external class CSSStyleSheet : org.w3c.dom.css.StyleSheet {
    /*primary*/ public constructor CSSStyleSheet()
    public open val cssRules: org.w3c.dom.css.CSSRuleList
        public open fun <get-cssRules>(): org.w3c.dom.css.CSSRuleList
    public open val ownerRule: org.w3c.dom.css.CSSRule?
        public open fun <get-ownerRule>(): org.w3c.dom.css.CSSRule?
    public final fun deleteRule(/*0*/ index: kotlin.Int): kotlin.Unit
    public final fun insertRule(/*0*/ rule: kotlin.String, /*1*/ index: kotlin.Int): kotlin.Int
}

public external interface ElementCSSInlineStyle {
    public abstract val style: org.w3c.dom.css.CSSStyleDeclaration
        public abstract fun <get-style>(): org.w3c.dom.css.CSSStyleDeclaration
}

public external interface LinkStyle {
    public open val sheet: org.w3c.dom.css.StyleSheet?
        public open fun <get-sheet>(): org.w3c.dom.css.StyleSheet?
}

public abstract external class MediaList : org.w3c.dom.ItemArrayLike<kotlin.String> {
    /*primary*/ public constructor MediaList()
    public open var mediaText: kotlin.String
        public open fun <get-mediaText>(): kotlin.String
        public open fun <set-mediaText>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public final fun appendMedium(/*0*/ medium: kotlin.String): kotlin.Unit
    public final fun deleteMedium(/*0*/ medium: kotlin.String): kotlin.Unit
    public open override /*1*/ fun item(/*0*/ index: kotlin.Int): kotlin.String?
}

public abstract external class StyleSheet {
    /*primary*/ public constructor StyleSheet()
    public open var disabled: kotlin.Boolean
        public open fun <get-disabled>(): kotlin.Boolean
        public open fun <set-disabled>(/*0*/ <set-?>: kotlin.Boolean): kotlin.Unit
    public open val href: kotlin.String?
        public open fun <get-href>(): kotlin.String?
    public open val media: org.w3c.dom.css.MediaList
        public open fun <get-media>(): org.w3c.dom.css.MediaList
    public open val ownerNode: org.w3c.dom.css.UnionElementOrProcessingInstruction?
        public open fun <get-ownerNode>(): org.w3c.dom.css.UnionElementOrProcessingInstruction?
    public open val parentStyleSheet: org.w3c.dom.css.StyleSheet?
        public open fun <get-parentStyleSheet>(): org.w3c.dom.css.StyleSheet?
    public open val title: kotlin.String?
        public open fun <get-title>(): kotlin.String?
    public open val type: kotlin.String
        public open fun <get-type>(): kotlin.String
}

public abstract external class StyleSheetList : org.w3c.dom.ItemArrayLike<org.w3c.dom.css.StyleSheet> {
    /*primary*/ public constructor StyleSheetList()
    public open override /*1*/ fun item(/*0*/ index: kotlin.Int): org.w3c.dom.css.StyleSheet?
}

public external interface UnionElementOrProcessingInstruction {
}