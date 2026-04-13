// RETURN_VALUE_CHECKER_MODE: CHECKER

@file:OptIn(ExperimentalContracts::class)

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.contracts.ExperimentalContracts

fun <T> returnsResultOfContract(block: () -> T): T {
    contract {
        returnsResultOf(block)
    }
    return block()
}
