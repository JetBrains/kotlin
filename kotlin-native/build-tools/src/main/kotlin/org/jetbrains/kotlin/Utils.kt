/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(ExperimentalStdlibApi::class)

package org.jetbrains.kotlin

import com.google.gson.GsonBuilder
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.konan.properties.loadProperties
import org.jetbrains.kotlin.konan.properties.propertyList
import org.jetbrains.kotlin.konan.properties.saveProperties
import org.jetbrains.kotlin.konan.target.*
import java.io.File
import java.util.concurrent.TimeUnit
import java.nio.file.Path
import org.jetbrains.kotlin.konan.file.File as KFile
import org.gradle.api.tasks.TaskProvider
import java.util.*
import kotlin.collections.HashSet

/**
 * Copy-pasted from [org.jetbrains.kotlin.library.KLIB_PROPERTY_NATIVE_TARGETS]
 */
private const val KLIB_PROPERTY_NATIVE_TARGETS = "native_targets"

//region Project properties.

val Project.platformManager
    get() = findProperty("platformManager") as PlatformManager

val Project.testTarget
    get() = findProperty("target") as? KonanTarget ?: HostManager.host

val Project.testTargetSuffix
    get() = (findProperty("target") as KonanTarget).name.replaceFirstChar { it.uppercase() }

val Project.verboseTest
    get() = hasProperty("test_verbose")

val Project.testOutputRoot
    get() = findProperty("testOutputRoot") as String

val Project.testOutputLocal
    get() = (findProperty("testOutputLocal") as File).toString()

val Project.testOutputFramework
    get() = (findProperty("testOutputFramework") as File).toString()

val Project.testOutputExternal
    get() = (findProperty("testOutputExternal") as File).toString()

val Project.compileOnlyTests: Boolean
    get() = hasProperty("test_compile_only")

val validPropertiesNames = listOf(
    "konan.home",
    "org.jetbrains.kotlin.native.home",
    "kotlin.native.home"
)

val Project.kotlinNativeDist
    get() = rootProject.currentKotlinNativeDist

val Project.currentKotlinNativeDist
    get() = file(validPropertiesNames.firstOrNull { hasProperty(it) }?.let { findProperty(it) } ?: "dist")

val kotlinNativeHome
    get() = validPropertiesNames.mapNotNull(System::getProperty).first()

val Project.useCustomDist
    get() = validPropertiesNames.any { hasProperty(it) }

val Project.nativeBundlesLocation
    get() = file(findProperty("nativeBundlesLocation") ?: project.projectDir)

private val libraryRegexp = Regex("""^import\s+platform\.(\S+)\..*$""")
fun File.dependencies() =
    readLines().filter(libraryRegexp::containsMatchIn)
        .map { libraryRegexp.matchEntire(it)?.groups?.get(1)?.value ?: "" }
        .toSortedSet()


fun Task.dependsOnPlatformLibs() {
    if (!project.hasPlatformLibs) {
        (this as? KonanTest)?.run {
            project.file(source).dependencies().forEach {
                this.dependsOn(":kotlin-native:platformLibs:${project.testTarget.name}-$it")
                //this.dependsOn(":kotlin-native:platformLibs:${project.testTarget.name}-${it}Cache")
            }
            if (this is KonanLinkTest) {
                project.file(lib).dependencies().forEach {
                    this.dependsOn(":kotlin-native:platformLibs:${project.testTarget.name}-$it")
                }
            }
            this.dependsOnDist()
        } ?: error("unsupported task : $this")
    }
}

@Suppress("UNCHECKED_CAST")
private fun Project.groovyPropertyArrayToList(property: String): List<String> =
    with(findProperty(property)) {
        if (this is Array<*>) this.toList() as List<String>
        else this as List<String>
    }

val Project.globalBuildArgs: List<String>
    get() = project.groovyPropertyArrayToList("globalBuildArgs")

val Project.globalTestArgs: List<String>
    get() = project.groovyPropertyArrayToList("globalTestArgs")

val Project.testTargetSupportsCodeCoverage: Boolean
    get() = this.testTarget.supportsCodeCoverage()

fun projectOrFiles(proj: Project, notation: String): Any? {
    val propertyMapper = proj.findProperty("notationMapping") ?: return proj.project(notation)
    val mapping = (propertyMapper as? Map<*, *>)?.get(notation) as? String ?: return proj.project(notation)
    return proj.files(mapping).also {
        proj.logger.info("MAPPING: $notation -> ${it.asPath}")
    }
}

//endregion

/**
 * Ad-hoc signing of the specified path.
 */
fun codesign(project: Project, path: String) {
    check(HostManager.hostIsMac) { "Apple specific code signing" }
    val (stdOut, stdErr, exitCode) = runProcess(
        executor = localExecutor(project), executable = "/usr/bin/codesign",
        args = listOf("--verbose", "-s", "-", path)
    )
    check(exitCode == 0) {
        """
        |Codesign failed with exitCode: $exitCode
        |stdout: $stdOut
        |stderr: $stdErr
        """.trimMargin()
    }
}

/**
 * Check that [target] is Apple simulator
 */
fun isSimulatorTarget(project: Project, target: KonanTarget): Boolean =
    project.platformManager.platform(target).targetTriple.isSimulator

/**
 * Creates a list of file paths to be compiled from the given [compile] list with regard to [exclude] list.
 */
fun Project.getFilesToCompile(compile: List<String>, exclude: List<String>): List<String> {
    // convert exclude list to paths
    val excludeFiles = exclude.map { project.file(it).absolutePath }.toList()

    // create list of tests to compile
    return compile.flatMap { f ->
        project.file(f)
            .walk()
            .filter { it.isFile && it.name.endsWith(".kt") && !excludeFiles.contains(it.absolutePath) }
            .map { it.absolutePath }
            .asIterable()
    }
}

//region Task dependency.

fun Project.findKonanBuildTask(artifact: String, target: KonanTarget): TaskProvider<Task> =
    tasks.named("compileKonan${artifact.replaceFirstChar { it.uppercase() }}${target.name.replaceFirstChar { it.uppercase() }}")

fun Project.dependsOnDist(taskName: String) {
    project.tasks.getByName(taskName).dependsOnDist()
}

fun TaskProvider<Task>.dependsOnDist() {
    configure {
        dependsOnDist()
    }
}

fun Task.isDependsOnPlatformLibs(): Boolean {
    return dependsOn.any {
        it.toString().contains(":kotlin-native:platformLibs") ||
                it.toString().contains(":kotlin-native:distPlatformLibs")
    }
}

val Project.isDefaultNativeHome: Boolean
    get() = kotlinNativeDist.absolutePath == project(":kotlin-native").file("dist").absolutePath

private val Project.hasPlatformLibs: Boolean
    get() {
        if (!isDefaultNativeHome) {
            return File(buildDistribution(project.kotlinNativeDist.absolutePath).platformLibs(project.testTarget))
                .exists()
        }
        return false
    }

private fun Project.isCrossDist(target: KonanTarget): Boolean {
    if (!isDefaultNativeHome) {
        return File(buildDistribution(project.kotlinNativeDist.absolutePath).runtime(target))
                .exists()
    }
    return false
}

fun Task.dependsOnDist() {
    val target = project.testTarget
    if (project.isDefaultNativeHome) {
        dependsOn(":kotlin-native:dist")
        if (target != HostManager.host) {
            // if a test_target property is set then tests should depend on a crossDist
            // otherwise, runtime components would not be build for a target.
            dependsOn(":kotlin-native:${target.name}CrossDist")
        }
    } else {
        if (!project.isCrossDist(project.testTarget)) {
            dependsOn(":kotlin-native:${target.name}CrossDist")
        }
    }
}

fun Task.dependsOnCrossDist(target: KonanTarget) {
    if (project.isDefaultNativeHome) {
        if (target != HostManager.host) {
            // if a test_target property is set then tests should depend on a crossDist
            // otherwise, runtime components would not be build for a target.
            dependsOn(":kotlin-native:${target.name}CrossDist")
        }
    } else {
        if (!project.isCrossDist(target)) {
            dependsOn(":kotlin-native:${target.name}CrossDist")
        }
    }
}

fun Task.konanOldPluginTaskDependenciesWalker(index: Int = 0, walker: Task.(Int) -> Unit) {
    walker(index + 1)
    dependsOn.forEach {
        val task = (it as? Task) ?: return@forEach
        if (task.name.startsWith("compileKonan"))
            task.konanOldPluginTaskDependenciesWalker(index + 1, walker)
    }
}

/**
 * Sets the same dependencies for the receiver task from the given [task]
 */
fun String.sameDependenciesAs(task: Task) {
    val t = task.project.tasks.getByName(this)
    t.sameDependenciesAs(task)
}

/**
 * Sets the same dependencies for the receiver task from the given [task]
 */
fun Task.sameDependenciesAs(task: Task) {
    val dependencies = task.dependsOn.toList() // save to the list, otherwise it will cause cyclic dependency.
    this.dependsOn(dependencies)
}

/**
 * Set dependency on [artifact] built by the Konan Plugin for the receiver task,
 * also make [artifact] depend on `dist` and all dependencies of the task to make [artifact] execute before the task.
 */
fun Task.dependsOnKonanBuildingTask(artifact: String, target: KonanTarget) {
    val buildTask = project.findKonanBuildTask(artifact, target)
    buildTask.get().apply {
        konanOldPluginTaskDependenciesWalker {
            dependsOnDist()
        }
        sameDependenciesAs(this@dependsOnKonanBuildingTask)
    }
    dependsOn(buildTask)
}

//endregion

@JvmOverloads
fun compileSwift(
    project: Project, target: KonanTarget, sources: List<String>, options: List<String>,
    output: Path, fullBitcode: Boolean = false
) {
    val platform = project.platformManager.platform(target)
    assert(platform.configurables is AppleConfigurables)
    val configs = platform.configurables as AppleConfigurables
    val compiler = configs.absoluteTargetToolchain + "/usr/bin/swiftc"

    val swiftTarget = configs.targetTriple.withOSVersion(configs.osVersionMin).toString()

    val args = listOf("-sdk", configs.absoluteTargetSysRoot, "-target", swiftTarget) +
            options + "-o" + output.toString() + sources +
            if (fullBitcode) listOf("-embed-bitcode", "-Xlinker", "-bitcode_verify") else listOf("-embed-bitcode-marker")

    val (stdOut, stdErr, exitCode) = runProcess(
            executor = localExecutor(project), executable = compiler, args = args,
            env = mapOf("DYLD_FALLBACK_FRAMEWORK_PATH" to configs.absoluteTargetToolchain + "/ExtraFrameworks")
    )

    println(
        """
        |$compiler finished with exit code: $exitCode
        |options: ${args.joinToString(separator = " ")}
        |stdout: $stdOut
        |stderr: $stdErr
        """.trimMargin()
    )
    check(exitCode == 0) { "Compilation failed" }
    check(output.toFile().exists()) { "Compiler swiftc hasn't produced an output file: $output" }
}

fun targetSupportsMimallocAllocator(targetName: String) =
    HostManager().targetByName(targetName).supportsMimallocAllocator()

fun targetSupportsLibBacktrace(targetName: String) =
    HostManager().targetByName(targetName).supportsLibBacktrace()

fun targetSupportsCoreSymbolication(targetName: String) =
    HostManager().targetByName(targetName).supportsCoreSymbolication()

fun targetSupportsThreads(targetName: String) =
    HostManager().targetByName(targetName).supportsThreads()

fun Project.mergeManifestsByTargets(source: File, destination: File) {
    logger.info("Merging manifests: $source -> $destination")

    val sourceFile = KFile(source.absolutePath)
    val sourceProperties = sourceFile.loadProperties()

    val destinationFile = KFile(destination.absolutePath)
    val destinationProperties = destinationFile.loadProperties()

    // check that all properties except for KLIB_PROPERTY_NATIVE_TARGETS are equivalent
    val mismatchedProperties = (sourceProperties.keys + destinationProperties.keys)
        .asSequence()
        .map { it.toString() }
        .filter { it != KLIB_PROPERTY_NATIVE_TARGETS }
        .sorted()
        .mapNotNull { propertyKey: String ->
            val sourceProperty: String? = sourceProperties.getProperty(propertyKey)
            val destinationProperty: String? = destinationProperties.getProperty(propertyKey)
            when {
                sourceProperty == null -> "\"$propertyKey\" is absent in $sourceFile"
                destinationProperty == null -> "\"$propertyKey\" is absent in $destinationFile"
                sourceProperty == destinationProperty -> {
                    // properties match, OK
                    null
                }
                sourceProperties.propertyList(propertyKey, escapeInQuotes = true).toSet() ==
                        destinationProperties.propertyList(propertyKey, escapeInQuotes = true).toSet() -> {
                    // properties match, OK
                    null
                }
                else -> "\"$propertyKey\" differ: [$sourceProperty] vs [$destinationProperty]"
            }
        }
        .toList()

    check(mismatchedProperties.isEmpty()) {
        buildString {
            appendln("Found mismatched properties while merging manifest files: $source -> $destination")
            mismatchedProperties.joinTo(this, "\n")
        }
    }

    // merge KLIB_PROPERTY_NATIVE_TARGETS property
    val sourceNativeTargets = sourceProperties.propertyList(KLIB_PROPERTY_NATIVE_TARGETS)
    val destinationNativeTargets = destinationProperties.propertyList(KLIB_PROPERTY_NATIVE_TARGETS)

    val mergedNativeTargets = HashSet<String>().apply {
        addAll(sourceNativeTargets)
        addAll(destinationNativeTargets)
    }

    destinationProperties[KLIB_PROPERTY_NATIVE_TARGETS] = mergedNativeTargets.joinToString(" ")

    destinationFile.saveProperties(destinationProperties)
}

fun Project.buildStaticLibrary(cSources: Collection<File>, output: File, objDir: File) {
    delete(objDir)
    delete(output)

    val platform = platformManager.platform(testTarget)

    objDir.mkdirs()
    ExecClang.create(project).execClangForCompilerTests(testTarget) {
        args = listOf("-c", *cSources.map { it.absolutePath }.toTypedArray())
        workingDir(objDir)
    }

    output.parentFile.mkdirs()
    exec {
        commandLine(
            "${platform.configurables.absoluteLlvmHome}/bin/llvm-ar",
            "-rc",
            output,
            *fileTree(objDir).files.toTypedArray()
        )
    }
}

fun Project.binaryFromToolchain(toolName: String): File {
    val platform = platformManager.platform(testTarget)
    return File("${platform.configurables.absoluteTargetToolchain}/bin/$toolName")
}

// Workaround the deprecation warning from stdlib's appendln, which is reported because this module is compiled with API version 1.3.
internal fun StringBuilder.appendln(o: Any?) {
    append(o)
    append('\n')
}

internal val Project.testTargetConfigurables: Configurables
    get() {
        val platformManager = project.platformManager
        val testTarget = project.testTarget
        return platformManager.platform(testTarget).configurables
    }

internal val gson = GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()!!

internal val Project.ext: ExtraPropertiesExtension
    get() = extensions.getByName("ext") as ExtraPropertiesExtension

internal val FileCollection.isNotEmpty: Boolean
    get() = !isEmpty

internal fun Provider<File>.resolve(child: String): Provider<File> = map { it.resolve(child) }
