// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.Composable

class Impl : <!MIXING_FUNCTIONAL_KINDS_IN_SUPERTYPES!>() -> Unit, @Composable (Int) -> Unit<!> {
    @Composable override <!CONFLICTING_OVERLOADS!>fun invoke()<!> {}
    @Composable override fun invoke(p0: Int) {}
}
