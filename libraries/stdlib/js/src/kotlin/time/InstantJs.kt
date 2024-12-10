/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

import kotlin.js.Date

@ExperimentalTime
internal actual fun systemClockNow(): Instant =
    Instant.fromEpochMilliseconds(Date().getTime().toLong())

@ExperimentalTime
internal actual fun serializedInstant(instant: Instant): Any =
    throw UnsupportedOperationException("Serialization is supported only in Kotlin/JVM")
