// FIR_IDENTICAL
// FIR_DISABLE_LAZY_RESOLVE_CHECKS
annotation class NoArg

open class Base(val s: String)

@NoArg
class <!NO_NOARG_CONSTRUCTOR_IN_SUPERCLASS!>Derived<!>(s: String) : Base(s)
