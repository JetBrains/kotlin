/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k

import com.intellij.openapi.project.Project
import com.intellij.psi.codeStyle.JavaCodeStyleSettings
import org.jetbrains.kotlin.idea.j2k.IdeaJavaToKotlinServices
import org.jetbrains.kotlin.j2k.AbstractJavaToKotlinConverterSingleFileTest
import org.jetbrains.kotlin.j2k.ConverterSettings
import org.jetbrains.kotlin.nj2k.postProcessing.NewJ2kPostProcessor
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractNewJavaToKotlinConverterSingleFileTest : AbstractJavaToKotlinConverterSingleFileTest() {
    override fun doTest(javaPath: String) {
        val directory = File(javaPath).parentFile
        val expectedFileName = "${File(javaPath).nameWithoutExtension}.external"
        val expectedFiles = directory.listFiles { _, name ->
            name == "$expectedFileName.kt" || name == "$expectedFileName.java"
        }!!.filterNotNull()
        for (expectedFile in expectedFiles) {
            addFile(expectedFile, dirName = null)
        }
        super.doTest(javaPath)
    }

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

    override fun getProjectDescriptor() =
        descriptorByFileDirective(File(testDataPath, fileName()), isAllFilesPresentInTest())
}