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

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.*
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.BasePlugin
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.gradle.plugin.KonanPlugin.Companion.COMPILE_ALL_TASK_NAME
import org.jetbrains.kotlin.gradle.plugin.tasks.*
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.PlatformManager
import org.jetbrains.kotlin.konan.target.customerDistribution
import java.io.File
import java.util.*
import javax.inject.Inject

/**
 * We use the following properties:
 *      konan.home          - directory where compiler is located (aka dist in konan project output).
 *      konan.version       - a konan compiler version for downloading.
 *      konan.build.targets - list of targets to build (by default all the declared targets are built).
 *      konan.jvmArgs       - additional args to be passed to a JVM executing the compiler/cinterop tool.
 */

internal fun Project.hasProperty(property: KonanPlugin.ProjectProperty) = hasProperty(property.propertyName)
internal fun Project.findProperty(property: KonanPlugin.ProjectProperty): Any? = findProperty(property.propertyName)

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
        assert(hasProperty(KonanPlugin.ProjectProperty.KONAN_HOME))
        return project.file(getProperty(KonanPlugin.ProjectProperty.KONAN_HOME)).canonicalPath
    }

internal val Project.konanBuildRoot          get() = buildDir.resolve("konan")
internal val Project.konanBinBaseDir         get() = konanBuildRoot.resolve("bin")
internal val Project.konanLibsBaseDir        get() = konanBuildRoot.resolve("libs")
internal val Project.konanBitcodeBaseDir     get() = konanBuildRoot.resolve("bitcode")

internal fun File.targetSubdir(target: KonanTarget) = resolve(target.visibleName)

internal val Project.konanDefaultSrcFiles         get() = fileTree("${projectDir.canonicalPath}/src/main/kotlin")
internal fun Project.konanDefaultDefFile(libName: String)
        = file("${projectDir.canonicalPath}/src/main/c_interop/$libName.def")

@Suppress("UNCHECKED_CAST")
internal val Project.konanArtifactsContainer: NamedDomainObjectContainer<KonanBuildingConfig<*>>
    get() = extensions.getByName(KonanPlugin.ARTIFACTS_CONTAINER_NAME)
            as NamedDomainObjectContainer<KonanBuildingConfig<*>>

internal val Project.platformManager: PlatformManager
    get() = findProperty("platformManager") as PlatformManager? ?:
            PlatformManager(customerDistribution(konanHome))

internal val Project.konanTargets: List<KonanTarget>
    get() = platformManager.toKonanTargets(konanExtension.targets)
                .filter{ platformManager.isEnabled(it) }
                .distinct()

@Suppress("UNCHECKED_CAST")
internal val Project.konanExtension: KonanExtension
    get() = extensions.getByName(KonanPlugin.KONAN_EXTENSION_NAME) as KonanExtension

internal val Project.konanCompilerDownloadTask
    get() = tasks.getByName(KonanPlugin.KONAN_DOWNLOAD_TASK_NAME)

internal val Project.konanVersion
    get() = getProperty(KonanPlugin.ProjectProperty.KONAN_VERSION, KonanPlugin.DEFAULT_KONAN_VERSION) as String

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
        "kotlin-native-${project.simpleOsName}-${this.konanVersion}"

internal fun Project.konanCompilerDownloadDir(): String =
        KonanCompilerDownloadTask.KONAN_PARENT_DIR + "/" + project.konanCompilerName()

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

internal fun MutableList<String>.addListArg(parameter: String, values: List<String>) {
    if (values.isNotEmpty()) {
        addArg(parameter, values.joinToString(separator = " "))
    }
}

// endregion

internal fun dumpProperties(task: Task) {
    fun Iterable<String>.dump() = joinToString(prefix = "[", separator = ",\n${" ".repeat(22)}", postfix = "]")
    fun Collection<FileCollection>.dump() = flatMap { it.files }.map { it.canonicalPath }.dump()
    when (task) {
        is KonanCompileTask -> with(task) {
            println()
            println("Compilation task: ${name}")
            println("destinationDir     : ${destinationDir}")
            println("artifact           : ${artifact.canonicalPath}")
            println("srcFiles         : ${srcFiles.dump()}")
            println("produce            : ${produce}")
            println("libraries          : ${libraries.files.dump()}")
            println("                   : ${libraries.artifacts.map {
                it.artifact.canonicalPath
            }.dump()}")
            println("                   : ${libraries.namedKlibs.dump()}")
            println("nativeLibraries    : ${nativeLibraries.dump()}")
            println("linkerOpts         : ${linkerOpts}")
            println("enableDebug        : ${enableDebug}")
            println("noStdLib           : ${noStdLib}")
            println("noMain             : ${noMain}")
            println("enableOptimization : ${enableOptimizations}")
            println("enableAssertions   : ${enableAssertions}")
            println("noDefaultLibs      : ${noDefaultLibs}")
            println("target             : ${target}")
            println("languageVersion    : ${languageVersion}")
            println("apiVersion         : ${apiVersion}")
            println("konanVersion       : ${konanVersion}")
            println("konanHome          : ${konanHome}")
            println()
        }
        is KonanInteropTask -> with(task) {
            println()
            println("Stub generation task: ${name}")
            println("destinationDir     : ${destinationDir}")
            println("artifact           : ${artifact}")
            println("libraries          : ${libraries.files.dump()}")
            println("                   : ${libraries.artifacts.map {
                it.artifact.canonicalPath
            }.dump()}")
            println("                   : ${libraries.namedKlibs.dump()}")
            println("defFile            : ${defFile}")
            println("target             : ${target}")
            println("packageName        : ${packageName}")
            println("compilerOpts       : ${compilerOpts}")
            println("linkerOpts         : ${linkerOpts}")
            println("headers            : ${headers.dump()}")
            println("linkFiles          : ${linkFiles.dump()}")
            println("konanVersion       : ${konanVersion}")
            println("konanHome          : ${konanHome}")
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
    val jvmArgs = mutableListOf<String>()
}

class KonanPlugin @Inject constructor(private val registry: ToolingModelBuilderRegistry)
    : Plugin<ProjectInternal> {

    enum class ProjectProperty(val propertyName: String) {
        KONAN_HOME          ("konan.home"),
        KONAN_VERSION       ("konan.version"),
        KONAN_BUILD_TARGETS ("konan.build.targets"),
        KONAN_JVM_ARGS      ("konan.jvmArgs"),
        DOWNLOAD_COMPILER   ("download.compiler")
    }

    companion object {
        internal const val ARTIFACTS_CONTAINER_NAME = "konanArtifacts"
        internal const val KONAN_DOWNLOAD_TASK_NAME = "checkKonanCompiler"
        internal const val KONAN_GENERATE_CMAKE_TASK_NAME = "generateCMake"
        internal const val COMPILE_ALL_TASK_NAME = "compileKonan"

        internal const val KONAN_EXTENSION_NAME = "konan"

        internal val DEFAULT_KONAN_VERSION = Properties().apply {
            load(KonanPlugin::class.java.getResourceAsStream("/META-INF/gradle-plugins/konan.properties") ?:
                throw RuntimeException("Cannot find a properties file"))
        }.getProperty("default-konan-version") ?: throw RuntimeException("Cannot read the default compiler version")
    }

    private fun Project.cleanKonan() = project.tasks.withType(KonanBuildingTask::class.java).forEach {
        project.delete(it.artifact)
    }

    override fun apply(project: ProjectInternal?) {
        if (project == null) { return }
        registry.register(KonanToolingModelBuilder)
        project.plugins.apply("base")
        // Create necessary tasks and extensions.
        project.tasks.create(KONAN_DOWNLOAD_TASK_NAME, KonanCompilerDownloadTask::class.java)
        project.tasks.create(KONAN_GENERATE_CMAKE_TASK_NAME, KonanGenerateCMakeTask::class.java)
        project.extensions.create(KONAN_EXTENSION_NAME, KonanExtension::class.java)
        project.extensions.create(KonanArtifactContainer::class.java, ARTIFACTS_CONTAINER_NAME, KonanArtifactContainer::class.java, project)

        // Set additional project properties like konan.home, konan.build.targets etc.
        if (!project.hasProperty(ProjectProperty.KONAN_HOME)) {
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

        // Create task to run supported executables.
        project.getOrCreateTask("run").apply {
            dependsOn(project.getTask("build"))
            doLast {
                for (task in project.tasks
                        .withType(KonanCompileProgramTask::class.java)
                        .matching { !it.isCrossCompile}) {
                    project.exec {
                        with(it) {
                            commandLine(task.artifact.canonicalPath)
                            if (project.extensions.extraProperties.has("runArgs")) {
                                args(project.extensions.extraProperties.get("runArgs").toString().split(' '))
                            }
                        }
                    }
                }
            }
        }

        // Enable multiplatform support
        project.pluginManager.apply(KotlinNativePlatformPlugin::class.java)
    }
}
