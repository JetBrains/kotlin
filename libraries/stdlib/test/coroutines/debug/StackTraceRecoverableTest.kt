package test.coroutines.debug

import kotlin.coroutines.debug.StackTraceRecoverable
import kotlin.coroutines.ExperimentalStdlibCoroutineSupportApi
import kotlin.test.*

class StackTraceRecoverableTest {
    @Test
    fun testImplementingInCustomThrowable() {
        @OptIn(ExperimentalStdlibCoroutineSupportApi::class)
        class BadResponseCodeException private constructor(
            val responseCode: Int,
            cause: Throwable?
        ): Exception(cause), StackTraceRecoverable<BadResponseCodeException> {
            constructor(responseCode: Int): this(responseCode, null)
            override fun copyForStackTraceRecovery(): BadResponseCodeException =
                BadResponseCodeException(responseCode, this)
        }
    }
}
