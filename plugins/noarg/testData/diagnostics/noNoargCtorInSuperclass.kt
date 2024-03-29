// KT-67036: NoArgIrTransformer crashes during Fir2IR after NO_NOARG_CONSTRUCTOR_IN_SUPERCLASS
// SKIP_FIR2IR
// FIR_IDENTICAL
annotation class NoArg

open class Base(val s: String)

@NoArg
class <!NO_NOARG_CONSTRUCTOR_IN_SUPERCLASS!>Derived<!>(s: String) : Base(s)
