/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.settings

import gnu.trove.THashMap
import org.jetbrains.kotlin.konan.blackboxtest.support.util.TestDisposable
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import java.io.File
import kotlin.reflect.KClass

internal class Settings(settings: Iterable<Any>) : TestDisposable(parentDisposable = null) {
    private val map: Map<KClass<*>, Any> = THashMap<KClass<*>, Any>().apply {
        settings.forEach { setting ->
            val previous = put(setting::class, setting)
            assertTrue(previous == null) { "Duplicated settings: ${setting::class}, $previous, $setting" }
        }

        compact()
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(clazz: KClass<out T>): T = map[clazz] as T? ?: fail { "No such setting: $clazz" }

    inline fun <reified T : Any> get(): T = get(T::class)
}

/**
 * The directories with original sources (aka testData).
 */
internal class TestRoots(val roots: Set<File>, val baseDir: File)

/**
 * [testSourcesDir] - The directory with generated (preprocessed) test sources.
 * [sharedSourcesDir] - The directory with the sources of the shared modules (i.e. the modules that are widely used in multiple tests).
 */
internal class GeneratedSources(val testSourcesDir: File, val sharedSourcesDir: File)

/**
 * [testBinariesDir] - The directory with compiled test binaries (klibs and executable files).
 * [sharedBinariesDir] - The directory with compiled shared modules (klibs).
 */
internal class Binaries(val testBinariesDir: File, val sharedBinariesDir: File)
