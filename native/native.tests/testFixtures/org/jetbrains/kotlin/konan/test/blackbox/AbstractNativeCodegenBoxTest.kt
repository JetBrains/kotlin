/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.konan.test.blackbox.support.util.ExternalSourceTransformer
import org.jetbrains.kotlin.konan.test.blackbox.support.util.ExternalSourceTransformers
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WORKS_WHEN_VALUE_CLASS
import org.jetbrains.kotlin.test.preprocessors.JvmInlineSourceTransformer
import org.jetbrains.kotlin.test.utils.ReplacingSourceTransformer
import org.junit.jupiter.api.Tag
import java.io.File

@Tag("codegen")
abstract class AbstractNativeCodegenBoxTest : AbstractNativeBlackBoxTest() {
    override fun getSourceTransformers(testDataFile: File): ExternalSourceTransformers? = nativeCodegenSourceTransformers(testDataFile)

    companion object {
        /**
         * The source transformers of Native codegen box tests. Eager test grouping cannot reach a test instance,
         * so `EagerExtTestCaseGroupProvider.NativeCodegenSourceTransformers` calls this function too.
         */
        fun nativeCodegenSourceTransformers(testDataFile: File): ExternalSourceTransformers {
            val testDataText = testDataFile.readText()
            val needTransform = "// $WORKS_WHEN_VALUE_CLASS" in testDataText
            val jvmInlineTransformer = object : ExternalSourceTransformer {
                override fun invoke(content: String): String {
                    if (!needTransform) return content
                    val contentModifier =
                        JvmInlineSourceTransformer.computeModifier(TargetBackend.NATIVE, isFullValueClassesEnabled(testDataText))
                    return contentModifier.invoke(content)
                }
            }
            return listOf(
                jvmInlineTransformer,
                ReplacingSourceTransformer("BACKEND_UNDER_TEST", "\"NATIVE\""),
            )
        }

        /** Whether `// LANGUAGE: +Feature -Feature` directives of the test data leave [LanguageFeature.FullValueClasses] enabled. */
        private fun isFullValueClassesEnabled(testDataText: String): Boolean {
            val features = InTextDirectivesUtils.findListWithPrefixes(testDataText, "// LANGUAGE: ").flatMap { it.split(' ') }
            return when {
                "-${LanguageFeature.FullValueClasses.name}" in features -> false
                "+${LanguageFeature.FullValueClasses.name}" in features -> true
                else -> LanguageVersionSettingsImpl.DEFAULT.supportsFeature(LanguageFeature.FullValueClasses)
            }
        }
    }
}
