/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.checkers

import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.test.*
import org.jetbrains.kotlin.test.TestFiles.TestFileFactory
import java.io.File
import java.util.*

abstract class KotlinMultiFileTestWithJava<M : KotlinBaseTest.TestModule, F : KotlinBaseTest.TestFile> :
    KotlinBaseTest<F>() {
    protected lateinit var javaFilesDir: File
    private var kotlinSourceRoot: File? = null

    @Throws(Exception::class)
    public override fun setUp() {
        super.setUp()
        // TODO: do not create temporary directory for tests without Java sources
        javaFilesDir = KotlinTestUtils.tmpDir("java-files")
        if (isKotlinSourceRootNeeded()) {
            kotlinSourceRoot = KotlinTestUtils.tmpDir("kotlin-src")
        }
    }

    inner class ModuleAndDependencies internal constructor(val module: M?, val dependencies: List<String>, val friends: List<String>)

    override fun createTestFilesFromFile(file: File, expectedText: String): List<F> {
        return createTestFiles(file, expectedText, HashMap())
    }

    protected open fun isJavaSourceRootNeeded(): Boolean = true

    protected open fun setupEnvironment(
        environment: KotlinCoreEnvironment,
        testDataFile: File,
        files: List<BaseDiagnosticsTest.TestFile>
    ) {
        setupEnvironment(environment)
    }

    protected open fun getExtraClasspath(): List<File> = emptyList()

    protected open fun getEnvironmentConfigFiles(): EnvironmentConfigFiles = EnvironmentConfigFiles.JVM_CONFIG_FILES

    protected open fun isKotlinSourceRootNeeded(): Boolean = false

    protected open fun createTestFileFromPath(filePath: String): File {
        return File(filePath)
    }

    @Throws(Exception::class)
    public override fun doTest(filePath: String) {
        val file = createTestFileFromPath(filePath)
        val expectedText = KotlinTestUtils.doLoadFile(file)
        //TODO: move to proper tests
        if (InTextDirectivesUtils.isDirectiveDefined(expectedText, "// SKIP_JAVAC")) return
        super.doTest(file.path)
    }

    protected abstract fun createTestModule(name: String, dependencies: List<String>, friends: List<String>): M?

    protected abstract fun createTestFile(module: M?, fileName: String, text: String, directives: Directives): F

    protected open fun createTestFiles(
        file: File,
        expectedText: String,
        modules: MutableMap<String?, ModuleAndDependencies>
    ): List<F> {
        return TestFiles.createTestFiles(file.name, expectedText, object : TestFileFactory<M, F> {
            override fun createFile(
                module: M?,
                fileName: String,
                text: String,
                directives: Directives
            ): F? {
                if (fileName.endsWith(".java")) {
                    writeSourceFile(fileName, text, javaFilesDir!!)
                }
                if ((fileName.endsWith(".kt") || fileName.endsWith(".kts")) && kotlinSourceRoot != null) {
                    writeSourceFile(fileName, text, kotlinSourceRoot!!)
                }
                return createTestFile(module, fileName, text, directives)
            }

            override fun createModule(name: String, dependencies: List<String>, friends: List<String>): M? {
                val module = createTestModule(name, dependencies, friends)
                val oldValue = modules.put(name, ModuleAndDependencies(module, dependencies, friends))
                assert(oldValue == null) { "Module $name declared more than once" }
                return module
            }

            private fun writeSourceFile(fileName: String, content: String, targetDir: File) {
                val file = File(targetDir, fileName)
                KotlinTestUtils.mkdirs(file.parentFile)
                file.writeText(content, Charsets.UTF_8)
            }
        }, coroutinesPackage)
    }

    companion object {
        private fun isScriptingNeeded(file: File): Boolean {
            return file.name.endsWith(KotlinParserDefinition.STD_SCRIPT_EXT)
        }
    }
}