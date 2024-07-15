import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import org.jetbrains.kotlin.formver.plugin.NeverConvert

@NeverConvert
public inline fun <R> copiedRun(block: () -> R): R = block()

@NeverConvert
public inline fun <T, R> T.copiedRun(block: T.() -> R): R = block()

@NeverConvert
public inline fun intRun(block: () -> Int): Int = block()

@NeverConvert
public inline fun equalsThree(block: () -> Int): Boolean {
    val result = block()
    return result == 3
}

@NeverConvert
public inline fun equalsThreeParametrized(block: (Int) -> Int): Boolean {
    val result = block(1)
    return result == 3
}

@NeverConvert
public inline fun equalsThreeExtension(block: Int.() -> Int): Boolean {
    val result = 1.block()
    return result == 3
}

@NeverConvert
public inline fun doubleEqualsThree(block: Int.() -> Int): Boolean {
    val result = 1.block().block()
    return result == 3
}

@NeverConvert
public inline fun Int.doubleIntRun(block: Int.() -> Int): Int = block().block()

@OptIn(ExperimentalContracts::class)
public fun <!VIPER_TEXT!>useRun<!>(): Boolean {
    contract {
        returns(true)
    }
    val one = 1
    val two = 2
    val three = 3
    val genericResult = copiedRun { 1 } + copiedRun { 2 } == copiedRun { 3 }
    val capturedResult = copiedRun { 1 } + copiedRun { 2 } == copiedRun { 3 }
    val intResult = intRun { 1 } + intRun { 2 } == intRun { 3 }
//    val stdlibResult = run { 1 } + run { 2 } == run { 3 }
    val doubleIntRunResult = 1.doubleIntRun { plus(1) } == 3
    val genericReceiverResult = 1.copiedRun { plus(2) } == 3

    return intResult
            && genericResult
//            && stdlibResult
            && capturedResult
            && equalsThree { 1 + 2 }
            && !equalsThree { 4 }
            && equalsThreeParametrized { it + 2 }
            && !equalsThreeParametrized { arg -> arg }
            && equalsThreeExtension { this + 2 }
            && equalsThreeExtension { plus(2) }
            && doubleEqualsThree { plus(1) }
            && doubleIntRunResult
            && genericReceiverResult
}

@NeverConvert
public inline fun <T> Boolean.ifTrue(block: () -> T?): T? = if (this) block() else null

@OptIn(ExperimentalContracts::class)
public fun <!VIPER_TEXT!>complexScenario<!>(arg: Boolean): Boolean {
    contract {
        returns(true) implies arg
        returns(false) implies !arg
    }

    return arg.ifTrue {
        equalsThreeParametrized {
            it.copiedRun {
                plus(1) // unused
                plus(1) // unused
                doubleIntRun { // receiver is `it` (== 1 in `equalsThreeParametrized`)
                    plus(1) // unused
                    plus(1)
                }
            }
        }
    } ?: copiedRun { // run with no receiver
        equalsThreeExtension {
            copiedRun { // run with receiver
                plus(1).copiedRun { // receiver is `this`
                    plus(1).copiedRun {
                        plus(1)
                    }
                }
            }
        }
    }
}

class CustomClass {
    val member: Int = 42

    @NeverConvert
    inline fun <T> memberRun(block: CustomClass.() -> T): T = block()
}

@NeverConvert
inline fun <T> CustomClass.extensionRun(block: CustomClass.() -> T): T = block()

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>testCustomClass<!>(): Boolean {
    contract {
        returns(true)
    }
    val custom = CustomClass()
    return custom.memberRun { member } == custom.extensionRun { member }
}