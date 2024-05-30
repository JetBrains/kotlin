import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

open class A {
    private var field: Boolean = false
        get() {
            return !field
        }

    fun <!VIPER_TEXT!>getBooleanField<!>() = field
}

open class B : A() {
    private val field: String = ""

    fun <!VIPER_TEXT!>getStringField<!>() = field
}

class C : B() {
    var field: Int = 0
}

class D: B() {
    val field: Int = 0
}

@OptIn(ExperimentalContracts::class)
@Suppress("USELESS_IS_CHECK")
fun <!VIPER_TEXT!>extractPublic<!>(): Boolean {
    contract {
        returns(false) implies false
    }
    return C().field is Int && D().field is Int
}