/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline operator fun org.w3c.dom.css.CSSRuleList.get(index: kotlin.Int): org.w3c.dom.css.CSSRule?
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline operator fun org.w3c.dom.css.CSSStyleDeclaration.get(index: kotlin.Int): kotlin.String?
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline operator fun org.w3c.dom.css.MediaList.get(index: kotlin.Int): kotlin.String?
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline operator fun org.w3c.dom.css.StyleSheetList.get(index: kotlin.Int): org.w3c.dom.css.StyleSheet?
/*∆*/ 
/*∆*/ public abstract external class CSS {
/*∆*/     public constructor CSS()
/*∆*/ 
/*∆*/     public companion object of CSS {
/*∆*/         public final fun escape(ident: kotlin.String): kotlin.String
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class CSSGroupingRule : org.w3c.dom.css.CSSRule {
/*∆*/     public constructor CSSGroupingRule()
/*∆*/ 
/*∆*/     public open val cssRules: org.w3c.dom.css.CSSRuleList { get; }
/*∆*/ 
/*∆*/     public final fun deleteRule(index: kotlin.Int): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun insertRule(rule: kotlin.String, index: kotlin.Int): kotlin.Int
/*∆*/ 
/*∆*/     public companion object of CSSGroupingRule {
/*∆*/         public final val CHARSET_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val FONT_FACE_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val IMPORT_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val MARGIN_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val MEDIA_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NAMESPACE_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PAGE_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val STYLE_RULE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class CSSImportRule : org.w3c.dom.css.CSSRule {
/*∆*/     public constructor CSSImportRule()
/*∆*/ 
/*∆*/     public open val href: kotlin.String { get; }
/*∆*/ 
/*∆*/     public open val media: org.w3c.dom.css.MediaList { get; }
/*∆*/ 
/*∆*/     public open val styleSheet: org.w3c.dom.css.CSSStyleSheet { get; }
/*∆*/ 
/*∆*/     public companion object of CSSImportRule {
/*∆*/         public final val CHARSET_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val FONT_FACE_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val IMPORT_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val MARGIN_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val MEDIA_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NAMESPACE_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PAGE_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val STYLE_RULE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class CSSMarginRule : org.w3c.dom.css.CSSRule {
/*∆*/     public constructor CSSMarginRule()
/*∆*/ 
/*∆*/     public open val name: kotlin.String { get; }
/*∆*/ 
/*∆*/     public open val style: org.w3c.dom.css.CSSStyleDeclaration { get; }
/*∆*/ 
/*∆*/     public companion object of CSSMarginRule {
/*∆*/         public final val CHARSET_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val FONT_FACE_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val IMPORT_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val MARGIN_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val MEDIA_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NAMESPACE_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PAGE_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val STYLE_RULE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class CSSMediaRule : org.w3c.dom.css.CSSGroupingRule {
/*∆*/     public constructor CSSMediaRule()
/*∆*/ 
/*∆*/     public open val media: org.w3c.dom.css.MediaList { get; }
/*∆*/ 
/*∆*/     public companion object of CSSMediaRule {
/*∆*/         public final val CHARSET_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val FONT_FACE_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val IMPORT_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val MARGIN_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val MEDIA_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NAMESPACE_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PAGE_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val STYLE_RULE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class CSSNamespaceRule : org.w3c.dom.css.CSSRule {
/*∆*/     public constructor CSSNamespaceRule()
/*∆*/ 
/*∆*/     public open val namespaceURI: kotlin.String { get; }
/*∆*/ 
/*∆*/     public open val prefix: kotlin.String { get; }
/*∆*/ 
/*∆*/     public companion object of CSSNamespaceRule {
/*∆*/         public final val CHARSET_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val FONT_FACE_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val IMPORT_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val MARGIN_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val MEDIA_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NAMESPACE_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PAGE_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val STYLE_RULE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class CSSPageRule : org.w3c.dom.css.CSSGroupingRule {
/*∆*/     public constructor CSSPageRule()
/*∆*/ 
/*∆*/     public open var selectorText: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open val style: org.w3c.dom.css.CSSStyleDeclaration { get; }
/*∆*/ 
/*∆*/     public companion object of CSSPageRule {
/*∆*/         public final val CHARSET_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val FONT_FACE_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val IMPORT_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val MARGIN_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val MEDIA_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NAMESPACE_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PAGE_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val STYLE_RULE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class CSSRule {
/*∆*/     public constructor CSSRule()
/*∆*/ 
/*∆*/     public open var cssText: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open val parentRule: org.w3c.dom.css.CSSRule? { get; }
/*∆*/ 
/*∆*/     public open val parentStyleSheet: org.w3c.dom.css.CSSStyleSheet? { get; }
/*∆*/ 
/*∆*/     public open val type: kotlin.Short { get; }
/*∆*/ 
/*∆*/     public companion object of CSSRule {
/*∆*/         public final val CHARSET_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val FONT_FACE_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val IMPORT_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val MARGIN_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val MEDIA_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NAMESPACE_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PAGE_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val STYLE_RULE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class CSSRuleList : org.w3c.dom.ItemArrayLike<org.w3c.dom.css.CSSRule> {
/*∆*/     public constructor CSSRuleList()
/*∆*/ 
/*∆*/     public open override fun item(index: kotlin.Int): org.w3c.dom.css.CSSRule?
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class CSSStyleDeclaration : org.w3c.dom.ItemArrayLike<kotlin.String> {
/*∆*/     public constructor CSSStyleDeclaration()
/*∆*/ 
/*∆*/     public open var _camel_cased_attribute: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var _dashed_attribute: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var _webkit_cased_attribute: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var alignContent: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var alignItems: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var alignSelf: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var animation: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var animationDelay: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var animationDirection: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var animationDuration: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var animationFillMode: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var animationIterationCount: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var animationName: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var animationPlayState: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var animationTimingFunction: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var backfaceVisibility: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var background: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var backgroundAttachment: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var backgroundClip: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var backgroundColor: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var backgroundImage: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var backgroundOrigin: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var backgroundPosition: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var backgroundRepeat: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var backgroundSize: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var border: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var borderBottom: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var borderBottomColor: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var borderBottomLeftRadius: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var borderBottomRightRadius: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var borderBottomStyle: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var borderBottomWidth: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var borderCollapse: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var borderColor: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var borderImage: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var borderImageOutset: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var borderImageRepeat: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var borderImageSlice: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var borderImageSource: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var borderImageWidth: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var borderLeft: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var borderLeftColor: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var borderLeftStyle: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var borderLeftWidth: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var borderRadius: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var borderRight: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var borderRightColor: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var borderRightStyle: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var borderRightWidth: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var borderSpacing: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var borderStyle: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var borderTop: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var borderTopColor: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var borderTopLeftRadius: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var borderTopRightRadius: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var borderTopStyle: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var borderTopWidth: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var borderWidth: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var bottom: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var boxDecorationBreak: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var boxShadow: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var boxSizing: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var breakAfter: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var breakBefore: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var breakInside: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var captionSide: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var clear: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var clip: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var color: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var columnCount: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var columnFill: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var columnGap: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var columnRule: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var columnRuleColor: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var columnRuleStyle: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var columnRuleWidth: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var columnSpan: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var columnWidth: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var columns: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var content: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var counterIncrement: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var counterReset: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var cssFloat: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var cssText: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var cursor: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var direction: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var display: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var emptyCells: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var filter: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var flex: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var flexBasis: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var flexDirection: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var flexFlow: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var flexGrow: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var flexShrink: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var flexWrap: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var font: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var fontFamily: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var fontFeatureSettings: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var fontKerning: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var fontLanguageOverride: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var fontSize: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var fontSizeAdjust: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var fontStretch: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var fontStyle: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var fontSynthesis: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var fontVariant: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var fontVariantAlternates: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var fontVariantCaps: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var fontVariantEastAsian: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var fontVariantLigatures: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var fontVariantNumeric: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var fontVariantPosition: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var fontWeight: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var hangingPunctuation: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var height: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var hyphens: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var imageOrientation: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var imageRendering: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var imageResolution: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var imeMode: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var justifyContent: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var left: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var letterSpacing: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var lineBreak: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var lineHeight: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var listStyle: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var listStyleImage: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var listStylePosition: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var listStyleType: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var margin: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var marginBottom: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var marginLeft: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var marginRight: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var marginTop: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var mark: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var markAfter: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var markBefore: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var marks: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var marqueeDirection: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var marqueePlayCount: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var marqueeSpeed: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var marqueeStyle: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var mask: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var maskType: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var maxHeight: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var maxWidth: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var minHeight: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var minWidth: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var navDown: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var navIndex: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var navLeft: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var navRight: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var navUp: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var objectFit: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var objectPosition: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var opacity: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var order: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var orphans: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var outline: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var outlineColor: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var outlineOffset: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var outlineStyle: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var outlineWidth: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var overflowWrap: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var overflowX: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var overflowY: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var padding: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var paddingBottom: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var paddingLeft: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var paddingRight: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var paddingTop: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var pageBreakAfter: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var pageBreakBefore: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var pageBreakInside: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open val parentRule: org.w3c.dom.css.CSSRule? { get; }
/*∆*/ 
/*∆*/     public open var perspective: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var perspectiveOrigin: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var phonemes: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var position: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var quotes: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var resize: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var rest: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var restAfter: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var restBefore: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var right: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var tabSize: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var tableLayout: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var textAlign: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var textAlignLast: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var textCombineUpright: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var textDecoration: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var textDecorationColor: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var textDecorationLine: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var textDecorationStyle: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var textIndent: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var textJustify: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var textOrientation: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var textOverflow: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var textShadow: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var textTransform: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var textUnderlinePosition: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var top: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var transform: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var transformOrigin: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var transformStyle: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var transition: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var transitionDelay: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var transitionDuration: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var transitionProperty: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var transitionTimingFunction: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var unicodeBidi: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var verticalAlign: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var visibility: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var voiceBalance: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var voiceDuration: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var voicePitch: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var voicePitchRange: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var voiceRate: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var voiceStress: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var voiceVolume: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var whiteSpace: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var widows: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var width: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var wordBreak: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var wordSpacing: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var wordWrap: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var writingMode: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open var zIndex: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public final fun getPropertyPriority(property: kotlin.String): kotlin.String
/*∆*/ 
/*∆*/     public final fun getPropertyValue(property: kotlin.String): kotlin.String
/*∆*/ 
/*∆*/     public open override fun item(index: kotlin.Int): kotlin.String
/*∆*/ 
/*∆*/     public final fun removeProperty(property: kotlin.String): kotlin.String
/*∆*/ 
/*∆*/     public final fun setProperty(property: kotlin.String, value: kotlin.String, priority: kotlin.String = ...): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun setPropertyPriority(property: kotlin.String, priority: kotlin.String): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun setPropertyValue(property: kotlin.String, value: kotlin.String): kotlin.Unit
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class CSSStyleRule : org.w3c.dom.css.CSSRule {
/*∆*/     public constructor CSSStyleRule()
/*∆*/ 
/*∆*/     public open var selectorText: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open val style: org.w3c.dom.css.CSSStyleDeclaration { get; }
/*∆*/ 
/*∆*/     public companion object of CSSStyleRule {
/*∆*/         public final val CHARSET_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val FONT_FACE_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val IMPORT_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val MARGIN_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val MEDIA_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NAMESPACE_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val PAGE_RULE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val STYLE_RULE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class CSSStyleSheet : org.w3c.dom.css.StyleSheet {
/*∆*/     public constructor CSSStyleSheet()
/*∆*/ 
/*∆*/     public open val cssRules: org.w3c.dom.css.CSSRuleList { get; }
/*∆*/ 
/*∆*/     public open val ownerRule: org.w3c.dom.css.CSSRule? { get; }
/*∆*/ 
/*∆*/     public final fun deleteRule(index: kotlin.Int): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun insertRule(rule: kotlin.String, index: kotlin.Int): kotlin.Int
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface ElementCSSInlineStyle {
/*∆*/     public abstract val style: org.w3c.dom.css.CSSStyleDeclaration { get; }
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface LinkStyle {
/*∆*/     public open val sheet: org.w3c.dom.css.StyleSheet? { get; }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class MediaList : org.w3c.dom.ItemArrayLike<kotlin.String> {
/*∆*/     public constructor MediaList()
/*∆*/ 
/*∆*/     public open var mediaText: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public final fun appendMedium(medium: kotlin.String): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun deleteMedium(medium: kotlin.String): kotlin.Unit
/*∆*/ 
/*∆*/     public open override fun item(index: kotlin.Int): kotlin.String?
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class StyleSheet {
/*∆*/     public constructor StyleSheet()
/*∆*/ 
/*∆*/     public open var disabled: kotlin.Boolean { get; set; }
/*∆*/ 
/*∆*/     public open val href: kotlin.String? { get; }
/*∆*/ 
/*∆*/     public open val media: org.w3c.dom.css.MediaList { get; }
/*∆*/ 
/*∆*/     public open val ownerNode: org.w3c.dom.css.UnionElementOrProcessingInstruction? { get; }
/*∆*/ 
/*∆*/     public open val parentStyleSheet: org.w3c.dom.css.StyleSheet? { get; }
/*∆*/ 
/*∆*/     public open val title: kotlin.String? { get; }
/*∆*/ 
/*∆*/     public open val type: kotlin.String { get; }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class StyleSheetList : org.w3c.dom.ItemArrayLike<org.w3c.dom.css.StyleSheet> {
/*∆*/     public constructor StyleSheetList()
/*∆*/ 
/*∆*/     public open override fun item(index: kotlin.Int): org.w3c.dom.css.StyleSheet?
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface UnionElementOrProcessingInstruction {
/*∆*/ }