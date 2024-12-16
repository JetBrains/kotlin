/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters

/**
 * [Value sources][ValueSource] implementing this abstract class makes their value of type [T] be opaque to Configuration Cache.
 * i.e. Gradle will ignore any changes of [T] when checking configuration cache up-to-date state
 */
internal abstract class ConfigurationCacheOpaqueValueSource<T> : ValueSource<ConfigurationCacheOpaqueValue<T>, ValueSourceParameters.None> {
    abstract fun obtainValue(): T

    final override fun obtain(): ConfigurationCacheOpaqueValue<T> = ConfigurationCacheOpaqueValue(
        value = obtainValue(),
        builtBy = this.javaClass
    )
}

internal class ConfigurationCacheOpaqueValue<T>(
    val value: T,
    private val builtBy: Class<*>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConfigurationCacheOpaqueValue<*>

        return builtBy == other.builtBy
    }

    override fun hashCode(): Int {
        var result = builtBy.hashCode()
        return result
    }
}