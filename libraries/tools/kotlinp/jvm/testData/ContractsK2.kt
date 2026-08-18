// RETURN_VALUE_CHECKER_MODE: CHECKER
// LANGUAGE: +AllowReturnsResultOfContract

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

fun <T> returnsParameterContract(value: T): T {
    contract {
        returnsParameter(value)
    }
    return value
}
