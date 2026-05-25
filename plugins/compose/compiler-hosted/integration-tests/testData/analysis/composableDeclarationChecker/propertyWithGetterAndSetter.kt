// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.Composable

var <!COMPOSABLE_VAR!>bam2<!>: Int
    @Composable get() { return 123 }
    set(value) { print(value) }

var <!COMPOSABLE_VAR!>bam3<!>: Int
    @Composable get() { return 123 }
    <!WRONG_ANNOTATION_TARGET!>@Composable<!> set(value) { print(value) }

var <!COMPOSABLE_VAR!>bam4<!>: Int
    get() { return 123 }
    <!WRONG_ANNOTATION_TARGET!>@Composable<!> set(value) { print(value) }
