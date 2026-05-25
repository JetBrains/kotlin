// RUN_PIPELINE_TILL: FRONTEND
// ALLOW_DANGEROUS_LANGUAGE_VERSION_TESTING
// LANGUAGE_VERSION: 2.2
// API_VERSION: 2.2

import androidx.compose.runtime.Composable
interface A {
    @Composable fun foo(x: Int = 0) {}
}
