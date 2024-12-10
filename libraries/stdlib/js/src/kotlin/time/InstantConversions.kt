/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

import kotlin.js.Date

/**
 * Converts the [Instant] to an instance of JS [Date].
 *
 * The conversion is lossy: JS uses millisecond precision to represent dates, and [Instant] allows for nanosecond
 * resolution.
 */
@SinceKotlin("2.1")
@ExperimentalTime
public fun Instant.toJSDate(): Date = Date(milliseconds = toEpochMilliseconds().toDouble())

/**
 * Converts the JS [Date] to the corresponding [Instant].
 */
@SinceKotlin("2.1")
@ExperimentalTime
public fun Date.toKotlinInstant(): Instant = Instant.fromEpochMilliseconds(getTime().toLong())
