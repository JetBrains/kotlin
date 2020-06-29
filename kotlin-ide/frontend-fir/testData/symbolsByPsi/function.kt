fun foo(x: Int) {}

// SYMBOLS:
/*
KtFirFunctionValueParameterSymbol:
  isVararg: false
  name: x
  origin: SOURCE
  symbolKind: LOCAL
  type: kotlin/Int

KtFirFunctionSymbol:
  fqName: foo
  isExtension: false
  isOperator: false
  isSuspend: false
  modality: FINAL
  name: foo
  origin: SOURCE
  symbolKind: TOP_LEVEL
  type: kotlin/Unit
  typeParameters: []
  valueParameters: [KtFirFunctionValueParameterSymbol(x)]
*/
