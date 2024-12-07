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

package org.jetbrains.kotlin.jps.build

import com.intellij.testFramework.RunAll
import com.intellij.util.ThrowableRunnable
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.compilerRunner.JpsKotlinCompilerRunner
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.config.KotlinFacetSettings
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.daemon.common.isDaemonEnabled
import org.jetbrains.kotlin.jps.build.KotlinJpsBuildTestBase.LibraryDependency.JVM_FULL_RUNTIME
import org.jetbrains.kotlin.jps.build.fixtures.EnableICFixture
import org.jetbrains.kotlin.jps.model.JpsKotlinFacetModuleExtension
import org.jetbrains.kotlin.jps.model.kotlinCommonCompilerArguments
import org.jetbrains.kotlin.jps.model.kotlinCompilerArguments
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.KMutableProperty1

class KotlinJpsBuildTestIncremental : KotlinJpsBuildTest() {
    private val enableICFixture = EnableICFixture()

    override fun setUp() {
        super.setUp()
        enableICFixture.setUp()
    }

    override fun tearDown() {
        RunAll(
            ThrowableRunnable { enableICFixture.tearDown() },
            ThrowableRunnable { super.tearDown() }
        ).run()
    }

    fun testJpsBuildReportIC() {

        val reportDir = workDir.resolve("buildReport")

        @Suppress("UNREACHABLE_CODE")
        fun getReportFile(): File {
            return Files.list(reportDir.toPath()).let {
                val files = it.toArray()
                val singleFile = (files.singleOrNull() as Path?).also {
                    it ?: fail("The directory must contain a single file, but got: $files")
                }

                return singleFile?.toFile()!!
            }
        }

        fun assertFileContains(
            file: File,
            vararg expectedText: String,
        ) {
            val text = file.readText()
            val textNotInTheFile = expectedText.filterNot { text.contains(it) }
            assert(textNotInTheFile.isEmpty()) {
                """
                |$file does not contain:
                |${textNotInTheFile.joinToString(separator = "\n")}
                |
                |actual file content:
                |"$text"
                |       
                """.trimMargin()
            }
        }

        fun validateAndDeleteReportFile(vararg expectedText: String) {
            assertTrue(reportDir.exists())
            val reportFile = getReportFile()
            assertFileContains(reportFile, *expectedText)
            reportFile.delete()
        }

        val reportMetricsList = arrayOf(
            "Task 'kotlinProject' finished in",
            "Task info:",
            "Kotlin language version: " + LanguageVersion.LATEST_STABLE,
            "Time metrics:",
            "Jps iteration:",
            "Compiler code analysis:",
            "Compiler code generation:"
        )

        fun testImpl() {
            assertTrue("Daemon was not enabled!", isDaemonEnabled())
            doTest()

            validateAndDeleteReportFile(
                *reportMetricsList,
                "Changed files: [${workDir.resolve("src/Foo.kt").path}, ${workDir.resolve("src/main.kt").path}]"
            )

            val mainKt = File(workDir, "src/main.kt")
            change(mainKt.path, "fun main() {}")

            buildAllModules().assertSuccessful()

            validateAndDeleteReportFile(
                *reportMetricsList,
                "Changed files: [${workDir.resolve("src/main.kt").path}]"
            )
        }

        withDaemon {
            withSystemProperty("kotlin.build.report.file.output_dir", reportDir.path) {
                testImpl()
            }
        }
    }

    fun testJpsDaemonIC() {
        fun testImpl() {
            assertTrue("Daemon was not enabled!", isDaemonEnabled())

            doTest()
            val module = myProject.modules[0]
            val mainKtClassFile = findFileInOutputDir(module, "MainKt.class")
            assertTrue("$mainKtClassFile does not exist!", mainKtClassFile.exists())

            val fooKt = File(workDir, "src/Foo.kt")
            change(fooKt.path, null)
            buildAllModules().assertSuccessful()
            assertCompiled(KotlinBuilder.KOTLIN_BUILDER_NAME, "src/Foo.kt")

            change(fooKt.path, "class Foo(val x: Int = 0)")
            buildAllModules().assertSuccessful()
            assertCompiled(KotlinBuilder.KOTLIN_BUILDER_NAME, "src/main.kt", "src/Foo.kt")
        }

        withDaemon {
            withSystemProperty(JpsKotlinCompilerRunner.FAIL_ON_FALLBACK_PROPERTY, "true") {
                testImpl()
            }
        }
    }

    fun testManyFiles() {
        doTest()

        val module = myProject.modules[0]
        assertFilesExistInOutput(module, "foo/MainKt.class", "boo/BooKt.class", "foo/Bar.class")

        checkWhen(createTouchAction("src/main.kt"), null, packageClasses("kotlinProject", "src/main.kt", "foo.MainKt"))
        checkWhen(createTouchAction("src/boo.kt"), null, packageClasses("kotlinProject", "src/boo.kt", "boo.BooKt"))
        checkWhen(createTouchAction("src/Bar.kt"), arrayOf("src/Bar.kt"), arrayOf(module("kotlinProject"), klass("kotlinProject", "foo.Bar")))

        checkWhen(
            createDeleteAction("src/main.kt"),
            pathsToCompile = null,
            pathsToDelete = packageClasses("kotlinProject", "src/main.kt", "foo.MainKt")
        )
        assertFilesExistInOutput(module, "boo/BooKt.class", "foo/Bar.class")
        assertFilesNotExistInOutput(module, "foo/MainKt.class")

        checkWhen(createTouchAction("src/boo.kt"), null, packageClasses("kotlinProject", "src/boo.kt", "boo.BooKt"))
        checkWhen(createTouchAction("src/Bar.kt"), null, arrayOf(module("kotlinProject"), klass("kotlinProject", "foo.Bar")))
    }

    fun testManyFilesForPackage() {
        doTest()

        val module = myProject.modules[0]
        assertFilesExistInOutput(module, "foo/MainKt.class", "boo/BooKt.class", "foo/Bar.class")

        checkWhen(createTouchAction("src/main.kt"), null, packageClasses("kotlinProject", "src/main.kt", "foo.MainKt"))
        checkWhen(createTouchAction("src/boo.kt"), null, packageClasses("kotlinProject", "src/boo.kt", "boo.BooKt"))
        checkWhen(
            createTouchAction("src/Bar.kt"),
            arrayOf("src/Bar.kt"),
            arrayOf(
                klass("kotlinProject", "foo.Bar"),
                packagePartClass("kotlinProject", "src/Bar.kt", "foo.MainKt"),
                module("kotlinProject")
            )
        )

        checkWhen(
            createDeleteAction("src/main.kt"),
            pathsToCompile = null,
            pathsToDelete = packageClasses("kotlinProject", "src/main.kt", "foo.MainKt")
        )
        assertFilesExistInOutput(module, "boo/BooKt.class", "foo/Bar.class")

        checkWhen(createTouchAction("src/boo.kt"), null, packageClasses("kotlinProject", "src/boo.kt", "boo.BooKt"))
        checkWhen(
            createTouchAction("src/Bar.kt"), null,
            arrayOf(
                klass("kotlinProject", "foo.Bar"),
                packagePartClass("kotlinProject", "src/Bar.kt", "foo.MainKt"),
                module("kotlinProject")
            )
        )
    }

    @WorkingDir("LanguageOrApiVersionChanged")
    fun testLanguageVersionChanged() {
        languageOrApiVersionChanged(CommonCompilerArguments::languageVersion)
    }

    @WorkingDir("LanguageOrApiVersionChanged")
    fun testApiVersionChanged() {
        languageOrApiVersionChanged(CommonCompilerArguments::apiVersion)
    }

    @WorkingDir("LanguageOrApiVersionChanged")
    fun testLanguageVersionExperimental() {
        initProject(LibraryDependency.JVM_MOCK_RUNTIME)
        val module = myProject.modules.first()
        val args = module.kotlinCompilerArguments

        // Try to set Language version to Stable+2 (there is no promises that metadata will be supported)
        val experimentalLevelVersion: LanguageVersion
        try {
            experimentalLevelVersion = LanguageVersion.values()[LanguageVersion.LATEST_STABLE.ordinal + 2]
        } catch (e: ArrayIndexOutOfBoundsException) {
            // there is no Stable+2 version for now, skiping test
            return
        }
        CommonCompilerArguments::languageVersion.set(args, experimentalLevelVersion.versionString)
        myProject.kotlinCommonCompilerArguments = args

        buildAllModules().assertSuccessful()
        assertCompiled(KotlinBuilder.KOTLIN_BUILDER_NAME, "src/Bar.kt", "src/Foo.kt")
    }

    /*
     * This test checks the correct work of caches after clean.
     * Let's imagine that we have `module2` and it depends on `module1`.
     * We change the function name in `Base` class of `module1` -- this counted as removal and adding new function
     * Such change leads to removing any mentions in caches of `module2` (which depends on `module1`)
     * In fact during the compilation of `module1` it will open caches of `module2`
     * So if we also have changed facet configuration of `module2` - it will be marked for recompilation and will try to clean its caches.
     * Such `clean` action of opened maps will lead to "storage is already closed" exception if maps are not reopened properly
     */
    fun testRebuildAfterCachesOpened() {
        assertTrue(IncrementalCompilation.isEnabledForJvm())

        // Init and rebuild
        initProject(JVM_FULL_RUNTIME)
        rebuildAllModules()

        // Change facet of Derived module and change function name of Base class in Base module
        val facet = KotlinFacetSettings()
        facet.useProjectSettings = false
        facet.compilerArguments = K2JVMCompilerArguments()
        findModule("module2").let {
            (facet.compilerArguments as K2JVMCompilerArguments).lambdas = null // "class" value was here before from iml file

            it.container.setChild(
                JpsKotlinFacetModuleExtension.KIND,
                JpsKotlinFacetModuleExtension(facet)
            )
        }

        // Change foo() to bar()
        val newContent = """
            open class Base {
                fun bar() = "boo"
            }
        """.trimIndent()
        checkWhen(createChangeAction("module1/src/Base.kt", newContent), null, null)
    }

    fun testUseSerializationPluginWithClassesInOut() {
        assertTrue(IncrementalCompilation.isEnabledForJvm())
        initProject(LibraryDependency.SERIALIZATION)
        rebuildAllModules()
        val jpsCaches = myDataStorageRoot.resolve("targets/java-production")
        assertExists(jpsCaches)
        assertTrue(jpsCaches.deleteRecursively())
        checkWhen(createTouchAction("src/Bar.kt"), null, null)
    }

    private fun languageOrApiVersionChanged(versionProperty: KMutableProperty1<CommonCompilerArguments, String?>) {
        initProject(LibraryDependency.JVM_MOCK_RUNTIME)

        assertEquals(1, myProject.modules.size)
        val module = myProject.modules.first()
        val args = module.kotlinCompilerArguments

        fun setVersion(newVersion: String) {
            versionProperty.set(args, newVersion)
            myProject.kotlinCommonCompilerArguments = args
        }

        assertNull(args.apiVersion)
        buildAllModules().assertSuccessful()

        setVersion(LanguageVersion.LATEST_STABLE.versionString)
        buildAllModules().assertSuccessful()
        assertCompiled(KotlinBuilder.KOTLIN_BUILDER_NAME)

        setVersion(LanguageVersion.KOTLIN_1_6.versionString)
        buildAllModules().assertSuccessful()
        assertCompiled(KotlinBuilder.KOTLIN_BUILDER_NAME, "src/Bar.kt", "src/Foo.kt")
    }
}