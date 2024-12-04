/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.resolver

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.NpmVersions
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.nodejs.TasksRequirements
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinRootNpmResolution
import java.io.File
import java.io.Serializable

class KotlinRootNpmResolver internal constructor(
    val rootProjectName: String,
    val rootProjectVersion: String,
    val tasksRequirements: TasksRequirements,
    val versions: NpmVersions,
    val projectPackagesDir: Provider<Directory>,
    val rootProjectDir: File,
) : Serializable {

    internal var resolution: KotlinRootNpmResolution? = null

    val projectResolvers: MutableMap<String, KotlinProjectNpmResolver> = mutableMapOf()

    fun alreadyResolvedMessage(action: String) = "Cannot $action. NodeJS projects already resolved."

    fun addProject(target: Project) {
        synchronized(projectResolvers) {
            check(resolution == null) { alreadyResolvedMessage("add new project: $target") }
            val kotlinProjectNpmResolver = KotlinProjectNpmResolver(target, this)
            projectResolvers[target.path] = kotlinProjectNpmResolver
        }
    }

    internal operator fun get(projectPath: String) =
        projectResolvers[projectPath] ?: error("$projectPath is not configured for JS usage")

    val compilations: Collection<KotlinJsIrCompilation>
        get() = projectResolvers.values.flatMap { it.compilationResolvers.map { it.compilation } }

    internal fun findDependentResolver(src: Project, target: Project): List<KotlinCompilationNpmResolver>? {
        // todo: proper finding using KotlinTargetComponent.findUsageContext
        val targetResolver = this[target.path]
        val mainCompilations = targetResolver.compilationResolvers.filter { it.compilation.isMain() }

        if (mainCompilations.isEmpty()) return null

        //TODO[Ilya Goncharov, Igor Iakovlev] Hack for Mixed mode of legacy and IR tooling and Wasm
        var containsWasmJs = false
        var containsWasmWasi = false
        var containsIrJs = false
        val errorMessage = "Cannot resolve project dependency $src -> $target." +
                "Dependency to project with multiple js/wasm compilations is not supported yet."

        // legacy + ir + wasmJs + wasmWasi
        val maxMainCompilationsCount = 4

        check(mainCompilations.size <= maxMainCompilationsCount) { errorMessage }
        for (npmResolver in mainCompilations) {
            val compilation = npmResolver.compilation
            if (compilation.platformType == KotlinPlatformType.wasm) {
                val jsTarget = compilation.target as KotlinJsIrTarget
                if (jsTarget.wasmTargetType == KotlinWasmTargetType.JS) {
                    check(!containsWasmJs) { errorMessage }
                    containsWasmJs = true
                }
                if (jsTarget.wasmTargetType == KotlinWasmTargetType.WASI) {
                    check(!containsWasmWasi) { errorMessage }
                    containsWasmWasi = true
                }
            } else {
                check(!containsIrJs) { errorMessage }
                containsIrJs = true
            }
        }
        check(containsWasmJs || containsWasmWasi || containsIrJs) { errorMessage }

        return mainCompilations
    }

    internal fun close(): KotlinRootNpmResolution {
        return resolution ?: KotlinRootNpmResolution(
            projectResolvers
                .map { (key, value) -> key to value.close() }
                .toMap(),
            rootProjectName,
            rootProjectVersion
        )
    }
}

const val PACKAGE_JSON_UMBRELLA_TASK_NAME = "packageJsonUmbrella"