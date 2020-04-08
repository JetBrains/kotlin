// !DIAGNOSTICS_NUMBER: 1
// !DIAGNOSTICS: MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_WHEN_NO_EXPLICIT_OVERRIDE

interface C {
    fun foo(x: Int = 42)
}

interface D {
    fun foo(x: Int = 239) {}
}

class multipleDefaultsFromSupertypes : C, D
