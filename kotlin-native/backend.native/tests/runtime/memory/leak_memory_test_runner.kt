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
