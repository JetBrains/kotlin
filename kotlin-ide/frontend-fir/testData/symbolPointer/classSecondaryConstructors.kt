class A() {
    constructor(x: Int): this()
    constructor(y: Int, z: String) : this(y)
}

// SYMBOLS:
KtFirConstructorSymbol:
  isPrimary: true
  origin: SOURCE
  owner: KtFirClassOrObjectSymbol(A)
  ownerClassId: A
  symbolKind: MEMBER
  type: A
  valueParameters: []

KtFirFunctionValueParameterSymbol:
  isVararg: false
  name: x
  origin: SOURCE
  symbolKind: LOCAL
  type: kotlin/Int

KtFirConstructorSymbol:
  isPrimary: false
  origin: SOURCE
  owner: KtFirClassOrObjectSymbol(A)
  ownerClassId: A
  symbolKind: MEMBER
  type: A
  valueParameters: Could not render due to java.lang.ClassCastException: org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirFunctionValueParameterSymbol cannot be cast to org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirConstructorValueParameterSymbol

KtFirFunctionValueParameterSymbol:
  isVararg: false
  name: y
  origin: SOURCE
  symbolKind: LOCAL
  type: kotlin/Int

KtFirFunctionValueParameterSymbol:
  isVararg: false
  name: z
  origin: SOURCE
  symbolKind: LOCAL
  type: kotlin/String

KtFirConstructorSymbol:
  isPrimary: false
  origin: SOURCE
  owner: KtFirClassOrObjectSymbol(A)
  ownerClassId: A
  symbolKind: MEMBER
  type: A
  valueParameters: Could not render due to java.lang.ClassCastException: org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirFunctionValueParameterSymbol cannot be cast to org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirConstructorValueParameterSymbol

KtFirClassOrObjectSymbol:
  classId: A
  classKind: CLASS
  modality: FINAL
  name: A
  origin: SOURCE
  symbolKind: TOP_LEVEL
  typeParameters: []
