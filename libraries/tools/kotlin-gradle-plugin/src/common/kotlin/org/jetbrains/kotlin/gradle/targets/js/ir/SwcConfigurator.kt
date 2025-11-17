/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Action
import org.gradle.api.file.DuplicatesStrategy
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.targets.js.swc.KotlinTranspileWithSwc
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.webTargetVariant
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.Mode
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.swc.JsPlatformType
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin.Companion.kotlinNodeJsRootExtension as wasmKotlinNodeJsRootExtension
import org.jetbrains.kotlin.gradle.utils.withType
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

internal class SwcConfigurator(private val subTarget: KotlinJsIrSubTarget) :
    SubTargetConfigurator<KotlinTranspileWithSwc, KotlinTranspileWithSwc> {
    private val project = subTarget.project
    private val propertiesProvider = PropertiesProvider(project)

    private val nodeJsRoot = subTarget.target.webTargetVariant(
        { project.rootProject.kotlinNodeJsRootExtension },
        { project.rootProject.wasmKotlinNodeJsRootExtension },
    )

    internal val isWasm: Boolean = subTarget.target.webTargetVariant(
        jsVariant = false,
        wasmVariant = true,
    )

    override fun setupBuild(compilation: KotlinJsIrCompilation) {
        if (isWasm || !propertiesProvider.delegateTranspilationToExternalTool) return

        if (subTarget is KotlinNodeJsIr) {
            compilation.binaries
                .withType<Executable>()
                .configureEach { configureSwcTask(it, compilation) }
        }

        compilation.binaries
            .withType<Library>()
            .configureEach { configureSwcTask(it, compilation) }

        // Add @swc/helpers dependency to minify the result output by swc
        compilation.defaultSourceSet.dependencies {
            with(nodeJsRoot.versions) {
                implementation(npm(swcHelpers.name, swcHelpers.version))
            }
        }
    }

    private fun configureSwcTask(binary: JsIrBinary, compilation: KotlinJsIrCompilation) {
        val mode = binary.mode
        val linkTask = binary.linkTask
        val linkSyncTask = binary.linkSyncTask

        val name = when (binary) {
            is Executable -> binary.executeTaskBaseName
            is Library -> binary.name
            else -> error("Unsupported binary type: ${binary::class.simpleName}")
        }

        val swcTask = subTarget.registerSubTargetTask<KotlinTranspileWithSwc>(
            subTarget.disambiguateCamelCased(name, SWC_TASK_NAME),
            listOf(compilation)
        ) { task ->
            val inputFilesDirectory = linkTask.flatMap { it.destinationDirectory }
            val outputDirectory = binary.distribution.distributionName.flatMap {
                project.layout.buildDirectory.dir("kotlin-swc/${compilation.target.name}/$it")
            }

            task.description = "transpile compiler output with Swc [${mode.name.toLowerCaseAsciiOnly()}]"

            task.versions.value(nodeJsRoot.versions).disallowChanges()
            task.inputFilesDirectory.value(inputFilesDirectory).disallowChanges()
            task.outputDirectory.value(outputDirectory).disallowChanges()
            task.npmToolingEnvDir.value(compilation.npmProject.dir).disallowChanges()

            task.mode.set(
                when (mode) {
                    KotlinJsBinaryMode.DEVELOPMENT -> Mode.DEVELOPMENT
                    KotlinJsBinaryMode.PRODUCTION -> Mode.PRODUCTION
                }
            )

            val compilerOptions = binary.linkTask.map(Kotlin2JsCompile::compilerOptions)

            task.config.apply {
                platformType.set(JsPlatformType.NODE)
                esTarget.set(compilerOptions.flatMap(KotlinJsCompilerOptions::target))
                moduleKind.set(compilerOptions.flatMap(KotlinJsCompilerOptions::moduleKind))
                sourceMaps.set(compilerOptions.flatMap(KotlinJsCompilerOptions::sourceMap))
            }

            task.dependsOn(linkTask)
        }

        linkSyncTask.configure { task ->
            task.from.from(swcTask.flatMap { it.outputDirectory })
            task.duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
    }

    override fun configureBuild(body: Action<KotlinTranspileWithSwc>) {
    }

    override fun setupRun(compilation: KotlinJsIrCompilation) {
    }

    override fun configureRun(body: Action<KotlinTranspileWithSwc>) {
    }

    internal companion object {
        internal const val SWC_TASK_NAME = "transpileWithSwc"
    }
}