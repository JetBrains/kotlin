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
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject.Companion.PACKAGE_JSON
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

    private val compilationName = compilation.disambiguatedName
    private val projectPath = project.path

    private val compilationResolution
        get() = nodeJs.npmResolutionManager.requireInstalled(
                services,
                logger
            )[projectPath][compilationName]


    // TODO: is map contains only serializable values?
    @get:Input
    val packageJsonCustomFields: Map<String, Any?> by lazy {
        PackageJson(fakePackageJsonValue, fakePackageJsonValue)
            .apply {
                compilation.packageJsonHandlers.forEach { it() }
            }.customFields
    }

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

    private val publicPackageJsonTaskName by lazy {
        npmProject.publicPackageJsonTaskName
    }

    private val defaultPackageJsonFile by lazy {
        project.buildDir
            .resolve("tmp")
            .resolve(publicPackageJsonTaskName)
            .resolve(PACKAGE_JSON)
    }

    @get:OutputFile
    var packageJsonFile: File by property { defaultPackageJsonFile }

    private val isJrIrCompilation = compilation is KotlinJsIrCompilation
    private val projectVersion = project.version.toString()

    @TaskAction
    fun resolve() {
        packageJson(npmProject.name, projectVersion, npmProject.main, externalDependencies).let { packageJson ->
            packageJson.main = "${npmProject.name}.js"

            if (isJrIrCompilation) {
                packageJson.types = "${npmProject.name}.d.ts"
            }

            packageJson.apply {
                listOf(
                    dependencies,
                    devDependencies,
                    peerDependencies,
                    optionalDependencies
                ).forEach { it.processDependencies() }
            }

            packageJson.saveTo(this@PublicPackageJsonTask.packageJsonFile)
        }
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