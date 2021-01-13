class A {
  fun x(): Int
  fun y()
}

// SYMBOLS:
KtFirFunctionSymbol:
  annotatedType: [] kotlin/Int
  annotations: []
  callableIdIfNonLocal: A.x
  dispatchType: A
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: FINAL
  name: x
  origin: SOURCE
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: []
  visibility: PUBLIC

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Unit
  annotations: []
  callableIdIfNonLocal: A.y
  dispatchType: A
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: FINAL
  name: y
  origin: SOURCE
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: []
  visibility: PUBLIC

KtFirClassOrObjectSymbol:
  annotations: []
  classIdIfNonLocal: A
  classKind: CLASS
  companionObject: null
  isData: false
  isExternal: false
  isFun: false
  isInline: false
  isInner: false
  modality: FINAL
  name: A
  origin: SOURCE
  superTypes: [[] kotlin/Any]
  symbolKind: TOP_LEVEL
  typeParameters: []
  visibility: PUBLIC
