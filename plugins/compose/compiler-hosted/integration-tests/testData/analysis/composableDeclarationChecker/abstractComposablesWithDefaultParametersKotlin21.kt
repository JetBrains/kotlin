// RUN_PIPELINE_TILL: FRONTEND
// ALLOW_DANGEROUS_LANGUAGE_VERSION_TESTING
// LANGUAGE_VERSION: 2.1
// API_VERSION: 2.1

import androidx.compose.runtime.Composable
abstract class A {
    @Composable abstract fun foo(x: Int = 0)
}
