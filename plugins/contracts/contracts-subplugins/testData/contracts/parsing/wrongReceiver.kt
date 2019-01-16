// !LANGUAGE: +ContextualEffects +UseCallsInPlaceEffect +AllowContractsForCustomFunctions +UseReturnsEffect
// !DIAGNOSTICS: -EXPERIMENTAL_API_USAGE_ERROR -DATA_CLASS_WITHOUT_PARAMETERS -CONTRACT_NOT_ALLOWED
// !RENDER_DIAGNOSTICS_MESSAGES

import kotlin.contracts.*
import org.jetbrains.kotlin.contracts.contextual.*
import org.jetbrains.kotlin.contracts.contextual.safebuilders.*

class A

fun bar() {}

fun fooGood(init: A.() -> Unit) {
    contract {
        expectsTo(init, CallKind(::bar, InvocationKind.EXACTLY_ONCE, receiverOf(init)))
    }
}

fun fooBad(init: () -> Unit) {
    contract {
        expectsTo(init, CallKind(::bar, InvocationKind.EXACTLY_ONCE, receiverOf(<!ERROR_IN_CONTRACT_DESCRIPTION(Argument of receiverOf must be lambda with receiver)!>init<!>)))
    }
}