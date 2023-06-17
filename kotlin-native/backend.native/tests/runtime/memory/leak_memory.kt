@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

import kotlinx.cinterop.*
import kotlin.native.Platform

fun main() {
    Platform.isMemoryLeakCheckerActive = true
    StableRef.create(Any())
}
