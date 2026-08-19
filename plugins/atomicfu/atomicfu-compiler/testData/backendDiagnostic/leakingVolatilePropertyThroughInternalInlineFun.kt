// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-87521
// DIAGNOSTICS: -NOTHING_TO_INLINE
// RENDER_IR_DIAGNOSTICS_FULL_TEXT

import kotlinx.atomicfu.*

object Error {
    private val pa = atomic(0)

    internal inline fun update() {
        <!IR_PRIVATE_CALLABLE_REFERENCED_BY_NON_PRIVATE_INLINE_FUNCTION_CASCADING_ERROR!>pa<!>.update { it + 1 }
    }
}
