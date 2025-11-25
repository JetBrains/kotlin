/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Action
import org.gradle.api.file.Directory
import org.gradle.api.file.DuplicatesStrategy
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants.ES_2015
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.targets.js.swc.SwcExec
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

    override fun setupBuild(compilation: KotlinJsIrCompilation) {
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

        val swcTask = SwcExec.register(
            compilation,
            subTarget.disambiguateCamelCased(name, SWC_TASK_NAME),
        ) {
            val sourceDirectory = linkTask.flatMap { it.destinationDirectory }
            val destinationDirectory = binary.distribution.distributionName.flatMap {
                project.layout.buildDirectory.dir("kotlin-swc/${compilation.target.name}/$it")
            }

            description = "transpile compiler output with Swc [${binaryMode.name.toLowerCaseAsciiOnly()}]"
            inputFilesDirectory.value(sourceDirectory).disallowChanges()
            outputDirectory.value(destinationDirectory).disallowChanges()
            npmToolingEnvDir.value(compilation.npmProject.dir).disallowChanges()

            mode.set(
                when (binaryMode) {
                    KotlinJsBinaryMode.DEVELOPMENT -> Mode.DEVELOPMENT
                    KotlinJsBinaryMode.PRODUCTION -> Mode.PRODUCTION
                }
            )

            val compilerOptions = binary.linkTask.map(Kotlin2JsCompile::compilerOptions)

            config.apply {
                platformType.set(JsPlatformType.NODE)
                esTarget.set(compilerOptions.flatMap(KotlinJsCompilerOptions::target))
                moduleKind.set(compilerOptions.flatMap(KotlinJsCompilerOptions::moduleKind))
                sourceMaps.set(compilerOptions.flatMap(KotlinJsCompilerOptions::sourceMap))
            }

            dependsOn(linkTask)
        }

        linkSyncTask.configure { task ->
            val swcOutput = linkTask
                .flatMap { it.compilerOptions.target }
                // We shouldn't run SWC if the configured target is the latest compiler supported target
                .filter { it != ES_2015 }
                .flatMap<Any> { swcTask.flatMap(SwcExec::outputDirectory) }
                .orElse(emptyArray<Directory>())

            task.from.from(swcOutput)
            task.duplicatesStrategy = DuplicatesStrategy.INCLUDE
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
    }
}