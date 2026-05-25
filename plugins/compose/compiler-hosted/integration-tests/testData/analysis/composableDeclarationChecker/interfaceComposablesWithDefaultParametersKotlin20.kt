// RUN_PIPELINE_TILL: FRONTEND
// ALLOW_DANGEROUS_LANGUAGE_VERSION_TESTING
// LANGUAGE_VERSION: 2.0
// API_VERSION: 2.0

import androidx.compose.runtime.Composable
interface A {
    @Composable fun foo(x: Int = <!ABSTRACT_COMPOSABLE_DEFAULT_PARAMETER_VALUE!>0<!>)
}
