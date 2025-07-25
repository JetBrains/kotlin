/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.project.IsolatedProject
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project
import java.io.File

abstract class CompilerTestsExtension(private val project: Project) {
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
    val stdlibJsRuntimeForTests: Configuration = project.configurations.create("stdlibJsRuntimeForTests") {
        isTransitive = false
    }
    val testJsRuntimeForTests: Configuration = project.configurations.create("testJsRuntimeForTests") {
        isTransitive = false
    }

    private val noOp = project.kotlinBuildProperties.isInJpsBuildIdeaSync
    private fun add(configuration: Configuration, dependency: DependencyHandler.() -> ProjectDependency) {
        if (!noOp) {
            project.dependencies { configuration(dependency(this)) }
        }
    }

    init {
        project.dependencies {
            add(stdlibRuntimeForTests) { project(":kotlin-stdlib") }
            add(stdlibMinimalRuntimeForTests) { project(":kotlin-stdlib-jvm-minimal-for-test") }
            add(kotlinReflectJarForTests) { project(":kotlin-reflect") }
        }
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

    fun withStdlibJsRuntime() {
        add(stdlibJsRuntimeForTests) { project(":kotlin-stdlib", "distJsKlib") }
    }

    fun withTestJsRuntime() {
        add(testJsRuntimeForTests) { project(":kotlin-test", "jsRuntimeElements") }
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

    // -------------------- testData configuration --------------------

    internal abstract val testDataFiles: ListProperty<Directory>
    internal val testDataMap: MutableMap<String, String> = mutableMapOf<String, String>()

    fun testData(isolatedProject: IsolatedProject, relativePath: String) {
        val testDataDirectory = isolatedProject.projectDirectory.dir(relativePath)
        testDataFiles.add(testDataDirectory)
        testDataMap.put(
            testDataDirectory.asFile.relativeTo(project.rootDir).path.replace("\\", "/"),
            testDataDirectory.asFile.canonicalPath.replace("\\", "/")
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
        if (skipInLocalBuild && project.kotlinBuildProperties.isTeamcityBuild) {
            return project.tasks.register(taskName)
        }
        if (jUnitMode == JUnitMode.JUnit5 && parallel != null) {
            project.logger.error("JUnit5 tests are parallel by default and its configured with `junit-platform.properties`, please remove `parallel=$parallel` argument")
        }
        return project.projectTest(
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

    fun testGenerator(
        fqName: String,
        taskName: String = "generateTests",
        sourceSet: SourceSet? = null,
        configure: JavaExec.() -> Unit = {}
    ) {
        project.generator(taskName, fqName, sourceSet, configure)
    }
}
