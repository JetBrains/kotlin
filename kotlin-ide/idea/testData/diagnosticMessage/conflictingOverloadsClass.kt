// !DIAGNOSTICS_NUMBER: 2
// !DIAGNOSTICS: CONFLICTING_OVERLOADS

class conflictingOverloads {
    fun lol(x: Int) = x
    fun lol(y: Int) = y
}
