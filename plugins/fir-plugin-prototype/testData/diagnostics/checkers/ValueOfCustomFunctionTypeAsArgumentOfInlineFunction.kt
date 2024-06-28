// ISSUE: KT-65959

import org.jetbrains.kotlin.fir.plugin.MyComposable

@MyComposable
inline fun <T> inlineFunction(block: @MyComposable () -> T): T {
    return block()
}
