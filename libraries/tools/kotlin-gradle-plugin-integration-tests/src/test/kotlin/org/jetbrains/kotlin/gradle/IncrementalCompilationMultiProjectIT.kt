package org.jetbrains.kotlin.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.checkedReplace
import org.jetbrains.kotlin.test.KtAssert.assertTrue
import org.junit.jupiter.api.DisplayName
import java.nio.file.Path
import kotlin.io.path.*

@JsGradlePluginTests
class IncrementalCompilationJsMultiProjectIT : BaseIncrementalCompilationMultiProjectIT() {
    override val defaultProjectName: String = "incrementalMultiproject"

    override fun defaultProject(
        gradleVersion: GradleVersion,
        buildOptions: BuildOptions,
        test: TestProject.() -> Unit
    ): TestProject = project(defaultProjectName, gradleVersion) {
        listOf("app", "lib").forEach {
            val subProject = subProject(it)
            subProject.javaSourcesDir().deleteRecursively()
            val buildGradleJs = subProject.projectPath.resolve("build-js.gradle")
            subProject.buildGradle.writeText(buildGradleJs.readText())
            buildGradleJs.deleteExisting()
        }
        test()
    }

    override val additionalLibDependencies: String =
        "implementation \"org.jetbrains.kotlin:kotlin-test-js:${'$'}kotlin_version\""

    override val compileKotlinTaskName: String
        get() = "compileKotlin2Js"

    override val compileCacheFolderName: String
        get() = "caches-js"

    //compileKotlin2Js's modification doe not work
    override fun testIncrementalBuildWithCompilationError(gradleVersion: GradleVersion) {}
    override fun testValidOutputsWithCacheCorrupted(gradleVersion: GradleVersion) {}
}

@JvmGradlePluginTests
open class IncrementalCompilationJvmMultiProjectIT : BaseIncrementalCompilationMultiProjectIT() {
    override val additionalLibDependencies: String =
        "implementation \"org.jetbrains.kotlin:kotlin-test:${'$'}kotlin_version\""

    override val compileKotlinTaskName: String
        get() = "compileKotlin"

    override val compileCacheFolderName: String
        get() = "caches-jvm"

    override val defaultProjectName: String = "incrementalMultiproject"

    // todo: do the same for js backend
    @DisplayName("Duplicated class")
    @GradleTest
    fun testDuplicatedClass(gradleVersion: GradleVersion) {
        project("duplicatedClass", gradleVersion) {
            build("assemble")

            val usagesFiles = listOf("useBuzz.kt", "useA.kt").map {
                subProject("app").kotlinSourcesDir().resolve(it)
            }
            usagesFiles.forEach { file -> file.modify { "$it\n " } }

            build("assemble") {
                assertCompiledKotlinSources(
                    usagesFiles.map { it.relativeTo(projectPath) },
                    output
                )
            }
        }
    }

    @DisplayName(
        "checks that multi-project ic is disabled when there is a task that outputs to javaDestination dir " +
                "that is not JavaCompile or KotlinCompile"
    )
    @GradleTest
    open fun testCompileLibWithGroovy(gradleVersion: GradleVersion) {
        testCompileLibWithGroovy_doTest(gradleVersion) { project, result ->
            val expectedSources = project.subProject("app").projectPath.resolve("src").allKotlinSources +
                    listOf(project.subProject("lib").kotlinSourcesDir().resolve("bar/A.kt"))

            assertCompiledKotlinSources(
                expectedSources.map { it.relativeTo(project.projectPath) },
                result.output
            )
        }
    }

    protected fun testCompileLibWithGroovy_doTest(
        gradleVersion: GradleVersion,
        assertResults: (TestProject, BuildResult) -> Unit
    ) {
        defaultProject(gradleVersion) {
            subProject("lib").buildGradle.modify {
                """
                plugins {
                    id 'groovy'
                    id 'org.jetbrains.kotlin.jvm'
                }
                
                dependencies {
                    implementation "org.jetbrains.kotlin:kotlin-stdlib:${"$"}kotlin_version"
                    implementation 'org.codehaus.groovy:groovy-all:2.4.8'
                }
                """.trimIndent()
            }

            val libGroovySrcBar = subProject("lib")
                .projectPath
                .resolve("src/main/groovy/bar")
                .apply { createDirectories() }
            val groovyClass = libGroovySrcBar.resolve("GroovyClass.groovy")
            groovyClass.writeText(
                """
                package bar
                
                class GroovyClass {}
                """.trimIndent()
            )

            build("assemble")

            changeMethodBodyInLib()
            build("build") {
                assertResults(this@defaultProject, this)
            }
        }
    }

    @DisplayName("KT-43489: Make sure build history mapping is not initialized too early")
    @GradleTest
    fun testBuildHistoryMappingLazilyComputedWithWorkers(gradleVersion: GradleVersion) {
        defaultProject(gradleVersion) {
            subProject("app").buildGradle.appendText(
                """
                // added to force eager configuration
                tasks.withType(JavaCompile) {
                    options.encoding = 'UTF-8'
                }
                """.trimIndent()
            )

            build("assemble")

            val aKt = subProject("lib").kotlinSourcesDir().resolve("bar/A.kt")
            aKt.writeText(
                """
                package bar
                
                open class A {
                    fun a() {}
                    fun newA() {}
                }
                """.trimIndent()
            )

            build("assemble") {
                val expectedSources = getExpectedKotlinSourcesForDefaultProject(
                    libSources = listOf("bar/A.kt", "bar/B.kt"),
                    appSources = listOf("foo/AA.kt", "foo/AAA.kt", "foo/BB.kt")
                )
                assertCompiledKotlinSources(expectedSources, output)
            }
        }
    }
}

class IncrementalCompilationFirJvmMultiProjectIT : IncrementalCompilationJvmMultiProjectIT() {
    override val defaultBuildOptions: BuildOptions = super.defaultBuildOptions.copy(useFir = true)
}

class IncrementalCompilationClasspathSnapshotJvmMultiProjectIT : IncrementalCompilationJvmMultiProjectIT() {

    override val defaultBuildOptions = super.defaultBuildOptions.copy(useGradleClasspathSnapshot = true)

    @DisplayName("Lib: Non ABI change in method body")
    @GradleTest
    override fun testNonAbiChangeInLib_changeMethodBody(gradleVersion: GradleVersion) {
        defaultProject(gradleVersion) {
            build("assemble")

            changeMethodBodyInLib()

            build("assemble") {
                assertTasksExecuted(":lib:$compileKotlinTaskName")
                assertTasksUpToDate(":app:$compileKotlinTaskName")
                assertCompiledKotlinSources(
                    getExpectedKotlinSourcesForDefaultProject(libSources = listOf("bar/A.kt")),
                    output
                )
            }
        }
    }

    @DisplayName("Add dependency in lib subproject")
    @GradleTest
    override fun testAddDependencyInLib(gradleVersion: GradleVersion) {
        defaultProject(gradleVersion) {
            build("assemble")

            testAddDependencyInLib_modifyProject()

            build("assemble") {
                assertTasksExecuted(":lib:$compileKotlinTaskName")
                assertTasksUpToDate(":app:$compileKotlinTaskName")
                // Lib compilation is incremental (no files are recompiled)
                assertCompiledKotlinSources(emptyList(), output)
            }
        }
    }

    @DisplayName("after lib project clean")
    @GradleTest
    override fun testAbiChangeInLib_afterLibClean(gradleVersion: GradleVersion) {
        defaultProject(gradleVersion) {
            build("assemble")

            build(":lib:clean")
            changeMethodSignatureInLib()

            build("assemble") {
                val expectedSources = getExpectedKotlinSourcesForDefaultProject(
                    appSources = listOf("foo/AA.kt", "foo/AAA.kt", "foo/BB.kt", "foo/fooUseA.kt")
                ) + subProject("lib").projectPath.resolve("src").allKotlinSources.map { it.relativeTo(projectPath) }

                assertCompiledKotlinSources(expectedSources, output)
            }
        }
    }

    @DisplayName("Compile lib with Groovy")
    @GradleTest
    override fun testCompileLibWithGroovy(gradleVersion: GradleVersion) {
        testCompileLibWithGroovy_doTest(gradleVersion) { project, result ->
            result.assertTasksExecuted(":lib:$compileKotlinTaskName")
            result.assertTasksUpToDate(":app:$compileKotlinTaskName") // App compilation has 'compile avoidance'

            assertCompiledKotlinSources(
                project.getExpectedKotlinSourcesForDefaultProject(libSources = listOf("bar/A.kt")),
                result.output
            )
        }
    }

    @DisplayName("Lib: after cleaning lib project")
    @GradleTest
    override fun testAbiChangeInLib_afterLibClean_withAbiSnapshot(gradleVersion: GradleVersion) {
        defaultProject(gradleVersion) {
            build("assemble")

            build(":lib:clean")
            changeMethodSignatureInLib()

            build("assemble") {
                val expectedSources = getExpectedKotlinSourcesForDefaultProject(
                    appSources = listOf("foo/AA.kt", "foo/AAA.kt", "foo/BB.kt", "foo/fooUseA.kt")
                ) + subProject("lib").projectPath.resolve("src").allKotlinSources.map { it.relativeTo(projectPath) }

                assertCompiledKotlinSources(expectedSources, output)
            }
        }
    }
}

abstract class BaseIncrementalCompilationMultiProjectIT : IncrementalCompilationBaseIT() {

    protected abstract val compileKotlinTaskName: String

    protected abstract val compileCacheFolderName: String

    protected abstract val additionalLibDependencies: String

    protected fun TestProject.changeMethodSignatureInLib() {
        subProject("lib").kotlinSourcesDir().resolve("bar/A.kt").modify {
            it.replace("fun a() {}", "fun a(): Int = 1")
        }
    }

    protected fun TestProject.changeMethodBodyInLib() {
        subProject("lib").kotlinSourcesDir().resolve("bar/A.kt").modify {
            it.replace("fun a() {}", "fun a() { println() }")
        }
    }

    protected fun TestProject.getExpectedKotlinSourcesForDefaultProject(
        libSources: List<String> = emptyList(),
        appSources: List<String> = emptyList()
    ): Iterable<Path> {
        val expectedLibSources = if (libSources.isNotEmpty()) {
            sourceFilesRelativeToProject(
                libSources,
                sourcesDir = { kotlinSourcesDir() },
                subProjectName = "lib"
            )
        } else {
            emptyList()
        }

        val expectedAppSources = if (appSources.isNotEmpty()) {
            sourceFilesRelativeToProject(
                appSources,
                sourcesDir = { kotlinSourcesDir() },
                subProjectName = "app"
            )
        } else {
            emptyList()
        }

        return expectedLibSources + expectedAppSources
    }

    @DisplayName("Lib: method signature ABI change")
    @GradleTest
    fun testAbiChangeInLib_changeMethodSignature(gradleVersion: GradleVersion) {
        defaultProject(gradleVersion) {
            build("assemble")

            changeMethodSignatureInLib()

            build("assemble") {
                val expectedSources = getExpectedKotlinSourcesForDefaultProject(
                    libSources = listOf("bar/A.kt", "bar/B.kt", "bar/barUseA.kt"),
                    appSources = listOf("foo/AA.kt", "foo/AAA.kt", "foo/BB.kt", "foo/fooUseA.kt")
                )

                assertCompiledKotlinSources(expectedSources, output)
            }
        }
    }

    @DisplayName("Lib: add new method changing ABI")
    @GradleTest
    fun testAbiChangeInLib_addNewMethod(gradleVersion: GradleVersion) {
        defaultProject(gradleVersion) {
            build("assemble")

            subProject("lib").kotlinSourcesDir().resolve("bar/A.kt").modify {
                it.replace("fun a() {}", "fun a() {}\nfun newA() {}")
            }

            build("assemble") {
                val expectedSources = getExpectedKotlinSourcesForDefaultProject(
                    libSources = listOf("bar/A.kt", "bar/B.kt"),
                    appSources = listOf("foo/AA.kt", "foo/AAA.kt", "foo/BB.kt")
                )
                assertCompiledKotlinSources(expectedSources, output)
            }
        }
    }

    @DisplayName("Lib: change method body with non-ABI change")
    @GradleTest
    open fun testNonAbiChangeInLib_changeMethodBody(gradleVersion: GradleVersion) {
        defaultProject(gradleVersion) {
            build("assemble")

            changeMethodBodyInLib()

            build("assemble") {
                assertCompiledKotlinSources(
                    getExpectedKotlinSourcesForDefaultProject(
                        libSources = listOf("bar/A.kt")
                    ),
                    output
                )
            }
        }
    }

    @DisplayName("Add new dependency in lib project")
    @GradleTest
    open fun testAddDependencyInLib(gradleVersion: GradleVersion) {
        defaultProject(gradleVersion) {
            build("assemble")

            testAddDependencyInLib_modifyProject()

            build("assemble") {
                assertTasksExecuted(":lib:$compileKotlinTaskName")
                assertTasksUpToDate(":app:$compileKotlinTaskName")
                assertCompiledKotlinSources(
                    subProject("lib").projectPath.resolve("src").allKotlinSources.relativizeTo(projectPath),
                    output
                )
            }
        }
    }

    protected fun TestProject.testAddDependencyInLib_modifyProject() {
        subProject("lib").buildGradle.modify {
            """
            $it

            dependencies {
                $additionalLibDependencies
            }
            """.trimIndent()
        }
    }

    @DisplayName("ABI change in lib after lib clean")
    @GradleTest
    open fun testAbiChangeInLib_afterLibClean(gradleVersion: GradleVersion) {
        // To see if app compilation can be incremental after non-incremental lib compilation
        defaultProject(gradleVersion) {
            build("assemble")

            build(":lib:clean")
            changeMethodSignatureInLib()

            build("assemble") {
                assertCompiledKotlinSources(
                    subProject("lib")
                        .projectPath
                        .resolve("src")
                        .allKotlinSources
                        .relativizeTo(projectPath) +
                            subProject("app")
                                .projectPath
                                .resolve("src")
                                .allKotlinSources
                                .relativizeTo(projectPath),
                    output
                )
            }
        }
    }

    @DisplayName("Move function from lib module into app module")
    @GradleTest
    fun testMoveFunctionFromLibToApp(gradleVersion: GradleVersion) {
        defaultProject(gradleVersion) {
            build("assemble")

            val origFile = subProject("lib").kotlinSourcesDir().resolve("bar/barUseAB.kt")
            subProject("app").kotlinSourcesDir().run {
                resolve("bar").createDirectory()
                origFile.copyTo(resolve("bar/barUseAB.kt"))
                origFile.deleteExisting()
            }

            build("assemble") {
                assertCompiledKotlinSources(
                    getExpectedKotlinSourcesForDefaultProject(
                        appSources = listOf("foo/fooCallUseAB.kt", "bar/barUseAB.kt")
                    ),
                    output
                )
            }
        }
    }

    @DisplayName("Lib project classes became final")
    @GradleTest
    fun testLibClassBecameFinal(gradleVersion: GradleVersion) {
        defaultProject(gradleVersion) {
            build("assemble")

            subProject("lib").kotlinSourcesDir().resolve("bar/B.kt").modify {
                it.replace("open class", "class")
            }

            buildAndFail("assemble") {
                val expectedSources = getExpectedKotlinSourcesForDefaultProject(
                    libSources = listOf("bar/B.kt", "bar/barUseAB.kt", "bar/barUseB.kt"),
                    appSources = listOf("foo/BB.kt", "foo/fooCallUseAB.kt", "foo/fooUseB.kt")
                )
                assertCompiledKotlinSources(expectedSources, output)
            }
        }
    }

    @DisplayName("compile error in lib project")
    @GradleTest
    fun testCompileErrorInLib(gradleVersion: GradleVersion) {
        defaultProject(gradleVersion) {
            build("assemble")

            val bKt = subProject("lib").kotlinSourcesDir().resolve("bar/B.kt")
            val bKtContent = bKt.readText()
            bKt.deleteExisting()

            fun runFailingBuild() {
                buildAndFail("assemble") {
                    assertOutputContains("B.kt has been removed")
                    assertTasksFailed(":lib:$compileKotlinTaskName")
                    assertCompiledKotlinSources(
                        getExpectedKotlinSourcesForDefaultProject(
                            libSources = listOf("bar/barUseAB.kt", "bar/barUseB.kt")
                        ),
                        output
                    )
                }
            }

            runFailingBuild()
            runFailingBuild()

            bKt.writeText(bKtContent.replace("fun b", "open fun b"))

            build("assemble") {
                val expectedSources = getExpectedKotlinSourcesForDefaultProject(
                    libSources = listOf("bar/B.kt", "bar/barUseAB.kt", "bar/barUseB.kt"),
                    appSources = listOf("foo/BB.kt", "foo/fooUseB.kt")
                )
                assertCompiledKotlinSources(expectedSources, output)
            }
        }
    }

    @DisplayName("Remove library from classpath")
    @GradleTest
    fun testRemoveLibFromClasspath(gradleVersion: GradleVersion) {
        defaultProject(gradleVersion) {
            build("assemble")

            val appBuildGradleContent = subProject("app").buildGradle.readText()
            subProject("app").buildGradle.modify { it.checkedReplace("implementation project(':lib')", "") }
            val aaKt = subProject("app").kotlinSourcesDir().resolve("foo/AA.kt")
            aaKt.modify {
                """
                $it
                
                """.trimIndent()
            }

            buildAndFail("assemble")

            subProject("app").buildGradle.writeText(appBuildGradleContent)
            aaKt.modify {
                """
                $it
                
                """.trimIndent()
            }

            build("assemble") {
                assertCompiledKotlinSources(
                    listOf(aaKt.relativeTo(projectPath)),
                    output
                )
            }
        }
    }

    @DisplayName("KT-40875: move function from lib with remapped build dirs")
    @GradleTest
    fun testMoveFunctionFromLibWithRemappedBuildDirs(gradleVersion: GradleVersion) {
        defaultProject(gradleVersion) {
            buildGradle.appendText(
                """
                
                allprojects {
                    it.buildDir = new File(rootDir,  "../out" + it.path.replace(":", "/") + "/build")
                }
                """.trimIndent()
            )

            build("assemble")

            val barUseABKt = subProject("lib").kotlinSourcesDir().resolve("bar/barUseAB.kt")
            subProject("app").kotlinSourcesDir().run {
                resolve("bar").createDirectory()
                barUseABKt.copyTo(resolve("bar/barUseAB.kt"))
                barUseABKt.deleteExisting()
            }

            build("assemble") {
                assertCompiledKotlinSources(
                    getExpectedKotlinSourcesForDefaultProject(
                        appSources = listOf("foo/fooCallUseAB.kt", "bar/barUseAB.kt")
                    ),
                    output
                )
            }
        }
    }

    @DisplayName("Lib with ABI snapshot: add new ABI method")
    @GradleTest
    fun testAbiChangeInLib_addNewMethod_withAbiSnapshot(gradleVersion: GradleVersion) {
        defaultProject(
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(useGradleClasspathSnapshot = true)
        ) {
            build("assemble")

            subProject("lib").kotlinSourcesDir().resolve("bar/A.kt").modify {
                it.replace("fun a() {}", "fun a() {}\nfun newA() {}")
            }

            build("assemble") {
                val expectedSources = getExpectedKotlinSourcesForDefaultProject(
                    libSources = listOf("bar/A.kt", "bar/B.kt"),
                    // TODO(valtman): for abi-snapshot "BB.kt" should not be recompiled
                    appSources = listOf("foo/AA.kt", "foo/AAA.kt", "foo/BB.kt")
                )

                assertCompiledKotlinSources(expectedSources, output)
            }
        }
    }

    @DisplayName("Lib with abi snapshot: after clean build")
    @GradleTest
    open fun testAbiChangeInLib_afterLibClean_withAbiSnapshot(gradleVersion: GradleVersion) {
        defaultProject(
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(useGradleClasspathSnapshot = true)
        ) {
            build("assemble")

            build(":lib:clean")
            changeMethodSignatureInLib()

            build("assemble") {
                // TODO: With ABI snapshot, app compilation should be incremental, currently it is not.
                assertCompiledKotlinSources(
                    (subProject("lib")
                        .projectPath
                        .resolve("src")
                        .allKotlinSources +
                            subProject("app")
                                .projectPath
                                .resolve("src")
                                .allKotlinSources)
                        .map { it.relativeTo(projectPath) },
                    output
                )
            }
        }
    }

    @DisplayName("Lib with classpath snapshot: change isolated class")
    @GradleTest
    fun testChangeIsolatedClassInLib_withAbiSnapshot(gradleVersion: GradleVersion) {
        defaultProject(
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(useGradleClasspathSnapshot = true)
        ) {
            build("assemble")

            subProject("lib").kotlinSourcesDir().resolve("bar/BarDummy.kt").modify {
                "$it { fun m() = 42}"
            }

            build("assemble") {
                assertCompiledKotlinSources(
                    getExpectedKotlinSourcesForDefaultProject(
                        libSources = listOf("bar/BarDummy.kt")
                    ),
                    output
                )
            }
        }
    }

    @DisplayName("KT-49780: Valid outputs after cache corruption exception")
    @GradleTest
    open fun testValidOutputsWithCacheCorrupted(gradleVersion: GradleVersion) {
        defaultProject(gradleVersion) {
            breakCachesAfterCompileKotlinExecution(this)

            build("assemble")

            subProject("lib").kotlinSourcesDir().resolve("bar/B.kt").modify {
                it.replace("fun b() {}", "fun b() = 123")
            }

            build("assemble") {
                assertOutputContains("Non-incremental compilation will be performed: CACHE_CORRUPTION")
            }

            val lookupFile = projectPath.resolve("lib/build/kotlin/${compileKotlinTaskName}/cacheable/${compileCacheFolderName}/lookups/file-to-id.tab")
            assertTrue("Output is empty", lookupFile.exists())

        }
    }

    @DisplayName("KT-49780: No need to rebuild invalid code due to cache corruption")
    @GradleTest
    open fun testIncrementalBuildWithCompilationError(gradleVersion: GradleVersion) {
        defaultProject(gradleVersion) {
            breakCachesAfterCompileKotlinExecution(this)

            build("assemble")

            subProject("lib").kotlinSourcesDir().resolve("bar/B.kt").modify {
                it.replace("fun b() {}", "fun a() = 123")
            }

            buildAndFail("assemble") {
                printBuildOutput()
                assertOutputDoesNotContain("Possible caches corruption:")
                assertOutputDoesNotContain("Non-incremental compilation will be performed:")
            }
        }
    }

    private fun breakCachesAfterCompileKotlinExecution(testProject: TestProject) {
        listOf("app", "lib").forEach {
            testProject.subProject(it).buildGradle.appendText(
                """
                    $compileKotlinTaskName {
                        doLast {
                            def file = new File(projectDir.path, "/build/kotlin/${compileKotlinTaskName}/cacheable/${compileCacheFolderName}/lookups/file-to-id.tab")
                            println("Update lookup file " + file.path)
                            file.write("la-la")
                        }
                    }
                """.trimIndent()
            )
        }
    }

}

