/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.nj2k

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkModificator
import com.intellij.openapi.projectRoots.impl.JavaSdkImpl
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.codeStyle.JavaCodeStyleSettings
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.idea.j2k.IdeaJavaToKotlinServices
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.j2k.AbstractJavaToKotlinConverterSingleFileTest
import org.jetbrains.kotlin.j2k.ConverterSettings
import org.jetbrains.kotlin.nj2k.postProcessing.NewJ2kPostProcessor
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractNewJavaToKotlinConverterSingleFileTest : AbstractJavaToKotlinConverterSingleFileTest() {
    override fun compareResults(expectedFile: File, actual: String) {
        KotlinTestUtils.assertEqualsToFile(expectedFile, actual) {
            val file = createKotlinFile(it)
            file.dumpStructureText()
        }
    }

    override fun setUp() {
        super.setUp()
        JavaCodeStyleSettings.getInstance(project).USE_EXTERNAL_ANNOTATIONS = true
    }

    override fun tearDown() {
        JavaCodeStyleSettings.getInstance(project).USE_EXTERNAL_ANNOTATIONS = false
        super.tearDown()
    }

    override fun fileToKotlin(text: String, settings: ConverterSettings, project: Project): String {
        val file = createJavaFile(text)
        return NewJavaToKotlinConverter(project, module, settings, IdeaJavaToKotlinServices)
            .filesToKotlin(listOf(file), NewJ2kPostProcessor()).results.single()
    }

    override fun provideExpectedFile(javaPath: String): File =
        File(javaPath.replace(".java", ".new.kt")).takeIf { it.exists() }
            ?: super.provideExpectedFile(javaPath)

    private fun projectDescriptorByFileDirective(): LightProjectDescriptor {
        if (isAllFilesPresentInTest()) return KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
        val fileText = FileUtil.loadFile(File(testDataPath, fileName()), true)
        return if (InTextDirectivesUtils.isDirectiveDefined(fileText, "RUNTIME_WITH_FULL_JDK"))
            KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE_FULL_JDK
        else KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
    }

    override fun getProjectDescriptor(): KotlinWithJdkAndRuntimeLightProjectDescriptor =
        object : KotlinWithJdkAndRuntimeLightProjectDescriptor() {
            override fun getSdk(): Sdk? {
                val sdk = projectDescriptorByFileDirective().sdk ?: return null
                runWriteAction {
                    val modificator: SdkModificator = sdk.sdkModificator
                    JavaSdkImpl.attachJdkAnnotations(modificator)
                    modificator.commitChanges()
                }
                return sdk
            }
        }
}