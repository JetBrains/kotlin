// !DIAGNOSTICS_NUMBER: 1
// !DIAGNOSTICS: CANNOT_INFER_VISIBILITY

interface T {
    internal val foo: String
}

interface U {
    protected val foo: String
}

interface V : T, U
