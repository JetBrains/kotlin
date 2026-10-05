/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.config.nativeBinaryOptions.BinaryOptions
import org.jetbrains.kotlin.config.nativeBinaryOptions.CachePolicy
import org.jetbrains.kotlin.konan.config.NativeConfigurationKeys
import java.security.MessageDigest

/**
 * The [CachePolicy] of each of the [NativeConfigurationKeys] (the binary options declare their policies themselves).
 * Every key must be listed here, which is checked by a test.
 */
internal val nativeConfigurationKeysCachePolicies: Map<CompilerConfigurationKey<*>, CachePolicy> = with(NativeConfigurationKeys) {
    mapOf(
            CHECK_DEPENDENCIES to CachePolicy.NOT_PROPAGATED,
            DEBUG to CachePolicy.AFFECTS_CODEGEN,
            EMIT_LAZY_OBJC_HEADER_FILE to CachePolicy.NOT_PROPAGATED,
            ENABLE_ASSERTIONS to CachePolicy.AFFECTS_CODEGEN,
            KONAN_ENTRY_POINT to CachePolicy.NOT_PROPAGATED,
            EXPORTED_LIBRARIES to CachePolicy.NOT_PROPAGATED,
            FULL_EXPORTED_NAME_PREFIX to CachePolicy.NOT_PROPAGATED,
            KONAN_LIBRARY_TO_ADD_TO_CACHE to CachePolicy.NOT_PROPAGATED,
            CACHE_DIRECTORIES to CachePolicy.NOT_PROPAGATED,
            AUTO_CACHEABLE_FROM to CachePolicy.NOT_PROPAGATED,
            AUTO_CACHE_DIR to CachePolicy.NOT_PROPAGATED,
            INCREMENTAL_CACHE_DIR to CachePolicy.NOT_PROPAGATED,
            DUMP_BUILT_CACHES_TO to CachePolicy.NOT_PROPAGATED,
            CACHED_LIBRARIES to CachePolicy.NOT_PROPAGATED,
            FILES_TO_CACHE to CachePolicy.NOT_PROPAGATED,
            MAKE_PER_FILE_CACHE to CachePolicy.NOT_PROPAGATED,
            CACHED_LIBRARY_DEPENDENCIES_FINGERPRINT to CachePolicy.NOT_PROPAGATED,
            FRAMEWORK_IMPORT_HEADERS to CachePolicy.NOT_PROPAGATED,
            KONAN_FRIEND_LIBRARIES to CachePolicy.NOT_PROPAGATED,
            KONAN_REFINES_MODULES to CachePolicy.NOT_PROPAGATED,
            GENERATE_TEST_RUNNER to CachePolicy.NOT_PROPAGATED,
            KONAN_INCLUDED_BINARIES to CachePolicy.NOT_PROPAGATED,
            KONAN_HOME to CachePolicy.NOT_PROPAGATED,
            KONAN_LIBRARIES to CachePolicy.NOT_PROPAGATED,
            LIGHT_DEBUG to CachePolicy.AFFECTS_CODEGEN,
            GENERATE_DEBUG_TRAMPOLINE to CachePolicy.AFFECTS_CODEGEN,
            LINKER_ARGS to CachePolicy.NOT_PROPAGATED,
            LIST_TARGETS to CachePolicy.NOT_PROPAGATED,
            KONAN_MANIFEST_ADDEND to CachePolicy.NOT_PROPAGATED,
            KONAN_GENERATED_HEADER_KLIB_PATH to CachePolicy.NOT_PROPAGATED,
            // The native libraries are linked into the LLVM module being compiled,
            // so passing them to every cache build would link them into every cache.
            KONAN_NATIVE_LIBRARIES to CachePolicy.NOT_PROPAGATED,
            KONAN_NO_DEFAULT_LIBS to CachePolicy.NOT_PROPAGATED,
            NOMAIN to CachePolicy.NOT_PROPAGATED,
            KONAN_NO_STDLIB to CachePolicy.NOT_PROPAGATED,
            KONAN_DONT_COMPRESS_KLIB to CachePolicy.NOT_PROPAGATED,
            OPTIMIZATION to CachePolicy.AFFECTS_CODEGEN,
            KONAN_OUTPUT_PATH to CachePolicy.NOT_PROPAGATED,
            OVERRIDE_CLANG_OPTIONS to CachePolicy.AFFECTS_CODEGEN,
            ALLOCATION_MODE to CachePolicy.AFFECTS_RUNTIME,
            KONAN_PRINT_BITCODE to CachePolicy.NOT_PROPAGATED,
            KONAN_PRINT_IR to CachePolicy.NOT_PROPAGATED,
            KONAN_PRINT_FILES to CachePolicy.NOT_PROPAGATED,
            KONAN_PRODUCED_ARTIFACT_KIND to CachePolicy.NOT_PROPAGATED,
            RUNTIME_FILE to CachePolicy.AFFECTS_RUNTIME,
            KONAN_INCLUDED_LIBRARIES to CachePolicy.NOT_PROPAGATED,
            KONAN_SHORT_MODULE_NAME to CachePolicy.NOT_PROPAGATED,
            STATIC_FRAMEWORK to CachePolicy.NOT_PROPAGATED,
            KONAN_TARGET to CachePolicy.AFFECTS_CODEGEN,
            TEMPORARY_FILES_DIR to CachePolicy.NOT_PROPAGATED,
            SAVE_LLVM_IR to CachePolicy.NOT_PROPAGATED,
            VERIFY_BITCODE to CachePolicy.PROPAGATED,
            VERIFY_COMPILER to CachePolicy.PROPAGATED,
            KONAN_WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO to CachePolicy.NOT_PROPAGATED,
            DEBUG_INFO_VERSION to CachePolicy.AFFECTS_CODEGEN,
            OBJC_GENERICS to CachePolicy.NOT_PROPAGATED,
            // TODO: The prefix map affects the debug info in the caches, but its paths are specific to the build,
            //  so making it a part of the cache flavor would prevent sharing the caches between the projects.
            DEBUG_PREFIX_MAP to CachePolicy.NOT_PROPAGATED,
            PRE_LINK_CACHES to CachePolicy.NOT_PROPAGATED,
            // May change the toolchain (and so, the generated code), but is commonly used to tweak the environment
            // (e.g., to use a provisioned Xcode), so isn't a part of the cache flavor.
            OVERRIDE_KONAN_PROPERTIES to CachePolicy.PROPAGATED,
            PROPERTY_LAZY_INITIALIZATION to CachePolicy.AFFECTS_CODEGEN,
            EXTERNAL_DEPENDENCIES to CachePolicy.PROPAGATED,
            LLVM_VARIANT to CachePolicy.PROPAGATED,
            RUNTIME_LOGS to CachePolicy.AFFECTS_RUNTIME,
            TEST_DUMP_OUTPUT_PATH to CachePolicy.NOT_PROPAGATED,
            OMIT_FRAMEWORK_BINARY to CachePolicy.NOT_PROPAGATED,
            COMPILE_FROM_BITCODE to CachePolicy.NOT_PROPAGATED,
            SERIALIZED_DEPENDENCIES to CachePolicy.NOT_PROPAGATED,
            SAVE_DEPENDENCIES_PATH to CachePolicy.NOT_PROPAGATED,
            SAVE_LLVM_IR_DIRECTORY to CachePolicy.NOT_PROPAGATED,
            KONAN_DATA_DIR to CachePolicy.PROPAGATED,
            KONAN_MANIFEST_NATIVE_TARGETS to CachePolicy.NOT_PROPAGATED,
            LLVM_MODULE_PASSES to CachePolicy.AFFECTS_CODEGEN,
            LLVM_LTO_PASSES to CachePolicy.AFFECTS_CODEGEN,
            SERIALIZED_KLIB_DAG to CachePolicy.NOT_PROPAGATED,
    )
}

private val cachePolicies: Map<CompilerConfigurationKey<*>, CachePolicy> =
        nativeConfigurationKeysCachePolicies + BinaryOptions.all.associate { it.compilerConfigurationKey to it.cachePolicy }

private val policiesAffectingCaches = setOf(CachePolicy.AFFECTS_RUNTIME, CachePolicy.AFFECTS_CODEGEN)

private val keysPropagatedToCacheBuilds: List<CompilerConfigurationKey<*>> = cachePolicies
        .filterValues { it == CachePolicy.PROPAGATED || it in policiesAffectingCaches }
        .keys.sortedBy { it.toString() }

// The options accounted for in the fixed part of the cache flavor, see [NativeSecondStageCompilationConfig].
private val flavorPrefixKeys = setOf(NativeConfigurationKeys.KONAN_TARGET, NativeConfigurationKeys.DEBUG, NativeConfigurationKeys.OPTIMIZATION)

/** Returns a name of the first option which prevents using the caches, if any. */
internal fun CompilerConfiguration.findOptionIncompatibleWithCaches(): String? = cachePolicies.entries
        .filter { it.value == CachePolicy.INCOMPATIBLE_WITH_CACHES }
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
     * Returns the part of the cache flavor accounting for the options with the [policies].
     * It is empty for the default options.
     */
    fun flavor(policies: Set<CachePolicy>): String = buildString {
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
    else -> MessageDigest.getInstance("SHA-256").digest(value.toString().toByteArray()).take(4).joinToString("") { "%02x".format(it) }
}
