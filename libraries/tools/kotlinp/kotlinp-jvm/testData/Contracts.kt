@file:OptIn(ExperimentalContracts::class)

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.contracts.ExperimentalContracts

fun returnsTrue(condition: Boolean) {
    contract {
        returns(true) implies (condition)
    }
}

fun returnsNull(condition: Boolean) {
    contract {
        returns(null) implies (condition)
    }
}

fun returnsNotNull(condition: Boolean) {
    contract {
        returnsNotNull() implies (condition)
    }
}

fun Any?.receiverIsNotNull(): Boolean {
    contract {
        returns(true) implies (this@receiverIsNotNull != null)
    }
    return this != null
}

inline fun callsInPlaceAtMostOnce(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
}

inline fun callsInPlaceUnknown(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.UNKNOWN)
    }
}

fun conjunction(a: Boolean, b: Boolean, c: Boolean) {
    contract {
        returns() implies (a && !b && c)
    }
}

fun disjunction(a: Boolean, b: Boolean, c: Boolean) {
    contract {
        returns() implies (a || !b || c)
    }
}

fun complexBoolean(a: Any?, b: Any?, c: Any?, d: Any?) {
    contract {
        returns() implies ((a != null && c != null) || (b == null && d != null))
    }
}

fun negatedIsAndConjunction(a: Any?, b: Boolean, c: Any?) {
    contract {
        returns() implies (a !is List<*> && b && c == null)
    }
}
