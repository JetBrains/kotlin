/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("InstantConversionsJDK8Kt")
@file:JvmPackageName("kotlin.time.jdk8")

package kotlin.time


/**
 * Converts this [kotlin.time.Instant][Instant] value to a [java.time.Instant][java.time.Instant] value.
 *
 * @sample samples.time.Instants.toJavaInstant
 */
@SinceKotlin("2.1")
@ExperimentalTime
public fun Instant.toJavaInstant(): java.time.Instant =
    java.time.Instant.ofEpochSecond(epochSeconds, nanosecondsOfSecond.toLong())

/**
 * Converts this [java.time.Instant][java.time.Instant] value to a [kotlin.time.Instant][Instant] value.
 *
 * @sample samples.time.Instants.toKotlinInstant
 */
@SinceKotlin("2.1")
@ExperimentalTime
public fun java.time.Instant.toKotlinInstant(): Instant =
    Instant.fromEpochSeconds(epochSecond, nano)