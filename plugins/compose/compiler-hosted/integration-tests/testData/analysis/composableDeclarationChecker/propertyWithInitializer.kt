// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.Composable

val <!COMPOSABLE_PROPERTY_BACKING_FIELD!>bar<!>: Int = 123
    @Composable get() = field
