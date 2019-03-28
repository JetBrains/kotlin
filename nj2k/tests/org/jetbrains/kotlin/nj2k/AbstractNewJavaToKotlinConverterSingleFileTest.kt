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

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.j2k.IdeaJavaToKotlinServices
import org.jetbrains.kotlin.j2k.AbstractJavaToKotlinConverterSingleFileTest
import org.jetbrains.kotlin.j2k.ConverterSettings
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.AfterClass
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.test.AfterTest

abstract class AbstractNewJavaToKotlinConverterSingleFileTest : AbstractJavaToKotlinConverterSingleFileTest() {
    override fun compareResults(expectedFile: File, actual: String) {
        KotlinTestUtils.assertEqualsToFile(expectedFile, actual) {
            val file = createKotlinFile(it)
            file.dumpStructureText()
        }
    }

    override fun fileToKotlin(text: String, settings: ConverterSettings, project: Project): String {
        val file = createJavaFile(text)
        return NewJavaToKotlinConverter(project, settings, IdeaJavaToKotlinServices)
            .filesToKotlin(listOf(file), NewJ2kPostProcessor(true, settings)).results.single()
    }

    override fun provideExpectedFile(javaPath: String): File =
        File(javaPath.replace(".java", ".new.kt")).takeIf { it.exists() }
            ?: super.provideExpectedFile(javaPath)
}