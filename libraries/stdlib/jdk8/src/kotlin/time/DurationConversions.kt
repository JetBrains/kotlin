/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:JvmName("DurationConversionsJDK8Kt")
@file:kotlin.jvm.JvmPackageName("kotlin.time.jdk8")
package kotlin.time

/**
 * Converts [java.time.Duration][java.time.Duration] value to [kotlin.time.Duration][Duration] value.
 *
 * Durations less than 104 days are converted exactly, and durations greater than that can lose some precision
 * due to rounding.
 */
@SinceKotlin("1.6")
@WasExperimental(ExperimentalTime::class)
@kotlin.internal.InlineOnly
public inline fun java.time.Duration.toKotlinDuration(): Duration =
    this.seconds.toDuration(DurationUnit.SECONDS) + this.nano.toDuration(DurationUnit.NANOSECONDS)


/**
 * Converts [kotlin.time.Duration][Duration] value to [java.time.Duration][java.time.Duration] value.
 *
 * Durations greater than [Long.MAX_VALUE] seconds are cut to that value.
 */
@SinceKotlin("1.6")
@WasExperimental(ExperimentalTime::class)
@kotlin.internal.InlineOnly
public inline fun Duration.toJavaDuration(): java.time.Duration =
    toComponents { seconds, nanoseconds -> java.time.Duration.ofSeconds(seconds, nanoseconds.toLong()) }