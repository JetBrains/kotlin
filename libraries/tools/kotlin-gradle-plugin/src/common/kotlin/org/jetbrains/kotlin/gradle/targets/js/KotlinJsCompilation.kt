/*
* Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
* Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
*/

@file:Suppress("PackageDirectoryMismatch", "DEPRECATION", "TYPEALIAS_EXPANSION_DEPRECATION", "TYPEALIAS_EXPANSION_DEPRECATION_ERROR")

// Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.DeprecatedHasCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinAnyOptionsDeprecated
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationImpl
import org.jetbrains.kotlin.gradle.targets.js.ir.JsBinary
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsBinaryContainer
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJson
import org.jetbrains.kotlin.gradle.targets.js.targetVersion
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.js.config.EcmaVersion
import org.jetbrains.kotlin.js.config.supportsEsModules
import javax.inject.Inject

open class KotlinJsCompilation @Inject internal constructor(
    compilation: KotlinCompilationImpl,
) : DeprecatedAbstractKotlinCompilationToRunnableFiles<KotlinAnyOptionsDeprecated>(compilation),
    HasBinaries<KotlinJsBinaryContainer> {

    override val target: KotlinJsIrTarget
        get() = super.target as KotlinJsIrTarget

    @Deprecated(
        "To configure compilation compiler options use 'compileTaskProvider':\ncompilation.compileTaskProvider.configure{\n" +
                "    compilerOptions {}\n}"
    )
    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    final override val compilerOptions: DeprecatedHasCompilerOptions<KotlinJsCompilerOptions>
        get() = compilation.compilerOptions as DeprecatedHasCompilerOptions<KotlinJsCompilerOptions>

    override val binaries: KotlinJsBinaryContainer =
        compilation.target.project.objects.newInstance(
            KotlinJsBinaryContainer::class.java,
            compilation.target,
            compilation.target.project.objects.domainObjectSet(JsBinary::class.java)
        )

    val outputModuleName: Provider<String> = target.outputModuleName
        .map { targetModuleName ->
            buildNpmProjectName(targetModuleName, compilationName)
        }

    /**
     * The JS module kind of this compilation, resolved from its compiler options, including the free compiler arguments.
     */
    internal val jsModuleKind: Provider<JsModuleKind>
        get() = compilerOptions.options.effectiveModuleKind

    /**
     * The JS target of this compilation, resolved from its compiler options, including the free compiler arguments.
     */
    internal val jsTarget: Provider<String>
        get() = compilerOptions.options.effectiveTarget

    override val processResourcesTaskName: String
        get() = disambiguateName("processResources")

    val npmAggregatedConfigurationName
        get() = compilation.disambiguateName("npmAggregated")

    val publicPackageJsonConfigurationName
        get() = compilation.disambiguateName("publicPackageJsonConfiguration")

    override fun getAttributes(): AttributeContainer {
        return compilation.attributes
    }

    @Suppress("UNCHECKED_CAST")
    override val compileTaskProvider: TaskProvider<Kotlin2JsCompile>
        get() = compilation.compileTaskProvider as TaskProvider<Kotlin2JsCompile>

    internal val packageJsonHandlers = mutableListOf<Action<PackageJson>>()

    fun packageJson(handler: Action<PackageJson>) {
        packageJsonHandlers.add(handler)
    }

    fun packageJson(handler: Closure<*>) {
        packageJson {
            project.configure(this, handler)
        }
    }

    private companion object {
        private fun buildNpmProjectName(targetPart: String, compilationName: String): String {
            val filteredCompilationName = if (compilationName != KotlinCompilation.MAIN_COMPILATION_NAME) {
                compilationName
            } else null

            return listOfNotNull(
                targetPart,
                filteredCompilationName
            )
                .joinToString("-")
        }
    }
}

val KotlinJsCompilation.fileExtension: Provider<String>
    get() {
        val isWasm = platformType == KotlinPlatformType.wasm
        return compilerOptions.options.moduleKind
            .orElse(
                compilerOptions.options.targetVersion.map {
                    if (it.supportsEsModules) JsModuleKind.MODULE_ES else JsModuleKind.MODULE_UMD
                }
            )
            .map { moduleKind ->
                if (isWasm || moduleKind == JsModuleKind.MODULE_ES) {
                    JsFileExtensions.MODULE
                } else {
                    JsFileExtensions.REGULAR
                }
            }
    }

internal object JsFileExtensions {
    const val REGULAR = "js"
    const val MODULE = "mjs"
}

/**
 * The JS `target` the compiler is invoked with.
 *
 * Free compiler arguments are passed to the compiler after the dedicated options, so they take precedence.
 * (The compiler keeps the last value of a repeated argument.)
 */
internal val KotlinJsCompilerOptions.effectiveTarget: Provider<String>
    get() = parsedFreeCompilerArgs
        .map<String> { args -> args.target }
        .orElse(target)

/**
 * The module kind the compiler generates:
 * - the explicit one (`-module-kind` from [KotlinJsCompilerOptions.freeCompilerArgs], otherwise [KotlinJsCompilerOptions.moduleKind]),
 * - or the one implied by [effectiveTarget] the same way the compiler does it: ES modules for an ES2015+ target and UMD otherwise.
 */
internal val KotlinJsCompilerOptions.effectiveModuleKind: Provider<JsModuleKind>
    get() = parsedFreeCompilerArgs
        .map<JsModuleKind> { args -> JsModuleKind.entries.firstOrNull { it.kind == args.moduleKind } }
        .orElse(moduleKind)
        .orElse(
            effectiveTarget.map { effectiveTarget ->
                if (EcmaVersion.fromName(effectiveTarget).supportsEsModules) JsModuleKind.MODULE_ES else JsModuleKind.MODULE_UMD
            }
        )

private val KotlinJsCompilerOptions.parsedFreeCompilerArgs: Provider<K2JSCompilerArguments>
    get() = freeCompilerArgs.map { parseCommandLineArguments<K2JSCompilerArguments>(it) }
