/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.project.IsolatedProject
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.build.project.tests.CollectTestDataTask

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "Unless your tests use the compiler distribution directly, consider depending on individual dist artifacts"
)
@Target(AnnotationTarget.FUNCTION)
annotation class KotlinCompilerDistUsage

abstract class ProjectTestsExtension(val project: Project) {
    abstract val allowFlaky: Property<Boolean>

    // -------------------- dependencies for runtime of tests --------------------

    fun withJvmStdlibAndReflect() {
        project.tasks.withType(Test::class.java).configureEach { withJvmStdlibAndReflect() }
    }

    fun withJvmStdlibSources() {
        project.tasks.withType(Test::class.java).configureEach { withJvmStdlibSources() }
    }

    fun withStdlibCommon() {
        project.tasks.withType(Test::class.java).configureEach { withStdlibCommon() }
    }

    fun withScriptRuntime() {
        project.tasks.withType(Test::class.java).configureEach { withScriptRuntime() }
    }

    fun withTestJar() {
        project.tasks.withType(Test::class.java).configureEach { withTestJar() }
    }

    fun withAnnotations() {
        project.tasks.withType(Test::class.java).configureEach { withAnnotations() }
    }

    fun withStdlibWeb() {
        project.tasks.withType(Test::class.java).configureEach { withStdlibWeb() }
    }

    fun withJsRuntime() {
        project.tasks.withType(Test::class.java).configureEach { withJsRuntime() }
    }

    fun withWasmRuntime() {
        project.tasks.withType(Test::class.java).configureEach { withWasmRuntime() }
    }

    fun withScriptingPlugin() {
        project.tasks.withType(Test::class.java).configureEach { withScriptingPlugin() }
    }

    fun withTestScriptDefinition() {
        project.tasks.withType(Test::class.java).configureEach { withTestScriptDefinition() }
    }

    @KotlinCompilerDistUsage
    fun withDist() {
        project.tasks.withType(Test::class.java).configureEach { withDist() }
    }

    fun withMockJdkRuntime() {
        project.tasks.withType(Test::class.java).configureEach { withMockJdkRuntime() }
    }

    fun withMockJDKModifiedRuntime() {
        project.tasks.withType(Test::class.java).configureEach { withMockJDKModifiedRuntime() }
    }

    fun withMockJdkAnnotationsJar() {
        project.tasks.withType(Test::class.java).configureEach { withMockJdkAnnotationsJar() }
    }

    fun withThirdPartyAnnotations() {
        project.tasks.withType(Test::class.java).configureEach { withThirdPartyAnnotations() }
    }

    fun withThirdPartyJava8Annotations() {
        project.tasks.withType(Test::class.java).configureEach { withThirdPartyJava8Annotations() }
    }

    fun withThirdPartyJava9Annotations() {
        project.tasks.withType(Test::class.java).configureEach { withThirdPartyJava9Annotations() }
    }

    fun withThirdPartyJsr305() {
        project.tasks.withType(Test::class.java).configureEach { withThirdPartyJsr305() }
    }

    fun withPluginSandboxAnnotations() {
        project.tasks.withType(Test::class.java).configureEach { withPluginSandboxAnnotations() }
    }

    fun withPluginSandboxJar() {
        project.tasks.withType(Test::class.java).configureEach { withPluginSandboxJar() }
    }

    // -------------------- testData configuration --------------------

    internal abstract val testDataFiles: ListProperty<Directory>
    internal val testDataMap: MutableMap<String, String> = mutableMapOf()

    fun testData(isolatedProject: IsolatedProject, relativePath: String) {
        val testDataDirectory = isolatedProject.projectDirectory.dir(relativePath)
        testDataFiles.add(testDataDirectory)
        testDataMap[testDataDirectory.asFile.relativeTo(project.rootDir).path.toSystemIndependentPath()] =
            testDataDirectory.asFile.canonicalPath.toSystemIndependentPath()
    }

    // -------------------- test task definitions --------------------

    fun testTask(
        parallel: Boolean? = null,
        jUnitMode: JUnitMode,
        javaLauncher: JdkMajorVersion = DEFAULT_JAVA_LAUNCHER_FOR_TESTS,
        maxHeapSizeMb: Int? = null,
        minHeapSizeMb: Int? = null,
        maxMetaspaceSizeMb: Int = 512,
        reservedCodeCacheSizeMb: Int = 256,
        defineJDKEnvVariables: List<JdkMajorVersion> = emptyList(),
        enableGroupingTestEngine: Boolean = false,
        body: Test.() -> Unit = {},
    ): TaskProvider<Test> {
        @Suppress("UNCHECKED_CAST")
        return testTask(
            taskName = "test",
            parallel,
            jUnitMode,
            javaLauncher,
            maxHeapSizeMb,
            minHeapSizeMb,
            maxMetaspaceSizeMb,
            reservedCodeCacheSizeMb,
            defineJDKEnvVariables,
            enableGroupingTestEngine,
            skipInLocalBuild = false,
            body
        ) as TaskProvider<Test>
    }

    fun testTask(
        taskName: String,
        parallel: Boolean? = null,
        jUnitMode: JUnitMode,
        javaLauncher: JdkMajorVersion = DEFAULT_JAVA_LAUNCHER_FOR_TESTS,
        maxHeapSizeMb: Int? = null,
        minHeapSizeMb: Int? = null,
        maxMetaspaceSizeMb: Int = 512,
        reservedCodeCacheSizeMb: Int = 256,
        defineJDKEnvVariables: List<JdkMajorVersion> = emptyList(),
        enableGroupingTestEngine: Boolean = false,
        skipInLocalBuild: Boolean,
        body: Test.() -> Unit = {},
    ): TaskProvider<out Task> {
        if (skipInLocalBuild && !project.kotlinBuildProperties.isTeamcityBuild.get()) {
            return project.tasks.register(taskName)
        }
        if (jUnitMode == JUnitMode.JUnit5 && parallel != null) {
            throw GradleException("JUnit5 tests are parallel by default and its configured with `junit-platform.properties`, please remove `parallel=$parallel` argument")
        }
        if (enableGroupingTestEngine) {
            when (jUnitMode) {
                JUnitMode.JUnit4 -> throw GradleException("JUnit4 tests are not supported with grouping test engine. Change the JUnitMode to JUnit5")
                JUnitMode.JUnit5 -> {
                    project.dependencies {
                        add(
                            configurationName = "testRuntimeOnly",
                            dependencyNotation = testFixtures(project(":compiler:test-infrastructure:grouping-test-engine")),
                        )
                    }
                }
            }
        }
        return project.createGeneralTestTask(
            taskName,
            parallel ?: false,
            jUnitMode,
            javaLauncher,
            maxHeapSizeMb,
            minHeapSizeMb,
            maxMetaspaceSizeMb,
            reservedCodeCacheSizeMb,
            defineJDKEnvVariables,
        ) {
            if (jUnitMode == JUnitMode.JUnit5) {
                useJUnitPlatform()
            }
            body()
        }
    }

    /**
     * [doNotSetFixturesSourceSetDependency] exits only for a migration period and used in projects which are not migrated to `testFixtures` yet.
     * Please don't use set it to `true` for new generator tasks.
     */
    fun testGenerator(
        fqName: String,
        taskName: String = "generateTests",
        doNotSetFixturesSourceSetDependency: Boolean = false,
        generateTestsInBuildDirectory: Boolean = false,
        skipCollectDataTask: Boolean = false,
        configureTestDataCollection: CollectTestDataTask.() -> Unit = {},
        configure: JavaExec.() -> Unit = {},
    ) {
        val fixturesSourceSet = if (doNotSetFixturesSourceSetDependency) {
            null
        } else {
            project.sourceSets.named("testFixtures").get()
        }
        val generationPath = when (generateTestsInBuildDirectory) {
            false -> project.layout.projectDirectory.dir("tests-gen")
            true -> project.layout.buildDirectory.dir("tests-gen").get()
        }
        val generatorTask = project.generator(
            taskName = taskName,
            fqName = fqName,
            sourceSet = fixturesSourceSet ?: project.testSourceSet,
            inputKind = when (doNotSetFixturesSourceSetDependency) {
                true -> GeneratorInputKind.RuntimeClasspath
                false -> GeneratorInputKind.SourceSetJar
            }
        ) {
            this.args = buildList {
                add(generationPath.asFile.absolutePath)
                if (generateTestsInBuildDirectory) {
                    add("allowGenerationOnTeamCity")
                    add("skipTestAllFilesCheck")
                }
            }
            if (generateTestsInBuildDirectory) {
                this.outputs.dir(generationPath).withPropertyName("generatedTests")
                doFirst {
                    // We need to delete previously generated tests to handle
                    // the case when the generated runner was removed from the generation
                    generationPath.asFile.deleteRecursively()
                }
            }
            configure()
        }
        if (generateTestsInBuildDirectory && !skipCollectDataTask) {
            project.sourceSets.named(SourceSet.TEST_SOURCE_SET_NAME) {
                generatedDir(project, generatorTask.map { generationPath })
            }
            configureCollectTestDataTask(generatorTask, configureTestDataCollection)
        }
    }

    private fun configureCollectTestDataTask(generatorTask: TaskProvider<out Task>, configure: CollectTestDataTask.() -> Unit) {
        val collectTestDataTask = project.tasks.register<CollectTestDataTask>("collectTestData") {
            projectName.set(project.name)
            rootDirPath.set(project.rootDir.absolutePath)
            targetFile.set(project.layout.buildDirectory.file("testDataInfo/testDataFilesList.txt"))
            testDataFiles.set(this@ProjectTestsExtension.testDataFiles)
            configure()
        }
        generatorTask.configure {
            inputs.file(collectTestDataTask.map { it.targetFile })
                .withPropertyName("testDataFilesList")
                .withPathSensitivity(PathSensitivity.RELATIVE)
        }
        project.tasks.named("compileTestKotlin") {
            inputs.dir(generatorTask.map { it.outputs.files.singleFile })
                .withPropertyName("generatedTestSources")
                .withPathSensitivity(PathSensitivity.RELATIVE)
        }
    }

    private fun String.toSystemIndependentPath(): String = replace('\\', '/')
}
