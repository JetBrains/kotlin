namespace org.w3c.dom.svg;


// Downloaded from https://www.w3.org/TR/SVG2/single-page.html
interface SVGElement : Element {

  [SameObject] readonly attribute SVGAnimatedString className;

  [SameObject] readonly attribute DOMStringMap dataset;

  readonly attribute SVGSVGElement? ownerSVGElement;
  readonly attribute SVGElement? viewportElement;

  attribute long tabIndex;
  void focus();
  void blur();
};

SVGElement implements GlobalEventHandlers;
SVGElement implements SVGElementInstance;
dictionary SVGBoundingBoxOptions {
  boolean fill = true;
  boolean stroke = false;
  boolean markers = false;
  boolean clipped = false;
};

interface SVGGraphicsElement : SVGElement {
  [SameObject] readonly attribute SVGAnimatedTransformList transform;

  DOMRect getBBox(optional SVGBoundingBoxOptions options);
  DOMMatrix? getCTM();
  DOMMatrix? getScreenCTM();
};

SVGGraphicsElement implements SVGTests;
interface SVGGeometryElement : SVGGraphicsElement {
  [SameObject] readonly attribute SVGAnimatedNumber pathLength;

  boolean isPointInFill(DOMPoint point);
  boolean isPointInStroke(DOMPoint point);
  float getTotalLength();
  DOMPoint getPointAtLength(float distance);
};
interface SVGNumber {
  attribute float value;
};
interface SVGLength {

  // Length Unit Types
  const unsigned short SVG_LENGTHTYPE_UNKNOWN = 0;
  const unsigned short SVG_LENGTHTYPE_NUMBER = 1;
  const unsigned short SVG_LENGTHTYPE_PERCENTAGE = 2;
  const unsigned short SVG_LENGTHTYPE_EMS = 3;
  const unsigned short SVG_LENGTHTYPE_EXS = 4;
  const unsigned short SVG_LENGTHTYPE_PX = 5;
  const unsigned short SVG_LENGTHTYPE_CM = 6;
  const unsigned short SVG_LENGTHTYPE_MM = 7;
  const unsigned short SVG_LENGTHTYPE_IN = 8;
  const unsigned short SVG_LENGTHTYPE_PT = 9;
  const unsigned short SVG_LENGTHTYPE_PC = 10;

  readonly attribute unsigned short unitType;
           attribute float value;
           attribute float valueInSpecifiedUnits;
           attribute DOMString valueAsString;

  void newValueSpecifiedUnits(unsigned short unitType, float valueInSpecifiedUnits);
  void convertToSpecifiedUnits(unsigned short unitType);
};
interface SVGAngle {

  // Angle Unit Types
  const unsigned short SVG_ANGLETYPE_UNKNOWN = 0;
  const unsigned short SVG_ANGLETYPE_UNSPECIFIED = 1;
  const unsigned short SVG_ANGLETYPE_DEG = 2;
  const unsigned short SVG_ANGLETYPE_RAD = 3;
  const unsigned short SVG_ANGLETYPE_GRAD = 4;

  readonly attribute unsigned short unitType;
           attribute float value;
           attribute float valueInSpecifiedUnits;
           attribute DOMString valueAsString;

  void newValueSpecifiedUnits(unsigned short unitType, float valueInSpecifiedUnits);
  void convertToSpecifiedUnits(unsigned short unitType);
};
interface SVGNameList {

  readonly attribute unsigned long length;
  readonly attribute unsigned long numberOfItems;

  void clear();
  Type initialize(Type newItem);
  getter Type getItem(unsigned long index);
  Type insertItemBefore(Type newItem, unsigned long index);
  Type replaceItem(Type newItem, unsigned long index);
  Type removeItem(unsigned long index);
  Type appendItem(Type newItem);
  setter void (unsigned long index, Type newItem);
};
interface SVGNumberList {

  readonly attribute unsigned long length;
  readonly attribute unsigned long numberOfItems;

  void clear();
  SVGNumber initialize(SVGNumber newItem);
  getter SVGNumber getItem(unsigned long index);
  SVGNumber insertItemBefore(SVGNumber newItem, unsigned long index);
  SVGNumber replaceItem(SVGNumber newItem, unsigned long index);
  SVGNumber removeItem(unsigned long index);
  SVGNumber appendItem(SVGNumber newItem);
  setter void (unsigned long index, SVGNumber newItem);
};
interface SVGLengthList {

  readonly attribute unsigned long length;
  readonly attribute unsigned long numberOfItems;

  void clear();
  SVGLength initialize(SVGLength newItem);
  getter SVGLength getItem(unsigned long index);
  SVGLength insertItemBefore(SVGLength newItem, unsigned long index);
  SVGLength replaceItem(SVGLength newItem, unsigned long index);
  SVGLength removeItem(unsigned long index);
  SVGLength appendItem(SVGLength newItem);
  setter void (unsigned long index, SVGLength newItem);
};
interface SVGAnimatedBoolean {
           attribute boolean baseVal;
  readonly attribute boolean animVal;
};
interface SVGAnimatedEnumeration {
           attribute unsigned short baseVal;
  readonly attribute unsigned short animVal;
};
interface SVGAnimatedInteger {
           attribute long baseVal;
  readonly attribute long animVal;
};
interface SVGAnimatedNumber {
           attribute float baseVal;
  readonly attribute float animVal;
};
interface SVGAnimatedLength {
  [SameObject] readonly attribute SVGLength baseVal;
  [SameObject] readonly attribute SVGLength animVal;
};
interface SVGAnimatedAngle {
  [SameObject] readonly attribute SVGAngle baseVal;
  [SameObject] readonly attribute SVGAngle animVal;
};
interface SVGAnimatedString {
           attribute DOMString baseVal;
  readonly attribute DOMString animVal;
};
interface SVGAnimatedRect {
  [SameObject] readonly attribute DOMRect baseVal;
  [SameObject] readonly attribute DOMRectReadOnly animVal;
};
interface SVGAnimatedNumberList {
  [SameObject] readonly attribute SVGNumberList baseVal;
  [SameObject] readonly attribute SVGNumberList animVal;
};
interface SVGAnimatedLengthList {
  [SameObject] readonly attribute SVGLengthList baseVal;
  [SameObject] readonly attribute SVGLengthList animVal;
};
interface SVGStringList {

  readonly attribute unsigned long length;
  readonly attribute unsigned long numberOfItems;

  void clear();
  DOMString initialize(DOMString newItem);
  getter DOMString getItem(unsigned long index);
  DOMString insertItemBefore(DOMString newItem, unsigned long index);
  DOMString replaceItem(DOMString newItem, unsigned long index);
  DOMString removeItem(unsigned long index);
  DOMString appendItem(DOMString newItem);
  setter void (unsigned long index, DOMString newItem);
};
[NoInterfaceObject]
interface SVGUnitTypes {
  // Unit Types
  const unsigned short SVG_UNIT_TYPE_UNKNOWN = 0;
  const unsigned short SVG_UNIT_TYPE_USERSPACEONUSE = 1;
  const unsigned short SVG_UNIT_TYPE_OBJECTBOUNDINGBOX = 2;
};
[NoInterfaceObject]
interface SVGTests {
  [SameObject] readonly attribute SVGStringList requiredExtensions;
  [SameObject] readonly attribute SVGStringList systemLanguage;
};
[NoInterfaceObject]
interface SVGFitToViewBox {
  [SameObject] readonly attribute SVGAnimatedRect viewBox;
  [SameObject] readonly attribute SVGAnimatedPreserveAspectRatio preserveAspectRatio;
};
[NoInterfaceObject]
interface SVGZoomAndPan {

  // Zoom and Pan Types
  const unsigned short SVG_ZOOMANDPAN_UNKNOWN = 0;
  const unsigned short SVG_ZOOMANDPAN_DISABLE = 1;
  const unsigned short SVG_ZOOMANDPAN_MAGNIFY = 2;

  attribute unsigned short zoomAndPan;
};
[NoInterfaceObject]
interface SVGURIReference {
  [SameObject] readonly attribute SVGAnimatedString href;
};
partial interface Document {
  readonly attribute SVGSVGElement? rootElement;
};
// must only be implemented in certain implementations
partial interface Document {
  readonly attribute DOMString title;
  readonly attribute DOMString referrer;
  readonly attribute DOMString domain;
  readonly attribute Element? activeElement;
};
interface SVGSVGElement : SVGGraphicsElement {

  [SameObject] readonly attribute SVGAnimatedLength x;
  [SameObject] readonly attribute SVGAnimatedLength y;
  [SameObject] readonly attribute SVGAnimatedLength width;
  [SameObject] readonly attribute SVGAnimatedLength height;

  attribute float currentScale;
  [SameObject] readonly attribute DOMPointReadOnly currentTranslate;

  NodeList getIntersectionList(DOMRectReadOnly rect, SVGElement? referenceElement);
  NodeList getEnclosureList(DOMRectReadOnly rect, SVGElement? referenceElement);
  boolean checkIntersection(SVGElement element, DOMRectReadOnly rect);
  boolean checkEnclosure(SVGElement element, DOMRectReadOnly rect);

  void deselectAll();

  SVGNumber createSVGNumber();
  SVGLength createSVGLength();
  SVGAngle createSVGAngle();
  DOMPoint createSVGPoint();
  DOMMatrix createSVGMatrix();
  DOMRect createSVGRect();
  SVGTransform createSVGTransform();
  SVGTransform createSVGTransformFromMatrix(DOMMatrixReadOnly matrix);

  Element getElementById(DOMString elementId);

  // Deprecated methods that have no effect when called,
  // but which are kept for compatibility reasons.
  unsigned long suspendRedraw(unsigned long maxWaitMilliseconds);
  void unsuspendRedraw(unsigned long suspendHandleID);
  void unsuspendRedrawAll();
  void forceRedraw();
};

SVGSVGElement implements SVGFitToViewBox;
SVGSVGElement implements SVGZoomAndPan;
SVGSVGElement implements WindowEventHandlers;
interface SVGGElement : SVGGraphicsElement {
};
interface SVGUnknownElement : SVGGraphicsElement {
};
interface SVGDefsElement : SVGGraphicsElement {
};
interface SVGDescElement : SVGElement {
};
interface SVGMetadataElement : SVGElement {
};
interface SVGTitleElement : SVGElement {
};
interface SVGSymbolElement : SVGGraphicsElement {
};

SVGSymbolElement implements SVGFitToViewBox;
interface SVGUseElement : SVGGraphicsElement {
  [SameObject] readonly attribute SVGAnimatedLength x;
  [SameObject] readonly attribute SVGAnimatedLength y;
  [SameObject] readonly attribute SVGAnimatedLength width;
  [SameObject] readonly attribute SVGAnimatedLength height;
  [SameObject] readonly attribute SVGElement? instanceRoot;
  [SameObject] readonly attribute SVGElement? animatedInstanceRoot;
};

SVGUseElement implements SVGURIReference;
interface SVGUseElementShadowRoot : ShadowRoot {

};
[NoInterfaceObject]
interface SVGElementInstance {
  [SameObject] readonly attribute SVGElement? correspondingElement;
  [SameObject] readonly attribute SVGUseElement? correspondingUseElement;
};
[Constructor (Animation source, Animatable newTarget) ]
interface ShadowAnimation : Animation {
  [SameObject] readonly attribute Animation sourceAnimation;
};
interface SVGSwitchElement : SVGGraphicsElement {
};
[NoInterfaceObject]
interface GetSVGDocument {
  Document getSVGDocument();
};
interface SVGStyleElement : SVGElement {
  attribute DOMString type;
  attribute DOMString media;
  attribute DOMString title;
};

SVGStyleElement implements LinkStyle;
interface SVGTransform {

  // Transform Types
  const unsigned short SVG_TRANSFORM_UNKNOWN = 0;
  const unsigned short SVG_TRANSFORM_MATRIX = 1;
  const unsigned short SVG_TRANSFORM_TRANSLATE = 2;
  const unsigned short SVG_TRANSFORM_SCALE = 3;
  const unsigned short SVG_TRANSFORM_ROTATE = 4;
  const unsigned short SVG_TRANSFORM_SKEWX = 5;
  const unsigned short SVG_TRANSFORM_SKEWY = 6;

  readonly attribute unsigned short type;
  [SameObject] readonly attribute DOMMatrix matrix;
  readonly attribute float angle;

  void setMatrix(DOMMatrixReadOnly matrix);
  void setTranslate(float tx, float ty);
  void setScale(float sx, float sy);
  void setRotate(float angle, float cx, float cy);
  void setSkewX(float angle);
  void setSkewY(float angle);
};
interface SVGTransformList {

  readonly attribute unsigned long length;
  readonly attribute unsigned long numberOfItems;

  void clear();
  SVGTransform initialize(SVGTransform newItem);
  getter SVGTransform getItem(unsigned long index);
  SVGTransform insertItemBefore(SVGTransform newItem, unsigned long index);
  SVGTransform replaceItem(SVGTransform newItem, unsigned long index);
  SVGTransform removeItem(unsigned long index);
  SVGTransform appendItem(SVGTransform newItem);
  setter void (unsigned long index, SVGTransform newItem);

  // Additional methods not common to other list interfaces.
  SVGTransform createSVGTransformFromMatrix(DOMMatrixReadOnly matrix);
  SVGTransform? consolidate();
};
interface SVGAnimatedTransformList {
  [SameObject] readonly attribute SVGTransformList baseVal;
  [SameObject] readonly attribute SVGTransformList animVal;
};
interface SVGPreserveAspectRatio {

  // Alignment Types
  const unsigned short SVG_PRESERVEASPECTRATIO_UNKNOWN = 0;
  const unsigned short SVG_PRESERVEASPECTRATIO_NONE = 1;
  const unsigned short SVG_PRESERVEASPECTRATIO_XMINYMIN = 2;
  const unsigned short SVG_PRESERVEASPECTRATIO_XMIDYMIN = 3;
  const unsigned short SVG_PRESERVEASPECTRATIO_XMAXYMIN = 4;
  const unsigned short SVG_PRESERVEASPECTRATIO_XMINYMID = 5;
  const unsigned short SVG_PRESERVEASPECTRATIO_XMIDYMID = 6;
  const unsigned short SVG_PRESERVEASPECTRATIO_XMAXYMID = 7;
  const unsigned short SVG_PRESERVEASPECTRATIO_XMINYMAX = 8;
  const unsigned short SVG_PRESERVEASPECTRATIO_XMIDYMAX = 9;
  const unsigned short SVG_PRESERVEASPECTRATIO_XMAXYMAX = 10;

  // Meet-or-slice Types
  const unsigned short SVG_MEETORSLICE_UNKNOWN = 0;
  const unsigned short SVG_MEETORSLICE_MEET = 1;
  const unsigned short SVG_MEETORSLICE_SLICE = 2;

  attribute unsigned short align;
  attribute unsigned short meetOrSlice;
};
interface SVGAnimatedPreserveAspectRatio {
  [SameObject] readonly attribute SVGPreserveAspectRatio baseVal;
  [SameObject] readonly attribute SVGPreserveAspectRatio animVal;
};
interface SVGPathElement : SVGGeometryElement {

};
interface SVGRectElement : SVGGeometryElement {
  [SameObject] readonly attribute SVGAnimatedLength x;
  [SameObject] readonly attribute SVGAnimatedLength y;
  [SameObject] readonly attribute SVGAnimatedLength width;
  [SameObject] readonly attribute SVGAnimatedLength height;
  [SameObject] readonly attribute SVGAnimatedLength rx;
  [SameObject] readonly attribute SVGAnimatedLength ry;
};
interface SVGCircleElement : SVGGeometryElement {
  [SameObject] readonly attribute SVGAnimatedLength cx;
  [SameObject] readonly attribute SVGAnimatedLength cy;
  [SameObject] readonly attribute SVGAnimatedLength r;
};
interface SVGEllipseElement : SVGGeometryElement {
  [SameObject] readonly attribute SVGAnimatedLength cx;
  [SameObject] readonly attribute SVGAnimatedLength cy;
  [SameObject] readonly attribute SVGAnimatedLength rx;
  [SameObject] readonly attribute SVGAnimatedLength ry;
};
interface SVGLineElement : SVGGeometryElement {
  [SameObject] readonly attribute SVGAnimatedLength x1;
  [SameObject] readonly attribute SVGAnimatedLength y1;
  [SameObject] readonly attribute SVGAnimatedLength x2;
  [SameObject] readonly attribute SVGAnimatedLength y2;
};
interface SVGMeshElement : SVGGeometryElement {};

SVGMeshElement implements SVGURIReference;
[NoInterfaceObject]
interface SVGAnimatedPoints {
  [SameObject] readonly attribute SVGPointList points;
  [SameObject] readonly attribute SVGPointList animatedPoints;
};
interface SVGPointList {

  readonly attribute unsigned long length;
  readonly attribute unsigned long numberOfItems;

  void clear();
  DOMPoint initialize(DOMPoint newItem);
  getter DOMPoint getItem(unsigned long index);
  DOMPoint insertItemBefore(DOMPoint newItem, unsigned long index);
  DOMPoint replaceItem(DOMPoint newItem, unsigned long index);
  DOMPoint removeItem(unsigned long index);
  DOMPoint appendItem(DOMPoint newItem);
  setter void (unsigned long index, DOMPoint newItem);
};
interface SVGPolylineElement : SVGGeometryElement {
};

SVGPolylineElement implements SVGAnimatedPoints;
interface SVGPolygonElement : SVGGeometryElement {
};

SVGPolygonElement implements SVGAnimatedPoints;
interface SVGTextContentElement : SVGGraphicsElement {

  // lengthAdjust Types
  const unsigned short LENGTHADJUST_UNKNOWN = 0;
  const unsigned short LENGTHADJUST_SPACING = 1;
  const unsigned short LENGTHADJUST_SPACINGANDGLYPHS = 2;

  [SameObject] readonly attribute SVGAnimatedLength textLength;
  [SameObject] readonly attribute SVGAnimatedEnumeration lengthAdjust;

  long getNumberOfChars();
  float getComputedTextLength();
  float getSubStringLength(unsigned long charnum, unsigned long nchars);
  DOMPoint getStartPositionOfChar(unsigned long charnum);
  DOMPoint getEndPositionOfChar(unsigned long charnum);
  DOMRect getExtentOfChar(unsigned long charnum);
  float getRotationOfChar(unsigned long charnum);
  long getCharNumAtPosition(DOMPoint point);
  void selectSubString(unsigned long charnum, unsigned long nchars);
};
interface SVGTextPositioningElement : SVGTextContentElement {
  [SameObject] readonly attribute SVGAnimatedLengthList x;
  [SameObject] readonly attribute SVGAnimatedLengthList y;
  [SameObject] readonly attribute SVGAnimatedLengthList dx;
  [SameObject] readonly attribute SVGAnimatedLengthList dy;
  [SameObject] readonly attribute SVGAnimatedNumberList rotate;
};
interface SVGTextElement : SVGTextPositioningElement {
};
interface SVGTSpanElement : SVGTextPositioningElement {
};
interface SVGTextPathElement : SVGTextContentElement {

  // textPath Method Types
  const unsigned short TEXTPATH_METHODTYPE_UNKNOWN = 0;
  const unsigned short TEXTPATH_METHODTYPE_ALIGN = 1;
  const unsigned short TEXTPATH_METHODTYPE_STRETCH = 2;

  // textPath Spacing Types
  const unsigned short TEXTPATH_SPACINGTYPE_UNKNOWN = 0;
  const unsigned short TEXTPATH_SPACINGTYPE_AUTO = 1;
  const unsigned short TEXTPATH_SPACINGTYPE_EXACT = 2;

  [SameObject] readonly attribute SVGAnimatedLength startOffset;
  [SameObject] readonly attribute SVGAnimatedEnumeration method;
  [SameObject] readonly attribute SVGAnimatedEnumeration spacing;
};

SVGTextPathElement implements SVGURIReference;
interface SVGImageElement : SVGGraphicsElement {
  [SameObject] readonly attribute SVGAnimatedLength x;
  [SameObject] readonly attribute SVGAnimatedLength y;
  [SameObject] readonly attribute SVGAnimatedLength width;
  [SameObject] readonly attribute SVGAnimatedLength height;
  [SameObject] readonly attribute SVGAnimatedPreserveAspectRatio preserveAspectRatio;
  attribute DOMString? crossOrigin;
};

SVGImageElement implements SVGURIReference;
interface SVGForeignObjectElement : SVGGraphicsElement {
  [SameObject] readonly attribute SVGAnimatedLength x;
  [SameObject] readonly attribute SVGAnimatedLength y;
  [SameObject] readonly attribute SVGAnimatedLength width;
  [SameObject] readonly attribute SVGAnimatedLength height;
};
interface SVGMarkerElement : SVGElement {

  // Marker Unit Types
  const unsigned short SVG_MARKERUNITS_UNKNOWN = 0;
  const unsigned short SVG_MARKERUNITS_USERSPACEONUSE = 1;
  const unsigned short SVG_MARKERUNITS_STROKEWIDTH = 2;

  // Marker Orientation Types
  const unsigned short SVG_MARKER_ORIENT_UNKNOWN = 0;
  const unsigned short SVG_MARKER_ORIENT_AUTO = 1;
  const unsigned short SVG_MARKER_ORIENT_ANGLE = 2;

  [SameObject] readonly attribute SVGAnimatedLength refX;
  [SameObject] readonly attribute SVGAnimatedLength refY;
  [SameObject] readonly attribute SVGAnimatedEnumeration markerUnits;
  [SameObject] readonly attribute SVGAnimatedLength markerWidth;
  [SameObject] readonly attribute SVGAnimatedLength markerHeight;
  [SameObject] readonly attribute SVGAnimatedEnumeration orientType;
  [SameObject] readonly attribute SVGAnimatedAngle orientAngle;
  attribute DOMString orient;

  void setOrientToAuto();
  void setOrientToAngle(SVGAngle angle);
};

SVGMarkerElement implements SVGFitToViewBox;
interface SVGSolidcolorElement : SVGElement {
};
interface SVGGradientElement : SVGElement {

  // Spread Method Types
  const unsigned short SVG_SPREADMETHOD_UNKNOWN = 0;
  const unsigned short SVG_SPREADMETHOD_PAD = 1;
  const unsigned short SVG_SPREADMETHOD_REFLECT = 2;
  const unsigned short SVG_SPREADMETHOD_REPEAT = 3;

  [SameObject] readonly attribute SVGAnimatedEnumeration gradientUnits;
  [SameObject] readonly attribute SVGAnimatedTransformList gradientTransform;
  [SameObject] readonly attribute SVGAnimatedEnumeration spreadMethod;
};

SVGGradientElement implements SVGURIReference;
SVGGradientElement implements SVGUnitTypes;
interface SVGLinearGradientElement : SVGGradientElement {
  [SameObject] readonly attribute SVGAnimatedLength x1;
  [SameObject] readonly attribute SVGAnimatedLength y1;
  [SameObject] readonly attribute SVGAnimatedLength x2;
  [SameObject] readonly attribute SVGAnimatedLength y2;
};
interface SVGRadialGradientElement : SVGGradientElement {
  [SameObject] readonly attribute SVGAnimatedLength cx;
  [SameObject] readonly attribute SVGAnimatedLength cy;
  [SameObject] readonly attribute SVGAnimatedLength r;
  [SameObject] readonly attribute SVGAnimatedLength fx;
  [SameObject] readonly attribute SVGAnimatedLength fy;
  [SameObject] readonly attribute SVGAnimatedLength fr;
};
interface SVGMeshGradientElement : SVGGradientElement {
};
interface SVGMeshrowElement : SVGElement {
};
interface SVGMeshpatchElement : SVGElement {
};
interface SVGStopElement : SVGElement {
  [SameObject] readonly attribute SVGAnimatedNumber offset;
};
interface SVGPatternElement : SVGElement {
  [SameObject] readonly attribute SVGAnimatedEnumeration patternUnits;
  [SameObject] readonly attribute SVGAnimatedEnumeration patternContentUnits;
  [SameObject] readonly attribute SVGAnimatedTransformList patternTransform;
  [SameObject] readonly attribute SVGAnimatedLength x;
  [SameObject] readonly attribute SVGAnimatedLength y;
  [SameObject] readonly attribute SVGAnimatedLength width;
  [SameObject] readonly attribute SVGAnimatedLength height;
};

SVGPatternElement implements SVGFitToViewBox;
SVGPatternElement implements SVGURIReference;
SVGPatternElement implements SVGUnitTypes;
interface SVGHatchElement : SVGElement {
};
interface SVGHatchpathElement : SVGElement {
};
interface SVGCursorElement : SVGElement {
  [SameObject] readonly attribute SVGAnimatedLength x;
  [SameObject] readonly attribute SVGAnimatedLength y;
};

SVGCursorElement implements SVGURIReference;
interface SVGScriptElement : SVGElement {
  attribute DOMString type;
  attribute DOMString? crossOrigin;
};

SVGScriptElement implements SVGURIReference;
interface SVGAElement : SVGGraphicsElement {
  [SameObject] readonly attribute SVGAnimatedString target;
  [SameObject] readonly attribute SVGAnimatedString download;
  [SameObject] readonly attribute SVGAnimatedString rel;
  [SameObject] readonly attribute SVGAnimatedString relList;
  [SameObject] readonly attribute SVGAnimatedString hreflang;
  [SameObject] readonly attribute SVGAnimatedString type;
};

SVGAElement implements SVGURIReference;
SVGAElement implements HTMLHyperlinkElementUtils;
interface SVGViewElement : SVGElement {};

SVGViewElement implements SVGFitToViewBox;
SVGViewElement implements SVGZoomAndPan;
interface SVGElement : Element {

  [SameObject] readonly attribute SVGAnimatedString className;

  [SameObject] readonly attribute DOMStringMap dataset;

  readonly attribute SVGSVGElement? ownerSVGElement;
  readonly attribute SVGElement? viewportElement;

  attribute long tabIndex;
  void focus();
  void blur();
};

SVGElement implements GlobalEventHandlers;
SVGElement implements SVGElementInstance;

dictionary SVGBoundingBoxOptions {
  boolean fill = true;
  boolean stroke = false;
  boolean markers = false;
  boolean clipped = false;
};

interface SVGGraphicsElement : SVGElement {
  [SameObject] readonly attribute SVGAnimatedTransformList transform;

  DOMRect getBBox(optional SVGBoundingBoxOptions options);
  DOMMatrix? getCTM();
  DOMMatrix? getScreenCTM();
};

SVGGraphicsElement implements SVGTests;

interface SVGGeometryElement : SVGGraphicsElement {
  [SameObject] readonly attribute SVGAnimatedNumber pathLength;

  boolean isPointInFill(DOMPoint point);
  boolean isPointInStroke(DOMPoint point);
  float getTotalLength();
  DOMPoint getPointAtLength(float distance);
};

interface SVGNumber {
  attribute float value;
};

interface SVGLength {

  // Length Unit Types
  const unsigned short SVG_LENGTHTYPE_UNKNOWN = 0;
  const unsigned short SVG_LENGTHTYPE_NUMBER = 1;
  const unsigned short SVG_LENGTHTYPE_PERCENTAGE = 2;
  const unsigned short SVG_LENGTHTYPE_EMS = 3;
  const unsigned short SVG_LENGTHTYPE_EXS = 4;
  const unsigned short SVG_LENGTHTYPE_PX = 5;
  const unsigned short SVG_LENGTHTYPE_CM = 6;
  const unsigned short SVG_LENGTHTYPE_MM = 7;
  const unsigned short SVG_LENGTHTYPE_IN = 8;
  const unsigned short SVG_LENGTHTYPE_PT = 9;
  const unsigned short SVG_LENGTHTYPE_PC = 10;

  readonly attribute unsigned short unitType;
           attribute float value;
           attribute float valueInSpecifiedUnits;
           attribute DOMString valueAsString;

  void newValueSpecifiedUnits(unsigned short unitType, float valueInSpecifiedUnits);
  void convertToSpecifiedUnits(unsigned short unitType);
};

interface SVGAngle {

  // Angle Unit Types
  const unsigned short SVG_ANGLETYPE_UNKNOWN = 0;
  const unsigned short SVG_ANGLETYPE_UNSPECIFIED = 1;
  const unsigned short SVG_ANGLETYPE_DEG = 2;
  const unsigned short SVG_ANGLETYPE_RAD = 3;
  const unsigned short SVG_ANGLETYPE_GRAD = 4;

  readonly attribute unsigned short unitType;
           attribute float value;
           attribute float valueInSpecifiedUnits;
           attribute DOMString valueAsString;

  void newValueSpecifiedUnits(unsigned short unitType, float valueInSpecifiedUnits);
  void convertToSpecifiedUnits(unsigned short unitType);
};

interface SVGNumberList {

  readonly attribute unsigned long length;
  readonly attribute unsigned long numberOfItems;

  void clear();
  SVGNumber initialize(SVGNumber newItem);
  getter SVGNumber getItem(unsigned long index);
  SVGNumber insertItemBefore(SVGNumber newItem, unsigned long index);
  SVGNumber replaceItem(SVGNumber newItem, unsigned long index);
  SVGNumber removeItem(unsigned long index);
  SVGNumber appendItem(SVGNumber newItem);
  setter void (unsigned long index, SVGNumber newItem);
};

interface SVGLengthList {

  readonly attribute unsigned long length;
  readonly attribute unsigned long numberOfItems;

  void clear();
  SVGLength initialize(SVGLength newItem);
  getter SVGLength getItem(unsigned long index);
  SVGLength insertItemBefore(SVGLength newItem, unsigned long index);
  SVGLength replaceItem(SVGLength newItem, unsigned long index);
  SVGLength removeItem(unsigned long index);
  SVGLength appendItem(SVGLength newItem);
  setter void (unsigned long index, SVGLength newItem);
};

interface SVGAnimatedBoolean {
           attribute boolean baseVal;
  readonly attribute boolean animVal;
};

interface SVGAnimatedEnumeration {
           attribute unsigned short baseVal;
  readonly attribute unsigned short animVal;
};

interface SVGAnimatedInteger {
           attribute long baseVal;
  readonly attribute long animVal;
};

interface SVGAnimatedNumber {
           attribute float baseVal;
  readonly attribute float animVal;
};

interface SVGAnimatedLength {
  [SameObject] readonly attribute SVGLength baseVal;
  [SameObject] readonly attribute SVGLength animVal;
};

interface SVGAnimatedAngle {
  [SameObject] readonly attribute SVGAngle baseVal;
  [SameObject] readonly attribute SVGAngle animVal;
};

interface SVGAnimatedString {
           attribute DOMString baseVal;
  readonly attribute DOMString animVal;
};

interface SVGAnimatedRect {
  [SameObject] readonly attribute DOMRect baseVal;
  [SameObject] readonly attribute DOMRectReadOnly animVal;
};

interface SVGAnimatedNumberList {
  [SameObject] readonly attribute SVGNumberList baseVal;
  [SameObject] readonly attribute SVGNumberList animVal;
};

interface SVGAnimatedLengthList {
  [SameObject] readonly attribute SVGLengthList baseVal;
  [SameObject] readonly attribute SVGLengthList animVal;
};

interface SVGStringList {

  readonly attribute unsigned long length;
  readonly attribute unsigned long numberOfItems;

  void clear();
  DOMString initialize(DOMString newItem);
  getter DOMString getItem(unsigned long index);
  DOMString insertItemBefore(DOMString newItem, unsigned long index);
  DOMString replaceItem(DOMString newItem, unsigned long index);
  DOMString removeItem(unsigned long index);
  DOMString appendItem(DOMString newItem);
  setter void (unsigned long index, DOMString newItem);
};

[NoInterfaceObject]
interface SVGUnitTypes {
  // Unit Types
  const unsigned short SVG_UNIT_TYPE_UNKNOWN = 0;
  const unsigned short SVG_UNIT_TYPE_USERSPACEONUSE = 1;
  const unsigned short SVG_UNIT_TYPE_OBJECTBOUNDINGBOX = 2;
};

[NoInterfaceObject]
interface SVGTests {
  [SameObject] readonly attribute SVGStringList requiredExtensions;
  [SameObject] readonly attribute SVGStringList systemLanguage;
};

[NoInterfaceObject]
interface SVGFitToViewBox {
  [SameObject] readonly attribute SVGAnimatedRect viewBox;
  [SameObject] readonly attribute SVGAnimatedPreserveAspectRatio preserveAspectRatio;
};

[NoInterfaceObject]
interface SVGZoomAndPan {

  // Zoom and Pan Types
  const unsigned short SVG_ZOOMANDPAN_UNKNOWN = 0;
  const unsigned short SVG_ZOOMANDPAN_DISABLE = 1;
  const unsigned short SVG_ZOOMANDPAN_MAGNIFY = 2;

  attribute unsigned short zoomAndPan;
};

[NoInterfaceObject]
interface SVGURIReference {
  [SameObject] readonly attribute SVGAnimatedString href;
};

partial interface Document {
  readonly attribute SVGSVGElement? rootElement;
};

interface SVGSVGElement : SVGGraphicsElement {

  [SameObject] readonly attribute SVGAnimatedLength x;
  [SameObject] readonly attribute SVGAnimatedLength y;
  [SameObject] readonly attribute SVGAnimatedLength width;
  [SameObject] readonly attribute SVGAnimatedLength height;

  attribute float currentScale;
  [SameObject] readonly attribute DOMPointReadOnly currentTranslate;

  NodeList getIntersectionList(DOMRectReadOnly rect, SVGElement? referenceElement);
  NodeList getEnclosureList(DOMRectReadOnly rect, SVGElement? referenceElement);
  boolean checkIntersection(SVGElement element, DOMRectReadOnly rect);
  boolean checkEnclosure(SVGElement element, DOMRectReadOnly rect);

  void deselectAll();

  SVGNumber createSVGNumber();
  SVGLength createSVGLength();
  SVGAngle createSVGAngle();
  DOMPoint createSVGPoint();
  DOMMatrix createSVGMatrix();
  DOMRect createSVGRect();
  SVGTransform createSVGTransform();
  SVGTransform createSVGTransformFromMatrix(DOMMatrixReadOnly matrix);

  Element getElementById(DOMString elementId);

  // Deprecated methods that have no effect when called,
  // but which are kept for compatibility reasons.
  unsigned long suspendRedraw(unsigned long maxWaitMilliseconds);
  void unsuspendRedraw(unsigned long suspendHandleID);
  void unsuspendRedrawAll();
  void forceRedraw();
};

SVGSVGElement implements SVGFitToViewBox;
SVGSVGElement implements SVGZoomAndPan;
SVGSVGElement implements WindowEventHandlers;

interface SVGGElement : SVGGraphicsElement {
};

interface SVGUnknownElement : SVGGraphicsElement {
};

interface SVGDefsElement : SVGGraphicsElement {
};

interface SVGDescElement : SVGElement {
};

interface SVGMetadataElement : SVGElement {
};

interface SVGTitleElement : SVGElement {
};

interface SVGSymbolElement : SVGGraphicsElement {
};

SVGSymbolElement implements SVGFitToViewBox;

interface SVGUseElement : SVGGraphicsElement {
  [SameObject] readonly attribute SVGAnimatedLength x;
  [SameObject] readonly attribute SVGAnimatedLength y;
  [SameObject] readonly attribute SVGAnimatedLength width;
  [SameObject] readonly attribute SVGAnimatedLength height;
  [SameObject] readonly attribute SVGElement? instanceRoot;
  [SameObject] readonly attribute SVGElement? animatedInstanceRoot;
};

SVGUseElement implements SVGURIReference;


interface SVGUseElementShadowRoot : ShadowRoot {

};

[NoInterfaceObject]
interface SVGElementInstance {
  [SameObject] readonly attribute SVGElement? correspondingElement;
  [SameObject] readonly attribute SVGUseElement? correspondingUseElement;
};


[Constructor (Animation source, Animatable newTarget) ]
interface ShadowAnimation : Animation {
  [SameObject] readonly attribute Animation sourceAnimation;
};

interface SVGSwitchElement : SVGGraphicsElement {
};

[NoInterfaceObject]
interface GetSVGDocument {
  Document getSVGDocument();
};

interface SVGStyleElement : SVGElement {
  attribute DOMString type;
  attribute DOMString media;
  attribute DOMString title;
};

SVGStyleElement implements LinkStyle;

interface SVGTransform {

  // Transform Types
  const unsigned short SVG_TRANSFORM_UNKNOWN = 0;
  const unsigned short SVG_TRANSFORM_MATRIX = 1;
  const unsigned short SVG_TRANSFORM_TRANSLATE = 2;
  const unsigned short SVG_TRANSFORM_SCALE = 3;
  const unsigned short SVG_TRANSFORM_ROTATE = 4;
  const unsigned short SVG_TRANSFORM_SKEWX = 5;
  const unsigned short SVG_TRANSFORM_SKEWY = 6;

  readonly attribute unsigned short type;
  [SameObject] readonly attribute DOMMatrix matrix;
  readonly attribute float angle;

  void setMatrix(DOMMatrixReadOnly matrix);
  void setTranslate(float tx, float ty);
  void setScale(float sx, float sy);
  void setRotate(float angle, float cx, float cy);
  void setSkewX(float angle);
  void setSkewY(float angle);
};

interface SVGTransformList {

  readonly attribute unsigned long length;
  readonly attribute unsigned long numberOfItems;

  void clear();
  SVGTransform initialize(SVGTransform newItem);
  getter SVGTransform getItem(unsigned long index);
  SVGTransform insertItemBefore(SVGTransform newItem, unsigned long index);
  SVGTransform replaceItem(SVGTransform newItem, unsigned long index);
  SVGTransform removeItem(unsigned long index);
  SVGTransform appendItem(SVGTransform newItem);
  setter void (unsigned long index, SVGTransform newItem);

  // Additional methods not common to other list interfaces.
  SVGTransform createSVGTransformFromMatrix(DOMMatrixReadOnly matrix);
  SVGTransform? consolidate();
};

interface SVGAnimatedTransformList {
  [SameObject] readonly attribute SVGTransformList baseVal;
  [SameObject] readonly attribute SVGTransformList animVal;
};

interface SVGPreserveAspectRatio {

  // Alignment Types
  const unsigned short SVG_PRESERVEASPECTRATIO_UNKNOWN = 0;
  const unsigned short SVG_PRESERVEASPECTRATIO_NONE = 1;
  const unsigned short SVG_PRESERVEASPECTRATIO_XMINYMIN = 2;
  const unsigned short SVG_PRESERVEASPECTRATIO_XMIDYMIN = 3;
  const unsigned short SVG_PRESERVEASPECTRATIO_XMAXYMIN = 4;
  const unsigned short SVG_PRESERVEASPECTRATIO_XMINYMID = 5;
  const unsigned short SVG_PRESERVEASPECTRATIO_XMIDYMID = 6;
  const unsigned short SVG_PRESERVEASPECTRATIO_XMAXYMID = 7;
  const unsigned short SVG_PRESERVEASPECTRATIO_XMINYMAX = 8;
  const unsigned short SVG_PRESERVEASPECTRATIO_XMIDYMAX = 9;
  const unsigned short SVG_PRESERVEASPECTRATIO_XMAXYMAX = 10;

  // Meet-or-slice Types
  const unsigned short SVG_MEETORSLICE_UNKNOWN = 0;
  const unsigned short SVG_MEETORSLICE_MEET = 1;
  const unsigned short SVG_MEETORSLICE_SLICE = 2;

  attribute unsigned short align;
  attribute unsigned short meetOrSlice;
};

interface SVGAnimatedPreserveAspectRatio {
  [SameObject] readonly attribute SVGPreserveAspectRatio baseVal;
  [SameObject] readonly attribute SVGPreserveAspectRatio animVal;
};

interface SVGPathElement : SVGGeometryElement {

};

interface SVGRectElement : SVGGeometryElement {
  [SameObject] readonly attribute SVGAnimatedLength x;
  [SameObject] readonly attribute SVGAnimatedLength y;
  [SameObject] readonly attribute SVGAnimatedLength width;
  [SameObject] readonly attribute SVGAnimatedLength height;
  [SameObject] readonly attribute SVGAnimatedLength rx;
  [SameObject] readonly attribute SVGAnimatedLength ry;
};

interface SVGCircleElement : SVGGeometryElement {
  [SameObject] readonly attribute SVGAnimatedLength cx;
  [SameObject] readonly attribute SVGAnimatedLength cy;
  [SameObject] readonly attribute SVGAnimatedLength r;
};

interface SVGEllipseElement : SVGGeometryElement {
  [SameObject] readonly attribute SVGAnimatedLength cx;
  [SameObject] readonly attribute SVGAnimatedLength cy;
  [SameObject] readonly attribute SVGAnimatedLength rx;
  [SameObject] readonly attribute SVGAnimatedLength ry;
};

interface SVGLineElement : SVGGeometryElement {
  [SameObject] readonly attribute SVGAnimatedLength x1;
  [SameObject] readonly attribute SVGAnimatedLength y1;
  [SameObject] readonly attribute SVGAnimatedLength x2;
  [SameObject] readonly attribute SVGAnimatedLength y2;
};

interface SVGMeshElement : SVGGeometryElement {};

SVGMeshElement implements SVGURIReference;

[NoInterfaceObject]
interface SVGAnimatedPoints {
  [SameObject] readonly attribute SVGPointList points;
  [SameObject] readonly attribute SVGPointList animatedPoints;
};

interface SVGPointList {

  readonly attribute unsigned long length;
  readonly attribute unsigned long numberOfItems;

  void clear();
  DOMPoint initialize(DOMPoint newItem);
  getter DOMPoint getItem(unsigned long index);
  DOMPoint insertItemBefore(DOMPoint newItem, unsigned long index);
  DOMPoint replaceItem(DOMPoint newItem, unsigned long index);
  DOMPoint removeItem(unsigned long index);
  DOMPoint appendItem(DOMPoint newItem);
  setter void (unsigned long index, DOMPoint newItem);
};

interface SVGPolylineElement : SVGGeometryElement {
};

SVGPolylineElement implements SVGAnimatedPoints;

interface SVGPolygonElement : SVGGeometryElement {
};

SVGPolygonElement implements SVGAnimatedPoints;

interface SVGTextContentElement : SVGGraphicsElement {

  // lengthAdjust Types
  const unsigned short LENGTHADJUST_UNKNOWN = 0;
  const unsigned short LENGTHADJUST_SPACING = 1;
  const unsigned short LENGTHADJUST_SPACINGANDGLYPHS = 2;

  [SameObject] readonly attribute SVGAnimatedLength textLength;
  [SameObject] readonly attribute SVGAnimatedEnumeration lengthAdjust;

  long getNumberOfChars();
  float getComputedTextLength();
  float getSubStringLength(unsigned long charnum, unsigned long nchars);
  DOMPoint getStartPositionOfChar(unsigned long charnum);
  DOMPoint getEndPositionOfChar(unsigned long charnum);
  DOMRect getExtentOfChar(unsigned long charnum);
  float getRotationOfChar(unsigned long charnum);
  long getCharNumAtPosition(DOMPoint point);
  void selectSubString(unsigned long charnum, unsigned long nchars);
};

interface SVGTextPositioningElement : SVGTextContentElement {
  [SameObject] readonly attribute SVGAnimatedLengthList x;
  [SameObject] readonly attribute SVGAnimatedLengthList y;
  [SameObject] readonly attribute SVGAnimatedLengthList dx;
  [SameObject] readonly attribute SVGAnimatedLengthList dy;
  [SameObject] readonly attribute SVGAnimatedNumberList rotate;
};

interface SVGTextElement : SVGTextPositioningElement {
};

interface SVGTSpanElement : SVGTextPositioningElement {
};

interface SVGTextPathElement : SVGTextContentElement {

  // textPath Method Types
  const unsigned short TEXTPATH_METHODTYPE_UNKNOWN = 0;
  const unsigned short TEXTPATH_METHODTYPE_ALIGN = 1;
  const unsigned short TEXTPATH_METHODTYPE_STRETCH = 2;

  // textPath Spacing Types
  const unsigned short TEXTPATH_SPACINGTYPE_UNKNOWN = 0;
  const unsigned short TEXTPATH_SPACINGTYPE_AUTO = 1;
  const unsigned short TEXTPATH_SPACINGTYPE_EXACT = 2;

  [SameObject] readonly attribute SVGAnimatedLength startOffset;
  [SameObject] readonly attribute SVGAnimatedEnumeration method;
  [SameObject] readonly attribute SVGAnimatedEnumeration spacing;
};

SVGTextPathElement implements SVGURIReference;

interface SVGImageElement : SVGGraphicsElement {
  [SameObject] readonly attribute SVGAnimatedLength x;
  [SameObject] readonly attribute SVGAnimatedLength y;
  [SameObject] readonly attribute SVGAnimatedLength width;
  [SameObject] readonly attribute SVGAnimatedLength height;
  [SameObject] readonly attribute SVGAnimatedPreserveAspectRatio preserveAspectRatio;
  attribute DOMString? crossOrigin;
};

SVGImageElement implements SVGURIReference;

interface SVGForeignObjectElement : SVGGraphicsElement {
  [SameObject] readonly attribute SVGAnimatedLength x;
  [SameObject] readonly attribute SVGAnimatedLength y;
  [SameObject] readonly attribute SVGAnimatedLength width;
  [SameObject] readonly attribute SVGAnimatedLength height;
};

interface SVGMarkerElement : SVGElement {

  // Marker Unit Types
  const unsigned short SVG_MARKERUNITS_UNKNOWN = 0;
  const unsigned short SVG_MARKERUNITS_USERSPACEONUSE = 1;
  const unsigned short SVG_MARKERUNITS_STROKEWIDTH = 2;

  // Marker Orientation Types
  const unsigned short SVG_MARKER_ORIENT_UNKNOWN = 0;
  const unsigned short SVG_MARKER_ORIENT_AUTO = 1;
  const unsigned short SVG_MARKER_ORIENT_ANGLE = 2;

  [SameObject] readonly attribute SVGAnimatedLength refX;
  [SameObject] readonly attribute SVGAnimatedLength refY;
  [SameObject] readonly attribute SVGAnimatedEnumeration markerUnits;
  [SameObject] readonly attribute SVGAnimatedLength markerWidth;
  [SameObject] readonly attribute SVGAnimatedLength markerHeight;
  [SameObject] readonly attribute SVGAnimatedEnumeration orientType;
  [SameObject] readonly attribute SVGAnimatedAngle orientAngle;
  attribute DOMString orient;

  void setOrientToAuto();
  void setOrientToAngle(SVGAngle angle);
};

SVGMarkerElement implements SVGFitToViewBox;

interface SVGSolidcolorElement : SVGElement {
};

interface SVGGradientElement : SVGElement {

  // Spread Method Types
  const unsigned short SVG_SPREADMETHOD_UNKNOWN = 0;
  const unsigned short SVG_SPREADMETHOD_PAD = 1;
  const unsigned short SVG_SPREADMETHOD_REFLECT = 2;
  const unsigned short SVG_SPREADMETHOD_REPEAT = 3;

  [SameObject] readonly attribute SVGAnimatedEnumeration gradientUnits;
  [SameObject] readonly attribute SVGAnimatedTransformList gradientTransform;
  [SameObject] readonly attribute SVGAnimatedEnumeration spreadMethod;
};

SVGGradientElement implements SVGURIReference;
SVGGradientElement implements SVGUnitTypes;

interface SVGLinearGradientElement : SVGGradientElement {
  [SameObject] readonly attribute SVGAnimatedLength x1;
  [SameObject] readonly attribute SVGAnimatedLength y1;
  [SameObject] readonly attribute SVGAnimatedLength x2;
  [SameObject] readonly attribute SVGAnimatedLength y2;
};

interface SVGRadialGradientElement : SVGGradientElement {
  [SameObject] readonly attribute SVGAnimatedLength cx;
  [SameObject] readonly attribute SVGAnimatedLength cy;
  [SameObject] readonly attribute SVGAnimatedLength r;
  [SameObject] readonly attribute SVGAnimatedLength fx;
  [SameObject] readonly attribute SVGAnimatedLength fy;
  [SameObject] readonly attribute SVGAnimatedLength fr;
};

interface SVGMeshGradientElement : SVGGradientElement {
};

interface SVGMeshrowElement : SVGElement {
};

interface SVGMeshpatchElement : SVGElement {
};

interface SVGStopElement : SVGElement {
  [SameObject] readonly attribute SVGAnimatedNumber offset;
};

interface SVGPatternElement : SVGElement {
  [SameObject] readonly attribute SVGAnimatedEnumeration patternUnits;
  [SameObject] readonly attribute SVGAnimatedEnumeration patternContentUnits;
  [SameObject] readonly attribute SVGAnimatedTransformList patternTransform;
  [SameObject] readonly attribute SVGAnimatedLength x;
  [SameObject] readonly attribute SVGAnimatedLength y;
  [SameObject] readonly attribute SVGAnimatedLength width;
  [SameObject] readonly attribute SVGAnimatedLength height;
};

SVGPatternElement implements SVGFitToViewBox;
SVGPatternElement implements SVGURIReference;
SVGPatternElement implements SVGUnitTypes;

interface SVGHatchElement : SVGElement {
};

interface SVGHatchpathElement : SVGElement {
};

interface SVGCursorElement : SVGElement {
  [SameObject] readonly attribute SVGAnimatedLength x;
  [SameObject] readonly attribute SVGAnimatedLength y;
};

SVGCursorElement implements SVGURIReference;

interface SVGScriptElement : SVGElement {
  attribute DOMString type;
  attribute DOMString? crossOrigin;
};

SVGScriptElement implements SVGURIReference;

interface SVGAElement : SVGGraphicsElement {
  [SameObject] readonly attribute SVGAnimatedString target;
  [SameObject] readonly attribute SVGAnimatedString download;
  [SameObject] readonly attribute SVGAnimatedString rel;
  [SameObject] readonly attribute SVGAnimatedString relList;
  [SameObject] readonly attribute SVGAnimatedString hreflang;
  [SameObject] readonly attribute SVGAnimatedString type;
};

SVGAElement implements SVGURIReference;
SVGAElement implements HTMLHyperlinkElementUtils;

interface SVGViewElement : SVGElement {};

SVGViewElement implements SVGFitToViewBox;
SVGViewElement implements SVGZoomAndPan;

