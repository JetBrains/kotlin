// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key

@Composable fun Test(a: String) {
    <!INLINE_FROM_HIGHER_PLATFORM, KEY_CALL_WITH_NO_ARGUMENTS!>key<!> { println(a) }
    <!INLINE_FROM_HIGHER_PLATFORM!>key<!>(a) { println(a) }
}
