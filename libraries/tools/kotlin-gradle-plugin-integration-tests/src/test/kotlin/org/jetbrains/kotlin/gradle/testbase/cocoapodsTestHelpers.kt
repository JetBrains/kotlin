/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.BaseGradleIT
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.POD_INSTALL_TASK_NAME
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.gradle.util.runProcess
import java.io.File
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.test.fail

val String.normalizeCocoapadsFrameworkName: String
    get() = replace('-', '_')

enum class ImportMode(val directive: String) {
    FRAMEWORKS("use_frameworks!"),
    MODULAR_HEADERS("use_modular_headers!")
}

fun TestProject.useCustomCocoapodsFrameworkName(
    subprojectName: String,
    frameworkName: String,
    iosAppLocation: String? = null,
) {
    // Change the name at the Gradle side.
    subProject(subprojectName)
        .buildGradleKts
        .addFrameworkBlock("baseName = \"$frameworkName\"")

    // Change swift sources import if needed.
    if (iosAppLocation != null) {
        projectPath
            .resolve(iosAppLocation)
            .resolve("ios-app/ViewController.swift")
            .replaceText(
                "import ${subprojectName.normalizeCocoapadsFrameworkName}",
                "import $frameworkName"
            )
    }
}

/**
 * Prepares the Podfile for an iOS app in the [TestProject]
 *
 * @param iosAppLocation The relative location of the iOS app directory within the [TestProject]
 * @param mode The [ImportMode] to be set for the Podfile.
 *
 */
fun TestProject.preparePodfile(iosAppLocation: String, mode: ImportMode) {
    val iosAppDir = projectPath.resolve(iosAppLocation)

    // Set import mode for Podfile.
    iosAppDir.resolve("Podfile")
        .takeIf { it.exists() }
        ?.replaceText(podfileImportDirectivePlaceholder, mode.directive)
}

/**
 * Wraps the given string into a specRepos block and adds this block to the end of the [this] path.
 *
 * @param specRepo The code to be wrapped with the Cocoapods block.
 */

fun Path.addSpecRepo(specRepo: String) = addCocoapodsBlock("url(\"$specRepo\")".wrapIntoBlock("specRepos"))

/**
 * Wraps the given string into a Cocoapods block and adds this block to the end of the [this] path.
 *
 * @param str The code to be wrapped with the Cocoapods block.
 */
fun Path.addCocoapodsBlock(str: String) = addKotlinBlock(str.wrapIntoBlock("cocoapods"))

/**
 * Wraps the given string into a Kotlin block and adds this block to the end of the [this] path.
 *
 * @param str The code to be wrapped with the Cocoapods block.
 */
fun Path.addKotlinBlock(str: String) = appendLine(str.wrapIntoBlock("kotlin"))

/**
 * Wraps the given string into a Framework block and adds this block to the end of the [this] path.
 *
 * @param str The code to be wrapped with the Cocoapods block.
 */
fun Path.addFrameworkBlock(str: String) = addCocoapodsBlock(str.wrapIntoBlock("framework"))


/**
 * Adds a Cocoapods dependency to [this] build script.
 *
 * @param podName The name of the Cocoapods dependency to be added.
 * @param configuration The optional configuration string for the Cocoapods dependency.
 */
fun Path.addPod(podName: String, configuration: String? = null) {
    val pod = "pod(\"$podName\")"
    val podBlock = configuration?.wrapIntoBlock(pod) ?: pod
    addCocoapodsBlock(podBlock)
}

/**
 * Removes a Cocoapods dependency from [this] build script.
 *
 * @param podName The name of the Cocoapods dependency to be removes.
 */
fun Path.removePod(podName: String) {
    val text = readText()
    val begin = text.indexOf("""pod("$podName")""")
    require(begin != -1) {
        """
        Pod doesn't exist in file. File content is:
        ${text}
        """.trimIndent()
    }
    var index = begin + """pod("$podName")""".length - 1
    if (text.indexOf("""pod("$podName") {""", startIndex = begin) != -1) {
        index += 2
        var bracket = 1
        while (bracket != 0) {
            if (text[++index] == '{') {
                bracket++
            } else if (text[index] == '}') {
                bracket--
            }
        }
    }
    writeText(text.removeRange(begin..index))
}

/**
 * Method returns required environment variables for cocoapods tests with execution of [POD_INSTALL_TASK_NAME]
 */
fun cocoaPodsEnvironmentVariables(): Map<String, String> {
    if (!shouldInstallLocalCocoapods) {
        return emptyMap()
    }

    val path = cocoapodsBinPath.absolutePathString() + File.pathSeparator + System.getenv("PATH")
    val gemPath = System.getenv("GEM_PATH")?.let {
        cocoapodsInstallationRoot.absolutePathString() + File.pathSeparator + it
    } ?: cocoapodsInstallationRoot.absolutePathString()
    return mapOf(
        "PATH" to path,
        "GEM_PATH" to gemPath,
        // CocoaPods 1.11 requires UTF-8 locale being set, more details: https://github.com/CocoaPods/CocoaPods/issues/10939
        "LC_ALL" to "en_US.UTF-8"
    )
}

/**
 * This method checks if Cocoapods should be installed and verifies its installation status.
 * If [shouldInstallLocalCocoapods] is true, it tries to install Cocoapods into the specified [cocoapodsInstallationRoot]
 * if it is not already installed.
 *
 * @throws AssertionError if [shouldInstallLocalCocoapods] is false and  cocoapods has not been installed
 */
fun ensureCocoapodsInstalled() {
    if (shouldInstallLocalCocoapods) {
        val installDir = cocoapodsInstallationRoot.absolutePathString()
        println("Installing CocoaPods...")

        //https://github.com/ffi/ffi/issues/864#issuecomment-875242776
        gem("install", "--install-dir", installDir, "ffi", "-v", "1.15.5", "--", "--enable-libffi-alloc")

        gem("install", "--install-dir", installDir, "cocoapods", "-v", TestVersions.COCOAPODS.VERSION)
    } else if (!isCocoapodsInstalled()) {
        fail(
            """
                    Running CocoaPods integration tests requires cocoapods to be installed.
                    Please install them manually:
                        gem install cocoapods
                    Or re-run the tests with the 'installCocoapods=true' Gradle property.
                """.trimIndent()
        )
    }
}

@EnvironmentalVariablesOverride
fun KGPBaseTest.nativeProjectWithCocoapodsAndIosAppPodFile(
    projectName: String = templateProjectName,
    gradleVersion: GradleVersion,
    buildOptions: BuildOptions = this.defaultBuildOptions,
    projectBlock: TestProject.() -> Unit = {},
) {
    nativeProject(
        projectName,
        gradleVersion,
        buildOptions = buildOptions,
        environmentVariables = EnvironmentalVariables(cocoaPodsEnvironmentVariables())
    ) {
        preparePodfile("ios-app", ImportMode.FRAMEWORKS)
        projectBlock()
    }
}

private val templateProjectName = "native-cocoapods-template"

private val shouldInstallLocalCocoapods: Boolean = System.getProperty("installCocoapods").toBoolean()
private val cocoapodsInstallationRoot: Path by lazy { createTempDirectory("cocoapods") }
private val cocoapodsBinPath: Path by lazy { cocoapodsInstallationRoot.resolve("bin") }

private fun isCocoapodsInstalled(): Boolean {
    // Do not use 'gem list' because the gem may be installed but PATH may miss its executables.
    // Try to access the pod executable directly instead
    return try {
        val result = runProcess(
            listOf("pod", "--version"),
            File("."),
        )
        result.isSuccessful
    } catch (e: IOException) {
        false
    }
}

private fun gem(vararg args: String): String {
    val command = listOf("gem", *args)
    println("Run command: ${command.joinToString(separator = " ")}")
    val result = runProcess(command, File("."), options = BaseGradleIT.BuildOptions(forceOutputToStdout = true))
    check(result.isSuccessful) {
        "Process 'gem ${args.joinToString(separator = " ")}' exited with error code ${result.exitCode}. See log for details."
    }
    return result.output
}

private fun Path.appendLine(s: String) = appendText("\n$s")

private const val podfileImportDirectivePlaceholder = "<import_mode_directive>"