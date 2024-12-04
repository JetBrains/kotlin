// FIR_IDENTICAL
annotation class NoArg

open class Base(val s: String)

@NoArg
class <!NO_NOARG_CONSTRUCTOR_IN_SUPERCLASS!>Derived<!>(s: String) : Base(s)
