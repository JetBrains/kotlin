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
import org.jetbrains.jps.incremental.messages.BuildMessage
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.java.JpsJavaDependencyScope
import org.jetbrains.jps.model.module.JpsModule
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.compilerRunner.JpsKotlinCompilerRunner
import org.jetbrains.kotlin.compilerRunner.btapi.JpsBtaToolchainLoader
import org.jetbrains.kotlin.config.CompilerSettings
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.config.KotlinFacetSettings
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.daemon.common.isDaemonEnabled
import org.jetbrains.kotlin.jps.build.KotlinJpsBuildTestBase.LibraryDependency.JVM_FULL_RUNTIME
import org.jetbrains.kotlin.jps.build.fixtures.EnableICFixture
import org.jetbrains.kotlin.jps.model.JpsKotlinFacetModuleExtension
import org.jetbrains.kotlin.jps.model.kotlinCommonCompilerArguments
import org.jetbrains.kotlin.jps.model.kotlinCompilerArguments
import org.jetbrains.kotlin.jps.model.kotlinCompilerSettings
import org.jetbrains.kotlin.test.kotlinPathsForDistDirectoryForTests
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase.assertExists
import org.jetbrains.kotlin.utils.PathUtil
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.condition.DisabledIfSystemProperty
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.reflect.KMutableProperty1

class KotlinJpsBuildTestIncremental : KotlinJpsBuildTest() {
    private val enableICFixture = EnableICFixture()

    @BeforeEach
    override fun setUp() {
        super.setUp()
        enableICFixture.setUp()
    }

    @AfterEach
    override fun tearDown() {
        RunAll(
            { enableICFixture.tearDown() },
            { super.tearDown() }
        ).run()
    }

    @Test
    @DisabledIfSystemProperty(
        named = JpsBtaToolchainLoader.IMPL_HOME_PROPERTY,
        matches = ".+",
        disabledReason = "KT-89967: no compiler metrics in build reports when compiling through the Build Tools API",
    )
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
            "Compiler initialization time:",
            "Compiler code analysis:",
            "Compiler code generation:"
        )

        @Test
        fun testImpl() {
            assertTrue(isDaemonEnabled(), "Daemon was not enabled!")
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

    @Test
    fun testJpsDaemonIC() {
        @Test
        fun testImpl() {
            assertTrue(isDaemonEnabled(), "Daemon was not enabled!")

            doTest()
            val module = myProject.modules[0]
            val mainKtClassFile = findFileInOutputDir(module, "MainKt.class")
            assertTrue(mainKtClassFile.exists(), "$mainKtClassFile does not exist!")

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

    @Test
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

    @Test
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
    @Test
    fun testLanguageVersionChanged() {
        languageOrApiVersionChanged(CommonCompilerArguments::languageVersion)
    }

    @WorkingDir("LanguageOrApiVersionChanged")
    @Test
    fun testApiVersionChanged() {
        languageOrApiVersionChanged(CommonCompilerArguments::apiVersion)
    }

    @WorkingDir("LanguageOrApiVersionChanged")
    @Test
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
    @Test
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

    @Test
    fun testUseSerializationPluginWithClassesInOut() {
        assertTrue(IncrementalCompilation.isEnabledForJvm())
        initProject(LibraryDependency.SERIALIZATION)
        rebuildAllModules()
        val jpsCaches = myDataStorageRoot.resolve("targets/java-production")
        assertExists(jpsCaches)
        assertTrue(jpsCaches.deleteRecursively())
        checkWhen(createTouchAction("src/Bar.kt"), null, null)
    }

    @Test
    fun testKotlinProjectWithJavaModule() {
        buildKotlinProjectWithJavaModule()
    }

    // kotlinc doesn't add the classpath to the module path when the source root of module-info.java has a package prefix
    @WorkingDir("KotlinProjectWithJavaModule")
    @Test
    fun testKotlinProjectWithJavaModuleAndPackagePrefix() {
        buildKotlinProjectWithJavaModule(packagePrefix = "app")
    }

    @WorkingDir("KotlinProjectWithJavaModule")
    @Test
    fun testKotlinProjectWithJavaModuleAndAdditionalModule() {
        buildKotlinProjectWithJavaModule(packagePrefix = "app") {
            compilerArguments = K2JVMCompilerArguments().apply { additionalJavaModules = arrayOf("annotations") }
        }
    }

    @WorkingDir("KotlinProjectWithJavaModule")
    @Test
    fun testKotlinProjectWithJavaModuleAndModulePathArgument() {
        val kotlinTestPath = PathUtil.kotlinPathsForDistDirectoryForTests.kotlinTestPath
        buildKotlinProjectWithJavaModuleAndModulePathArgument(listOf("-Xmodule-path=$kotlinTestPath"), ::passInAdditionalArguments)
    }

    @WorkingDir("KotlinProjectWithJavaModule")
    @Test
    fun testKotlinProjectWithJavaModuleAndModulePathArgumentInCommonArguments() {
        val kotlinTestPath = PathUtil.kotlinPathsForDistDirectoryForTests.kotlinTestPath
        buildKotlinProjectWithJavaModuleAndModulePathArgument(listOf("-Xmodule-path=$kotlinTestPath"), ::passInCommonArguments)
    }

    @WorkingDir("KotlinProjectWithJavaModule")
    @Test
    fun testKotlinProjectWithJavaModuleAndSeparateModulePathValue() {
        val kotlinTestPath = PathUtil.kotlinPathsForDistDirectoryForTests.kotlinTestPath.path
        buildKotlinProjectWithJavaModuleAndModulePathArgument(listOf("-Xmodule-path", kotlinTestPath), ::passInAdditionalArguments)
    }

    @WorkingDir("KotlinProjectWithJavaModule")
    @Test
    fun testKotlinProjectWithJavaModuleAndSeparateModulePathValueInCommonArguments() {
        val kotlinTestPath = PathUtil.kotlinPathsForDistDirectoryForTests.kotlinTestPath.path
        buildKotlinProjectWithJavaModuleAndModulePathArgument(listOf("-Xmodule-path", kotlinTestPath), ::passInCommonArguments)
    }

    private fun passInAdditionalArguments(arguments: List<String>) {
        findModule("app").container.setChild(
            JpsKotlinFacetModuleExtension.KIND,
            JpsKotlinFacetModuleExtension(KotlinFacetSettings().apply {
                useProjectSettings = false
                compilerArguments = K2JVMCompilerArguments()
                compilerSettings = CompilerSettings().apply { additionalArguments = arguments.joinToString(" ") }
            })
        )
    }

    private fun passInCommonArguments(arguments: List<String>) {
        myProject.kotlinCommonCompilerArguments = myProject.kotlinCommonCompilerArguments.apply { freeArgs = arguments }
    }

    private fun buildKotlinProjectWithJavaModuleAndModulePathArgument(
        modulePathArguments: List<String>,
        passArguments: (List<String>) -> Unit,
    ) {
        val app = loadKotlinProjectWithJavaModule(packagePrefix = "app")
        passArguments(modulePathArguments + "-Xadd-modules=kotlin.test")

        val result = buildAllModules()
        result.assertSuccessful()
        assertFilesExistInOutput(app, "app/AppKt.class", "module-info.class")
        val warnings = result.getMessages(BuildMessage.Kind.WARNING).map { it.messageText }
        assertTrue(warnings.none { "-Xmodule-path" in it }) { "Unexpected warnings: $warnings" }
    }

    @WorkingDir("KotlinProjectWithJavaModule")
    @Test
    fun testKotlinProjectWithJavaModuleAndArgumentFile() {
        buildKotlinProjectWithJavaModuleAndArgumentFile { argumentFile ->
            myProject.kotlinCompilerSettings = CompilerSettings().apply { additionalArguments = "@$argumentFile" }
        }
    }

    @WorkingDir("KotlinProjectWithJavaModule")
    @Test
    fun testKotlinProjectWithJavaModuleAndArgumentFileInCommonArguments() {
        buildKotlinProjectWithJavaModuleAndArgumentFile { argumentFile ->
            myProject.kotlinCommonCompilerArguments = myProject.kotlinCommonCompilerArguments.apply {
                freeArgs = listOf("@$argumentFile")
            }
        }
    }

    private fun buildKotlinProjectWithJavaModuleAndArgumentFile(passArgumentFile: (File) -> Unit) {
        val argumentFile = File(workDir, "arguments.txt")
        argumentFile.writeText("-Xmodule-path=${PathUtil.kotlinPathsForDistDirectoryForTests.kotlinTestPath} -Xadd-modules=kotlin.test")
        val app = loadKotlinProjectWithJavaModule()
        passArgumentFile(argumentFile)

        val result = buildAllModules()
        result.assertSuccessful()
        assertFilesExistInOutput(app, "app/AppKt.class")
        // The Build Tools API reports a `-Xmodule-path` from an argument file as passed twice without a module path too
        val libOutput = File("out/production/lib").path
        val warnings = result.getMessages(BuildMessage.Kind.WARNING).map { it.messageText }
        assertTrue(warnings.none { "-Xmodule-path" in it && libOutput in it }) { "Unexpected warnings: $warnings" }
    }

    // The Build Tools API rejects a classpath entry that contains the path separator
    @LegacyRunnerOnly
    @WorkingDir("KotlinProjectWithJavaModule")
    @Test
    fun testKotlinProjectWithJavaModuleAndPathSeparatorInDependencyPath() {
        val kotlinTest = File(workDir, "a${File.pathSeparator}b/kotlin-test.jar")
        PathUtil.kotlinPathsForDistDirectoryForTests.kotlinTestPath.copyTo(kotlinTest)
        val app = loadKotlinProjectWithJavaModule {
            compilerArguments = K2JVMCompilerArguments().apply { additionalJavaModules = arrayOf("kotlin.test") }
        }
        addDependency(JpsJavaDependencyScope.COMPILE, listOf(app), false, "kotlin-test", kotlinTest)

        val result = buildAllModules()
        result.assertSuccessful()
        val warnings = result.getMessages(BuildMessage.Kind.WARNING).map { it.messageText }
        assertTrue(warnings.none { "non-existent" in it }) { "Unexpected warnings: $warnings" }
    }

    // The Kotlin classpath includes only exported dependencies, but the module path of javac includes all of them
    @WorkingDir("KotlinProjectWithJavaModule")
    @Test
    fun testKotlinProjectWithJavaModuleAndNotExportedDependency() {
        File(workDir, "lib/src/module-info.java").writeText("module lib {\n    requires transitive base;\n    exports lib;\n}\n")
        removeBaseDependencyFromApp()
        buildKotlinProjectWithJavaModule()
    }

    @LegacyRunnerOnly
    @WorkingDir("KotlinProjectWithJavaModule")
    @Test
    fun testKotlinProjectWithJavaModuleWithDaemon() {
        withDaemon {
            withSystemProperty(JpsKotlinCompilerRunner.FAIL_ON_FALLBACK_PROPERTY, "true") {
                buildKotlinProjectWithJavaModule(packagePrefix = "app")
            }
        }
    }

    @WorkingDir("KotlinProjectWithJavaModule")
    @Test
    fun testKotlinProjectWithJavaModuleAndChangeInNotExportedDependency() {
        File(workDir, "base/src/base/Base.java").delete()
        val baseKt = File(workDir, "base/src/base/Base.kt")
        baseKt.writeText("package base\n\nopen class Base\n")
        File(workDir, "base/src/module-info.java").writeText("module base {\n    requires kotlin.stdlib;\n    exports base;\n}\n")
        File(workDir, "lib/src/module-info.java").writeText("module lib {\n    requires transitive base;\n    exports lib;\n}\n")
        removeBaseDependencyFromApp()
        File(workDir, "app/src/app/Sub.kt").writeText("package app\n\nclass Sub : base.Base()\n")
        File(workDir, "app/src/app/App.kt").writeText(
            """
            package app

            fun Sub.member(x: Int) = "extension"

            fun use() = Sub().member(1)
            """.trimIndent()
        )
        buildKotlinProjectWithJavaModule()

        change(baseKt.path, "package base\n\nopen class Base {\n    fun member(x: Int) = \"member\"\n}\n")
        buildAllModules().assertSuccessful()
        assertCompiled(KotlinBuilder.KOTLIN_BUILDER_NAME, "app/src/app/App.kt", "base/src/base/Base.kt")
    }

    @WorkingDir("KotlinProjectWithJavaModule")
    @Test
    fun testKotlinProjectWithJavaModuleAndSameModuleInNotExportedDependency() {
        val kotlinTestPath = PathUtil.kotlinPathsForDistDirectoryForTests.kotlinTestPath
        val appKotlinTest = kotlinTestPath.copyTo(File(workDir, "app-lib/kotlin-test.jar"))
        val libKotlinTest = kotlinTestPath.copyTo(File(workDir, "lib-lib/kotlin-test.jar"))
        val app = loadKotlinProjectWithJavaModule {
            compilerArguments = K2JVMCompilerArguments().apply { additionalJavaModules = arrayOf("kotlin.test") }
        }
        addDependency(JpsJavaDependencyScope.COMPILE, listOf(app), false, "app-kotlin-test", appKotlinTest)
        addDependency(JpsJavaDependencyScope.COMPILE, listOf(findModule("lib")), false, "lib-kotlin-test", libKotlinTest)

        assertNoIgnoredRootWarnings(buildAllModules())
    }

    @WorkingDir("KotlinProjectWithJavaModule")
    @Test
    fun testKotlinProjectWithJavaModuleAndSameModuleInTwoNotExportedDependencies() {
        File(workDir, "lib/src/module-info.java").writeText("module lib {\n    requires transitive base;\n    exports lib;\n}\n")
        removeBaseDependencyFromApp()
        val kotlinTestPath = PathUtil.kotlinPathsForDistDirectoryForTests.kotlinTestPath
        val libKotlinTest = kotlinTestPath.copyTo(File(workDir, "lib-lib/kotlin-test.jar"))
        val baseKotlinTest = kotlinTestPath.copyTo(File(workDir, "base-lib/kotlin-test.jar"))
        loadKotlinProjectWithJavaModule {
            compilerArguments = K2JVMCompilerArguments().apply { additionalJavaModules = arrayOf("kotlin.test") }
        }
        addDependency(JpsJavaDependencyScope.COMPILE, listOf(findModule("lib")), false, "lib-kotlin-test", libKotlinTest)
        addDependency(JpsJavaDependencyScope.COMPILE, listOf(findModule("base")), false, "base-kotlin-test", baseKotlinTest)

        assertNoIgnoredRootWarnings(buildAllModules())
    }

    private fun assertNoIgnoredRootWarnings(result: BuildResult) {
        result.assertSuccessful()
        val warnings = result.getMessages(BuildMessage.Kind.WARNING).map { it.messageText }
        assertTrue(warnings.none { "root is ignored" in it }) { "Unexpected warnings: $warnings" }
    }

    // Without a package prefix, kotlinc prints the warning for its copy of the classpath entry anyway
    @WorkingDir("KotlinProjectWithJavaModule")
    @Test
    fun testKotlinProjectWithJavaModuleAndSameModuleInModulePathArgument() {
        val kotlinTestPath = PathUtil.kotlinPathsForDistDirectoryForTests.kotlinTestPath
        val libraryKotlinTest = kotlinTestPath.copyTo(File(workDir, "app-lib/kotlin-test.jar"))
        val app = loadKotlinProjectWithJavaModule(packagePrefix = "app")
        addDependency(JpsJavaDependencyScope.COMPILE, listOf(app), false, "kotlin-test", libraryKotlinTest)
        passInAdditionalArguments(listOf("-Xmodule-path=$kotlinTestPath", "-Xadd-modules=kotlin.test"))

        assertNoIgnoredRootWarnings(buildAllModules())
    }

    // kotlinc doesn't read the jars in a directory on the module path, unlike javac
    @WorkingDir("KotlinProjectWithJavaModule")
    @Test
    fun testKotlinProjectWithJavaModuleAndSameModuleInDirectoryInModulePathArgument() {
        val kotlinTestPath = PathUtil.kotlinPathsForDistDirectoryForTests.kotlinTestPath
        val libraryKotlinTest = kotlinTestPath.copyTo(File(workDir, "app-lib/kotlin-test.jar"))
        val modulesDirectory = File(workDir, "modules")
        kotlinTestPath.copyTo(File(modulesDirectory, "kotlin-test.jar"))
        val app = loadKotlinProjectWithJavaModule(packagePrefix = "app")
        addDependency(JpsJavaDependencyScope.COMPILE, listOf(app), false, "kotlin-test", libraryKotlinTest)
        passInAdditionalArguments(listOf("-Xmodule-path=$modulesDirectory", "-Xadd-modules=kotlin.test"))

        buildAllModules().assertSuccessful()
    }

    // kotlinc reads a jar with a class in the default package as an automatic module, but ModuleFinder rejects it
    @WorkingDir("KotlinProjectWithJavaModule")
    @Test
    fun testKotlinProjectWithJavaModuleAndUnreadableJarInModulePathArgument() {
        val jar = File(workDir, "legacy.jar")
        JarOutputStream(jar.outputStream()).use {
            it.putNextEntry(JarEntry("Legacy.class"))
            it.closeEntry()
        }
        loadKotlinProjectWithJavaModule(packagePrefix = "app")
        passInAdditionalArguments(listOf("-Xmodule-path=$jar"))

        buildAllModules().assertSuccessful()
    }

    // The module path of javac has only the dependencies that are ordered after the JDK
    @WorkingDir("KotlinProjectWithJavaModule")
    @Test
    fun testKotlinProjectWithJavaModuleAndDependencyBeforeJdk() {
        PathUtil.kotlinPathsForDistDirectoryForTests.kotlinTestPath.copyTo(File(workDir, "app/kotlin-test.jar"))
        val library = "<library><CLASSES><root url=\"jar://\$MODULE_DIR\$/kotlin-test.jar!/\" /></CLASSES></library>"
        val appModuleFile = File(workDir, "app/app.iml")
        appModuleFile.writeText(
            appModuleFile.readText().replace(
                "<orderEntry type=\"jdk\"",
                "<orderEntry type=\"module-library\">$library</orderEntry>\n    <orderEntry type=\"jdk\"",
            )
        )
        buildKotlinProjectWithJavaModule(packagePrefix = "app") {
            compilerArguments = K2JVMCompilerArguments().apply { additionalJavaModules = arrayOf("kotlin.test") }
        }
    }

    @WorkingDir("KotlinProjectWithJavaModule")
    @Test
    fun testDependentTargetsOfNotExportedDependencyOfJavaModule() {
        File(workDir, "lib/src/module-info.java").writeText("module lib {\n    requires transitive base;\n    exports lib;\n}\n")
        removeBaseDependencyFromApp()
        loadKotlinProjectWithJavaModule()

        val report = buildAndReportDependentTargets()
        val baseDependents = report.substringAfter("Targets dependent on Module 'base' production:\n").substringBefore("---------")
        assertTrue("Module 'app' production" in baseDependents) { report }
    }

    private fun removeBaseDependencyFromApp() {
        val appModuleFile = File(workDir, "app/app.iml")
        appModuleFile.writeText(appModuleFile.readLines().filterNot { "module-name=\"base\"" in it }.joinToString("\n"))
    }

    @WorkingDir("KotlinProjectWithJavaModule")
    @Test
    fun testKotlinProjectWithInvalidJavaModule() {
        File(workDir, "app/src/module-info.java").writeText("module app {\n    requires lib\n}\n")
        loadKotlinProjectWithJavaModule()

        val errors = buildAllModules().getMessages(BuildMessage.Kind.ERROR).map { it.messageText }
        assertTrue(errors.isNotEmpty() && errors.none { "Internal Error" in it }) { "Unexpected errors: $errors" }
    }

    private fun buildKotlinProjectWithJavaModule(
        packagePrefix: String? = null,
        configureFacet: (KotlinFacetSettings.() -> Unit)? = null,
    ): BuildResult {
        val app = loadKotlinProjectWithJavaModule(packagePrefix, configureFacet)
        return buildAllModules().apply {
            assertSuccessful()
            assertFilesExistInOutput(app, "app/AppKt.class", "module-info.class")
        }
    }

    private fun loadKotlinProjectWithJavaModule(
        packagePrefix: String? = null,
        configureFacet: (KotlinFacetSettings.() -> Unit)? = null,
    ): JpsModule {
        addJdk11()
        loadProject(workDir.absolutePath + File.separator + PROJECT_NAME + ".ipr")
        addKotlinStdlibDependency()

        val app = findModule("app")
        if (packagePrefix != null) {
            app.sourceRoots.single().getProperties(JavaSourceRootType.SOURCE)!!.packagePrefix = packagePrefix
        }
        if (configureFacet != null) {
            val facet = KotlinFacetSettings().apply {
                useProjectSettings = false
                compilerArguments = K2JVMCompilerArguments()
                configureFacet()
            }
            app.container.setChild(JpsKotlinFacetModuleExtension.KIND, JpsKotlinFacetModuleExtension(facet))
        }
        return app
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

        setVersion(LanguageVersion.FIRST_API_SUPPORTED.versionString)
        buildAllModules().assertSuccessful()
        assertCompiled(KotlinBuilder.KOTLIN_BUILDER_NAME, "src/Bar.kt", "src/Foo.kt")
    }
}
