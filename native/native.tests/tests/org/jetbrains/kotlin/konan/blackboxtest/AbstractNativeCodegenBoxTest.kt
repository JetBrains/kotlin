/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.konan.blackboxtest.support.ClassLevelProperty
import org.jetbrains.kotlin.konan.blackboxtest.support.group.DisabledTestsIfProperty
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.ExternalSourceTransformersProvider
import org.jetbrains.kotlin.konan.blackboxtest.support.util.ExternalSourceTransformer
import org.jetbrains.kotlin.konan.blackboxtest.support.util.ExternalSourceTransformers
import org.jetbrains.kotlin.konan.blackboxtest.support.util.ThreadSafeCache
import org.jetbrains.kotlin.konan.blackboxtest.support.util.getAbsoluteFile
import org.junit.jupiter.api.Tag
import java.io.File

// Disable codegen/box/properties/lateinit/isInitializedAndDeinitialize tests with ONE_STAGE_MULTI_MODULE
//  They should be disabled only for K2 but this is not possible right now.
@DisabledTestsIfProperty(
    sourceLocations = ["compiler/testData/codegen/box/properties/lateinit/isInitializedAndDeinitialize/*.kt"],
    property = ClassLevelProperty.TEST_MODE,
    propertyValue = "ONE_STAGE_MULTI_MODULE"
)
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
