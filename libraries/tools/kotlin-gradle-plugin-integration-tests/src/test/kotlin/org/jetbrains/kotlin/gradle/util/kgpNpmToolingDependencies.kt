/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.kotlin.dsl.withType
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.targets.js.allDependenciesInternal
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNpmTooling
import org.jetbrains.kotlin.gradle.targets.wasm.npm.WasmNpmExtension
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootExtension
import org.jetbrains.kotlin.gradle.testbase.TestProject
import org.jetbrains.kotlin.gradle.testbase.buildScriptInjection
import org.jetbrains.kotlin.gradle.testbase.buildScriptReturn
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.Serializable
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.reflect.KClass

internal val kgpPackageLockJsonFileContent: String by lazy {
    NodeJsPlugin::class.loadResource("/org/jetbrains/kotlin/gradle/targets/js/npm/package-lock.json")
}

internal val kgpYarnLockFileContent: String by lazy {
    NodeJsPlugin::class.loadResource("/org/jetbrains/kotlin/gradle/targets/js/yarn/yarn.lock")
}

private fun KClass<*>.loadResource(path: String): String {
    java.getResourceAsStream(path).use { source ->
        requireNotNull(source) { "Resource not found: $path" }
        return source.bufferedReader().readText()
    }
}

/**
 * Create a `package.json` file containing KGP's tooling dependencies.
 *
 * @see [org.jetbrains.kotlin.gradle.targets.js.NpmVersions]
 */
private fun createKgpNpmToolingPackageJson(
    defaultNpmVersions: List<NpmPackageVersionForTests>,
): String {
    val json = Json {
        prettyPrint = true
    }

    val packageJson: JsonObject =
        buildJsonObject {
            put("name", "kotlin-tooling-dependencies")
            put("version", "1.0.0")
            put("private", true)
            put("dependencies", buildJsonObject {
                defaultNpmVersions.forEach { dep ->
                    put(dep.name, dep.requestedVersion)
                }
            })
        }

    return json.encodeToString(JsonObject.serializer(), packageJson)
}

/**
 * Configure a custom installation directory for KGP's npm tooling dependencies.
 */
internal fun TestProject.setupCustomKgpNpmToolingDependenciesDir(
    toolingCustomDir: Path,
    useYarn: Boolean,
) {
    // Use the same NpmVersions that KGP uses (which is important, because we're also using KGP's lockfiles)
    val defaultNpmVersions = extractDefaultKgpNpmPackageVersions()
    val packageJson = createKgpNpmToolingPackageJson(defaultNpmVersions)

    createCustomKgpNpmToolingDependenciesDir(
        toolingCustomDir = toolingCustomDir,
        useYarn = useYarn,
        packageJson = packageJson,
    )
    setCustomKgpNpmToolingDependenciesDir(toolingCustomDir = toolingCustomDir)
    registerCustomNpmToolingInstallTask(
        useYarn = useYarn,
        toolingCustomDir = toolingCustomDir,
    )
}

/**
 * Extract KGP's default npm dependencies.
 */
private fun TestProject.extractDefaultKgpNpmPackageVersions(): List<NpmPackageVersionForTests> {
    return buildScriptReturn {
        val npmVersions = project.extensions.getByName<WasmNodeJsRootExtension>(WasmNodeJsRootExtension.EXTENSION_NAME).versions
        @Suppress("INVISIBLE_REFERENCE")
        npmVersions.allDependenciesInternal(project.objects, project.providers).map { npv ->
            NpmPackageVersionForTests(
                name = npv.name.get(),
                requestedVersion = npv.requestedVersion.get(),
            )
        }
    }.buildAndReturn()
}

/**
 * Serializable copy of [org.jetbrains.kotlin.gradle.targets.js.NpmPackageVersionInternal].
 * (This class is required because `buildScriptReturn {}` can't serialize
 * `NpmPackageVersionInternal` because it can't serialize [org.gradle.api.provider.Property].)
 */
private data class NpmPackageVersionForTests(
    val name: String,
    val requestedVersion: String,
) : Serializable

private fun createCustomKgpNpmToolingDependenciesDir(
    toolingCustomDir: Path,
    useYarn: Boolean,
    packageJson: String,
) {
    toolingCustomDir.createDirectories()

    // A `package.json` file is required for `npm install` and `yarn install` commands to work.
    toolingCustomDir.resolve("package.json").writeText(packageJson)

    if (useYarn) {
        toolingCustomDir.resolve("yarn.lock").writeText(kgpYarnLockFileContent)
    } else {
        toolingCustomDir.resolve("package-lock.json").writeText(kgpPackageLockJsonFileContent)
    }
}

private fun TestProject.setCustomKgpNpmToolingDependenciesDir(
    toolingCustomDir: Path,
) {
    val toolingCustomDir = toolingCustomDir.toFile()
    buildScriptInjection {
        project.rootProject.plugins.withType<WasmNodeJsRootPlugin>().configureEach { _ ->
            project.rootProject.extensions.getByType(WasmNpmTooling::class.java).apply {
                installationDir.fileValue(project.projectDir.resolve(toolingCustomDir))
            }
        }
    }
}

/**
 * Register a task to install KGP npm tooling dependencies into a custom directory.
 *
 * Used to test if users can specify custom npm/yarn executable locations,
 * meaning KGP does not need to install them.
 */
private fun TestProject.registerCustomNpmToolingInstallTask(
    useYarn: Boolean,
    toolingCustomDir: Path,
) {
    val toolingCustomDir = toolingCustomDir.toFile()

    buildScriptInjection {
        project.tasks.register("toolingInstall").configure { task ->
            task.description = "Custom installer task for KGP npm tooling dependencies."

            val nodeJsEnvSpec = project.extensions.getByType(WasmNodeJsEnvSpec::class.java)
            val nodejsExecutable = nodeJsEnvSpec.executable

            with(nodeJsEnvSpec) {
                task.dependsOn(project.nodeJsSetupTaskProvider)
            }
            if (useYarn) {
                task.dependsOn(WasmYarnRootExtension[project.rootProject].yarnSetupTaskProvider)
            }

            val exec = project.serviceOf<ExecOperations>()

            val toolExecutable = if (useYarn) {
                nodejsExecutable
            } else {
                project.provider {
                    project.rootProject.extensions.getByType(WasmNpmExtension::class.java).requireConfigured().executable
                }
            }

            val installArgs = if (useYarn) {
                project.rootProject.extensions.getByType(WasmYarnRootEnvSpec::class.java).executable.map { yarnExecutable ->
                    listOf(
                        yarnExecutable,
                        "install",
                        "--ignore-scripts",
                        "--frozen-lockfile",
                    )
                }
            } else {
                project.provider {
                    listOf("install", "--ignore-scripts")
                }
            }

            task.doLast { _ ->
                val execOutput = ByteArrayOutputStream()
                val result = exec.exec { exec ->
                    exec.executable(toolExecutable.get())
                    exec.args(installArgs.get())
                    exec.workingDir(toolingCustomDir)
                    exec.standardOutput = execOutput
                    exec.errorOutput = execOutput
                    exec.isIgnoreExitValue = true

                    if (!useYarn) {
                        val nodePath = File(nodejsExecutable.get()).parent
                        exec.environment["PATH"] =
                            "$nodePath${File.pathSeparator}${System.getenv("PATH")}"
                    }
                }
                require(result.exitValue == 0) {
                    buildString {
                        appendLine("${task.path}} failed")
                        appendLine(execOutput)
                    }
                }
            }
        }
    }
}
