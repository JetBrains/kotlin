/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.testFramework.LightPlatformTestCase
import org.jetbrains.kotlin.idea.j2k.IdeaJavaToKotlinServices
import org.jetbrains.kotlin.idea.j2k.J2kPostProcessor
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.dumpTextWithErrors
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File
import java.util.regex.Pattern

abstract class AbstractJavaToKotlinConverterSingleFileTest : AbstractJavaToKotlinConverterTest() {
    val testHeaderPattern = Pattern.compile("//(element|expression|statement|method|class|file|comp)\n")

    fun doTest(javaPath: String) {
        val project = LightPlatformTestCase.getProject()!!
        val javaFile = File(javaPath)
        val fileContents = FileUtil.loadFile(javaFile, true)
        val matcher = testHeaderPattern.matcher(fileContents)

        val (prefix, javaCode) = if (matcher.find()) {
            Pair(matcher.group().trim().substring(2), matcher.replaceFirst(""))
        }
        else {
            Pair("file", fileContents)
        }

        val settings = ConverterSettings.defaultSettings.copy()
        val directives = KotlinTestUtils.parseDirectives(javaCode)
        for ((name, value) in directives) {
            when (name) {
                "forceNotNullTypes" -> settings.forceNotNullTypes = value.toBoolean()
                "specifyLocalVariableTypeByDefault" -> settings.specifyLocalVariableTypeByDefault = value.toBoolean()
                "specifyFieldTypeByDefault" -> settings.specifyFieldTypeByDefault = value.toBoolean()
                "openByDefault" -> settings.openByDefault = value.toBoolean()
                else -> throw IllegalArgumentException("Unknown option: $name")
            }
        }

        val rawConverted = when (prefix) {
            "expression" -> expressionToKotlin(javaCode, settings, project)
            "statement" -> statementToKotlin(javaCode, settings, project)
            "method" -> methodToKotlin(javaCode, settings, project)
            "class" -> fileToKotlin(javaCode, settings, project)
            "file" -> fileToKotlin(javaCode, settings, project)
            else -> throw IllegalStateException("Specify what is it: file, class, method, statement or expression " +
                                                "using the first line of test data file")
        }

        val reformatInFun = prefix in setOf("element", "expression", "statement")

        var actual = reformat(rawConverted, project, reformatInFun)

        if (prefix == "file") {
            actual = createKotlinFile(actual).dumpTextWithErrors()
        }


        val expectedFile = provideExpectedFile(javaPath)
        compareResults(expectedFile, actual)
    }

    open fun provideExpectedFile(javaPath: String): File {
        val kotlinPath = javaPath.replace(".java", ".kt")
        return File(kotlinPath)
    }

    open fun compareResults(expectedFile: File, actual: String) {
        KotlinTestUtils.assertEqualsToFile(expectedFile, actual)
    }

    private fun reformat(text: String, project: Project, inFunContext: Boolean): String {
        val funBody = text.lines().joinToString(separator = "\n", transform = { "  $it" })
        val textToFormat = if (inFunContext) "fun convertedTemp() {\n$funBody\n}" else text

        val convertedFile = KotlinTestUtils.createFile("converted", textToFormat, project)
        WriteCommandAction.runWriteCommandAction(project) {
            CodeStyleManager.getInstance(project)!!.reformat(convertedFile)
        }

        val reformattedText = convertedFile.text!!

        return if (inFunContext)
            reformattedText.removeFirstLine().removeLastLine().trimIndent()
        else
            reformattedText
    }

    protected open fun fileToKotlin(text: String, settings: ConverterSettings, project: Project): String {
        val file = createJavaFile(text)
        val converter = JavaToKotlinConverter(project, settings, IdeaJavaToKotlinServices)
        return converter.filesToKotlin(listOf(file), J2kPostProcessor(formatCode = true)).results.single()
    }

    private fun methodToKotlin(text: String, settings: ConverterSettings, project: Project): String {
        val result = fileToKotlin("final class C {$text}", settings, project)
        return result
                .substringBeforeLast("}")
                .replace("internal class C {", "\n")
                .replace("internal object C {", "\n")
                .trimIndent().trim()
    }

    private fun statementToKotlin(text: String, settings: ConverterSettings, project: Project): String {
        val funBody = text.lines().joinToString(separator = "\n", transform = { "  $it" })
        val result = methodToKotlin("public void main() {\n$funBody\n}", settings, project)

        return result
                .substringBeforeLast("}")
                .replaceFirst("fun main() {", "\n")
                .trimIndent().trim()
    }

    private fun expressionToKotlin(code: String, settings: ConverterSettings, project: Project): String {
        val result = statementToKotlin("final Object o =$code}", settings, project)
        return result
                .replaceFirst("val o: Any? = ", "")
                .replaceFirst("val o: Any = ", "")
                .replaceFirst("val o = ", "")
                .trim()
    }

    override fun getProjectDescriptor(): KotlinWithJdkAndRuntimeLightProjectDescriptor {
        val testName = getTestName(false)
        return if (testName.contains("WithFullJdk") || testName.contains("withFullJdk"))
            KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE_FULL_JDK
        else
            KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
    }

    private fun String.removeFirstLine() = substringAfter('\n', "")

    private fun String.removeLastLine() = substringBeforeLast('\n', "")

    protected fun createJavaFile(text: String): PsiJavaFile {
        return myFixture.configureByText("converterTestFile.java", text) as PsiJavaFile
    }

    protected fun createKotlinFile(text: String): KtFile {
        return myFixture.configureByText("converterTestFile.kt", text) as KtFile
    }
}
