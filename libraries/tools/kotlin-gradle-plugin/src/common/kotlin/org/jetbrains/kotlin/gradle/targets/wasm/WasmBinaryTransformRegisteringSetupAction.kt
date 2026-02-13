/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm

import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptionsHelper
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationSideEffect
import org.jetbrains.kotlin.gradle.targets.js.internal.LibraryFilterCachingService
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.internal.NoOpWasmBinaryTransform
import org.jetbrains.kotlin.gradle.targets.wasm.internal.WasmBinaryAttribute
import org.jetbrains.kotlin.gradle.targets.wasm.internal.WasmBinaryTransform
import org.jetbrains.kotlin.gradle.targets.wasm.internal.supportsPerKlibCompilation
import org.jetbrains.kotlin.gradle.utils.kotlinSessionsDir

@OptIn(ExperimentalWasmDsl::class)
internal val WasmBinaryTransformRegisteringSetupAction = KotlinCompilationSideEffect { compilation ->
    if (compilation !is KotlinJsIrCompilation) return@KotlinCompilationSideEffect
    val project = compilation.project
    if (!compilation.wasmTarget.supportsPerKlibCompilation()) return@KotlinCompilationSideEffect

    val propertiesProvider = project.kotlinPropertiesProvider
    if (!propertiesProvider.wasmPerModule) return@KotlinCompilationSideEffect

    WasmBinaryAttribute.setupTransform(project)

    compilation.binaries.all { binary ->
        val linkTaskProvider = binary.linkTask

        // sometimes there are jar files in classpath, not KLIB
        // we need to provide no-op transform for "jar" -> "klib"
        project.dependencies.registerTransform(
            NoOpWasmBinaryTransform::class.java
        ) { transform ->
            transform.from.attributes.attribute(
                WasmBinaryAttribute.attribute,
                WasmBinaryAttribute.WASM_BINARY_DEVELOPMENT
            )
            transform.to.attributes.attribute(
                WasmBinaryAttribute.attribute,
                WasmBinaryAttribute.KLIB_ATTRIBUTE_VALUE
            )
        }

        project.dependencies.registerTransform(
            WasmBinaryTransform::class.java,
        ) { transform ->
            transform.from.attributes.attribute(WasmBinaryAttribute.attribute, WasmBinaryAttribute.KLIB_ATTRIBUTE_VALUE)
            transform.from.attributes.attribute(WasmBinaryAttribute.compilationNameAttribute, WasmBinaryAttribute.KLIB_ATTRIBUTE_VALUE)
            transform.to.attributes.attribute(
                WasmBinaryAttribute.attribute,
                WasmBinaryAttribute.modeToAttribute(binary.mode)
            )
            transform.to.attributes.attribute(
                WasmBinaryAttribute.compilationNameAttribute,
                compilation.name
            )

            transform.parameters { parameters ->
                parameters.currentJvmJdkToolsJar.set(
                    linkTaskProvider.flatMap { it.defaultKotlinJavaToolchain }
                        .flatMap { it.currentJvmJdkToolsJar }
                )
                parameters.defaultCompilerClasspath.setFrom(linkTaskProvider.map { it.defaultCompilerClasspath })
                parameters.kotlinPluginVersion.set(
                    getKotlinPluginVersion(project.logger)
                )
                parameters.pathProvider.set(
                    linkTaskProvider.map { it.path }
                )
                parameters.projectRootFile.set(
                    project.projectDir
                )
                parameters.projectName.set(project.name)
                parameters.projectSessionsDir.set(project.kotlinSessionsDir)

                parameters.buildDir.set(project.layout.buildDirectory.asFile)

                parameters.compilerOptions.set(
                    linkTaskProvider.map {
                        val args = K2JSCompilerArguments()
                        KotlinCommonCompilerOptionsHelper.fillCompilerArguments(it.compilerOptions, args)
                        args
                    }
                )
                parameters.enhancedFreeCompilerArgs.set(linkTaskProvider.flatMap { it.enhancedFreeCompilerArgs })
                parameters.classpath.from(
                    compilation.configurations.runtimeDependencyConfiguration
                        ?.incoming
                        ?.artifactView {
                            it.componentFilter { id ->
                                id is ModuleComponentIdentifier
                            }
                        }
                        ?.files ?: error("JS or Wasm compilation should contain runtime configuration")
                )
                propertiesProvider.kotlinDaemonJvmArgs?.let { kotlinDaemonJvmArgs ->
                    parameters.kotlinDaemonJvmArguments.set(project.provider {
                        kotlinDaemonJvmArgs.split("\\s+".toRegex())
                    })
                }
                parameters.compilerExecutionStrategy.convention(propertiesProvider.kotlinCompilerExecutionStrategy)
                    .finalizeValueOnRead()
                parameters.useDaemonFallbackStrategy.convention(propertiesProvider.kotlinDaemonUseFallbackStrategy)
                    .finalizeValueOnRead()

                BinaryenPlugin.apply(project)
                parameters.binaryenExec.set(project.extensions.findByType(BinaryenEnvSpec::class.java)!!.executable)
                parameters.mode.set(binary.mode)
            }
        }
    }
}
