/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.ExternalSourceTransformersProvider
import org.jetbrains.kotlin.konan.test.blackbox.support.util.ExternalSourceTransformer
import org.jetbrains.kotlin.konan.test.blackbox.support.util.ExternalSourceTransformers
import org.jetbrains.kotlin.konan.test.blackbox.support.util.ThreadSafeCache
import org.jetbrains.kotlin.konan.test.blackbox.support.util.getAbsoluteFile
import org.junit.jupiter.api.Tag
import java.io.File

@Tag("codegen")
abstract class AbstractNativeCodegenBoxTest : ExternalSourceTransformersProvider, AbstractNativeBlackBoxTest() {
    private val registeredSourceTransformers: ThreadSafeCache<File, MutableList<ExternalSourceTransformer>> = ThreadSafeCache()

    override fun getSourceTransformers(testDataFile: File): ExternalSourceTransformers? = registeredSourceTransformers[testDataFile]

    /**
     * Called directly from test class constructor.
     */
    fun register(@TestDataFile testDataFilePath: String, sourceTransformer: ExternalSourceTransformer) {
        registeredSourceTransformers.computeIfAbsent(getAbsoluteFile(testDataFilePath)) { mutableListOf() } += sourceTransformer
    }
}
