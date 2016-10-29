/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.kapt3.test

import com.intellij.openapi.util.text.StringUtil
import com.sun.tools.javac.comp.CompileStates
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit
import org.jetbrains.kotlin.codegen.CodegenTestCase
import org.jetbrains.kotlin.codegen.CodegenTestUtil
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.kapt3.*
import org.jetbrains.kotlin.kapt3.stubs.ClassFileToSourceStubConverter
import org.jetbrains.kotlin.kapt3.util.KaptLogger
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.util.trimTrailingWhitespacesAndAddNewlineAtEOF
import com.sun.tools.javac.util.List as JavacList
import java.io.File
import java.nio.file.Files

abstract class AbstractKotlinKapt3Test : CodegenTestCase() {
    protected companion object {
        val FILE_SEPARATOR = "\n\n////////////////////\n\n"
    }

    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>, javaFilesDir: File?) {
        val javaSources = javaFilesDir?.let { arrayOf(it) } ?: emptyArray()

        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.ALL, *javaSources)
        loadMultiFiles(files)

        val txtFile = File(wholeFile.parentFile, wholeFile.nameWithoutExtension + ".txt")
        val classBuilderFactory = Kapt3BuilderFactory()
        val factory = CodegenTestUtil.generateFiles(myEnvironment, myFiles, classBuilderFactory)
        val typeMapper = factory.generationState.typeMapper

        val logger = KaptLogger(isVerbose = true)
        val kaptContext = KaptContext(logger, classBuilderFactory.compiledClasses, classBuilderFactory.origins)
        try {
            check(kaptContext, typeMapper, txtFile)
        } finally {
            kaptContext.close()
        }
    }

    protected fun convert(kaptRunner: KaptContext, typeMapper: KotlinTypeMapper): JavacList<JCCompilationUnit> {
        val converter = ClassFileToSourceStubConverter(kaptRunner, typeMapper)
        return converter.convert()
    }

    protected abstract fun check(
            kaptRunner: KaptContext,
            typeMapper: KotlinTypeMapper,
            txtFile: File)
}

abstract class AbstractClassFileToSourceStubConverterTest : AbstractKotlinKapt3Test() {
    override fun check(kaptRunner: KaptContext, typeMapper: KotlinTypeMapper, txtFile: File) {
        val javaFiles = convert(kaptRunner, typeMapper)
        kaptRunner.compiler.enterTrees(javaFiles)

        val actualRaw = javaFiles.joinToString (FILE_SEPARATOR)
        val actual = StringUtil.convertLineSeparators(actualRaw.trim({ it <= ' ' })).trimTrailingWhitespacesAndAddNewlineAtEOF()

        if (kaptRunner.compiler.shouldStop(CompileStates.CompileState.ENTER)) {
            error("There were errors during analysis. See errors above. Stubs:\n\n$actual")
        }
        KotlinTestUtils.assertEqualsToFile(txtFile, actual)
    }
}

abstract class AbstractKotlinKaptRunnerTest : AbstractKotlinKapt3Test() {
    override fun check(kaptRunner: KaptContext, typeMapper: KotlinTypeMapper, txtFile: File) {
        val compilationUnits = convert(kaptRunner, typeMapper)
        val sourceOutputDir = Files.createTempDirectory("kaptRunner").toFile()
        try {
            kaptRunner.doAnnotationProcessing(emptyList(), listOf(KaptRunnerTest.simpleProcessor()),
                                              classpath = listOf(), sourcesOutputDir = sourceOutputDir, classesOutputDir = sourceOutputDir,
                                              additionalSources = compilationUnits)

            val javaFiles = sourceOutputDir.walkTopDown().filter { it.isFile && it.extension == "java" }
            val actualRaw = javaFiles.sortedBy { it.name }.joinToString(FILE_SEPARATOR) { it.name + ":\n\n" + it.readText() }
            val actual = StringUtil.convertLineSeparators(actualRaw.trim({ it <= ' ' })).trimTrailingWhitespacesAndAddNewlineAtEOF()
            KotlinTestUtils.assertEqualsToFile(txtFile, actual)
        } finally {
            sourceOutputDir.deleteRecursively()
        }
    }
}