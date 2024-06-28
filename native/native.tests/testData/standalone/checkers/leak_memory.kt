@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, ExperimentalForeignApi::class)

import kotlinx.cinterop.*
import kotlin.native.Platform

fun main() {
    Platform.isMemoryLeakCheckerActive = true
    StableRef.create(Any())
}
