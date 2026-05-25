// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +MultiPlatformProjects

// MODULE: common
import androidx.compose.runtime.Composable
@Composable expect fun A()
expect fun B()

// MODULE: main()()(common)
import androidx.compose.runtime.Composable
actual fun <!MISMATCHED_COMPOSABLE_IN_EXPECT_ACTUAL!>A<!>() {}
@Composable actual fun <!MISMATCHED_COMPOSABLE_IN_EXPECT_ACTUAL!>B<!>() {}
