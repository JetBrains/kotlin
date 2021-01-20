/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin

import org.gradle.api.Project
import org.gradle.api.Task
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
import org.jetbrains.report.json.*
import java.nio.file.Path
import org.jetbrains.kotlin.konan.file.File as KFile

//region Project properties.

val Project.platformManager
    get() = findProperty("platformManager") as PlatformManager

val Project.testTarget
    get() = findProperty("target") as KonanTarget

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

val Project.kotlinNativeDist
    get() = this.rootProject.file(this.findProperty("org.jetbrains.kotlin.native.home")
            ?: this.findProperty("konan.home") ?: "dist")

@Suppress("UNCHECKED_CAST")
val Project.globalTestArgs: List<String>
    get() = with(findProperty("globalTestArgs")) {
            if (this is Array<*>) this.toList() as List<String>
            else this as List<String>
    }

val Project.testTargetSupportsCodeCoverage: Boolean
    get() = this.testTarget.supportsCodeCoverage()

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

fun Project.findKonanBuildTask(artifact: String, target: KonanTarget): Task =
    tasks.getByName("compileKonan${artifact.capitalize()}${target.name.capitalize()}")

fun Project.dependsOnDist(taskName: String) {
    project.tasks.getByName(taskName).dependsOnDist()
}

fun Task.dependsOnDist() {
    val rootTasks = project.rootProject.tasks
    // We don't build the compiler if a custom dist path is specified.
    if (!(project.findProperty("useCustomDist") as Boolean)) {
        dependsOn(rootTasks.getByName("dist"))
        val target = project.testTarget
        if (target != HostManager.host) {
            // if a test_target property is set then tests should depend on a crossDist
            // otherwise, runtime components would not be build for a target.
            dependsOn(rootTasks.getByName("${target.name}CrossDist"))
        }
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
    buildTask.dependsOnDist()
    buildTask.sameDependenciesAs(this)
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

// List of commits.
class CommitsList(data: JsonElement): ConvertedFromJson {

    val commits: List<Commit>

    init {
        if (data !is JsonObject) {
            error("Commits description is expected to be a json object!")
        }
        val changesElement = data.getOptionalField("change")
        commits = changesElement?.let {
            if (changesElement !is JsonArray) {
                error("Change field is expected to be an array. Please, check source.")
            }
            changesElement.jsonArray.map {
                with(it as JsonObject) {
                    Commit(elementToString(getRequiredField("version"), "version"),
                            elementToString(getRequiredField("username"), "username"),
                            elementToString(getRequiredField("webUrl"), "webUrl")
                    )
                }
            }
        } ?: listOf<Commit>()
    }
}

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

fun getBuildProperty(buildJsonDescription: String, property: String) =
        with(JsonTreeParser.parse(buildJsonDescription) as JsonObject) {
            if (getPrimitive("count").int == 0) {
                error("No build information on TeamCity for $buildJsonDescription!")
            }
            (getArray("build").getObject(0).getPrimitive(property) as JsonLiteral).unquoted()
        }

@JvmOverloads
fun compileSwift(project: Project, target: KonanTarget, sources: List<String>, options: List<String>,
                 output: Path, fullBitcode: Boolean = false) {
    val platform = project.platformManager.platform(target)
    assert(platform.configurables is AppleConfigurables)
    val configs = platform.configurables as AppleConfigurables
    val compiler = configs.absoluteTargetToolchain + "/usr/bin/swiftc"

    val swiftTarget = when (target) {
        KonanTarget.IOS_X64   -> "x86_64-apple-ios" + configs.osVersionMin
        KonanTarget.IOS_ARM32 -> "armv7-apple-ios" + configs.osVersionMin
        KonanTarget.IOS_ARM64 -> "arm64-apple-ios" + configs.osVersionMin
        KonanTarget.TVOS_X64   -> "x86_64-apple-tvos" + configs.osVersionMin
        KonanTarget.TVOS_ARM64 -> "arm64-apple-tvos" + configs.osVersionMin
        KonanTarget.MACOS_X64 -> "x86_64-apple-macosx" + configs.osVersionMin
        KonanTarget.MACOS_ARM64 -> "arm64-apple-macos" + configs.osVersionMin
        KonanTarget.WATCHOS_X86 -> "i386-apple-watchos" + configs.osVersionMin
        KonanTarget.WATCHOS_X64 -> "x86_64-apple-watchos" + configs.osVersionMin
        else -> throw IllegalStateException("Test target $target is not supported")
    }

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
    exec {
        it.commandLine(platform.clang.clangC(
                "-c",
                *cSources.map { it.absolutePath }.toTypedArray()
        ))
        it.workingDir(objDir)
    }

    output.parentFile.mkdirs()
    exec {
        it.commandLine(
                "${platform.configurables.absoluteLlvmHome}/bin/llvm-ar",
                "-rc",
                output,
                *fileTree(objDir).files.toTypedArray()
        )
    }
}