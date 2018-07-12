package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.allKotlinFiles
import org.jetbrains.kotlin.gradle.util.getFileByName
import org.jetbrains.kotlin.gradle.util.getFilesByNames
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Assert
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
}

class IncrementalCompilationJvmMultiProjectIT : BaseIncrementalCompilationMultiProjectIT() {
    override val additionalLibDependencies: String =
        "implementation \"org.jetbrains.kotlin:kotlin-stdlib:${'$'}kotlin_version\""

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
    fun testCompileLibWithGroovy() {
        val project = defaultProject()
        project.setupWorkingDir()
        val lib = File(project.projectDir, "lib")
        val libBuildGradle = File(lib, "build.gradle")
        libBuildGradle.modify {
            """
            apply plugin: 'groovy'
            apply plugin: 'kotlin'

            dependencies {
                compile 'org.codehaus.groovy:groovy-all:2.4.7'
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

        project.projectDir.getFileByName("barUseB.kt").delete()
        project.build("build") {
            assertSuccessful()
            val affectedSources = File(project.projectDir, "app").allKotlinFiles()
            val relativePaths = project.relativize(affectedSources)
            assertCompiledKotlinSources(relativePaths)
        }
    }
}

abstract class BaseIncrementalCompilationMultiProjectIT : BaseGradleIT() {
    override fun defaultBuildOptions(): BuildOptions =
        super.defaultBuildOptions().copy(withDaemon = true, incremental = true)

    protected abstract fun defaultProject(): Project

    @Test
    fun testMoveFunctionFromLib() {
        val project = defaultProject()
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
    fun testAddNewMethodToLib() {
        val project = defaultProject()
        project.build("build") {
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

        project.build("build") {
            assertSuccessful()
            val affectedSources = project.projectDir.getFilesByNames("A.kt", "B.kt", "AA.kt", "AAA.kt", "BB.kt")
            val relativePaths = project.relativize(affectedSources)
            assertCompiledKotlinSources(relativePaths)
        }
    }

    @Test
    fun testLibClassBecameFinal() {
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
    fun testCleanBuildLib() {
        val project = defaultProject()

        project.build("build") {
            assertSuccessful()
        }

        project.build(":lib:clean") {
            assertSuccessful()
        }

        // Change file so Gradle won't skip :app:compile
        project.projectFile("BarDummy.kt").modify {
            it.replace("class BarDummy", "open class BarDummy")
        }
        project.build("build") {
            assertSuccessful()
            val affectedSources = project.projectDir.allKotlinFiles()
            val relativePaths = project.relativize(affectedSources)
            assertCompiledKotlinSources(relativePaths)
        }

        val aaKt = project.projectFile("AA.kt")
        aaKt.modify { "$it " }
        project.build("build") {
            assertSuccessful()
            assertCompiledKotlinSources(project.relativize(aaKt))
        }
    }

    protected abstract val additionalLibDependencies: String

    @Test
    fun testAddDependencyToLib() {
        val project = defaultProject()

        project.build("build") {
            assertSuccessful()
        }

        val libBuildGradle = File(project.projectDir, "lib/build.gradle")
        Assert.assertTrue("$libBuildGradle does not exist", libBuildGradle.exists())
        libBuildGradle.modify {
            """
                $it

                dependencies {
                    $additionalLibDependencies
                }
            """.trimIndent()
        }
        // Change file so Gradle won't skip :app:compile
        project.projectFile("BarDummy.kt").modify {
            it.replace("class BarDummy", "open class BarDummy")
        }

        project.build("build") {
            assertSuccessful()
            val affectedSources = project.projectDir.allKotlinFiles()
            val relativePaths = project.relativize(affectedSources)
            assertCompiledKotlinSources(relativePaths)
        }

        val aaKt = project.projectFile("AA.kt")
        aaKt.modify { "$it " }
        project.build("build") {
            assertSuccessful()
            assertCompiledKotlinSources(project.relativize(aaKt))
        }
    }

    @Test
    fun testCompileErrorInLib() {
        val project = defaultProject()
        project.build("build") {
            assertSuccessful()
        }

        val bKt = project.projectDir.getFileByName("B.kt")
        bKt.delete()

        project.build("build") {
            assertFailed()
        }

        project.projectDir.getFileByName("barUseB.kt").delete()
        project.projectDir.getFileByName("barUseAB.kt").delete()

        project.build("build") {
            assertFailed()
            val affectedSources = project.projectDir.allKotlinFiles()
            val relativePaths = project.relativize(affectedSources)
            assertCompiledKotlinSources(relativePaths)
        }
    }
}

