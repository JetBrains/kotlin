/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.process.ExecSpec
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.disambiguateName
import org.jetbrains.kotlin.gradle.plugin.mpp.fileExtension
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin.Companion.kotlinNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinPackageJsonTask
import org.jetbrains.kotlin.gradle.targets.js.webTargetVariant
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.io.Serializable
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin.Companion.kotlinNodeJsEnvSpec as wasmKotlinNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin.Companion.kotlinNodeJsRootExtension as wasmKotlinNodeJsRootExtension

val KotlinJsIrCompilation.npmProject: NpmProject
    get() = NpmProject(this)

@Deprecated("Use npmProject for KotlinJsIrCompilation. Scheduled for removal in Kotlin 2.3.", level = DeprecationLevel.ERROR)
val KotlinJsCompilation.npmProject: NpmProject
    get() = NpmProject(this as KotlinJsIrCompilation)

/**
 * Basic info for [NpmProject] created from [compilation].
 * This class contains only basic info.
 *
 * More info can be obtained from [KotlinCompilationNpmResolution], which is available after project resolution (after [KotlinNpmInstallTask] execution).
 */
open class NpmProject(@Transient val compilation: KotlinJsIrCompilation) : Serializable {
    val compilationName = compilation.disambiguatedName

    private val extension: Provider<String> = compilation.fileExtension

    val name: Provider<String> = compilation.outputModuleName

    @delegate:Transient
    val nodeJsRoot by lazy {
        compilation.webTargetVariant(
            { project.rootProject.kotlinNodeJsRootExtension },
            { project.rootProject.wasmKotlinNodeJsRootExtension },
        )
    }

    @delegate:Transient
    val nodeJs by lazy {
        compilation.webTargetVariant(
            { project.kotlinNodeJsEnvSpec },
            { project.wasmKotlinNodeJsEnvSpec },
        )
    }

    val dir: Provider<Directory> = nodeJsRoot.projectPackagesDirectory.zip(name) { directory, name ->
        directory.dir(name)
    }

    val target: KotlinJsTargetDsl
        get() = compilation.target

    val project: Project
        get() = target.project

    val nodeModulesDir: Provider<Directory> = nodeJsRoot.rootPackageDirectory.map { it.dir(NODE_MODULES) }

    val packageJsonFile: Provider<RegularFile>
        get() = dir.map { it.file(PACKAGE_JSON) }

    val packageJsonTaskName: String
        get() = compilation.disambiguateName("packageJson")

    val packageJsonTask: KotlinPackageJsonTask
        get() = project.tasks.getByName(packageJsonTaskName) as KotlinPackageJsonTask

    val packageJsonTaskPath: String
        get() = packageJsonTask.path

    val dist: Provider<Directory>
        get() = dir.map { it.dir(DIST_FOLDER) }

    val main: Provider<String> = extension.zip(name) { ext, name ->
        "${DIST_FOLDER}/$name.$ext"
    }

    private val typesFileExtension = extension
        .zip(compilation.target.shouldGenerateTypeScriptDefinitions) { extension, shouldGenerateTypeScriptDefinitions ->
            runIf(shouldGenerateTypeScriptDefinitions) {
                when (extension) {
                    "mjs" -> "d.mts"
                    "js" -> "d.ts"
                    else -> error("Illegal JS-file extension provided: $extension")
                }
            }
        }

    val typesFileName: Provider<String> = name.zip(typesFileExtension) { name, extension ->
        "$name.$extension"
    }

    val typesFilePath: Provider<String> = typesFileName.map { "$DIST_FOLDER/$it" }

    val publicPackageJsonTaskName: String
        get() = compilation.disambiguateName(PublicPackageJsonTask.NAME)

    internal val modules by lazy {
        NpmProjectModules(dir.getFile())
    }

    internal val nodeExecutable by lazy {
        nodeJs.executable.get()
    }

    @Deprecated("Internal KGP utility. Scheduled for removal in Kotlin 2.4.")
    fun useTool(
        exec: ExecSpec,
        tool: String,
        nodeArgs: List<String> = listOf(),
        args: List<String>,
    ) {
        exec.workingDir(dir)
        exec.executable(nodeExecutable)
        @Suppress("DEPRECATION")
        exec.args = nodeArgs + require(tool) + args
    }

    /**
     * Require [request] nodejs module and return canonical path to it's main js file.
     */
    @Deprecated("Internal KGP utility. Scheduled for removal in Kotlin 2.4.")
    fun require(request: String): String {
//        nodeJs.npmResolutionManager.requireAlreadyInstalled(project)
        return modules.require(request)
    }

    override fun toString() = "NpmProject(${name.get()})"

    companion object {
        const val PACKAGE_JSON = "package.json"
        const val NODE_MODULES = "node_modules"
        const val DIST_FOLDER = "kotlin"
    }
}
