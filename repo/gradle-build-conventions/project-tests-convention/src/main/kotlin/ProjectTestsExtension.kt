/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.attributes.Usage
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.project.IsolatedProject
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.project
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.build.project.tests.CollectTestDataTask
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.attributes.KlibPackaging
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import java.io.File

abstract class ProjectTestsExtension(val project: Project) {
    abstract val allowFlaky: Property<Boolean>

    // -------------------- dependencies for runtime of tests --------------------

    val stdlibRuntimeForTests: Configuration = project.configurations.create("stdlibRuntimeForTests") {
        isTransitive = false
    }
    val stdlibMinimalRuntimeForTests: Configuration = project.configurations.create("stdlibMinimalRuntimeForTests") {
        isTransitive = false
    }
    val kotlinReflectJarForTests: Configuration = project.configurations.create("kotlinReflectJarForTests") {
        isTransitive = false
    }
    val stdlibCommonRuntimeForTests: Configuration = project.configurations.create("stdlibCommonRuntimeForTests") {
        isTransitive = false
    }
    val scriptRuntimeForTests: Configuration = project.configurations.create("scriptRuntimeForTests") {
        isTransitive = false
    }
    val kotlinTestJarForTests: Configuration = project.configurations.create("kotlinTestJarForTests") {
        isTransitive = false
    }
    val kotlinAnnotationsForTests: Configuration = project.configurations.create("kotlinAnnotationsForTests") {
        isTransitive = false
    }
    val scriptingPluginForTests: Configuration = project.configurations.create("scriptingPluginForTests") {
        isTransitive = false
    }
    var testScriptDefinitionForTests: Configuration = project.configurations.create("testScriptDefinitionForTests") {
        isTransitive = false
    }
    val stdlibWebRuntimeForTests: Configuration = project.configurations.create("stdlibWebRuntimeForTests") {
        isTransitive = false
    }
    val distForTests: Configuration = project.configurations.create("distForTests") {
        isTransitive = false
    }
    val stdlibJsRuntimeForTests: Configuration = project.configurations.create("stdlibJsRuntimeForTests") {
        isTransitive = false
    }
    val stdlibJsMinimalRuntimeForTests: Configuration = project.configurations.create("stdlibJsMinimalRuntimeForTests") {
        isTransitive = false
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        attributes.attribute(KlibPackaging.ATTRIBUTE, project.objects.named(KlibPackaging.NON_PACKED))
    }
    val testJsRuntimeForTests: Configuration = project.configurations.create("testJsRuntimeForTests") {
        isTransitive = false
    }
    val stdlibWasmJsRuntimeForTests: Configuration = project.configurations.create("stdlibWasmJsRuntimeForTests") {
        isTransitive = false
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        attributes.attribute(KlibPackaging.ATTRIBUTE, project.objects.named(KlibPackaging.NON_PACKED))
    }
    val stdlibWasmWasiRuntimeForTests: Configuration = project.configurations.create("stdlibWasmWasiRuntimeForTests") {
        isTransitive = false
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        attributes.attribute(KlibPackaging.ATTRIBUTE, project.objects.named(KlibPackaging.NON_PACKED))
    }
    val testWasmJsRuntimeForTests: Configuration = project.configurations.create("testWasmJsRuntimeForTests") {
        isTransitive = false
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        attributes.attribute(KlibPackaging.ATTRIBUTE, project.objects.named(KlibPackaging.NON_PACKED))
    }
    val testWasmWasiRuntimeForTests: Configuration = project.configurations.create("testWasmWasiRuntimeForTests") {
        isTransitive = false
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        attributes.attribute(KlibPackaging.ATTRIBUTE, project.objects.named(KlibPackaging.NON_PACKED))
    }

    val pluginSandboxAnnotationsJar: Configuration = project.configurations.create("pluginSandboxAnnotationsJar") {
        isTransitive = false
    }

    val pluginSandboxAnnotationsJsKlib: Configuration = project.configurations.create("pluginSandboxAnnotationsJsKlib") {
        isTransitive = false
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(KotlinUsages.KOTLIN_RUNTIME))
            attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
        }
    }

    private fun add(configuration: Configuration, dependency: DependencyHandler.() -> ProjectDependency) {
        project.dependencies { configuration(dependency(this)) }
    }

    fun withJvmStdlibAndReflect() {
        add(stdlibRuntimeForTests) { project(":kotlin-stdlib") }
        add(stdlibMinimalRuntimeForTests) { project(":kotlin-stdlib-jvm-minimal-for-test") }
        add(kotlinReflectJarForTests) { project(":kotlin-reflect") }
    }

    fun withStdlibCommon() {
        add(stdlibCommonRuntimeForTests) { project(":kotlin-stdlib", "commonMainMetadataElements") }
    }

    fun withScriptRuntime() {
        add(scriptRuntimeForTests) { project(":kotlin-script-runtime") }
    }

    fun withTestJar() {
        add(kotlinTestJarForTests) { project(":kotlin-test") }
    }

    fun withAnnotations() {
        add(kotlinAnnotationsForTests) { project(":kotlin-annotations-jvm") }
    }

    fun withStdlibWeb() {
        add(stdlibWebRuntimeForTests) { project(":kotlin-stdlib", "webMainMetadataElements") }
    }

    fun withJsRuntime() {
        add(stdlibJsRuntimeForTests) { project(":kotlin-stdlib", "distJsKlib") }
        add(stdlibJsMinimalRuntimeForTests) { project(":kotlin-stdlib-js-ir-minimal-for-test", "jsRuntimeElements") }
        add(testJsRuntimeForTests) { project(":kotlin-test", "jsRuntimeElements") }
    }

    fun withWasmRuntime() {
        add(stdlibWasmJsRuntimeForTests) { project(":kotlin-stdlib", "wasmJsRuntimeElements") }
        add(stdlibWasmWasiRuntimeForTests) { project(":kotlin-stdlib", "wasmWasiRuntimeElements") }
        add(testWasmJsRuntimeForTests) { project(":kotlin-test", "wasmJsRuntimeElements") }
        add(testWasmWasiRuntimeForTests) { project(":kotlin-test", "wasmWasiRuntimeElements") }
    }

    fun withScriptingPlugin() {
        add(scriptingPluginForTests) { project(":kotlin-scripting-compiler") }
        add(scriptingPluginForTests) { project(":kotlin-scripting-compiler-impl") }
        add(scriptingPluginForTests) { project(":kotlin-scripting-common") }
        add(scriptingPluginForTests) { project(":kotlin-scripting-jvm") }
        /*
        KOTLIN_SCRIPTING_COMPILER_PLUGIN_JAR
        KOTLIN_SCRIPTING_COMPILER_IMPL_JAR
        KOTLIN_SCRIPTING_COMMON_JAR
        KOTLIN_SCRIPTING_JVM_JAR
        */
    }

    fun withTestScriptDefinition() {
        add(testScriptDefinitionForTests) { project(":plugins:scripting:test-script-definition", "testFixturesApiElements") }
    }

    fun withDist() {
        add(distForTests) { project(":kotlin-compiler", "distElements") }
    }

    abstract val mockJdkRuntime: RegularFileProperty
    abstract val mockJDKModifiedRuntime: RegularFileProperty
    abstract val mockJdkAnnotationsJar: RegularFileProperty
    abstract val thirdPartyAnnotations: DirectoryProperty
    abstract val thirdPartyJava8Annotations: DirectoryProperty
    abstract val thirdPartyJava9Annotations: DirectoryProperty
    abstract val thirdPartyJsr305: DirectoryProperty

    fun withMockJdkRuntime() {
        mockJdkRuntime.value { File(project.rootDir, "compiler/testData/mockJDK/jre/lib/rt.jar") }
    }

    fun withMockJDKModifiedRuntime() {
        mockJDKModifiedRuntime.value { File(project.rootDir, "compiler/testData/mockJDKModified/rt.jar") }
    }

    fun withMockJdkAnnotationsJar() {
        mockJdkAnnotationsJar.value { File(project.rootDir, "compiler/testData/mockJDK/jre/lib/annotations.jar") }
    }

    fun withThirdPartyAnnotations() {
        thirdPartyAnnotations.set(File(project.rootDir, "third-party/annotations"))
    }

    fun withThirdPartyJava8Annotations() {
        thirdPartyJava8Annotations.set(File(project.rootDir, "third-party/java8-annotations"))
    }

    fun withThirdPartyJava9Annotations() {
        thirdPartyJava9Annotations.set(File(project.rootDir, "third-party/java9-annotations"))
    }

    fun withThirdPartyJsr305() {
        thirdPartyJsr305.set(File(project.rootDir, "third-party/jsr305"))
    }

    fun withPluginSandboxAnnotations() {
        add(pluginSandboxAnnotationsJar) { project(":plugins:plugin-sandbox:plugin-annotations") }
        add(pluginSandboxAnnotationsJsKlib) { project(":plugins:plugin-sandbox:plugin-annotations", "jsRuntimeElements") }
    }

    // -------------------- testData configuration --------------------

    internal abstract val testDataFiles: ListProperty<Directory>
    internal val testDataMap: MutableMap<String, String> = mutableMapOf()

    fun testData(isolatedProject: IsolatedProject, relativePath: String) {
        val testDataDirectory = isolatedProject.projectDirectory.dir(relativePath)
        testDataFiles.add(testDataDirectory)
        testDataMap.put(
            testDataDirectory.asFile.relativeTo(project.rootDir).path.toSystemIndependentPath(),
            testDataDirectory.asFile.canonicalPath.toSystemIndependentPath()
        )
    }

    // -------------------- test task definitions --------------------

    fun testTask(
        parallel: Boolean? = null,
        jUnitMode: JUnitMode,
        maxHeapSizeMb: Int? = null,
        minHeapSizeMb: Int? = null,
        maxMetaspaceSizeMb: Int = 512,
        reservedCodeCacheSizeMb: Int = 256,
        defineJDKEnvVariables: List<JdkMajorVersion> = emptyList(),
        body: Test.() -> Unit = {},
    ): TaskProvider<Test> {
        @Suppress("UNCHECKED_CAST")
        return testTask(
            taskName = "test",
            parallel,
            jUnitMode,
            maxHeapSizeMb,
            minHeapSizeMb,
            maxMetaspaceSizeMb,
            reservedCodeCacheSizeMb,
            defineJDKEnvVariables,
            skipInLocalBuild = false,
            body
        ) as TaskProvider<Test>
    }

    fun testTask(
        taskName: String,
        parallel: Boolean? = null,
        jUnitMode: JUnitMode,
        maxHeapSizeMb: Int? = null,
        minHeapSizeMb: Int? = null,
        maxMetaspaceSizeMb: Int = 512,
        reservedCodeCacheSizeMb: Int = 256,
        defineJDKEnvVariables: List<JdkMajorVersion> = emptyList(),
        skipInLocalBuild: Boolean,
        body: Test.() -> Unit = {},
    ): TaskProvider<out Task> {
        if (skipInLocalBuild && !project.kotlinBuildProperties.isTeamcityBuild.get()) {
            return project.tasks.register(taskName)
        }
        if (jUnitMode == JUnitMode.JUnit5 && parallel != null) {
            throw GradleException("JUnit5 tests are parallel by default and its configured with `junit-platform.properties`, please remove `parallel=$parallel` argument")
        }
        return project.createGeneralTestTask(
            taskName,
            parallel ?: false,
            jUnitMode,
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
