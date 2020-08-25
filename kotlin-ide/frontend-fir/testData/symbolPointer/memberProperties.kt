class A {
    val x: Int = 10
    val Int.y get() = this
}

// SYMBOLS:
KtFirPropertySymbol:
  callableIdIfNonLocal: A.x
  isExtension: false
  isVal: true
  modality: FINAL
  name: x
  origin: SOURCE
  receiverType: null
  symbolKind: MEMBER
  type: kotlin/Int

KtFirPropertyGetterSymbol:
  isDefault: false
  modality: FINAL
  origin: SOURCE
  type: kotlin/Int

KtFirPropertySymbol:
  callableIdIfNonLocal: A.y
  isExtension: true
  isVal: true
  modality: FINAL
  name: y
  origin: SOURCE
  receiverType: kotlin/Int
  symbolKind: MEMBER
  type: kotlin/Int

KtFirClassOrObjectSymbol:
  classIdIfNonLocal: A
  classKind: CLASS
  modality: FINAL
  name: A
  origin: SOURCE
  symbolKind: TOP_LEVEL
  typeParameters: []
