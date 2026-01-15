/*
* Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
* Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
*/

package org.jetbrains.kotlin.gradle.targets.js.swc

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants.ES_2015
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.json
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.Mode
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.property
import org.jetbrains.kotlin.js.config.ModuleKind
import org.jetbrains.kotlin.platform.js.SwcConfig

/**
 * A Gradle task to generate .swcrc config for the SWC (Speedy Web Compiler) CLI.
 *
 * @see [Speedy Web Compiler](https://swc.rs/)
 */
@CacheableTask
internal abstract class GenerateSwcConfig : DefaultTask() {
    @get:Input
    abstract val esTarget: Property<String>

    @get:Input
    abstract val sourceMaps: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val moduleKind: Property<JsModuleKind>

    @get:Internal
    internal val moduleSystemToUse: Provider<ModuleKind> =
        moduleKind
            .orElse(esTarget.map { if (it == ES_2015) JsModuleKind.MODULE_ES else JsModuleKind.MODULE_UMD })
            .map { ModuleKind.fromType(it.kind) }

    @get:OutputFile
    abstract val configFile: RegularFileProperty

    @TaskAction
    fun writeConfig() {
        val config = SwcConfig.getConfigWhen(
            sourceMapEnabled = sourceMaps.get(),
            target = esTarget.get(),
            // To reduce the final code size, we always turn on this option
            includeExternalHelpers = true,
            moduleKind = moduleSystemToUse.get()
        )

        configFile.get().asFile.writeText(json(config))
    }

    companion object {
        fun register(
            compilation: KotlinJsIrCompilation,
            name: String,
            configuration: GenerateSwcConfig.() -> Unit = {},
        ): TaskProvider<GenerateSwcConfig> {
            val target = compilation.target
            val project = target.project

            val npmProjectDir = compilation.npmProject.dir
            val configFile = npmProjectDir.map { it.file(".swcrc") }

            return project.registerTask(name) {
                it.configFile.set(configFile)
                it.configuration()
            }
        }
    }
}
