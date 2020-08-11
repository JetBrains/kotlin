enum class X {
    Y, Z;
}

// SYMBOLS:
KtFirEnumEntrySymbol:
  enumClassId: X
  name: Y
  origin: SOURCE
  symbolKind: MEMBER
  type: X

KtFirEnumEntrySymbol:
  enumClassId: X
  name: Z
  origin: SOURCE
  symbolKind: MEMBER
  type: X

KtFirClassOrObjectSymbol:
  classId: X
  classKind: ENUM_CLASS
  modality: FINAL
  name: X
  origin: SOURCE
  symbolKind: TOP_LEVEL
  typeParameters: []
