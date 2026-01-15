/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Action
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants.ES_2015
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.targets.js.swc.SwcExec
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.webTargetVariant
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.Mode
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.swc.GenerateSwcConfig
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin.Companion.kotlinNodeJsRootExtension as wasmKotlinNodeJsRootExtension
import org.jetbrains.kotlin.gradle.utils.withType
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

internal class SwcConfigurator(private val subTarget: KotlinJsIrSubTarget) :
    SubTargetConfigurator<SwcExec, SwcExec> {
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

    override fun setupBuild(compilation: KotlinJsIrCompilation) =
        setupCompilation(compilation)

    override fun setupTest(compilation: KotlinJsIrCompilation) =
        setupCompilation(compilation)

    private fun setupCompilation(compilation: KotlinJsIrCompilation) {
        if (isWasm || !propertiesProvider.delegateTranspilationToExternalTool) return

        compilation.binaries
            .withType<Executable>()
            .configureEach { configureSwcTask(it, compilation) }

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
        val binaryMode = binary.mode
        val linkTask = binary.linkTask
        val linkSyncTask = binary.linkSyncTask

        val name = when (binary) {
            is Executable -> binary.executeTaskBaseName
            is Library -> binary.name
            else -> error("Unsupported binary type: ${binary::class.simpleName}")
        }

        val generateConfigTask = GenerateSwcConfig.register(
            compilation,
            subTarget.disambiguateCamelCased(name, SWC_CONFIG_TASK_NAME),
        ) {
            val compilerOptions = binary.linkTask.map(Kotlin2JsCompile::compilerOptions)

            esTarget.set(compilerOptions.flatMap(KotlinJsCompilerOptions::target))
            moduleKind.set(compilerOptions.flatMap(KotlinJsCompilerOptions::moduleKind))
            sourceMaps.set(compilerOptions.flatMap(KotlinJsCompilerOptions::sourceMap))
        }

        val swcTask = SwcExec.register(
            compilation,
            subTarget.disambiguateCamelCased(name, SWC_TASK_NAME),
            generateConfigTask,
        ) {
            val outputGranularity = linkTask.map(KotlinJsIrLink::outputGranularity)
            val sourceDirectory = linkTask.flatMap(KotlinJsIrLink::destinationDirectory)
            val destinationDirectory = binary.distribution.distributionName.flatMap {
                project.layout.buildDirectory.dir("kotlin-swc/${compilation.target.name}/$it")
            }

            description = "transpile compiler output with Swc [${binaryMode.name.toLowerCaseAsciiOnly()}]"
            inputDirectory.set(sourceDirectory)
            outputDirectory.set(destinationDirectory)
            granularity.set(outputGranularity)

            mode.set(
                when (binaryMode) {
                    KotlinJsBinaryMode.DEVELOPMENT -> Mode.DEVELOPMENT
                    KotlinJsBinaryMode.PRODUCTION -> Mode.PRODUCTION
                }
            )
        }

        linkSyncTask.configure { task ->
            val swcOrLinkTaskOutputs = linkTask
                .flatMap { it.compilerOptions.target }
                // We shouldn't run SWC if the configured target is the latest compiler supported target
                .flatMap {
                    if (it == JsPlatforms.latestSupportedTarget) {
                        project.providers.provider { null }
                    } else swcTask.flatMap(SwcExec::outputDirectory)
                }
                .orElse(binary.defaultLinkSyncTaskInput)

            // TODO(KT-83097): think to move this setup to JsIrBinary
            // Override the 'from' of the linkSync task to consume JS files post-processed by SWC, instead of the files from link task
            task.from.setFrom(swcOrLinkTaskOutputs, binary.linkSyncTaskRegisteredResources)
        }
    }

    override fun configureBuild(body: Action<SwcExec>) {
    }

    override fun setupRun(compilation: KotlinJsIrCompilation) {
    }

    override fun configureRun(body: Action<SwcExec>) {
    }

    internal companion object {
        internal const val SWC_TASK_NAME = "transpileWithSwc"
        internal const val SWC_CONFIG_TASK_NAME = "generateSwcConfig"
    }
}
