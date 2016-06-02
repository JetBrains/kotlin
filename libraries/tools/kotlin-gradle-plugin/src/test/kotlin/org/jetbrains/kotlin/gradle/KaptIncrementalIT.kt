package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.allKotlinFiles
import org.jetbrains.kotlin.gradle.util.findFileByName
import org.jetbrains.kotlin.gradle.util.getFileByName
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Test

class KaptIncrementalNoStubsIT : KaptIncrementalBaseIT(shouldUseStubs = false)
class KaptIncrementalWithStubsIT : KaptIncrementalBaseIT(shouldUseStubs = true)

abstract class KaptIncrementalBaseIT(val shouldUseStubs: Boolean): BaseGradleIT() {

    companion object {
        private const val GRADLE_VERSION = "2.10"
        private val EXAMPLE_ANNOTATION_REGEX = "@(field:)?example.ExampleAnnotation".toRegex()
        private const val GENERATE_STUBS_PLACEHOLDER = "GENERATE_STUBS_PLACEHOLDER"
    }

    private fun getProject() =
            Project("kaptIncrementalCompilationProject", GRADLE_VERSION).apply {
                setupWorkingDir()
                val buildGradle = projectDir.parentFile.getFileByName("build.gradle")
                buildGradle.modify { it.replace(GENERATE_STUBS_PLACEHOLDER, shouldUseStubs.toString()) }
            }

    private val annotatedElements =
            arrayOf("A", "funA", "valA", "funUtil", "valUtil", "B", "funB", "valB", "useB")

    override fun defaultBuildOptions(): BuildOptions =
            super.defaultBuildOptions().copy(incremental = true)

    @Test
    fun testBasic() {
        val project = getProject()

        project.build("build") {
            assertSuccessful()
            checkStubUsage()
            checkGenerated(*annotatedElements)
            checkNotGenerated("notAnnotatedFun")
            assertContains("foo.ATest PASSED")
        }

        project.build("build") {
            assertSuccessful()
            assertContains(":compileKotlin UP-TO-DATE",
                           ":compileJava UP-TO-DATE")

            if (shouldUseStubs) {
                assertContains(":compileKotlinAfterJava UP-TO-DATE")
            }
        }
    }

    @Test
    fun testAddAnnotatedElement() {
        val project = getProject()
        project.build("build") {
            assertSuccessful()
        }

        val utilKt = project.projectDir.getFileByName("util.kt")
        utilKt.modify { oldContent ->
            """$oldContent

            @example.ExampleAnnotation
            fun newUtilFun() {}"""
        }

        project.build("build") {
            assertSuccessful()
            // todo: for kapt with stubs check compileKotlin and compileKotlinAfterJava separately
            assertCompiledKotlinSources(project.relativize(utilKt))
            checkGenerated(*(annotatedElements + arrayOf("newUtilFun")))
        }
    }

    @Test
    fun testAddAnnotation() {
        val project = getProject()
        project.build("build") {
            assertSuccessful()
        }

        val utilKt = project.projectDir.getFileByName("util.kt")
        utilKt.modify { it.replace("fun notAnnotatedFun", "@example.ExampleAnnotation fun notAnnotatedFun") }

        project.build("build") {
            assertSuccessful()
            assertCompiledKotlinSources(project.relativize(utilKt))
            checkGenerated(*(annotatedElements + arrayOf("notAnnotatedFun")))
        }
    }

    @Test
    fun testRemoveSourceFile() {
        val project = getProject()
        project.build("build") {
            assertSuccessful()
        }

        with (project.projectDir) {
            getFileByName("B.kt").delete()
            getFileByName("useB.kt").delete()
        }

        project.build("build") {
            assertFailed()
        }

        project.projectDir.getFileByName("JavaClass.java").delete()

        project.build("build") {
            assertSuccessful()
            assertCompiledKotlinSources(project.relativize(project.projectDir.allKotlinFiles()))
            val affectedElements = arrayOf("B", "funB", "valB", "useB")
            checkGenerated(*(annotatedElements.toSet() - affectedElements).toTypedArray())
            checkNotGenerated(*affectedElements)
        }
    }

    @Test
    fun testRemoveAnnotations() {
        val project = getProject()
        project.build("build") {
            assertSuccessful()
        }

        val bKt = project.projectDir.getFileByName("B.kt")
        bKt.modify { it.replace(EXAMPLE_ANNOTATION_REGEX, "") }
        val affectedElements = arrayOf("B", "funB", "valB")

        project.build("build") {
            assertSuccessful()
            if (shouldUseStubs) {
                // java removal is detected
                assertCompiledKotlinSources(project.relativize(project.projectDir.allKotlinFiles()))
            }
            else {
                val useBKt = project.projectDir.getFileByName("useB.kt")
                assertCompiledKotlinSources(project.relativize(bKt, useBKt))
            }
            checkGenerated(*(annotatedElements.toSet() - affectedElements).toTypedArray())
            checkNotGenerated(*affectedElements)
        }
    }

    @Test
    fun testChangeAnnotatedPropertyType() {
        val project = getProject()
        project.build("build") {
            assertSuccessful()
        }

        val bKt = project.projectDir.getFileByName("B.kt")
        val useBKt = project.projectDir.getFileByName("useB.kt")
        bKt.modify { it.replace("val valB = \"text\"", "val valB = 4") }

        project.build("build") {
            assertSuccessful()
            assertCompiledKotlinSources(project.relativize(bKt, useBKt))
            checkGenerated(*annotatedElements)
        }
    }

    private fun CompiledProject.checkGenerated(vararg annotatedElementNames: String) {
        getGeneratedFileNames(*annotatedElementNames).forEach {
            val file = project.projectDir.getFileByName(it)
            assert(file.isFile) { "$file must exist" }
        }
    }

    private fun CompiledProject.checkNotGenerated(vararg annotatedElementNames: String) {
        getGeneratedFileNames(*annotatedElementNames).forEach {
            val file = project.projectDir.findFileByName(it)
            assert(file == null) { "$file must not exist" }
        }
    }

    private fun getGeneratedFileNames(vararg annotatedElementNames: String): Iterable<String> {
        val names = annotatedElementNames.map { it.capitalize() + "Generated" }
        return names.map { it + ".java" }
    }

    private fun CompiledProject.checkStubUsage() {
        val usingStubs = "kapt: Using class file stubs"

        if (shouldUseStubs) {
            assertContains(usingStubs)
        }
        else {
            assertNotContains(usingStubs)
        }
    }
}