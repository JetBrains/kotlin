// ISSUE: KT-65959

import org.jetbrains.kotlin.plugin.sandbox.MyComposable

@MyComposable
inline fun <T> inlineFunction(block: @MyComposable () -> T): T {
    return block()
}
