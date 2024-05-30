import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

open class Base {
    open val field: Int? = null
}

open class OpenClassOpenFieldVarDerived: Base() {
    override var field: Int = 0
}

class FinalClassOpenFieldVarDerived: Base() {
    override var field: Int = 0
}

open class OpenClassFinalFieldVarDerived: Base() {
    final override var field: Int = 0
}

class FinalClassFinalFieldValDerived: Base() {
    final override val field: Int = 0
}

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>extractInt<!>(base: Base, returnNull: Boolean): Int? {
    contract {
        returns(null) implies returnNull
    }
    return if (returnNull) null
    else if (base is OpenClassOpenFieldVarDerived) base.field
    else if (base is FinalClassOpenFieldVarDerived) base.field
    else if (base is FinalClassFinalFieldValDerived) base.field
    else if (base is OpenClassFinalFieldVarDerived) base.field
    else 0
}


