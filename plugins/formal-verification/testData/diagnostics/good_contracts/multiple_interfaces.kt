import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import org.jetbrains.kotlin.formver.plugin.NeverVerify
import org.jetbrains.kotlin.formver.plugin.NeverConvert

/*
All parent classes are enumerated in this test to
keep names of implementation classes laconic.
 */

interface InterfaceWithImplementation1 {
    val field: Int
        get() {
            return 0
        }
}

interface InterfaceWithoutImplementation2 {
    val field: Int
}

interface AnyInterfaceWithoutImplementation5 {
    val field: Any
}

interface InheritingInterfaceWithoutImplementation6:
    AnyInterfaceWithoutImplementation5, InterfaceWithoutImplementation2

abstract class AbstractWithFinalImplementation3 {
    val field: Int = 0
}

abstract class AbstractWithOpenImplementation4 {
    open val field: Int = 0
}

fun <!VIPER_TEXT!>take1<!>(obj: InterfaceWithImplementation1) {
    obj.field
}

fun <!VIPER_TEXT!>take2<!>(obj: InterfaceWithoutImplementation2) {
    obj.field
}

fun <!VIPER_TEXT!>take3<!>(obj: AbstractWithFinalImplementation3) {
    obj.field
}

fun <!VIPER_TEXT!>take4<!>(obj: AbstractWithOpenImplementation4) {
    obj.field
}

class Impl12: InterfaceWithImplementation1, InterfaceWithoutImplementation2 {
    override val field: Int = 0
}

class Impl3: AbstractWithFinalImplementation3()
class Impl23: InheritingInterfaceWithoutImplementation6, AbstractWithFinalImplementation3()

class Impl24: InterfaceWithoutImplementation2, AbstractWithOpenImplementation4()

class Impl14: InterfaceWithImplementation1, AbstractWithOpenImplementation4() {
    override val field: Int = 0
}

@NeverConvert
fun create6() = object : InheritingInterfaceWithoutImplementation6 {
    override val field: Int = 0
}

@OptIn(ExperimentalContracts::class)
@Suppress("USELESS_IS_CHECK")
fun <!VIPER_TEXT!>createImpls<!>(): Boolean{
    contract {
        returns(false) implies false
    }
    val impl12 = Impl12()
    val start12 = impl12.field + 1 - 1
    take1(impl12)
    take2(impl12)

    val impl23 = Impl23()
    val start23 = impl23.field + 1 - 1
    take2(impl23)
    take3(impl23)

    val impl3 = Impl3()
    val start3 = impl3.field + 1 - 1
    take3(impl3)

    //TODO: it seems that we should be able to prove start == finish here
    val impl24 = Impl24()
    val start24 = impl24.field + 1 - 1
    take2(impl24)
    take4(impl24)

    val impl14 = Impl14()
    val start14 = impl14.field + 1 - 1
    take1(impl14)
    take4(impl14)

    val impl6 = create6()
    val start6 = impl6.field + 1 - 1

    return true &&
//        start12 == impl12.field &&
//        start23 == impl23.field &&
//        start3 == impl3.field &&
        start14 == impl14.field &&
//        start6 is Int &&
        true
}

