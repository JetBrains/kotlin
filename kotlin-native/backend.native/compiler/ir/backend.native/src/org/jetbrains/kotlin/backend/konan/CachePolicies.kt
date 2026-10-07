/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.config.nativeBinaryOptions.BinaryOptions
import org.jetbrains.kotlin.konan.config.NativeConfigurationKeys
import java.security.MessageDigest

private val cachePolicies: Map<CompilerConfigurationKey<*>, CachePolicy> =
        NativeConfigurationKeys.cachePolicies + BinaryOptions.all.associate { it.compilerConfigurationKey to it.cachePolicy }

private val policiesAffectingCaches = setOf(CachePolicy.AFFECTS_RUNTIME, CachePolicy.AFFECTS_CODEGEN)

private val keysPropagatedToCacheBuilds: List<CompilerConfigurationKey<*>> = cachePolicies
        .filterValues { it == CachePolicy.PROPAGATED || it in policiesAffectingCaches }
        .keys.sortedBy { it.toString() }

// The options accounted for in the fixed part of the cache flavor, see [NativeSecondStageCompilationConfig].
private val flavorPrefixKeys = setOf(NativeConfigurationKeys.KONAN_TARGET, NativeConfigurationKeys.DEBUG, NativeConfigurationKeys.OPTIMIZATION)

/** Returns a name of the first option which prevents using the caches, if any. */
internal fun CompilerConfiguration.findOptionIncompatibleWithCaches(): String? = cachePolicies.entries
        .filter { it.value == CachePolicy.INCOMPATIBLE }
        .map { it.key }
        .sortedBy { it.toString() }
        .firstOrNull { this[it] != null }?.toString()

/**
 * The options passed to the cache builds. The cache flavor is computed from the same values,
 * so a cache is always looked up by the values of the options it is built with.
 *
 * The options which the compiler validates or adjusts are to be declared explicitly with their resulting values (see [option]).
 * All other options which are to be propagated according to their [CachePolicy], are passed as is from [configuration].
 */
internal class CacheBuildOptions(private val configuration: CompilerConfiguration) {
    private class Option<T : Any>(
            val key: CompilerConfigurationKey<T>,
            val value: T?,
            val default: T?,
            val flavorValue: ((T) -> String)?,
    ) {
        fun setUp(cacheBuildConfiguration: CompilerConfiguration) = cacheBuildConfiguration.putIfNotNull(key, value)

        fun flavor(policies: Set<CachePolicy>): String? {
            if (flavorValue == null || cachePolicies[key] !in policies) return null
            if (value == null || value == default) return null
            return "-$key=${flavorValue(value)}"
        }
    }

    private val declaredOptions = mutableListOf<Option<*>>()
    private val declaredKeys = mutableSetOf<CompilerConfigurationKey<*>>()

    private fun declare(option: Option<*>) {
        require(declaredKeys.add(option.key)) { "Option ${option.key} is declared twice" }
        declaredOptions += option
    }

    /**
     * Declares an option affecting the caches. It is a part of the cache flavor when its [value] differs from the [default]
     * (otherwise the flavor of the caches shipped with the distribution would change).
     */
    fun <T : Any> option(key: CompilerConfigurationKey<T>, value: T?, default: T?, flavorValue: (T) -> String = ::renderFlavorValue) {
        require(cachePolicies[key] in policiesAffectingCaches) { "Option $key doesn't affect the caches according to its policy" }
        declare(Option(key, value, default, flavorValue))
    }

    /** Declares an option which is passed to the cache builds, but isn't a part of the options part of the cache flavor. */
    fun <T : Any> propagate(key: CompilerConfigurationKey<T>, value: T?) {
        require(cachePolicies[key] == CachePolicy.PROPAGATED || key in flavorPrefixKeys) {
            "Option $key affects the caches according to its policy, declare it with `option`"
        }
        declare(Option(key, value, default = null, flavorValue = null))
    }

    fun setUp(cacheBuildConfiguration: CompilerConfiguration) {
        declaredOptions.forEach { it.setUp(cacheBuildConfiguration) }
        for (key in keysPropagatedToCacheBuilds) {
            if (key in declaredKeys) continue
            configuration[key]?.let { cacheBuildConfiguration.put(key, it) }
        }
    }

    /**
     * Returns the part of the cache flavor accounting for the options with the [policy].
     * It is empty for the default options.
     */
    fun flavor(vararg policy: CachePolicy): String = buildString {
        val policies = setOf(*policy)
        declaredOptions.forEach { option -> option.flavor(policies)?.let(::append) }
        for (key in keysPropagatedToCacheBuilds) {
            if (key in declaredKeys || cachePolicies[key] !in policies) continue
            val value = when (val value = configuration[key]) {
                is Collection<*> -> value.takeIf { it.isNotEmpty() }
                is Map<*, *> -> value.takeIf { it.isNotEmpty() }
                else -> value
            } ?: continue
            append("-$key=${renderFlavorValue(value)}")
        }
    }
}

// Strings, lists, etc. might be long or contain characters not allowed in the file names, so use their hashes instead.
private fun renderFlavorValue(value: Any): String = when (value) {
    is Enum<*> -> value.name
    is Boolean, is Number, is UInt -> value.toString()
    else -> {
        MessageDigest.getInstance("SHA-256").digest(value.toString().toByteArray())
                .take(4).joinToString("") { "%02x".format(it) }
    }
}
