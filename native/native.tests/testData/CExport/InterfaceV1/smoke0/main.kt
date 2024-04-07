@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

import kotlin.native.CName

@CName("foo")
fun foo(): Int {
    return 42
}