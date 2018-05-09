package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.allKotlinFiles
import org.jetbrains.kotlin.gradle.util.getFileByName
import org.jetbrains.kotlin.gradle.util.getFilesByNames
import org.jetbrains.kotlin.gradle.util.modify
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Test
import java.io.File

class IncrementalCompilationMultiProjectIT : BaseGradleIT() {
    companion object {
        private val ANDROID_GRADLE_PLUGIN_VERSION = "1.5.+"
    }

    private fun androidBuildOptions() =
        BuildOptions(
            withDaemon = true,
            androidHome = KotlinTestUtils.findAndroidSdk(),
            androidGradlePluginVersion = ANDROID_GRADLE_PLUGIN_VERSION,
            incremental = true
        )

    override fun defaultBuildOptions(): BuildOptions =
        super.defaultBuildOptions().copy(withDaemon = true, incremental = true)

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

    @Test
    fun testMoveFunctionFromLib() {
        val project = Project("incrementalMultiproject")
        project.build("build") {
            assertSuccessful()
        }

        val barUseABKt = project.projectDir.getFileByName("barUseAB.kt")
        val barInApp = File(project.projectDir, "app/src/main/java/bar").apply { mkdirs() }
        barUseABKt.copyTo(File(barInApp, barUseABKt.name))
        barUseABKt.delete()

        project.build("build") {
            assertSuccessful()
            val affectedSources = project.projectDir.getFilesByNames("fooCallUseAB.kt", "barUseAB.kt")
            val relativePaths = project.relativize(affectedSources)
            assertCompiledKotlinSources(relativePaths, weakTesting = false)
        }
    }

    @Test
    fun testAddNewMethodToLib() {
        val project = Project("incrementalMultiproject")
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
            val affectedSources = project.projectDir.getFilesByNames("A.kt", "B.kt", "AA.kt", "BB.kt")
            val relativePaths = project.relativize(affectedSources)
            assertCompiledKotlinSources(relativePaths, weakTesting = false)
        }
    }

    @Test
    fun testLibClassBecameFinal() {
        val project = Project("incrementalMultiproject")
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
            assertCompiledKotlinSources(relativePaths, weakTesting = false)
        }
    }

    @Test
    fun testCleanBuildLib() {
        // Test with Gradle 3.4, since Gradle 3.5+ uses classpath hash normalization for JARs
        val project = Project("incrementalMultiproject", GradleVersionRequired.Exact("3.4"))

        project.setupWorkingDir()

        project.build("build") {
            assertSuccessful()
        }

        project.build(":lib:clean", ":lib:build") {
            assertSuccessful()
            val affectedSources = File(project.projectDir, "lib").allKotlinFiles()
            val relativePaths = project.relativize(affectedSources)
            assertCompiledKotlinSources(relativePaths, weakTesting = false)
        }

        project.build("build") {
            assertSuccessful()
            val affectedSources = File(project.projectDir, "app").allKotlinFiles()
            val relativePaths = project.relativize(affectedSources)
            assertCompiledKotlinSources(relativePaths, weakTesting = false)
        }
    }

    @Test
    fun testCompileErrorInLib() {
        val project = Project("incrementalMultiproject")
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
            assertCompiledKotlinSources(relativePaths, weakTesting = false)
        }
    }

    // checks that multi-project ic is disabled when there is a task that outputs to javaDestination dir
    // that is not JavaCompile or KotlinCompile
    @Test
    fun testCompileLibWithGroovy() {
        val gradleVersion = GradleVersionRequired.Exact("3.5") // With newer versions, Groovy uses separate classes dirs
        val project = Project("incrementalMultiproject", gradleVersion)
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
            assertCompiledKotlinSources(relativePaths, weakTesting = false)
        }
    }

    @Test
    fun testAndroid() {
        val project = Project("AndroidProject", GradleVersionRequired.Exact("2.10"))
        val options = androidBuildOptions()

        project.build("assembleDebug", options = options) {
            assertSuccessful()
        }

        val libUtilKt = project.projectDir.getFileByName("libUtil.kt")
        libUtilKt.modify { it.replace("fun libUtil(): String", "fun libUtil(): None") }

        project.build("assembleDebug", options = options) {
            assertSuccessful()
            val affectedSources = project.projectDir.getFilesByNames("libUtil.kt", "MainActivity2.kt")
            assertCompiledKotlinSources(project.relativize(affectedSources), weakTesting = false)
        }
    }
}

class IncrementalJavaChangeDefaultIT : IncrementalCompilationJavaChangesBase(usePreciseJavaTracking = null) {
    @Test
    override fun testModifySignatureTrackedJavaInLib() {
        doTest(trackedJavaClass, changeSignature, expectedAffectedSources = listOf("TrackedJavaClassChild.kt", "useTrackedJavaClass.kt"))
    }

    @Test
    override fun testModifyBodyTrackedJavaInLib() {
        doTest(trackedJavaClass, changeBody, expectedAffectedSources = listOf())
    }
}

class IncrementalJavaChangePreciseIT : IncrementalCompilationJavaChangesBase(usePreciseJavaTracking = true) {
    @Test
    override fun testModifySignatureTrackedJavaInLib() {
        doTest(trackedJavaClass, changeSignature, expectedAffectedSources = listOf("TrackedJavaClassChild.kt", "useTrackedJavaClass.kt"))
    }

    @Test
    override fun testModifyBodyTrackedJavaInLib() {
        doTest(trackedJavaClass, changeBody, expectedAffectedSources = listOf())
    }
}

class IncrementalJavaChangeDisablePreciseIT : IncrementalCompilationJavaChangesBase(usePreciseJavaTracking = false) {
    @Test
    override fun testModifySignatureTrackedJavaInLib() {
        doTest(
            trackedJavaClass, changeSignature,
            expectedAffectedSources = listOf(
                "TrackedJavaClassChild.kt", "useTrackedJavaClass.kt", "useTrackedJavaClassFooMethodUsage.kt",
                "useTrackedJavaClassSameModule.kt"
            )
        )
    }

    @Test
    override fun testModifyBodyTrackedJavaInLib() {
        doTest(
            trackedJavaClass, changeBody,
            expectedAffectedSources = listOf(
                "TrackedJavaClassChild.kt", "useTrackedJavaClass.kt", "useTrackedJavaClassFooMethodUsage.kt",
                "useTrackedJavaClassSameModule.kt"
            )
        )
    }
}

abstract class IncrementalCompilationJavaChangesBase(val usePreciseJavaTracking: Boolean?) : BaseGradleIT() {
    companion object {
        protected val GRADLE_VERSION = "2.10"
    }

    override fun defaultBuildOptions(): BuildOptions =
        super.defaultBuildOptions().copy(withDaemon = true, incremental = true)

    protected val trackedJavaClass = "TrackedJavaClass.java"
    private val javaClass = "JavaClass.java"
    protected val changeBody: (String) -> String = { it.replace("Hello, World!", "Hello, World!!!!") }
    protected val changeSignature: (String) -> String = { it.replace("String getString", "Object getString") }

    @Test
    fun testModifySignatureJavaInLib() {
        doTest(
            javaClass, changeBody,
            expectedAffectedSources = listOf("JavaClassChild.kt", "useJavaClass.kt", "useJavaClassFooMethodUsage.kt")
        )
    }

    @Test
    fun testModifyBodyJavaInLib() {
        doTest(
            javaClass, changeBody,
            expectedAffectedSources = listOf("JavaClassChild.kt", "useJavaClass.kt", "useJavaClassFooMethodUsage.kt")
        )
    }

    abstract fun testModifySignatureTrackedJavaInLib()
    abstract fun testModifyBodyTrackedJavaInLib()

    protected fun doTest(
        fileToModify: String,
        transformFile: (String) -> String,
        expectedAffectedSources: Collection<String>
    ) {
        val project = Project("incrementalMultiproject")

        val options = defaultBuildOptions().copy(usePreciseJavaTracking = usePreciseJavaTracking)
        project.build("build", options = options) {
            assertSuccessful()
        }

        val javaClassJava = project.projectDir.getFileByName(fileToModify)
        javaClassJava.modify(transformFile)

        project.build("build", options = options) {
            assertSuccessful()
            val affectedSources = project.projectDir.getFilesByNames(*expectedAffectedSources.toTypedArray())
            val relativePaths = project.relativize(affectedSources)
            assertCompiledKotlinSources(relativePaths, weakTesting = false)
        }
    }
}
