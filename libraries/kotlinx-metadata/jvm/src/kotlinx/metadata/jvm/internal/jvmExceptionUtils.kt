/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE") // InconsistentKotlinMetadataException

package kotlinx.metadata.jvm.internal

import kotlinx.metadata.InconsistentKotlinMetadataException

internal fun Metadata.requireNotEmpty(): Array<String> = data1.takeIf(Array<*>::isNotEmpty)
    ?: throw InconsistentKotlinMetadataException("Metadata is missing: kotlin.Metadata.data1 must not be an empty array")

internal inline fun <T> wrapIntoMetadataExceptionWhenNeeded(block: () -> T): T {
    return try {
        block()
    } catch (e: Throwable) {
        throw when (e) {
            is IllegalArgumentException -> e // rethrow IAE as it is already correct exception type
            is VirtualMachineError, is ThreadDeath -> e // rethrow VM errors
            // other exceptions, like IOOBE or InvalidProtocolBufferException from proto parser
            else -> InconsistentKotlinMetadataException("Exception occurred when reading Kotlin metadata", e)
        }
    }
}

internal inline fun <T> wrapWriteIntoIAE(block: () -> T): T {
    return try {
        block()
    } catch (e: Throwable) {
        throw when (e) {
            is IllegalArgumentException -> e // rethrow IAE as it is already correct exception type
            is VirtualMachineError, is ThreadDeath -> e // rethrow VM errors
            // lateinit vars or proto writer can throw exceptions if required data is missing
            else -> IllegalArgumentException("Kotlin metadata is not correct and can not be written", e)
        }
    }
}