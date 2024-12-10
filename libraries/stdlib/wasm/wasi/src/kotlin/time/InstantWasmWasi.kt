/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

@ExperimentalTime
internal actual fun systemClockNow(): Instant = realtimeClockTimeGet().let { time ->
    // Instant.MAX and Instant.MIN are never going to be exceeded using just the Long number of nanoseconds
    Instant(time.floorDiv(NANOS_PER_SECOND.toLong()), time.mod(NANOS_PER_SECOND.toLong()).toInt())
}

@ExperimentalTime
internal actual fun serializedInstant(instant: Instant): Any =
    throw UnsupportedOperationException("Serialization is supported only in Kotlin/JVM")