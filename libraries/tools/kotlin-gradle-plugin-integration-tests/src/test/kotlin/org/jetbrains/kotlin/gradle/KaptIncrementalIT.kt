package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import java.io.File
import java.nio.file.Path
import java.util.*
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.deleteRecursively
import kotlin.io.path.writeText
import kotlin.test.assertEquals

@DisplayName("Kapt incremental compilation")
@OtherGradlePluginTests
open class KaptIncrementalIT : KGPBaseTest() {
    companion object {
        private val EXAMPLE_ANNOTATION_REGEX = "@(field:)?example.ExampleAnnotation".toRegex()
        const val PROJECT_NAME = "kaptIncrementalCompilationProject"
        const val KAPT3_STUBS_PATH = "build/tmp/kapt3/stubs/main"
    }

    private val annotatedElements =
        arrayOf("A", "funA", "valA", "funUtil", "valUtil", "B", "funB", "valB", "useB")

    override val defaultBuildOptions = super.defaultBuildOptions.copy(
        incremental = true,
        kaptOptions = BuildOptions.KaptOptions(incrementalKapt = true)
    )

    protected open fun KGPBaseTest.kaptProject(
        gradleVersion: GradleVersion,
        buildOptions: BuildOptions = defaultBuildOptions,
        buildJdk: File? = null,
        test: TestProject.() -> Unit
    ): TestProject = project(
        PROJECT_NAME,
        gradleVersion,
        buildOptions = buildOptions,
        buildJdk = buildJdk,
        test = test
    )

    @DisplayName("After adding new line compilation is incremental")
    @GradleTest
    fun testAddNewLine(gradleVersion: GradleVersion) {
        kaptProject(gradleVersion) {
            build("clean", "build")

            javaSourcesDir().resolve("bar/useB.kt").modify { "\n$it" }
            build("build") {
                assertTasksExecuted(":kaptGenerateStubsKotlin", ":compileKotlin")
                assertTasksUpToDate(":kaptKotlin")
                assertTasksUpToDate(":compileJava")
            }
        }
    }

    @DisplayName("On rebuild without changes tasks should be UP-TO-DATE")
    @GradleTest
    fun testBasic(gradleVersion: GradleVersion) {
        kaptProject(
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            build("build") {
                checkGenerated(kaptGeneratedToPath, *annotatedElements)
                checkNotGenerated(kaptGeneratedToPath, "notAnnotatedFun")
                assertOutputContains("foo.ATest PASSED")
            }

            build("build") {
                assertTasksUpToDate(
                    ":compileKotlin",
                    ":compileJava"
                )

                assertTasksUpToDate(
                    ":kaptKotlin",
                    ":kaptGenerateStubsKotlin"
                )
            }
        }
    }

    @DisplayName("Successfully rebuild after error")
    @GradleTest
    fun testCompileError(gradleVersion: GradleVersion) {
        kaptProject(gradleVersion) {
            build("assemble")

            val bKt = javaSourcesDir().resolve("bar/B.kt")
            val errorKt = bKt.resolveSibling("error.kt")
            errorKt.writeText("<COMPILE_ERROR_MARKER>")

            buildAndFail("assemble") {
                assertTasksFailed(":kaptGenerateStubsKotlin")
            }

            errorKt.deleteIfExists()
            bKt.modify { "$it\n" }
            build("assemble", buildOptions = buildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertCompiledKotlinSources(listOf(projectPath.relativize(bKt)), output)
                assertTasksExecuted(":kaptGenerateStubsKotlin", ":compileKotlin")
            }
        }
    }

    @DisplayName("Change in the function body without changing the signature")
    @GradleTest
    fun testChangeFunctionBodyWithoutChangingSignature(gradleVersion: GradleVersion) {
        kaptProject(gradleVersion) {
            build("build", buildOptions = buildOptions.copy(logLevel = LogLevel.DEBUG)) {
                checkGenerated(kaptGeneratedToPath, *annotatedElements)
                checkNotGenerated(kaptGeneratedToPath, "notAnnotatedFun")
                assertOutputContains("foo.ATest PASSED")
            }

            val utilKt = javaSourcesDir().resolve("baz/util.kt")
            utilKt.modify { oldContent ->
                assert(oldContent.contains("2 * 2 == 4"))
                oldContent.replace("2 * 2 == 4", "2 * 2 == 5")
            }

            build("assemble") {
                assertTasksExecuted(":kaptGenerateStubsKotlin")
                assertTasksUpToDate(":kaptKotlin")
            }
        }
    }

    @DisplayName("Adding new annotated element")
    @GradleTest
    fun testAddAnnotatedElement(gradleVersion: GradleVersion) {
        kaptProject(gradleVersion) {
            build("assemble")

            val utilKt = javaSourcesDir().resolve("baz/util.kt")
            utilKt.modify { oldContent ->
                """
                $oldContent

                @example.ExampleAnnotation
                fun newUtilFun() {}
                """.trimIndent()
            }

            build("assemble", buildOptions = buildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertKapt3FullyExecuted()

                assertCompiledKotlinSourcesHandleKapt3(this, listOf(projectPath.relativize(utilKt)))
                checkGenerated(kaptGeneratedToPath, *(annotatedElements + arrayOf("newUtilFun")))
            }
        }
    }

    @DisplayName("Adding new annotation triggers kapt run")
    @GradleTest
    fun testAddAnnotation(gradleVersion: GradleVersion) {
        kaptProject(gradleVersion) {
            build("assemble")

            val utilKt = javaSourcesDir().resolve("baz/util.kt")
            utilKt.modify {
                it.replace("fun notAnnotatedFun", "@example.ExampleAnnotation fun notAnnotatedFun")
            }

            build("assemble", buildOptions = buildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertKapt3FullyExecuted()
                assertCompiledKotlinSources(listOf(projectPath.relativize(utilKt)), output)
                checkGenerated(kaptGeneratedToPath, *(annotatedElements + arrayOf("notAnnotatedFun")))
            }
        }
    }

    @DisplayName("Kapt run is incremental after source file was removed")
    @GradleTest
    fun testRemoveSourceFile(gradleVersion: GradleVersion) {
        kaptProject(gradleVersion) {
            val kapt3IncDataPath = "build/tmp/kapt3/incrementalData/main"
            val kapt3StubsPath = "build/tmp/kapt3/stubs/main"

            build("assemble") {
                assertKapt3FullyExecuted()

                assertFileInProjectExists("$kapt3IncDataPath/bar/B.class")
                assertFileInProjectExists("$kapt3IncDataPath/bar/UseBKt.class")
                assertFileInProjectExists("$kapt3StubsPath/bar/B.java")
                assertFileInProjectExists("$kapt3StubsPath/bar/B.kapt_metadata")
                assertFileInProjectExists("$kapt3StubsPath/bar/UseBKt.java")
                assertFileInProjectExists("$kapt3StubsPath/bar/UseBKt.kapt_metadata")
            }

            with(javaSourcesDir()) {
                resolve("bar/B.kt").deleteIfExists()
                resolve("bar/useB.kt").deleteIfExists()
            }

            buildAndFail("assemble") {
                assertFileInProjectNotExists("$kapt3IncDataPath/bar/B.class")
                assertFileInProjectNotExists("$kapt3IncDataPath/bar/UseBKt.class")
                assertFileInProjectNotExists("$kapt3StubsPath/bar/B.java")
                assertFileInProjectNotExists("$kapt3StubsPath/bar/B.kaptMetadata")
                assertFileInProjectNotExists("$kapt3StubsPath/bar/UseBKt.java")
                assertFileInProjectNotExists("$kapt3StubsPath/bar/UseBKt.kaptMetadata")
            }

            javaSourcesDir().resolve("foo/JavaClass.java").deleteIfExists()

            build("assemble", buildOptions = buildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertKapt3FullyExecuted()
                assertCompiledKotlinSourcesHandleKapt3(
                    this,
                    projectPath.allKotlinFiles.map { projectPath.relativize(it) }
                )
                val affectedElements = arrayOf("B", "funB", "valB", "useB")
                checkGenerated(kaptGeneratedToPath, *(annotatedElements.toSet() - affectedElements).toTypedArray())
                checkNotGenerated(kaptGeneratedToPath, *affectedElements)
            }
        }
    }

    @DisplayName("Incremental kapt run is correct after removing all Kotlin sources")
    @GradleTest
    fun testRemoveAllKotlinSources(gradleVersion: GradleVersion) {
        kaptProject(gradleVersion) {
            build("assemble") {
                assertFileInProjectExists("$KAPT3_STUBS_PATH/bar/UseBKt.java")
            }

            with(projectPath) {
                resolve("src/").deleteRecursively()
                resolve("src/main/java/bar").createDirectories()
                resolve("src/main/java/bar/MyClass.java").writeText(
                    """
                    package bar;
                    public class MyClass {}
                    """.trimIndent()
                )
            }

            build("assemble") {
                // Make sure all generated stubs are removed (except for NonExistentClass).
                assertEquals(
                    listOf(projectPath.resolve("$KAPT3_STUBS_PATH/error/NonExistentClass.java").toRealPath().toString()),
                    projectPath
                        .resolve(KAPT3_STUBS_PATH)
                        .toFile()
                        .walk()
                        .filter { it.extension == "java" }
                        .map { it.canonicalPath }
                        .toList()
                )
                // Make sure all compiled kt files are cleaned up.
                assertEquals(
                    emptyList(),
                    projectPath
                        .resolve("build/classes/kotlin")
                        .toFile()
                        .walk()
                        .filter { it.extension == "class" }
                        .toList()
                )
            }
        }
    }

    @DisplayName("On all annotations remove kapt and compile runs incremenatally")
    @GradleTest
    fun testRemoveAnnotations(gradleVersion: GradleVersion) {
        kaptProject(gradleVersion) {
            build("assemble")

            val bKt = javaSourcesDir().resolve("bar/B.kt")
            bKt.modify { it.replace(EXAMPLE_ANNOTATION_REGEX, "") }
            val affectedElements = arrayOf("B", "funB", "valB")

            build("assemble", buildOptions = buildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertKapt3FullyExecuted()

                val useBKt = javaSourcesDir().resolve("bar/useB.kt")
                assertCompiledKotlinSources(
                    listOf(projectPath.relativize(bKt), projectPath.relativize(useBKt)),
                    getOutputForTask(":kaptGenerateStubsKotlin"),
                    errorMessageSuffix = " in task 'kaptGenerateStubsKotlin'"
                )

                // java removal is detected
                assertCompiledKotlinSources(
                    projectPath.allKotlinFiles.map { projectPath.relativize(it) },
                    output
                )

                checkGenerated(
                    kaptGeneratedToPath,
                    *(annotatedElements.toSet() - affectedElements).toTypedArray()
                )
                checkNotGenerated(kaptGeneratedToPath, *affectedElements)
            }
        }
    }

    @DisplayName("Changing annotated property type is handled correctly")
    @GradleTest
    fun testChangeAnnotatedPropertyType(gradleVersion: GradleVersion) {
        kaptProject(gradleVersion) {
            build("assemble")

            val bKt = javaSourcesDir().resolve("bar/B.kt")
            val useBKt = javaSourcesDir().resolve("bar/useB.kt")
            bKt.modify { it.replace("val valB = \"text\"", "val valB = 4") }

            build("assemble", buildOptions = buildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertKapt3FullyExecuted()
                assertCompiledKotlinSourcesHandleKapt3(
                    this,
                    listOf(bKt, useBKt).map { projectPath.relativize(it) }
                )
                checkGenerated(kaptGeneratedToPath, *annotatedElements)
            }
        }
    }

    @DisplayName("Change in inline delegate is handled correctly")
    @GradleTest
    fun testChangeInlineDelegate(gradleVersion: GradleVersion) {
        kaptProject(gradleVersion) {
            build("assemble")

            val file = javaSourcesDir().resolve("delegate/Usage.kt")
            file.modify { "$it//" }

            build("assemble") {
                assertTasksExecuted(":kaptGenerateStubsKotlin", ":compileKotlin")
            }
        }
    }

    private fun TestProject.assertCompiledKotlinSourcesHandleKapt3(
        buildResult: BuildResult,
        sources: List<Path>
    ) {
        assertCompiledKotlinSources(
            sources,
            buildResult.getOutputForTask(":kaptGenerateStubsKotlin"),
            errorMessageSuffix = " in task 'kaptGenerateStubsKotlin"
        )

        assertCompiledKotlinSources(
            sources,
            buildResult.getOutputForTask(":compileKotlin"),
            errorMessageSuffix = " in task 'compileKotlin'"
        )
    }

    private fun BuildResult.assertKapt3FullyExecuted() {
        assertTasksExecuted(":kaptKotlin", ":kaptGenerateStubsKotlin")
    }

    private fun TestProject.checkGenerated(
        generateToPath: Path,
        vararg annotatedElementNames: String
    ) {
        getGeneratedFileNames(*annotatedElementNames).forEach {
            assertFileExistsInTree(generateToPath, it)
        }
    }

    private fun TestProject.checkNotGenerated(
        generateToPath: Path,
        vararg annotatedElementNames: String
    ) {
        getGeneratedFileNames(*annotatedElementNames).forEach {
            assertFileNotExistsInTree(generateToPath, it)
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun getGeneratedFileNames(vararg annotatedElementNames: String) =
        annotatedElementNames
            .map { name ->
                name.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                } + "Generated.java"
            }

    val TestProject.kaptGeneratedToPath get() = projectPath.resolve("build/generated/source/kapt")
}

@DisplayName("Kapt incremental compilation with precise compilation outputs backup")
class KaptIncrementalWithPreciseBackupIT : KaptIncrementalIT() {
    override val defaultBuildOptions = super.defaultBuildOptions.copy(usePreciseOutputsBackup = true, keepIncrementalCompilationCachesInMemory = true)
}