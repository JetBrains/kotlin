/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.j2k.test

import java.io.File
import com.intellij.openapi.util.io.FileUtil
import junit.framework.Assert
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiFile
import junit.framework.Test
import junit.framework.TestSuite
import java.io.FilenameFilter
import java.io.FileFilter
import java.util.Collections
import org.jetbrains.jet.j2k.Converter
import org.jetbrains.jet.j2k.JavaToKotlinTranslator
import org.jetbrains.jet.ConfigurationKind
import org.jetbrains.jet.JetTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations
import com.intellij.testFramework.UsefulTestCase

public abstract class StandaloneJavaToKotlinConverterTest(val dataPath: String, val name: String) : UsefulTestCase() {

    protected override fun runTest(): Unit {
        val jetCoreEnvironment = createEnvironmentWithMockJdkAndIdeaAnnotations(getTestRootDisposable(), ConfigurationKind.JDK_ONLY)
        val converter = Converter(jetCoreEnvironment.getProject())
        val javaPath = "j2k/tests/testData/" + getTestFilePath()
        val kotlinPath = javaPath.replace(".jav", ".kt")
        val kotlinFile = File(kotlinPath)
        if (!kotlinFile.exists()) {
            FileUtil.writeToFile(kotlinFile, "")
        }

        val expected = FileUtil.loadFile(kotlinFile, true)
        val javaFile = File(javaPath)
        val javaCode = FileUtil.loadFile(javaFile, true)
        val parentFileName = javaFile.getParentFile()?.getName()

        val actual = when (parentFileName) {
            "expression" -> expressionToKotlin(converter, javaCode)
            "statement" -> statementToKotlin(converter, javaCode)
            "method" -> methodToKotlin(converter, javaCode)
            "class" -> fileToKotlin(converter, javaCode)
            "file" -> fileToKotlin(converter, javaCode)
            "comp" -> fileToFileWithCompatibilityImport(javaCode)
            else -> throw IllegalStateException("Specify what is it: file, class, method, statement or expression:" +
                                                "$javaPath parent: $parentFileName")
        }

        val tmp = File(kotlinPath + ".tmp")
        if (expected != actual) {
            FileUtil.writeToFile(tmp, actual)
        }

        if (expected == actual && tmp.exists()) {
            tmp.delete()
        }

        Assert.assertEquals(expected, actual)
    }

    fun getTestFilePath(): String {
        return "$dataPath/$name.jav"
    }

    private fun fileToKotlin(converter: Converter, text: String): String {
        return generateKotlinCode(converter, JavaToKotlinTranslator.createFile(converter.project, text))
    }

    private fun methodToKotlin(converter: Converter, text: String?): String {
        var result = fileToKotlin(converter, "final class C {" + text + "}").replaceAll("class C\\(\\) \\{", "")
        result = result.substring(0, (result.lastIndexOf("}")))
        return prettify(result)
    }

    private fun statementToKotlin(converter: Converter, text: String?): String {
        var result = methodToKotlin(converter, "void main() {" + text + "}")
        val pos = result.lastIndexOf("}")
        result = result.substring(0, pos).replaceFirst("fun main\\(\\) : Unit \\{", "")
        return prettify(result)
    }

    private fun expressionToKotlin(converter: Converter, code: String?): String {
        var result = statementToKotlin(converter, "Object o =" + code + "}")
        result = result.replaceFirst("var o : Any\\? =", "")
        return prettify(result)
    }

    private fun fileToFileWithCompatibilityImport(text: String): String {
        return JavaToKotlinTranslator.generateKotlinCodeWithCompatibilityImport(text)
    }

    private fun generateKotlinCode(converter: Converter, file: PsiFile?): String {
        if (file is PsiJavaFile) {
            JavaToKotlinTranslator.setClassIdentifiers(converter, file)
            return prettify(converter.elementToKotlin(file))
        }

        return ""
    }

    private fun prettify(code: String?): String {
        if (code == null) {
            return ""
        }

        return code.trim().replaceAll("\r\n", "\n").replaceAll(" \n", "\n").replaceAll("\n ", "\n").replaceAll("\n+", "\n").replaceAll(" +", " ").trim()
    }

}
private val emptyFilter = object : FilenameFilter {
    public override fun accept(dir: File, name: String): Boolean {
        return true
    }
}

public trait NamedTestFactory {
    fun createTest(dataPath: String, name: String): Test
}

public fun suiteForDirectory(baseDataDir: String?, dataPath: String, factory: NamedTestFactory): TestSuite {
    return suiteForDirectory(baseDataDir, dataPath, true, emptyFilter, factory)
}

public fun suiteForDirectory(baseDataDir: String?, dataPath: String, recursive: Boolean, filter: FilenameFilter, factory: NamedTestFactory): TestSuite {
    val suite = TestSuite(dataPath)
    val extensionJava = ".jav"
    val extensionFilter = object : FilenameFilter {
        public override fun accept(dir: File, name: String): Boolean {
            return name.endsWith(extensionJava)
        }
    }

    val resultFilter =
            if (filter != emptyFilter) {
                object : FilenameFilter {
                    public override fun accept(dir: File, name: String): Boolean {
                        return (extensionFilter.accept(dir, name)) && filter.accept(dir, name)
                    }
                }
            }
            else {
                extensionFilter
            }

    val dir = File(baseDataDir + dataPath)
    val dirFilter = object : FileFilter {
        public override fun accept(pathname: File): Boolean {
            return pathname.isDirectory()
        }
    }

    if (recursive) {
        val files = dir.listFiles(dirFilter)
        val subdirs = files!!.toLinkedList()
        Collections.sort(subdirs)
        for (subdir in subdirs) {
            suite.addTest(suiteForDirectory(baseDataDir, dataPath + "/" + subdir.getName(), recursive, filter, factory))
        }
    }

    val files = (dir.listFiles(resultFilter))!!.toLinkedList()
    Collections.sort(files)
    for (file in files) {
        val testName = file.getName().substring(0, (file.getName().length()) - (extensionJava.length()))
        suite.addTest(factory.createTest(dataPath, testName))
    }
    return suite
}

