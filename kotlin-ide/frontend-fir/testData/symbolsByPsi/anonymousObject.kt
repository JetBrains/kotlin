class AnonymousContainer {
    val anonymousObject = object : Runnable {
        override fun run() {

        }
        val data = 123
    }
}

// SYMBOLS:
/*
KtFirFunctionSymbol:
  annotations: []
  callableIdIfNonLocal: <anonymous>.run
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: true
  isSuspend: false
  modality: FINAL
  name: run
  origin: SOURCE
  receiverTypeAndAnnotations: null
  symbolKind: MEMBER
  type: kotlin/Unit
  typeParameters: []
  valueParameters: []
  visibility: PUBLIC

KtFirKotlinPropertySymbol:
  annotations: []
  callableIdIfNonLocal: <anonymous>.data
  getter: KtFirPropertyGetterSymbol(<getter>)
  hasBackingField: true
  hasGetter: true
  hasSetter: false
  initializer: 123
  isConst: false
  isExtension: false
  isLateInit: false
  isOverride: false
  isVal: true
  modality: FINAL
  name: data
  origin: SOURCE
  receiverTypeAndAnnotations: null
  setter: null
  symbolKind: MEMBER
  type: kotlin/Int
  visibility: PUBLIC

KtFirAnonymousObjectSymbol:
  annotations: []
  origin: SOURCE
  superTypes: [java/lang/Runnable]
  symbolKind: LOCAL

KtFirKotlinPropertySymbol:
  annotations: []
  callableIdIfNonLocal: AnonymousContainer.anonymousObject
  getter: KtFirPropertyGetterSymbol(<getter>)
  hasBackingField: true
  hasGetter: true
  hasSetter: false
  initializer: KtUnsupportedConstantValue
  isConst: false
  isExtension: false
  isLateInit: false
  isOverride: false
  isVal: true
  modality: FINAL
  name: anonymousObject
  origin: SOURCE
  receiverTypeAndAnnotations: null
  setter: null
  symbolKind: MEMBER
  type: java/lang/Runnable
  visibility: PUBLIC

KtFirClassOrObjectSymbol:
  annotations: []
  classIdIfNonLocal: AnonymousContainer
  classKind: CLASS
  companionObject: null
  isInner: false
  modality: FINAL
  name: AnonymousContainer
  origin: SOURCE
  primaryConstructor: KtFirConstructorSymbol(<constructor>)
  superTypes: [kotlin/Any]
  symbolKind: TOP_LEVEL
  typeParameters: []
  visibility: PUBLIC
*/
