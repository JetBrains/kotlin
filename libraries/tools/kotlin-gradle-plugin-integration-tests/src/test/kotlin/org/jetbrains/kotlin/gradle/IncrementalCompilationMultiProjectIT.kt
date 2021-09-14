package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.*
import org.junit.Test
import java.io.File

class IncrementalCompilationJsMultiProjectIT : BaseIncrementalCompilationMultiProjectIT() {
    override fun defaultProject(): Project {
        val project = Project("incrementalMultiproject")
        project.setupWorkingDir()

        for (subProject in arrayOf("app", "lib")) {
            val subProjectDir = project.projectDir.resolve(subProject)
            subProjectDir.resolve("src/main/java").deleteRecursively()
            val buildGradle = subProjectDir.resolve("build.gradle")
            val buildJsGradle = subProjectDir.resolve("build-js.gradle")
            buildJsGradle.copyTo(buildGradle, overwrite = true)
            buildJsGradle.delete()
        }

        return project
    }

    override val additionalLibDependencies: String =
        "implementation \"org.jetbrains.kotlin:kotlin-test-js:${'$'}kotlin_version\""

    override val compileKotlinTaskName: String
        get() = "compileKotlin2Js"
}

open class IncrementalCompilationJvmMultiProjectIT : BaseIncrementalCompilationMultiProjectIT() {
    override val additionalLibDependencies: String =
        "implementation \"org.jetbrains.kotlin:kotlin-test:${'$'}kotlin_version\""

    override val compileKotlinTaskName: String
        get() = "compileKotlin"

    override fun defaultProject(): Project =
        Project("incrementalMultiproject")

    // todo: do the same for js backend
    @Test
    fun testDuplicatedClass() {
        val project = Project("duplicatedClass")
        project.build("build") {
            assertSuccessful()
        }

        val usagesFiles = listOf("useBuzz.kt", "useA.kt").map { project.projectFile(it) }
        usagesFiles.forEach { file -> file.modify { "$it\n " } }

        project.build("build") {
            assertSuccessful()
            assertCompiledKotlinSources(project.relativize(usagesFiles))
        }
    }

    // checks that multi-project ic is disabled when there is a task that outputs to javaDestination dir
    // that is not JavaCompile or KotlinCompile
    @Test
    open fun testCompileLibWithGroovy() {
        testCompileLibWithGroovy_doTest {
            assertCompiledKotlinFiles(
                File(project.projectDir, "app").allKotlinFiles() + File(project.projectDir, "lib").getFileByName("A.kt")
            )
        }
    }

    protected fun testCompileLibWithGroovy_doTest(assertResults: CompiledProject.() -> Unit) {
        val project = defaultProject()
        project.setupWorkingDir()
        val lib = File(project.projectDir, "lib")
        val libBuildGradle = File(lib, "build.gradle")
        libBuildGradle.modify {
            """
            apply plugin: 'groovy'
            apply plugin: 'kotlin'

            dependencies {
                implementation "org.jetbrains.kotlin:kotlin-stdlib:${"$"}kotlin_version"
                implementation 'org.codehaus.groovy:groovy-all:2.4.8'
            }
            """.trimIndent()
        }

        val libGroovySrcBar = File(lib, "src/main/groovy/bar").apply { mkdirs() }
        val groovyClass = File(libGroovySrcBar, "GroovyClass.groovy")
        groovyClass.writeText(
            """
            package bar

            class GroovyClass {}
        """
        )

        project.build("build") {
            assertSuccessful()
        }

        project.changeMethodBodyInLib()
        project.build("build") {
            assertSuccessful()
            assertResults()
        }
    }

    /** Regression test for KT-43489. Make sure build history mapping is not initialized too early. */
    @Test
    fun testBuildHistoryMappingLazilyComputedWithWorkers() {
        val project = defaultProject()
        project.setupWorkingDir()
        project.projectDir.resolve("app/build.gradle").appendText(
            """
                // added to force eager configuration
                tasks.withType(JavaCompile) {
                    options.encoding = 'UTF-8'
                }
            """.trimIndent()
        )
        val options = defaultBuildOptions().copy(parallelTasksInProject = true)
        project.build(options = options, params = arrayOf("build")) {
            assertSuccessful()
        }

        val aKt = project.projectDir.getFileByName("A.kt")
        aKt.writeText(
            """
package bar

open class A {
    fun a() {}
    fun newA() {}
}
"""
        )

        project.build(options = options, params = arrayOf("build")) {
            assertSuccessful()
            val affectedSources = project.projectDir.getFilesByNames("A.kt", "B.kt", "AA.kt", "AAA.kt", "BB.kt")
            val relativePaths = project.relativize(affectedSources)
            assertCompiledKotlinSources(relativePaths)
        }
    }
}

class IncrementalCompilationFirJvmMultiProjectIT : IncrementalCompilationJvmMultiProjectIT() {
    override fun defaultBuildOptions(): BuildOptions {
        return super.defaultBuildOptions().copy(useFir = true)
    }
}

class IncrementalCompilationClasspathSnapshotJvmMultiProjectIT : IncrementalCompilationJvmMultiProjectIT() {

    override fun defaultBuildOptions() = super.defaultBuildOptions().copy(useClasspathSnapshot = true)

    @Test
    override fun testNonAbiChangeInLib_changeMethodBody() {
        doTest(
            modifyProject = changeMethodBodyInLib,
            assertResults = {
                assertTasksExecuted(":lib:$compileKotlinTaskName")
                assertTasksUpToDate(":app:$compileKotlinTaskName") // App compilation has 'compile avoidance'
                assertCompiledKotlinFiles(File(project.projectDir, "lib").getFilesByNames("A.kt"))
            }
        )
    }

    @Test
    override fun testAddDependencyInLib() {
        doTest(
            modifyProject = { testAddDependencyInLib_modifyProject() },
            assertResults = {
                assertTasksExecuted(":lib:$compileKotlinTaskName")
                assertTasksUpToDate(":app:$compileKotlinTaskName")
                assertCompiledKotlinFiles(emptyList()) // Lib compilation is incremental (no files are recompiled)
            }
        )
    }

    @Test
    override fun testAbiChangeInLib_afterLibClean() {
        doTest(
            modifyProject = {
                build(":lib:clean") { assertSuccessful() }
                changeMethodSignatureInLib()
            },
            assertResults = {
                assertCompiledKotlinFiles(
                    // App compilation is incremental
                    File(project.projectDir, "app").getFilesByNames("AA.kt", "AAA.kt", "BB.kt", "fooUseA.kt") +
                            File(project.projectDir, "lib").allKotlinFiles()
                )
            }
        )
    }

    @Test
    override fun testCompileLibWithGroovy() {
        testCompileLibWithGroovy_doTest {
            assertTasksExecuted(":lib:$compileKotlinTaskName")
            assertTasksUpToDate(":app:$compileKotlinTaskName") // App compilation has 'compile avoidance'
            assertCompiledKotlinFiles(listOf(File(project.projectDir, "lib").getFileByName("A.kt")))
        }
    }

    @Test
    override fun testAbiChangeInLib_afterLibClean_withAbiSnapshot() {
        doTest(
            options = defaultBuildOptions().copy(abiSnapshot = true),
            modifyProject = {
                build(":lib:clean") { assertSuccessful() }
                changeMethodSignatureInLib()
            },
            assertResults = {
                assertCompiledKotlinFiles(
                    // App compilation is incremental
                    File(project.projectDir, "app").getFilesByNames("AA.kt", "AAA.kt", "BB.kt", "fooUseA.kt") +
                            File(project.projectDir, "lib").allKotlinFiles()
                )
            }
        )
    }
}

abstract class BaseIncrementalCompilationMultiProjectIT : IncrementalCompilationBaseIT() {

    protected abstract val compileKotlinTaskName: String

    protected abstract val additionalLibDependencies: String

    protected val changeMethodSignatureInLib: Project.() -> Unit = {
        File(projectDir, "lib").getFileByName("A.kt").modify {
            it.replace("fun a() {}", "fun a(): Int = 1")
        }
    }

    protected val changeMethodBodyInLib: Project.() -> Unit = {
        File(projectDir, "lib").getFileByName("A.kt").modify {
            it.replace("fun a() {}", "fun a() { println() }")
        }
    }

    @Test
    fun testAbiChangeInLib_changeMethodSignature() {
        doTest(
            modifyProject = changeMethodSignatureInLib,
            expectedCompiledFileNames = listOf(
                "A.kt", "B.kt", "barUseA.kt", // In lib
                "AA.kt", "AAA.kt", "BB.kt", "fooUseA.kt" // In app
            )
        )
    }

    @Test
    fun testAbiChangeInLib_addNewMethod() {
        doTest(
            modifyProject = {
                File(projectDir, "lib").getFileByName("A.kt").modify {
                    it.replace("fun a() {}", "fun a() {}\nfun newA() {}")
                }
            },
            expectedCompiledFileNames = listOf(
                "A.kt", "B.kt", // In lib
                "AA.kt", "AAA.kt", "BB.kt" // In app
            )
        )
    }

    @Test
    open fun testNonAbiChangeInLib_changeMethodBody() {
        doTest(
            modifyProject = changeMethodBodyInLib,
            assertResults = {
                assertCompiledKotlinFiles(File(project.projectDir, "lib").getFilesByNames("A.kt"))
            }
        )
    }

    @Test
    open fun testAddDependencyInLib() {
        doTest(
            modifyProject = { testAddDependencyInLib_modifyProject() },
            assertResults = {
                assertTasksExecuted(":lib:$compileKotlinTaskName")
                assertTasksUpToDate(":app:$compileKotlinTaskName")
                assertCompiledKotlinFiles(File(project.projectDir, "lib").allKotlinFiles())
            }
        )
    }

    protected fun Project.testAddDependencyInLib_modifyProject() {
        File(projectDir, "lib/build.gradle").modify {
            """
            $it

            dependencies {
                $additionalLibDependencies
            }
            """.trimIndent()
        }
    }

    @Test
    open fun testAbiChangeInLib_afterLibClean() { // To see if app compilation can be incremental after non-incremental lib compilation
        doTest(
            modifyProject = {
                build(":lib:clean") { assertSuccessful() }
                changeMethodSignatureInLib()
            },
            assertResults = {
                // App compilation is non-incremental
                assertCompiledKotlinFiles(project.projectDir.allKotlinFiles())
            }
        )
    }

    @Test
    fun testMoveFunctionFromLibToApp() {
        doTest(
            modifyProject = {
                val barUseABKt = projectDir.getFileByName("barUseAB.kt")
                val barInApp = File(projectDir, "app/src/main/kotlin/bar").apply { mkdirs() }
                barUseABKt.copyTo(File(barInApp, barUseABKt.name))
                barUseABKt.delete()
            },
            expectedCompiledFileNames = listOf("fooCallUseAB.kt", "barUseAB.kt")
        )
    }

    @Test
    fun testLibClassBecameFinal() {
        // TODO: fix fir IC and remove
        if (defaultBuildOptions().useFir) return

        val project = defaultProject()
        project.build("build") {
            assertSuccessful()
        }

        val bKt = project.projectDir.getFileByName("B.kt")
        bKt.modify { it.replace("open class", "class") }

        project.build("build") {
            assertFailed()
            val affectedSources = project.projectDir.getFilesByNames(
                "B.kt", "barUseAB.kt", "barUseB.kt",
                "BB.kt", "fooCallUseAB.kt", "fooUseB.kt"
            )
            val relativePaths = project.relativize(affectedSources)
            assertCompiledKotlinSources(relativePaths)
        }
    }

    @Test
    fun testCompileErrorInLib() {
        val project = defaultProject()
        project.build("build") {
            assertSuccessful()
        }

        val bKt = project.projectDir.getFileByName("B.kt")
        val bKtContent = bKt.readText()
        bKt.delete()

        fun runFailingBuild() {
            project.build("build") {
                assertFailed()
                assertContains("B.kt has been removed")
                assertTasksFailed(":lib:$compileKotlinTaskName")
                val affectedFiles = project.projectDir.getFilesByNames("barUseAB.kt", "barUseB.kt")
                assertCompiledKotlinSources(project.relativize(affectedFiles))
            }
        }

        runFailingBuild()
        runFailingBuild()

        bKt.writeText(bKtContent.replace("fun b", "open fun b"))

        project.build("build") {
            assertSuccessful()
            val affectedFiles = project.projectDir.getFilesByNames(
                "B.kt", "barUseAB.kt", "barUseB.kt",
                "BB.kt", "fooUseB.kt"
            )
            assertCompiledKotlinSources(project.relativize(affectedFiles))
        }
    }

    @Test
    fun testRemoveLibFromClasspath() {
        val project = defaultProject()
        project.build("build") {
            assertSuccessful()
        }

        val appBuildGradle = project.projectDir.resolve("app/build.gradle")
        val appBuildGradleContent = appBuildGradle.readText()
        appBuildGradle.modify { it.checkedReplace("implementation project(':lib')", "") }
        val aaKt = project.projectDir.getFileByName("AA.kt")
        aaKt.addNewLine()

        project.build("build") {
            assertFailed()
        }

        appBuildGradle.writeText(appBuildGradleContent)
        aaKt.addNewLine()

        project.build("build") {
            assertSuccessful()
            assertCompiledKotlinSources(project.relativize(aaKt))
        }
    }

    /** Regression test for KT-40875. */
    @Test
    fun testMoveFunctionFromLibWithRemappedBuildDirs() {
        val project = defaultProject()
        project.setupWorkingDir()
        project.projectDir.resolve("build.gradle").appendText(
            """

            allprojects {
                it.buildDir = new File(rootDir,  "../out" + it.path.replace(":", "/") + "/build")
            }
            """.trimIndent()
        )
        project.build("build") {
            assertSuccessful()
        }

        val barUseABKt = project.projectDir.getFileByName("barUseAB.kt")
        val barInApp = File(project.projectDir, "app/src/main/kotlin/bar").apply { mkdirs() }
        barUseABKt.copyTo(File(barInApp, barUseABKt.name))
        barUseABKt.delete()

        project.build("build") {
            assertSuccessful()
            val affectedSources = project.projectDir.getFilesByNames("fooCallUseAB.kt", "barUseAB.kt")
            val relativePaths = project.relativize(affectedSources)
            assertCompiledKotlinSources(relativePaths)
        }
    }

    @Test
    fun testAbiChangeInLib_addNewMethod_withAbiSnapshot() {
        doTest(
            options = defaultBuildOptions().copy(abiSnapshot = true),
            modifyProject = {
                File(projectDir, "lib").getFileByName("A.kt").modify {
                    it.replace("fun a() {}", "fun a() {}\nfun newA() {}")
                }
            },
            expectedCompiledFileNames = listOf(
                "A.kt", "B.kt", // In lib
                // TODO(valtman): for abi-snapshot "BB.kt" should not be recompiled
                "AA.kt", "AAA.kt", "BB.kt" // In app
            )
        )
    }

    @Test
    open fun testAbiChangeInLib_afterLibClean_withAbiSnapshot() {
        doTest(
            options = defaultBuildOptions().copy(abiSnapshot = true),
            modifyProject = {
                build(":lib:clean") { assertSuccessful() }
                changeMethodSignatureInLib()
            },
            assertResults = {
                // TODO: With ABI snapshot, app compilation should be incremental, currently it is not.
                assertCompiledKotlinFiles(project.projectDir.allKotlinFiles())
            }
        )
    }

    @Test
    fun testChangeIsolatedClassInLib_withAbiSnapshot() {
        doTest(
            options = defaultBuildOptions().copy(abiSnapshot = true),
            modifyProject = {
                File(projectDir, "lib").getFileByName("BarDummy.kt").modify {
                    "$it { fun m() = 42}"
                }
            },
            expectedCompiledFileNames = listOf("BarDummy.kt") // In lib
        )
    }
}
