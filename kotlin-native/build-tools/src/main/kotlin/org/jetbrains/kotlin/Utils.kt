/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskState
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.TaskCollection
import org.jetbrains.kotlin.konan.properties.loadProperties
import org.jetbrains.kotlin.konan.properties.propertyList
import org.jetbrains.kotlin.konan.properties.saveProperties
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.library.KLIB_PROPERTY_NATIVE_TARGETS
import java.io.File
import java.util.concurrent.TimeUnit
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64
import java.nio.file.Path
import org.jetbrains.kotlin.konan.file.File as KFile
import org.gradle.nativeplatform.toolchain.internal.*
import org.gradle.nativeplatform.toolchain.plugins.ClangCompilerPlugin
import org.gradle.api.Incubating
import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.NonNullApi
import org.gradle.api.Plugin
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.plugins.PotentialPlugin
import org.gradle.api.tasks.TaskProvider
import org.gradle.internal.operations.BuildOperationExecutor
import org.gradle.internal.os.OperatingSystem
import org.gradle.internal.reflect.Instantiator
import org.gradle.internal.service.ServiceRegistry
import org.gradle.internal.work.WorkerLeaseService
import org.gradle.model.Defaults
import org.gradle.model.RuleSource
import org.gradle.nativeplatform.internal.CompilerOutputFileNamingSchemeFactory
import org.gradle.nativeplatform.platform.internal.NativePlatformInternal
import org.gradle.nativeplatform.plugins.NativeComponentPlugin
import org.gradle.nativeplatform.toolchain.Clang
import org.gradle.nativeplatform.toolchain.internal.clang.ClangToolChain
import org.gradle.nativeplatform.toolchain.internal.gcc.AbstractGccCompatibleToolChain
import org.gradle.nativeplatform.toolchain.internal.gcc.DefaultGccPlatformToolChain
import org.gradle.nativeplatform.toolchain.internal.gcc.metadata.SystemLibraryDiscovery
import org.gradle.nativeplatform.toolchain.internal.metadata.CompilerMetaDataProviderFactory
import org.gradle.nativeplatform.toolchain.internal.tools.CommandLineToolSearchResult
import org.gradle.nativeplatform.toolchain.internal.tools.GccCommandLineToolConfigurationInternal
import org.gradle.nativeplatform.toolchain.internal.tools.ToolSearchPath
import org.gradle.process.internal.ExecActionFactory
import java.io.ByteArrayOutputStream
import java.net.URI

//region Project properties.

val Project.platformManager
    get() = findProperty("platformManager") as PlatformManager

val Project.testTarget
    get() = findProperty("target") as? KonanTarget ?: HostManager.host

val Project.testTargetSuffix
    get() = (findProperty("target") as KonanTarget).name.capitalize()

val Project.verboseTest
    get() = hasProperty("test_verbose")

val Project.testOutputRoot
    get() = findProperty("testOutputRoot") as String

val Project.testOutputLocal
    get() = (findProperty("testOutputLocal") as File).toString()

val Project.testOutputStdlib
    get() = (findProperty("testOutputStdlib") as File).toString()

val Project.testOutputFramework
    get() = (findProperty("testOutputFramework") as File).toString()

val Project.testOutputExternal
    get() = (findProperty("testOutputExternal") as File).toString()

val Project.cacheRedirectorEnabled
    get() = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() ?: false

val Project.compileOnlyTests: Boolean
    get() = hasProperty("test_compile_only")

fun Project.redirectIfEnabled(url: String):String = if (cacheRedirectorEnabled) {
    val base = URL(url)
    "https://cache-redirector.jetbrains.com/${base.host}/${base.path}"
} else
    url

val validPropertiesNames = listOf("konan.home",
        "org.jetbrains.kotlin.native.home",
        "kotlin.native.home")

val Project.kotlinNativeDist
    get() = rootProject.currentKotlinNativeDist

val Project.currentKotlinNativeDist
    get() = file(validPropertiesNames.firstOrNull { hasProperty(it) }?.let { findProperty(it) } ?: "dist")

val kotlinNativeHome
    get() = validPropertiesNames.mapNotNull(System::getProperty).first()

val Project.useCustomDist
    get() = validPropertiesNames.any { hasProperty(it) }

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
    val (stdOut, stdErr, exitCode) = runProcess(executor = localExecutor(project), executable = "/usr/bin/codesign",
            args = listOf("--verbose", "-s", "-", path))
    check(exitCode == 0) { """
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
 * Check that [target] is an Apple device.
 */
fun supportsRunningTestsOnDevice(target: KonanTarget): Boolean =
    target == KonanTarget.IOS_ARM32 || target == KonanTarget.IOS_ARM64

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
                .map{ it.absolutePath }
                .asIterable()
    }
}

//region Task dependency.

fun Project.findKonanBuildTask(artifact: String, target: KonanTarget): TaskProvider<Task> =
    tasks.named("compileKonan${artifact.capitalize()}${target.name.capitalize()}")

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

private val Project.isCrossDist: Boolean
    get() {
        if (!isDefaultNativeHome) {
            return File(buildDistribution(project.kotlinNativeDist.absolutePath).runtime(project.testTarget))
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
        if (!project.isCrossDist) {
            dependsOn(":kotlin-native:${target.name}CrossDist")
        }
    }
}

fun Task.konanOldPluginTaskDependenciesWalker(index:Int = 0, walker: Task.(Int)->Unit) {
    walker(index + 1)
    dependsOn.forEach{
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
// Run command line from string.
fun Array<String>.runCommand(workingDir: File = File("."),
                             timeoutAmount: Long = 60,
                             timeoutUnit: TimeUnit = TimeUnit.SECONDS): String {
    return try {
        ProcessBuilder(*this)
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start().apply {
                    waitFor(timeoutAmount, timeoutUnit)
                }.inputStream.bufferedReader().readText()
    } catch (e: Exception) {
        println("Couldn't run command ${this.joinToString(" ")}")
        println(e.stackTrace.joinToString("\n"))
        error(e.message!!)
    }
}

fun String.splitCommaSeparatedOption(optionName: String) =
        split("\\s*,\\s*".toRegex()).map {
            if (it.isNotEmpty()) listOf(optionName, it) else listOf(null)
        }.flatten().filterNotNull()

data class Commit(val revision: String, val developer: String, val webUrlWithDescription: String)

val teamCityUrl = "http://buildserver.labs.intellij.net"


fun buildsUrl(buildLocator: String) =
        "$teamCityUrl/app/rest/builds/?locator=$buildLocator"

fun getBuild(buildLocator: String, user: String, password: String) =
        try {
            sendGetRequest(buildsUrl(buildLocator), user, password)
        } catch (t: Throwable) {
            error("Try to get build! TeamCity is unreachable!")
        }

fun sendGetRequest(url: String, username: String? = null, password: String? = null) : String {
    val connection = URL(url).openConnection() as HttpURLConnection
    if (username != null && password != null) {
        val auth = Base64.getEncoder().encode(("$username:$password").toByteArray()).toString(Charsets.UTF_8)
        connection.addRequestProperty("Authorization", "Basic $auth")
    }
    connection.setRequestProperty("Accept", "application/json");
    connection.connect()
    return connection.inputStream.use { it.reader().use { reader -> reader.readText() } }
}


@JvmOverloads
fun compileSwift(project: Project, target: KonanTarget, sources: List<String>, options: List<String>,
                 output: Path, fullBitcode: Boolean = false) {
    val platform = project.platformManager.platform(target)
    assert(platform.configurables is AppleConfigurables)
    val configs = platform.configurables as AppleConfigurables
    val compiler = configs.absoluteTargetToolchain + "/usr/bin/swiftc"

    val swiftTarget = configs.targetTriple.withOSVersion(configs.osVersionMin).toString()

    val args = listOf("-sdk", configs.absoluteTargetSysRoot, "-target", swiftTarget) +
            options + "-o" + output.toString() + sources +
            if (fullBitcode) listOf("-embed-bitcode", "-Xlinker", "-bitcode_verify") else listOf("-embed-bitcode-marker")

    val (stdOut, stdErr, exitCode) = runProcess(executor = localExecutor(project), executable = compiler, args = args)

    println("""
        |$compiler finished with exit code: $exitCode
        |options: ${args.joinToString(separator = " ")}
        |stdout: $stdOut
        |stderr: $stdErr
        """.trimMargin())
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
