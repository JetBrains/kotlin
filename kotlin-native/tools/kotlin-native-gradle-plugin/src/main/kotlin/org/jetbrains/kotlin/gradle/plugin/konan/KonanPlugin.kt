/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.gradle.plugin.konan

import groovy.lang.Closure
import org.codehaus.groovy.runtime.GStringImpl
import org.gradle.api.*
import org.gradle.api.component.ComponentWithVariants
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.FeaturePreviews
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.internal.component.UsageContext
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.publication.MavenPublicationInternal
import org.gradle.api.tasks.Exec
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.jvm.toolchain.JavaToolchainSpec
import org.gradle.language.cpp.internal.NativeVariantIdentity
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.konan.KonanPlugin.Companion.COMPILE_ALL_TASK_NAME
import org.jetbrains.kotlin.gradle.plugin.tasks.*
import org.jetbrains.kotlin.konan.CURRENT
import org.jetbrains.kotlin.konan.CompilerVersion
import org.jetbrains.kotlin.konan.parseCompilerVersion
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.buildDistribution
import org.jetbrains.kotlin.konan.target.customerDistribution
import org.jetbrains.kotlin.konan.util.DependencyProcessor
import org.jetbrains.kotlin.*
import java.io.File
import java.util.*
import javax.inject.Inject

/**
 * We use the following properties:
 *      org.jetbrains.kotlin.native.home    - directory where compiler is located (aka dist in konan project output).
 *      org.jetbrains.kotlin.native.version - a konan compiler version for downloading.
 *      konan.build.targets                 - list of targets to build (by default all the declared targets are built).
 *      konan.jvmArgs                       - additional args to be passed to a JVM executing the compiler/cinterop tool.
 */

internal fun Project.warnAboutDeprecatedProperty(property: KonanPlugin.ProjectProperty) =
    property.deprecatedPropertyName?.let { deprecated ->
        if (project.hasProperty(deprecated)) {
            logger.warn("Project property '$deprecated' is deprecated. Use '${property.propertyName}' instead.")
        }
    }

internal fun Project.hasProperty(property: KonanPlugin.ProjectProperty) = with(property) {
    when {
        hasProperty(propertyName) -> true
        deprecatedPropertyName != null && hasProperty(deprecatedPropertyName) -> true
        else -> false
    }
}

internal fun Project.findProperty(property: KonanPlugin.ProjectProperty): Any? = with(property) {
    return findProperty(propertyName) ?: deprecatedPropertyName?.let { findProperty(it) }
}

internal fun Project.getProperty(property: KonanPlugin.ProjectProperty) = findProperty(property)
        ?: throw IllegalArgumentException("No such property in the project: ${property.propertyName}")

internal fun Project.getProperty(property: KonanPlugin.ProjectProperty, defaultValue: Any) =
        findProperty(property) ?: defaultValue

internal fun Project.setProperty(property: KonanPlugin.ProjectProperty, value: Any) {
    extensions.extraProperties.set(property.propertyName, value)
}

// konanHome extension is set by downloadKonanCompiler task.
internal val Project.konanHome: String
    get() {
        return project.kotlinNativeDist.absolutePath
    }

internal val Project.konanVersion: CompilerVersion
    get() = project.findProperty(KonanPlugin.ProjectProperty.KONAN_VERSION)
        ?.toString()?.let { CompilerVersion.fromString(it) }
        ?: project.findProperty("kotlinNativeVersion") as? CompilerVersion ?: CompilerVersion.CURRENT

internal val Project.konanBuildRoot          get() = buildDir.resolve("konan")
internal val Project.konanBinBaseDir         get() = konanBuildRoot.resolve("bin")
internal val Project.konanLibsBaseDir        get() = konanBuildRoot.resolve("libs")
internal val Project.konanBitcodeBaseDir     get() = konanBuildRoot.resolve("bitcode")

internal fun File.targetSubdir(target: KonanTarget) = resolve(target.visibleName)

internal val Project.konanDefaultSrcFiles         get() = fileTree("${projectDir.canonicalPath}/src/main/kotlin")
internal fun Project.konanDefaultDefFile(libName: String)
        = file("${projectDir.canonicalPath}/src/main/c_interop/$libName.def")

@Suppress("UNCHECKED_CAST")
internal val Project.konanArtifactsContainer: KonanArtifactContainer
    get() = extensions.getByName(KonanPlugin.ARTIFACTS_CONTAINER_NAME) as KonanArtifactContainer

// TODO: The Kotlin/Native compiler is downloaded manually by a special task so the compilation tasks
// are configured without the compile distribution. After target management refactoring
// we need .properties files from the distribution to configure targets. This is worked around here
// by using HostManager instead of PlatformManager. But we need to download the compiler at the configuration
// stage (e.g. by getting it from maven as a plugin dependency) and bring back the PlatformManager here.
internal val Project.hostManager: HostManager
    get() = findProperty("hostManager") as HostManager? ?:
            if (hasProperty("org.jetbrains.kotlin.native.experimentalTargets"))
                HostManager(buildDistribution(rootProject.rootDir.absolutePath), true)
            else
                HostManager(customerDistribution(konanHome))

internal val Project.konanTargets: List<KonanTarget>
    get() = hostManager.toKonanTargets(konanExtension.targets)
                .filter{ hostManager.isEnabled(it) }
                .distinct()

@Suppress("UNCHECKED_CAST")
internal val Project.konanExtension: KonanExtension
    get() = extensions.getByName(KonanPlugin.KONAN_EXTENSION_NAME) as KonanExtension

internal val Project.konanCompilerDownloadTask
    get() = tasks.getByName(KonanPlugin.KONAN_DOWNLOAD_TASK_NAME)

internal val Project.requestedTargets
    get() = findProperty(KonanPlugin.ProjectProperty.KONAN_BUILD_TARGETS)?.let {
        it.toString().trim().split("\\s+".toRegex())
    }.orEmpty()

internal val Project.jvmArgs
    get() = (findProperty(KonanPlugin.ProjectProperty.KONAN_JVM_ARGS) as String?)?.split("\\s+".toRegex()).orEmpty()

internal val Project.compileAllTask
    get() = getOrCreateTask(COMPILE_ALL_TASK_NAME)

internal fun Project.targetIsRequested(target: KonanTarget): Boolean {
    val targets = requestedTargets
    return (targets.isEmpty() || targets.contains(target.visibleName) || targets.contains("all"))
}

/** Looks for task with given name in the given project. Throws [UnknownTaskException] if there's not such task. */
private fun Project.getTask(name: String): Task = tasks.getByPath(name)

/**
 * Looks for task with given name in the given project.
 * If such task isn't found, will create it. Returns created/found task.
 */
private fun Project.getOrCreateTask(name: String): Task = with(tasks) {
    findByPath(name) ?: create(name, DefaultTask::class.java)
}

internal fun Project.konanCompilerName(): String =
        "kotlin-native-${project.simpleOsName}-${project.konanVersion}"

internal fun Project.konanCompilerDownloadDir(): String =
        DependencyProcessor.localKonanDir.resolve(project.konanCompilerName()).absolutePath

// region Useful extensions and functions ---------------------------------------

internal fun MutableList<String>.addArg(parameter: String, value: String) {
    add(parameter)
    add(value)
}

internal fun MutableList<String>.addArgs(parameter: String, values: Iterable<String>) {
    values.forEach {
        addArg(parameter, it)
    }
}

internal fun MutableList<String>.addArgIfNotNull(parameter: String, value: String?) {
    if (value != null) {
        addArg(parameter, value)
    }
}

internal fun MutableList<String>.addKey(key: String, enabled: Boolean) {
    if (enabled) {
        add(key)
    }
}

internal fun MutableList<String>.addFileArgs(parameter: String, values: FileCollection) {
    values.files.forEach {
        addArg(parameter, it.canonicalPath)
    }
}

internal fun MutableList<String>.addFileArgs(parameter: String, values: Collection<FileCollection>) {
    values.forEach {
        addFileArgs(parameter, it)
    }
}

// endregion

internal fun dumpProperties(task: Task) {
    fun Iterable<String>.dump() = joinToString(prefix = "[", separator = ",\n${" ".repeat(22)}", postfix = "]")
    fun Collection<FileCollection>.dump() = flatMap { it.files }.map { it.canonicalPath }.dump()
    when (task) {
        is KonanCompileTask -> with(task) {
            println()
            println("Compilation task: $name")
            println("destinationDir     : $destinationDir")
            println("artifact           : ${artifact.canonicalPath}")
            println("srcFiles         : ${srcFiles.dump()}")
            println("produce            : $produce")
            println("libraries          : ${libraries.files.dump()}")
            println("                   : ${libraries.artifacts.map {
                it.artifact.canonicalPath
            }.dump()}")
            println("                   : ${libraries.namedKlibs.dump()}")
            println("nativeLibraries    : ${nativeLibraries.dump()}")
            println("linkerOpts         : $linkerOpts")
            println("enableDebug        : $enableDebug")
            println("noStdLib           : $noStdLib")
            println("noMain             : $noMain")
            println("enableOptimization : $enableOptimizations")
            println("enableAssertions   : $enableAssertions")
            println("noDefaultLibs      : $noDefaultLibs")
            println("noEndorsedLibs     : $noEndorsedLibs")
            println("target             : $target")
            println("languageVersion    : $languageVersion")
            println("apiVersion         : $apiVersion")
            println("konanVersion       : ${CompilerVersion.CURRENT}")
            println("konanHome          : $konanHome")
            println()
        }
        is KonanInteropTask -> with(task) {
            println()
            println("Stub generation task: $name")
            println("destinationDir     : $destinationDir")
            println("artifact           : $artifact")
            println("libraries          : ${libraries.files.dump()}")
            println("                   : ${libraries.artifacts.map {
                it.artifact.canonicalPath
            }.dump()}")
            println("                   : ${libraries.namedKlibs.dump()}")
            println("defFile            : $defFile")
            println("target             : $target")
            println("packageName        : $packageName")
            println("compilerOpts       : $compilerOpts")
            println("linkerOpts         : $linkerOpts")
            println("headers            : ${headers.dump()}")
            println("linkFiles          : ${linkFiles.dump()}")
            println("konanVersion       : ${CompilerVersion.CURRENT}")
            println("konanHome          : $konanHome")
            println()
        }
        else -> {
            println("Unsupported task.")
        }
    }
}

open class KonanExtension {
    var targets = mutableListOf("host")
    var languageVersion: String? = null
    var apiVersion: String? = null
    var jvmArgs = mutableListOf<String>()
}

open class KonanSoftwareComponent(val project: ProjectInternal?): SoftwareComponentInternal, ComponentWithVariants {
    private val usages = mutableSetOf<UsageContext>()
    override fun getUsages(): MutableSet<out UsageContext> = usages

    private val variants = mutableSetOf<SoftwareComponent>()
    override fun getName() = "main"

    override fun getVariants(): Set<SoftwareComponent> = variants

    fun addVariant(component: SoftwareComponent) = variants.add(component)
}

class KonanPlugin @Inject constructor(private val registry: ToolingModelBuilderRegistry)
    : Plugin<ProjectInternal> {

    enum class ProjectProperty(val propertyName: String, val deprecatedPropertyName: String? = null) {
        KONAN_HOME                     ("org.jetbrains.kotlin.native.home", "konan.home"),
        KONAN_VERSION                  ("org.jetbrains.kotlin.native.version"),
        KONAN_BUILD_TARGETS            ("konan.build.targets"),
        KONAN_JVM_ARGS                 ("konan.jvmArgs"),
        KONAN_JVM_LAUNCHER             ("konan.javaLauncher"),
        KONAN_USE_ENVIRONMENT_VARIABLES("konan.useEnvironmentVariables"),
        DOWNLOAD_COMPILER              ("download.compiler"),

        // Properties used instead of env vars until https://github.com/gradle/gradle/issues/3468 is fixed.
        // TODO: Remove them when an API for env vars is provided.
        KONAN_CONFIGURATION_BUILD_DIR  ("konan.configuration.build.dir"),
        KONAN_DEBUGGING_SYMBOLS        ("konan.debugging.symbols"),
        KONAN_OPTIMIZATIONS_ENABLE     ("konan.optimizations.enable"),
    }

    companion object {
        internal const val ARTIFACTS_CONTAINER_NAME = "konanArtifacts"
        internal const val KONAN_DOWNLOAD_TASK_NAME = "checkKonanCompiler"
        internal const val KONAN_GENERATE_CMAKE_TASK_NAME = "generateCMake"
        internal const val COMPILE_ALL_TASK_NAME = "compileKonan"

        internal const val KONAN_EXTENSION_NAME = "konan"

        internal val REQUIRED_GRADLE_VERSION = GradleVersion.version("6.7")
    }

    private fun Project.cleanKonan() = project.tasks.withType(KonanBuildingTask::class.java).forEach {
        project.delete(it.artifact)
    }

    private fun checkGradleVersion() =  GradleVersion.current().let { current ->
        check(current >= REQUIRED_GRADLE_VERSION) {
            "Kotlin/Native Gradle plugin is incompatible with this version of Gradle.\n" +
            "The minimal required version is $REQUIRED_GRADLE_VERSION\n" +
            "Current version is ${current}"
        }
    }

    private lateinit var konanJvmLauncher: JavaLauncher

    private fun getJavaLauncher(project: Project): Provider<JavaLauncher> = project.providers.provider {
        if (!::konanJvmLauncher.isInitialized) {
            val toolchain = project.extensions.getByType(JavaPluginExtension::class.java).toolchain
            val service = project.extensions.getByType(JavaToolchainService::class.java)
            konanJvmLauncher = try {
                service.launcherFor(toolchain).get()
            } catch (ex: GradleException) {
                // If the JDK that was set is not available get the JDK 11 as a default
                service.launcherFor(object : Action<JavaToolchainSpec> {
                    override fun execute(toolchainSpec: JavaToolchainSpec) {
                        toolchainSpec.languageVersion.set(JavaLanguageVersion.of(JdkMajorVersion.JDK_11.majorVersion))
                    }
                }).get()
            }
        }
        konanJvmLauncher
    }

    override fun apply(project: ProjectInternal) {
        checkGradleVersion()
        project.plugins.apply("base")
        project.plugins.apply("java")
        // Create necessary tasks and extensions.
        project.tasks.create(KONAN_DOWNLOAD_TASK_NAME, KonanCompilerDownloadTask::class.java)
        project.extensions.create(KONAN_EXTENSION_NAME, KonanExtension::class.java)
        val container = project.extensions.create(
                KonanArtifactContainer::class.java,
                ARTIFACTS_CONTAINER_NAME,
                KonanArtifactContainer::class.java,
                project
        )
        project.setProperty(ProjectProperty.KONAN_JVM_LAUNCHER, getJavaLauncher(project))

        project.warnAboutDeprecatedProperty(ProjectProperty.KONAN_HOME)

        // Set additional project properties like org.jetbrains.kotlin.native.home, konan.build.targets etc.
        if (!project.useCustomDist) {
            project.setProperty(ProjectProperty.KONAN_HOME, project.konanCompilerDownloadDir())
            project.setProperty(ProjectProperty.DOWNLOAD_COMPILER, true)
        }

        // Create and set up aggregate building tasks.
        val compileKonanTask = project.getOrCreateTask(COMPILE_ALL_TASK_NAME).apply {
            group = BasePlugin.BUILD_GROUP
            description = "Compiles all the Kotlin/Native artifacts"
        }
        project.getTask("build").apply {
            dependsOn(compileKonanTask)
        }
        project.getTask("clean").apply {
            doLast { project.cleanKonan() }
        }

        project.afterEvaluate {
            project.tasks
                .withType(KonanCompileProgramTask::class.java)
                .forEach { task ->
                    val isCrossCompile = (task.target != HostManager.host.visibleName)
                    if (!isCrossCompile && !project.hasProperty("konanNoRun"))
                    task.runTask = project.tasks.register(
                        "run${
                            task.artifactName.replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(
                                    Locale.getDefault()
                                ) else it.toString()
                            }
                        }", Exec::class.java) {
                        group= "run"
                        dependsOn(task)
                        val artifactPathClosure = object : Closure<String>(this) {
                            override fun call() = task.artifactPath
                        }
                        // Use GString to evaluate a path to the artifact lazily thus allow changing it at configuration phase.
                        val lazyArtifactPath = GStringImpl(arrayOf(artifactPathClosure), arrayOf(""))
                        executable(lazyArtifactPath)
                        // Add values passed in the runArgs project property as arguments.
                        argumentProviders.add(task.RunArgumentProvider())
                    }
                }
        }

        val runTask = project.getOrCreateTask("run")
        project.afterEvaluate {
            project.konanArtifactsContainer
                .filterIsInstance(KonanProgram::class.java)
                .forEach { program ->
                    program.tasks().forEach { compile ->
                        compile.configure { this@configure.runTask?.let { runTask.dependsOn(it) } }
                    }
                }
        }

        // Enable multiplatform support
        project.pluginManager.apply(KotlinNativePlatformPlugin::class.java)
        project.afterEvaluate {
            project.pluginManager.withPlugin("maven-publish") {
                container.all { buildingConfig ->
                    val konanSoftwareComponent = buildingConfig.mainVariant
                    project.extensions.configure(PublishingExtension::class.java) {
                        val builtArtifact = buildingConfig.name
                        val mavenPublication = publications.maybeCreate(builtArtifact, MavenPublication::class.java)
                        mavenPublication.apply {
                            artifactId = builtArtifact
                            groupId = project.group.toString()
                            from(konanSoftwareComponent)
                        }
                        (mavenPublication as MavenPublicationInternal).publishWithOriginalFileName()
                        buildingConfig.pomActions.forEach {
                            mavenPublication.pom(it)
                        }
                    }

                    project.extensions.configure(PublishingExtension::class.java) {
                        for (v in konanSoftwareComponent.variants) {
                            this@configure.publications.create(v.name, MavenPublication::class.java) {
                                val coordinates = (v as NativeVariantIdentity).coordinates
                                project.logger.info("variant with coordinates($coordinates) and module: ${coordinates.module}")
                                artifactId = coordinates.module.name
                                groupId = coordinates.group
                                version = coordinates.version
                                from(v)
                                (this as MavenPublicationInternal).publishWithOriginalFileName()
                                buildingConfig.pomActions.forEach {
                                    pom(it)
                                }
                            }
                        }
                    }
                    true
                }
            }
        }
    }
}
