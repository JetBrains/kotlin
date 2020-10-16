/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject.Companion.PACKAGE_JSON
import org.jetbrains.kotlin.gradle.utils.disableTaskOnConfigurationCacheBuild
import org.jetbrains.kotlin.gradle.utils.property
import java.io.File
import javax.inject.Inject

open class PublicPackageJsonTask
@Inject
constructor(
    @Transient
    private val compilation: KotlinJsCompilation
) : DefaultTask() {
    private val npmProject = compilation.npmProject
    private val nodeJs = npmProject.nodeJs

    private val compilationResolution
        get() = nodeJs.npmResolutionManager.requireInstalled()[project][npmProject.compilation]

    init {
        // TODO: temporary workaround for configuration cache enabled builds
        disableTaskOnConfigurationCacheBuild { nodeJs.npmResolutionManager.toString() }
    }

    @get:Input
    val packageJsonCustomFields: Map<String, Any?>
        get() = PackageJson(fakePackageJsonValue, fakePackageJsonValue)
            .apply {
                compilation.packageJsonHandlers.forEach { it() }
            }.customFields

    @get:Nested
    internal val externalDependencies: Collection<NpmDependencyDeclaration>
        get() = compilationResolution.externalNpmDependencies
            .map {
                NpmDependencyDeclaration(
                    scope = it.scope,
                    name = it.name,
                    version = it.version,
                    generateExternals = it.generateExternals
                )
            }

    private val publicPackageJsonTaskName = npmProject.publicPackageJsonTaskName

    @get:OutputFile
    var packageJsonFile: File by property {
        project.buildDir
            .resolve("tmp")
            .resolve(publicPackageJsonTaskName)
            .resolve(PACKAGE_JSON)
    }

    @TaskAction
    fun resolve() {
        val compilation = npmProject.compilation

//        packageJson(npmProject, realExternalDependencies).let { packageJson ->
//            packageJson.main = "${npmProject.name}.js"
//
//            if (compilation is KotlinJsIrCompilation) {
//                packageJson.types = "${npmProject.name}.d.ts"
//            }
//
//            packageJson.apply {
//                listOf(
//                    dependencies,
//                    devDependencies,
//                    peerDependencies,
//                    optionalDependencies
//                ).forEach { it.processDependencies() }
//            }
//
//            packageJson.saveTo(this@PublicPackageJsonTask.packageJsonFile)
//        }
    }

    private fun MutableMap<String, String>.processDependencies() {
        filter { (_, version) ->
            version.isFileVersion()
        }.forEach { (key, _) ->
            remove(key)
        }
    }

    companion object {
        const val NAME = "publicPackageJson"
    }
}