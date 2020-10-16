class A {
    val a: Int = 10
    fun x() = 10
}

// SYMBOLS:
/*
KtFirPropertySymbol:
  annotations: []
  callableIdIfNonLocal: A.a
  getter: KtFirPropertyGetterSymbol(<getter>)
  hasBackingField: true
  isConst: false
  isExtension: false
  isOverride: false
  isVal: true
  modality: FINAL
  name: a
  origin: SOURCE
  receiverType: null
  setter: null
  symbolKind: MEMBER
  type: kotlin/Int
  visibility: PUBLIC

KtFirFunctionSymbol:
  annotations: []
  callableIdIfNonLocal: A.x
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
  type: kotlin/Int
  typeParameters: []
  valueParameters: []
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
*/
