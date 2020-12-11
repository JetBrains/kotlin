class A {
  val x: Int = 10
  val Int.y get() = this
}

// SYMBOLS:
KtFirKotlinPropertySymbol:
  annotations: []
  callableIdIfNonLocal: A.x
  getter: KtFirPropertyGetterSymbol(<getter>)
  hasBackingField: true
  hasGetter: true
  hasSetter: false
  initializer: 10
  isConst: false
  isExtension: false
  isLateInit: false
  isOverride: false
  isVal: true
  modality: FINAL
  name: x
  origin: SOURCE
  receiverTypeAndAnnotations: null
  setter: null
  symbolKind: MEMBER
  type: kotlin/Int
  visibility: PUBLIC

KtFirPropertyGetterSymbol:
  annotations: []
  hasBody: true
  isDefault: false
  isInline: false
  isOverride: false
  modality: FINAL
  origin: SOURCE
  symbolKind: TOP_LEVEL
  type: kotlin/Int
  visibility: PUBLIC

KtFirKotlinPropertySymbol:
  annotations: []
  callableIdIfNonLocal: A.y
  getter: KtFirPropertyGetterSymbol(<getter>)
  hasBackingField: false
  hasGetter: true
  hasSetter: false
  initializer: null
  isConst: false
  isExtension: true
  isLateInit: false
  isOverride: false
  isVal: true
  modality: FINAL
  name: y
  origin: SOURCE
  receiverTypeAndAnnotations: [] kotlin/Int
  setter: null
  symbolKind: MEMBER
  type: kotlin/Int
  visibility: PUBLIC

KtFirClassOrObjectSymbol:
  annotations: []
  classIdIfNonLocal: A
  classKind: CLASS
  companionObject: null
  isInner: false
  modality: FINAL
  name: A
  origin: SOURCE
  primaryConstructor: KtFirConstructorSymbol(<constructor>)
  superTypes: [kotlin/Any]
  symbolKind: TOP_LEVEL
  typeParameters: []
  visibility: PUBLIC
