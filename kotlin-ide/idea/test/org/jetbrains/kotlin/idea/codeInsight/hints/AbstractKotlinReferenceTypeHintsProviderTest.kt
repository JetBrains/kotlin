/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight.hints

import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.utils.inlays.InlayHintsProviderTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import java.io.File

@Suppress("UnstableApiUsage")
abstract class AbstractKotlinReferenceTypeHintsProviderTest :
    InlayHintsProviderTestCase() { // Abstract- prefix is just a convention for GenerateTests

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
    }

    fun doTest(testPath: String) { // named according to the convention imposed by GenerateTests
        assertThatActualHintsMatch(testPath)
    }

    private fun assertThatActualHintsMatch(fileName: String) {
        with(KotlinReferencesTypeHintsProvider()) {
            val fileContents = FileUtil.loadFile(File(fileName), true)
            val settings = createSettings()
            with(settings) {
                when (InTextDirectivesUtils.findStringWithPrefixes(fileContents, "// MODE: ")) {
                    "function_return" -> set(functionReturn = true)
                    "local_variable" -> set(localVariable = true)
                    "parameter" -> set(parameter = true)
                    "property" -> set(property = true)
                    "all" -> set(functionReturn = true, localVariable = true, parameter = true, property = true)
                    else -> set()
                }
            }

            testProvider("KotlinReferencesTypeHintsProvider.kt", fileContents, this, settings)
        }
    }

    private fun KotlinReferencesTypeHintsProvider.Settings.set(
        functionReturn: Boolean = false, localVariable: Boolean = false,
        parameter: Boolean = false, property: Boolean = false
    ) {
        this.functionReturnType = functionReturn
        this.localVariableType = localVariable
        this.parameterType = parameter
        this.propertyType = property
    }
}