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

package org.jetbrains.kotlin.j2k

import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.j2k.IdeaNewJavaToKotlinServices
import org.jetbrains.kotlin.idea.j2k.J2kPostProcessor
import org.jetbrains.kotlin.idea.j2k.NewJ2KPostProcessingRegistrarImpl
import org.jetbrains.kotlin.psi.KtPsiFactory
import java.io.File

abstract class AbstractNewJavaToKotlinConverterSingleFileTest : AbstractJavaToKotlinConverterSingleFileTest() {

    override fun fileToKotlin(text: String, settings: ConverterSettings, project: Project): String {
        val file = createJavaFile(text)
        val factory = KtPsiFactory(project, true)
        val postProcessor = J2kPostProcessor(true, NewJ2KPostProcessingRegistrarImpl)


        return NewJavaToKotlinConverter(project, settings, IdeaNewJavaToKotlinServices).filesToKotlin(listOf(file)).map {
            factory.createFileWithLightClassSupport("Dummy.kt", it, file)
        }.map {
            CommandProcessor.getInstance().runUndoTransparentAction {
                postProcessor.doAdditionalProcessing(it, null)
            }
            it.text
        }.single()
    }

    override fun provideExpectedFile(javaPath: String): File =
        File(javaPath.replace(".java", ".new.kt")).takeIf { it.exists() }
            ?: super.provideExpectedFile(javaPath)
}