// DUMP_IR

@file:OptIn(ExperimentalStdlibApi::class)
package example

import androidx.compose.runtime.Composable

class C {
    @Composable
    fun foo(
        a: Int,
        @IntroducedAt("1") b: String = "hello",
        @IntroducedAt("2") c: Boolean = true,
    ) = "$a/$b/$c"
}

@Composable
fun bar(
    a: Int,
    @IntroducedAt("1") b: String = "hello",
    @IntroducedAt("2") c: Boolean = true,
) = "$a/$b/$c"