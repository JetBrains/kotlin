val x: Int = 10
val Int.y = this

// SYMBOLS:
KtFirPropertySymbol:
  callableIdIfNonLocal: x
  isExtension: false
  isVal: true
  modality: FINAL
  name: x
  origin: SOURCE
  receiverType: kotlin/Int
  symbolKind: TOP_LEVEL
  type: kotlin/Int

KtFirPropertySymbol:
  callableIdIfNonLocal: y
  isExtension: true
  isVal: true
  modality: FINAL
  name: y
  origin: SOURCE
  receiverType: ERROR CLASS: Unresolved this@null
  symbolKind: TOP_LEVEL
  type: ERROR CLASS: Unresolved this@null
