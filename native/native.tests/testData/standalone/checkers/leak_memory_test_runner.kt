// KIND: REGULAR
// FREE_COMPILER_ARGS: -opt-in=kotlin.experimental.ExperimentalNativeApi,kotlinx.cinterop.ExperimentalForeignApi

import kotlin.test.*

import kotlinx.cinterop.*
import kotlin.native.Platform

@BeforeTest
fun enableMemoryChecker() {
    Platform.isMemoryLeakCheckerActive = true
}

@Test
fun test() {
    StableRef.create(Any())
}
