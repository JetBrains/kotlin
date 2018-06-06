namespace org.w3c.dom.css;


// Downloaded from https://drafts.csswg.org/cssom/
[ArrayClass]
interface MediaList {
  [TreatNullAs=EmptyString] stringifier attribute DOMString mediaText;
  readonly attribute unsigned long length;
  getter DOMString? item(unsigned long index);
  void appendMedium(DOMString medium);
  void deleteMedium(DOMString medium);
};
interface StyleSheet {
  readonly attribute DOMString type;
  readonly attribute DOMString? href;
  readonly attribute (Element or ProcessingInstruction)? ownerNode;
  readonly attribute StyleSheet? parentStyleSheet;
  readonly attribute DOMString? title;
  [SameObject, PutForwards=mediaText] readonly attribute MediaList media;
  attribute boolean disabled;
};
interface CSSStyleSheet : StyleSheet {
  readonly attribute CSSRule? ownerRule;
  [SameObject] readonly attribute CSSRuleList cssRules;
  unsigned long insertRule(DOMString rule, unsigned long index);
  void deleteRule(unsigned long index);
};
[ArrayClass]
interface StyleSheetList {
  getter StyleSheet? item(unsigned long index);
  readonly attribute unsigned long length;
};
partial interface Document {
  [SameObject] readonly attribute StyleSheetList styleSheets;
};
[NoInterfaceObject]
interface LinkStyle {
  readonly attribute StyleSheet? sheet;
};
ProcessingInstruction implements LinkStyle;
[ArrayClass]
interface CSSRuleList {
  getter CSSRule? item(unsigned long index);
  readonly attribute unsigned long length;
};
interface CSSRule {
  const unsigned short STYLE_RULE = 1;
  const unsigned short CHARSET_RULE = 2; // historical
  const unsigned short IMPORT_RULE = 3;
  const unsigned short MEDIA_RULE = 4;
  const unsigned short FONT_FACE_RULE = 5;
  const unsigned short PAGE_RULE = 6;
  const unsigned short MARGIN_RULE = 9;
  const unsigned short NAMESPACE_RULE = 10;
  readonly attribute unsigned short type;
  attribute DOMString cssText;
  readonly attribute CSSRule? parentRule;
  readonly attribute CSSStyleSheet? parentStyleSheet;
};
interface CSSStyleRule : CSSRule {
  attribute DOMString selectorText;
  [SameObject, PutForwards=cssText] readonly attribute CSSStyleDeclaration style;
};
interface CSSImportRule : CSSRule {
  readonly attribute DOMString href;
  [SameObject, PutForwards=mediaText] readonly attribute MediaList media;
  [SameObject] readonly attribute CSSStyleSheet styleSheet;
};
interface CSSGroupingRule : CSSRule {
  [SameObject] readonly attribute CSSRuleList cssRules;
  unsigned long insertRule(DOMString rule, unsigned long index);
  void deleteRule(unsigned long index);
};
interface CSSMediaRule : CSSGroupingRule {
  [SameObject, PutForwards=mediaText] readonly attribute MediaList media;
};
interface CSSPageRule : CSSGroupingRule {
           attribute DOMString selectorText;
  [SameObject, PutForwards=cssText] readonly attribute CSSStyleDeclaration style;
};
interface CSSMarginRule : CSSRule {
  readonly attribute DOMString name;
  [SameObject, PutForwards=cssText] readonly attribute CSSStyleDeclaration style;
};
interface CSSNamespaceRule : CSSRule {
  readonly attribute DOMString namespaceURI;
  readonly attribute DOMString prefix;
};
interface CSSStyleDeclaration {
  [CEReactions] attribute DOMString cssText;
  readonly attribute unsigned long length;
  getter DOMString item(unsigned long index);
  DOMString getPropertyValue(DOMString property);
  DOMString getPropertyPriority(DOMString property);
  [CEReactions] void setProperty(DOMString property, [TreatNullAs=EmptyString] DOMString value, [TreatNullAs=EmptyString] optional DOMString priority = "");
  [CEReactions] void setPropertyValue(DOMString property, [TreatNullAs=EmptyString] DOMString value);
  [CEReactions] void setPropertyPriority(DOMString property, [TreatNullAs=EmptyString] DOMString priority);
  [CEReactions] DOMString removeProperty(DOMString property);
  readonly attribute CSSRule? parentRule;
  [CEReactions, TreatNullAs=EmptyString] attribute DOMString cssFloat;
};
partial interface CSSStyleDeclaration {
  [CEReactions, TreatNullAs=EmptyString] attribute DOMString _dashed_attribute;
};
[NoInterfaceObject]
interface ElementCSSInlineStyle {
  [SameObject, PutForwards=cssText] readonly attribute CSSStyleDeclaration style;
};
HTMLElement implements ElementCSSInlineStyle;
SVGElement implements ElementCSSInlineStyle;
partial interface Window {
  [NewObject] CSSStyleDeclaration getComputedStyle(Element elt, optional DOMString? pseudoElt);
};
interface CSS {
  static DOMString escape(DOMString ident);
};
[ArrayClass]
interface MediaList {
  [TreatNullAs=EmptyString] stringifier attribute DOMString mediaText;
  readonly attribute unsigned long length;
  getter DOMString? item(unsigned long index);
  void appendMedium(DOMString medium);
  void deleteMedium(DOMString medium);
};

interface StyleSheet {
  readonly attribute DOMString type;
  readonly attribute DOMString? href;
  readonly attribute (Element or ProcessingInstruction)? ownerNode;
  readonly attribute StyleSheet? parentStyleSheet;
  readonly attribute DOMString? title;
  [SameObject, PutForwards=mediaText] readonly attribute MediaList media;
  attribute boolean disabled;
};

interface CSSStyleSheet : StyleSheet {
  readonly attribute CSSRule? ownerRule;
  [SameObject] readonly attribute CSSRuleList cssRules;
  unsigned long insertRule(DOMString rule, unsigned long index);
  void deleteRule(unsigned long index);
};

[ArrayClass]
interface StyleSheetList {
  getter StyleSheet? item(unsigned long index);
  readonly attribute unsigned long length;
};

partial interface Document {
  [SameObject] readonly attribute StyleSheetList styleSheets;
};

[NoInterfaceObject]
interface LinkStyle {
  readonly attribute StyleSheet? sheet;
};

ProcessingInstruction implements LinkStyle;
[ArrayClass]
interface CSSRuleList {
  getter CSSRule? item(unsigned long index);
  readonly attribute unsigned long length;
};

interface CSSRule {
  const unsigned short STYLE_RULE = 1;
  const unsigned short CHARSET_RULE = 2; // historical
  const unsigned short IMPORT_RULE = 3;
  const unsigned short MEDIA_RULE = 4;
  const unsigned short FONT_FACE_RULE = 5;
  const unsigned short PAGE_RULE = 6;
  const unsigned short MARGIN_RULE = 9;
  const unsigned short NAMESPACE_RULE = 10;
  readonly attribute unsigned short type;
  attribute DOMString cssText;
  readonly attribute CSSRule? parentRule;
  readonly attribute CSSStyleSheet? parentStyleSheet;
};

interface CSSStyleRule : CSSRule {
  attribute DOMString selectorText;
  [SameObject, PutForwards=cssText] readonly attribute CSSStyleDeclaration style;
};

interface CSSImportRule : CSSRule {
  readonly attribute DOMString href;
  [SameObject, PutForwards=mediaText] readonly attribute MediaList media;
  [SameObject] readonly attribute CSSStyleSheet styleSheet;
};

interface CSSGroupingRule : CSSRule {
  [SameObject] readonly attribute CSSRuleList cssRules;
  unsigned long insertRule(DOMString rule, unsigned long index);
  void deleteRule(unsigned long index);
};

interface CSSMediaRule : CSSGroupingRule {
  [SameObject, PutForwards=mediaText] readonly attribute MediaList media;
};

interface CSSPageRule : CSSGroupingRule {
           attribute DOMString selectorText;
  [SameObject, PutForwards=cssText] readonly attribute CSSStyleDeclaration style;
};

interface CSSMarginRule : CSSRule {
  readonly attribute DOMString name;
  [SameObject, PutForwards=cssText] readonly attribute CSSStyleDeclaration style;
};

interface CSSNamespaceRule : CSSRule {
  readonly attribute DOMString namespaceURI;
  readonly attribute DOMString prefix;
};

interface CSSStyleDeclaration {
  [CEReactions] attribute DOMString cssText;
  readonly attribute unsigned long length;
  getter DOMString item(unsigned long index);
  DOMString getPropertyValue(DOMString property);
  DOMString getPropertyPriority(DOMString property);
  [CEReactions] void setProperty(DOMString property, [TreatNullAs=EmptyString] DOMString value, [TreatNullAs=EmptyString] optional DOMString priority = "");
  [CEReactions] void setPropertyValue(DOMString property, [TreatNullAs=EmptyString] DOMString value);
  [CEReactions] void setPropertyPriority(DOMString property, [TreatNullAs=EmptyString] DOMString priority);
  [CEReactions] DOMString removeProperty(DOMString property);
  readonly attribute CSSRule? parentRule;
  [CEReactions, TreatNullAs=EmptyString] attribute DOMString cssFloat;
};

partial interface CSSStyleDeclaration {
  [CEReactions, TreatNullAs=EmptyString] attribute DOMString _camel_cased_attribute;
};

partial interface CSSStyleDeclaration {
  [CEReactions, TreatNullAs=EmptyString] attribute DOMString _webkit_cased_attribute;
};

partial interface CSSStyleDeclaration {
  [CEReactions, TreatNullAs=EmptyString] attribute DOMString _dashed_attribute;
};

[NoInterfaceObject]
interface ElementCSSInlineStyle {
  [SameObject, PutForwards=cssText] readonly attribute CSSStyleDeclaration style;
};

HTMLElement implements ElementCSSInlineStyle;

SVGElement implements ElementCSSInlineStyle;

partial interface Window {
  [NewObject] CSSStyleDeclaration getComputedStyle(Element elt, optional DOMString? pseudoElt);
};

interface CSS {
  static DOMString escape(DOMString ident);
};

