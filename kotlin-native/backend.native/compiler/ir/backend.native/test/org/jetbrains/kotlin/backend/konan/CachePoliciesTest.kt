/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.setupCommonArguments
import org.jetbrains.kotlin.cli.create
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.config.nativeBinaryOptions.BinaryOptions
import org.jetbrains.kotlin.config.nativeBinaryOptions.CachePolicy
import org.jetbrains.kotlin.config.nativeBinaryOptions.GC
import org.jetbrains.kotlin.config.nativeBinaryOptions.SanitizerKind
import org.jetbrains.kotlin.konan.config.NativeConfigurationKeys
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import java.lang.reflect.Modifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CachePoliciesTest {
    @Test
    fun everyNativeConfigurationKeyHasCachePolicy() {
        val keys = NativeConfigurationKeys::class.java.declaredFields
                .filter { Modifier.isStatic(it.modifiers) && CompilerConfigurationKey::class.java.isAssignableFrom(it.type) }
                .associateBy({ it.get(null) as CompilerConfigurationKey<*> }, { it.name })
        assertTrue(keys.isNotEmpty())

        val unclassified = keys.filterKeys { it !in nativeConfigurationKeysCachePolicies }.values.sorted()
        assertTrue(unclassified.isEmpty(), "Specify the cache policy for $unclassified in `nativeConfigurationKeysCachePolicies`")
        val unknown = nativeConfigurationKeysCachePolicies.keys.filter { it !in keys }
        assertTrue(unknown.isEmpty(), "`nativeConfigurationKeysCachePolicies` lists unknown keys: $unknown")
    }

    // The cache flavor shouldn't change for the default compiler arguments: otherwise, the caches shipped with the distribution
    // wouldn't be used. So the options the compiler always sets have to be declared with their defaults in
    // `NativeSecondStageCompilationConfig.cacheBuildOptions` (or be a part of the flavor prefix).
    @Test
    fun optionsSetByDefaultAreDeclaredWithDefaults() {
        val arguments = K2NativeCompilerArguments()
        val configuration = CompilerConfiguration.create()
        configuration.setupCommonArguments(arguments) { MetadataVersion(*it) }
        val disposable = Disposer.newDisposable()
        try {
            configuration.setupFromArguments(arguments, disposable)
        } finally {
            Disposer.dispose(disposable)
        }

        val flavor = CacheBuildOptions(configuration).flavor(CachePolicy.AFFECTS_RUNTIME, CachePolicy.AFFECTS_CODEGEN)
        val optionsSetByDefault = Regex("-(\\w+)=").findAll(flavor).map { it.groupValues[1] }.toSet()
        val declaredWithDefaults = setOf(
                NativeConfigurationKeys.KONAN_TARGET,
                NativeConfigurationKeys.DEBUG,
                NativeConfigurationKeys.OPTIMIZATION,
                NativeConfigurationKeys.ENABLE_ASSERTIONS,
                NativeConfigurationKeys.DEBUG_INFO_VERSION,
                BinaryOptions.checkStateAtExternalCalls, // Set from the deprecated `-Xcheck-state-at-external-calls`.
        ).mapTo(mutableSetOf()) { it.toString() }
        val undeclared = optionsSetByDefault - declaredWithDefaults
        assertTrue(undeclared.isEmpty(), "Options $undeclared are set by default, declare them with their default values")
    }

    @Test
    fun flavorOfOptionsPassedAsIs() {
        fun flavor(setup: CompilerConfiguration.() -> Unit) =
                CacheBuildOptions(CompilerConfiguration.create().apply(setup)).flavor(CachePolicy.AFFECTS_CODEGEN)

        assertEquals("-packFields=true", flavor { put(BinaryOptions.packFields, true) })
        assertEquals("-packFields=false", flavor { put(BinaryOptions.packFields, false) })
        // Strings are hashed.
        assertTrue(Regex("-debugCompilationDir=[0-9a-f]{8}").matches(flavor { put(BinaryOptions.debugCompilationDir, "/some/dir") }))
        // An empty list is the same as the absent value.
        assertEquals("", flavor { put(NativeConfigurationKeys.OVERRIDE_CLANG_OPTIONS, emptyList()) })
        // The options not affecting the code of all libraries aren't a part of the flavor.
        assertEquals("", flavor { put(BinaryOptions.concurrentWeakSweep, false) })
        assertEquals("", flavor { put(BinaryOptions.bundleId, "id") })
    }

    @Test
    fun declaredOptionsArePassedAndKeyedByTheirValues() {
        val configuration = CompilerConfiguration.create().apply {
            put(BinaryOptions.sanitizer, SanitizerKind.THREAD)
            put(BinaryOptions.gc, GC.NOOP)
            put(BinaryOptions.packFields, false)
        }
        val options = CacheBuildOptions(configuration).apply {
            // As if the compiler has dropped the sanitizer.
            option(BinaryOptions.sanitizer, null, default = null)
            option(BinaryOptions.gc, GC.STOP_THE_WORLD_MARK_AND_SWEEP, default = GC.CONCURRENT_MARK_AND_SWEEP)
            option(BinaryOptions.genericSafeCasts, true, default = true)
        }

        assertEquals("-gc=STOP_THE_WORLD_MARK_AND_SWEEP-packFields=false",
                options.flavor(CachePolicy.AFFECTS_RUNTIME, CachePolicy.AFFECTS_CODEGEN))
        assertEquals("-packFields=false", options.flavor(CachePolicy.AFFECTS_CODEGEN))

        val cacheBuildConfiguration = CompilerConfiguration.create()
        options.setUp(cacheBuildConfiguration)
        assertNull(cacheBuildConfiguration[BinaryOptions.sanitizer])
        assertEquals(GC.STOP_THE_WORLD_MARK_AND_SWEEP, cacheBuildConfiguration[BinaryOptions.gc])
        assertEquals(true, cacheBuildConfiguration[BinaryOptions.genericSafeCasts])
        assertEquals(false, cacheBuildConfiguration[BinaryOptions.packFields])
        assertNull(cacheBuildConfiguration[BinaryOptions.bundleId])
    }

    @Test
    fun declarationsMustMatchPolicies() {
        val options = CacheBuildOptions(CompilerConfiguration.create())
        assertFailsWith<IllegalArgumentException> { options.option(BinaryOptions.bundleId, "id", default = null) }
        assertFailsWith<IllegalArgumentException> { options.propagate(BinaryOptions.packFields, true) }
        options.option(BinaryOptions.packFields, true, default = true)
        assertFailsWith<IllegalArgumentException> { options.option(BinaryOptions.packFields, true, default = true) }
    }
}
