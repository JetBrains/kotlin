/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.konan.test.blackbox.support.settings.ExternalSourceTransformersProvider
import org.jetbrains.kotlin.konan.test.blackbox.support.util.ExternalSourceTransformer
import org.jetbrains.kotlin.konan.test.blackbox.support.util.ExternalSourceTransformers
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WORKS_WHEN_VALUE_CLASS
import org.jetbrains.kotlin.test.preprocessors.JvmInlineSourceTransformer
import org.junit.jupiter.api.Tag
import java.io.File

@Tag("codegen")
abstract class AbstractNativeCodegenBoxTest : AbstractNativeBlackBoxTest() {
    override fun getSourceTransformers(testDataFile: File): ExternalSourceTransformers? {
        val needTransform = "// $WORKS_WHEN_VALUE_CLASS" in testDataFile.readText()
        val transformer = object : ExternalSourceTransformer {
            override fun invoke(content: String): String {
                if (!needTransform) return content
                val contentModifier = JvmInlineSourceTransformer.computeModifier(TargetBackend.NATIVE)
                return contentModifier.invoke(content)
            }
        }
        return listOf(transformer)
    }
}
