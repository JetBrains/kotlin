package org.jetbrains.kotlin.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.build.report.metrics.BuildAttribute
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilerExecutionStrategy
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.checkedReplace
import org.jetbrains.kotlin.gradle.util.replaceText
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import java.nio.file.Path
import kotlin.io.path.*

@JsGradlePluginTests
abstract class IncrementalCompilationJsMultiProjectIT : BaseIncrementalCompilationMultiProjectIT() {
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
        get() = "compileKotlinJs"

    override val compileCacheFolderName: String
        get() = "caches-js"

    @Disabled("compileKotlinJs's modification does not work")
    override fun testFailureHandling_ToolError(gradleVersion: GradleVersion) {}

    @Disabled("In JS IR all dependencies effectively api, not implementation")
    @DisplayName("Add new dependency in lib project")
    @GradleTest
    override fun testAddDependencyInLib(gradleVersion: GradleVersion) {
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

    @DisplayName("ABI change in lib after lib clean")
    @GradleTest
    override fun testAbiChangeInLib_afterLibClean(gradleVersion: GradleVersion) {
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

    @DisplayName("Lib: change method body with non-ABI change")
    @GradleTest
    override fun testNonAbiChangeInLib_changeMethodBody(gradleVersion: GradleVersion) {
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

    @DisplayName("Lib: after cleaning lib project")
    @GradleTest
    override fun testAbiChangeInLib_afterLibClean_withAbiSnapshot(gradleVersion: GradleVersion) {
        defaultProject(gradleVersion) {
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

    @DisplayName("Lib project classes became final")
    @GradleTest
    override fun testLibClassBecameFinal(gradleVersion: GradleVersion) {
        // `impactedClassInAppIsRecompiled = false` for Kotlin/JS (KT-56197 was fixed for Kotlin/JVM only)
        doTestLibClassBecameFinal(gradleVersion, impactedClassInAppIsRecompiled = false)
    }

    @DisplayName("KT-56197: Change interface in lib which has subclass in app")
    @GradleTest
    override fun testChangeInterfaceInLib(gradleVersion: GradleVersion) {
        // `impactedClassInAppIsRecompiled = false` for Kotlin/JS (KT-56197 was fixed for Kotlin/JVM only)
        doTestChangeInterfaceInLib(gradleVersion, impactedClassInAppIsRecompiled = false)
    }
}

abstract class IncrementalCompilationJsMultiProjectWithPreciseBackupIT : IncrementalCompilationJsMultiProjectIT() {
    override val defaultBuildOptions = super.defaultBuildOptions.copy(usePreciseOutputsBackup = true, keepIncrementalCompilationCachesInMemory = true)
}

class IncrementalCompilationK1JsMultiProject : IncrementalCompilationJsMultiProjectIT() {
    override val defaultBuildOptions = super.defaultBuildOptions.copyEnsuringK1()
}

class IncrementalCompilationK2JsMultiProject : IncrementalCompilationJsMultiProjectIT() {
    override val defaultBuildOptions = super.defaultBuildOptions.copyEnsuringK2()

    @Disabled("KT-61153")
    override fun testRemoveLibFromClasspath(gradleVersion: GradleVersion) {
        super.testRemoveLibFromClasspath(gradleVersion)
    }
}

class IncrementalCompilationK1JsMultiProjectWithPreciseBackupIT : IncrementalCompilationJsMultiProjectWithPreciseBackupIT() {
    override val defaultBuildOptions = super.defaultBuildOptions.copyEnsuringK1()
}

class IncrementalCompilationK2JsMultiProjectWithPreciseBackupIT : IncrementalCompilationJsMultiProjectWithPreciseBackupIT() {
    override val defaultBuildOptions = super.defaultBuildOptions.copyEnsuringK2()
}

@JvmGradlePluginTests
abstract class IncrementalCompilationJvmMultiProjectIT : BaseIncrementalCompilationMultiProjectIT() {
    override val additionalLibDependencies: String =
        "implementation \"org.jetbrains.kotlin:kotlin-test:${'$'}kotlin_version\""

    override val compileKotlinTaskName: String
        get() = "compileKotlin"

    override val compileCacheFolderName: String
        get() = "caches-jvm"

    override val defaultProjectName: String = "incrementalMultiproject"

    @DisplayName("'inspectClassesForKotlinIC' task is added to execution plan")
    open fun testInspectClassesForKotlinICTask(gradleVersion: GradleVersion) {
        defaultProject(gradleVersion) {
            build("assemble") {
                assertTasksSkipped(
                    ":lib:inspectClassesForKotlinIC",
                    ":app:inspectClassesForKotlinIC"
                )
            }
        }
    }

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

    @DisplayName("Compile lib with Groovy")
    @GradleTest
    open fun testCompileLibWithGroovy(gradleVersion: GradleVersion) {
        testCompileLibWithGroovy_doTest(gradleVersion) { project, result ->
            result.assertTasksExecuted(":lib:$compileKotlinTaskName")
            result.assertTasksUpToDate(":app:$compileKotlinTaskName") // App compilation has 'compile avoidance'

            assertCompiledKotlinSources(
                project.getExpectedKotlinSourcesForDefaultProject(libSources = listOf("bar/A.kt")),
                result.output
            )
        }
    }

    //KT-55905
    @DisplayName("Imncremental compilation with source set update")
    @GradleTest
    open fun testSourceSetAdjustment(gradleVersion: GradleVersion) {
        val setUpExternalSource = "sourceSets[\"main\"].kotlin.srcDir(\"../external/src\")"
        defaultProject(gradleVersion) {
            subProject("lib").buildGradle.appendText("\n$setUpExternalSource")
            subProject("lib")
                .projectPath
                .resolve("src/main/kotlin/bar/A.kt").modify {
                    it.replace("fun a() {}","fun a() {}\nfun c() = ExternalClass()" )
                }
            build("assemble")
            subProject("lib").buildGradle.replaceText(setUpExternalSource, "")
            buildAndFail("assemble")
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
                    id 'org.jetbrains.kotlin.test.kotlin-compiler-args-properties'
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

abstract class IncrementalCompilationJvmMultiProjectWithPreciseBackupIT : IncrementalCompilationJvmMultiProjectIT() {
    override val defaultBuildOptions = super.defaultBuildOptions.copy(usePreciseOutputsBackup = true, keepIncrementalCompilationCachesInMemory = true)
}

class IncrementalCompilationK1JvmMultiProjectWithPreciseBackupIT : IncrementalCompilationJvmMultiProjectWithPreciseBackupIT() {
    override val defaultBuildOptions = super.defaultBuildOptions.copyEnsuringK1()
}

class IncrementalCompilationK2JvmMultiProjectWithPreciseBackupIT : IncrementalCompilationJvmMultiProjectWithPreciseBackupIT() {
    override val defaultBuildOptions = super.defaultBuildOptions.copyEnsuringK2()
}

class IncrementalCompilationK1JvmMultiProjectIT : IncrementalCompilationJvmMultiProjectIT() {
    override val defaultBuildOptions: BuildOptions = super.defaultBuildOptions.copyEnsuringK1()
}

class IncrementalCompilationK2JvmMultiProjectIT : IncrementalCompilationJvmMultiProjectIT() {
    override val defaultBuildOptions: BuildOptions = super.defaultBuildOptions.copyEnsuringK2()
}

open class IncrementalCompilationOldICJvmMultiProjectIT : IncrementalCompilationJvmMultiProjectIT() {

    override val defaultBuildOptions = super.defaultBuildOptions.copy(useGradleClasspathSnapshot = false)

    @DisplayName("'inspectClassesForKotlinIC' task is added to execution plan")
    @GradleTest
    override fun testInspectClassesForKotlinICTask(gradleVersion: GradleVersion) {
        defaultProject(gradleVersion) {
            build("assemble") {
                assertTasksExecuted(
                    ":lib:inspectClassesForKotlinIC",
                    ":app:inspectClassesForKotlinIC"
                )
            }
        }
    }


    @DisplayName("Lib: change method body with non-ABI change")
    @GradleTest
    override fun testNonAbiChangeInLib_changeMethodBody(gradleVersion: GradleVersion) {
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
    override fun testAddDependencyInLib(gradleVersion: GradleVersion) {
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

    @DisplayName("ABI change in lib after lib clean")
    @GradleTest
    override fun testAbiChangeInLib_afterLibClean(gradleVersion: GradleVersion) {
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

    @DisplayName(
        "checks that multi-project ic is disabled when there is a task that outputs to javaDestination dir " +
                "that is not JavaCompile or KotlinCompile"
    )
    @GradleTest
    override fun testCompileLibWithGroovy(gradleVersion: GradleVersion) {
        testCompileLibWithGroovy_doTest(gradleVersion) { project, result ->
            val expectedSources = project.subProject("app").projectPath.resolve("src").allKotlinSources +
                    listOf(project.subProject("lib").kotlinSourcesDir().resolve("bar/A.kt"))

            assertCompiledKotlinSources(
                expectedSources.map { it.relativeTo(project.projectPath) },
                result.output
            )
        }
    }


    @DisplayName("Lib with abi snapshot: after clean build")
    @GradleTest
    override fun testAbiChangeInLib_afterLibClean_withAbiSnapshot(gradleVersion: GradleVersion) {
        defaultProject(
            gradleVersion,
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

    @DisplayName("Lib project classes became final")
    @GradleTest
    override fun testLibClassBecameFinal(gradleVersion: GradleVersion) {
        // `impactedClassInAppIsRecompiled = false` for the old IC (KT-56197 was fixed for the new IC only)
        doTestLibClassBecameFinal(gradleVersion, impactedClassInAppIsRecompiled = false)
    }

    @DisplayName("KT-56197: Change interface in lib which has subclass in app")
    @GradleTest
    override fun testChangeInterfaceInLib(gradleVersion: GradleVersion) {
        // `impactedClassInAppIsRecompiled = false` for the old IC (KT-56197 was fixed for the new IC only)
        doTestChangeInterfaceInLib(gradleVersion, impactedClassInAppIsRecompiled = false)
    }
}

class IncrementalCompilationOldICJvmMultiProjectWithPreciseBackupIT : IncrementalCompilationOldICJvmMultiProjectIT() {
    override val defaultBuildOptions = super.defaultBuildOptions.copy(usePreciseOutputsBackup = true, keepIncrementalCompilationCachesInMemory = true)
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

    @DisplayName("Lib: Non ABI change in method body")
    @GradleTest
    open fun testNonAbiChangeInLib_changeMethodBody(gradleVersion: GradleVersion) {
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
    open fun testAddDependencyInLib(gradleVersion: GradleVersion) {
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

    @DisplayName("after lib project clean")
    @GradleTest
    open fun testAbiChangeInLib_afterLibClean(gradleVersion: GradleVersion) {
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
    open fun testLibClassBecameFinal(gradleVersion: GradleVersion) {
        doTestLibClassBecameFinal(gradleVersion)
    }

    protected fun doTestLibClassBecameFinal(gradleVersion: GradleVersion, impactedClassInAppIsRecompiled: Boolean = true) {
        defaultProject(gradleVersion) {
            build("assemble")

            subProject("lib").kotlinSourcesDir().resolve("bar/B.kt").modify {
                it.replace("open class", "class")
            }

            buildAndFail("assemble") {
                val expectedSources = getExpectedKotlinSourcesForDefaultProject(
                    libSources = listOf("bar/B.kt", "bar/barUseAB.kt", "bar/barUseB.kt"),
                    appSources = listOfNotNull(
                        "foo/BB.kt", "foo/fooUseB.kt", "foo/fooCallUseAB.kt",
                        "foo/fooUseBB.kt".takeIf { impactedClassInAppIsRecompiled }
                    )
                )
                assertCompiledKotlinSources(expectedSources, output)
            }
        }
    }

    @DisplayName("Remove library from classpath")
    @GradleTest
    open fun testRemoveLibFromClasspath(gradleVersion: GradleVersion) {
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

    @DisplayName("Lib: after cleaning lib project")
    @GradleTest
    open fun testAbiChangeInLib_afterLibClean_withAbiSnapshot(gradleVersion: GradleVersion) {
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

    @DisplayName("KT-56197: Change interface in lib which has subclass in app")
    @GradleTest
    open fun testChangeInterfaceInLib(gradleVersion: GradleVersion) {
        doTestChangeInterfaceInLib(gradleVersion)
    }

    protected fun doTestChangeInterfaceInLib(gradleVersion: GradleVersion, impactedClassInAppIsRecompiled: Boolean = true) {
        defaultProject(gradleVersion) {
            subProject("lib").kotlinSourcesDir().resolve("bar/InterfaceInLib.kt").writeText(
                """
                package bar
                interface InterfaceInLib {
                    fun someMethod() {}
                }
                """.trimIndent()
            )
            subProject("app").kotlinSourcesDir().resolve("foo/SubclassInApp.kt").writeText(
                """
                package foo
                import bar.InterfaceInLib
                class SubclassInApp : InterfaceInLib
                """.trimIndent()
            )
            subProject("app").kotlinSourcesDir().resolve("foo/ClassUsingSubclassInApp.kt").writeText(
                """
                package foo
                fun main() {
                    SubclassInApp().someMethod()
                }
                """.trimIndent()
            )
            build(":app:compileKotlin")

            subProject("lib").kotlinSourcesDir().resolve("bar/InterfaceInLib.kt").modify {
                it.replace("fun someMethod() {}", "fun someMethod(addedParam: Int = 0) {}")
            }

            build(":app:compileKotlin") {
                assertIncrementalCompilation(
                    expectedCompiledKotlinFiles = getExpectedKotlinSourcesForDefaultProject(
                        libSources = listOf("bar/InterfaceInLib.kt"),
                        appSources = listOfNotNull(
                            "foo/SubclassInApp.kt",
                            "foo/ClassUsingSubclassInApp.kt".takeIf { impactedClassInAppIsRecompiled }
                        )
                    )
                )
            }
        }
    }

    @DisplayName("Test compilation when incremental state is missing")
    @GradleTest
    fun testMissingIncrementalState(gradleVersion: GradleVersion) {
        defaultProject(gradleVersion) {
            // Perform the first non-incremental build without using Kotlin daemon so that incremental state is not produced
            build(
                ":lib:compileKotlin",
                buildOptions = defaultBuildOptions.copy(
                    compilerExecutionStrategy = KotlinCompilerExecutionStrategy.IN_PROCESS,
                ),
            ) {
                projectPath.resolve("lib/build/kotlin/${compileKotlinTaskName}/classpath-snapshot").let {
                    assert(!it.exists() || it.listDirectoryEntries().isEmpty())
                }
            }

            // Perform the next build using Kotlin daemon without making a change and check that tasks are up-to-date. This is to ensure
            // that the `kotlin.compiler.execution.strategy` property used above is not an input to the KotlinCompile task; otherwise the
            // test in the next build would not be effective.
            build(":lib:compileKotlin") {
                assertTasksUpToDate(":lib:$compileKotlinTaskName")
            }

            // Make a change in the source code
            changeMethodSignatureInLib()

            // In the next build, compilation should be non-incremental as incremental state is missing
            build(":lib:compileKotlin") {
                assertNonIncrementalCompilation()
            }
        }
    }

    @DisplayName("Test handling of failures caused by user errors")
    @GradleTest
    fun testFailureHandling_UserError(gradleVersion: GradleVersion) {
        defaultProject(gradleVersion) {
            // Perform the first non-incremental build
            build(":lib:compileKotlin")

            // Make a compile error in the source code
            var classAKtContents: String? = null
            subProject("lib").kotlinSourcesDir().resolve("bar/A.kt").modify {
                classAKtContents = it
                it.replace("fun a() {}", "fun a() { // Compile error: Missing closing bracket")
            }

            // In the next build, compilation should be incremental and fail (and not fall back to non-incremental compilation)
            buildAndFail(":lib:compileKotlin") {
                assertIncrementalCompilation()
                assertTasksFailed(":lib:$compileKotlinTaskName")
                assertOutputContains("Compilation error. See log for more details")
            }

            // Fix the compile error in the source code
            subProject("lib").kotlinSourcesDir().resolve("bar/A.kt").writeText(classAKtContents!!)
            changeMethodSignatureInLib()

            // In the next build, compilation should be incremental and succeed
            build(":lib:compileKotlin") {
                assertIncrementalCompilation(
                    expectedCompiledKotlinFiles = getExpectedKotlinSourcesForDefaultProject(
                        libSources = listOf("bar/A.kt", "bar/B.kt", "bar/barUseA.kt")
                    )
                )
            }
        }
    }

    @DisplayName("Test handling of failures caused by tool errors")
    @GradleTest
    open fun testFailureHandling_ToolError(gradleVersion: GradleVersion) {
        defaultProject(gradleVersion) {
            // Simulate a tool error by registering a doLast that breaks caches in the KotlinCompile task
            val lookupFile =
                projectPath.resolve("lib/build/kotlin/${compileKotlinTaskName}/cacheable/${compileCacheFolderName}/lookups/file-to-id.tab")
            breakCachesAfterKotlinCompile(subProject("lib"), lookupFile)

            // Perform the first non-incremental build
            build(":lib:compileKotlin") {
                // Caches should be in a corrupted state, which will ensure the next build will fail
                assertFileContains(lookupFile, "Invalid contents")
            }

            // Make a change in the source code
            changeMethodSignatureInLib()

            // In the next build, compilation should be incremental and fail, then fall back to non-incremental compilation and succeed
            build(":lib:compileKotlin") {
                assertIncrementalCompilationFellBackToNonIncremental(BuildAttribute.IC_FAILED_TO_COMPILE_INCREMENTALLY)
                // Also check that the output is not deleted (regression test for KT-49780)
                assertFileExists(lookupFile)
            }
        }
    }

    private fun breakCachesAfterKotlinCompile(gradleProject: GradleProject, lookupFile: Path) {
        gradleProject.buildGradle.appendText(
            """
            $compileKotlinTaskName {
                doLast {
                    new File("${lookupFile.toFile().invariantSeparatorsPath}").write("Invalid contents")
                }
            }
            """.trimIndent()
        )
    }

}

