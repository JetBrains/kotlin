@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

fun failingAssert() = assert(false) { "KT89892AssertionMarker" }
