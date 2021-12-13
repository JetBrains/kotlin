/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.konan.blackboxtest.support.util.ExternalSourceTransformer
import org.jetbrains.kotlin.konan.blackboxtest.support.util.ExternalSourceTransformers
import org.jetbrains.kotlin.konan.blackboxtest.support.util.ExternalSourceTransformersProvider
import org.jetbrains.kotlin.konan.blackboxtest.support.util.ThreadSafeCache
import org.jetbrains.kotlin.konan.blackboxtest.support.util.getAbsoluteFile
import org.junit.jupiter.api.TestInstance
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Create only one instance of every test class for consistent caching of source transformers.
abstract class AbstractExternalNativeBlackBoxTest : ExternalSourceTransformersProvider, AbstractNativeBlackBoxTest() {
    private val registeredSourceTransformers: ThreadSafeCache<File, MutableList<ExternalSourceTransformer>> = ThreadSafeCache()

    override fun getSourceTransformers(testDataFile: File): ExternalSourceTransformers? = registeredSourceTransformers[testDataFile]

    /**
     * Called directly from test class constructor.
     */
    fun register(@TestDataFile testDataFilePath: String, sourceTransformer: ExternalSourceTransformer) {
        registeredSourceTransformers.computeIfAbsent(getAbsoluteFile(testDataFilePath)) { mutableListOf() } += sourceTransformer
    }
}
