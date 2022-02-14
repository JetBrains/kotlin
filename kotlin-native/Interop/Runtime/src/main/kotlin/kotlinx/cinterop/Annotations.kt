/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.cinterop

/**
 * Marker for declarations that depend on numeric types of different bit width on at least two platforms.
 *
 * @param actualPlatformTypes: Contains platform types represented as `{konanTarget}: {type fqn}`
 * e.g. ["linux_x64: kotlin.Int", "linux_arm64: kotlin.Long"]
 */
@Suppress("unused") // Is emitted by the Commonizer
@Target(AnnotationTarget.TYPEALIAS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
annotation class UnsafeNumber(val actualPlatformTypes: Array<String>)