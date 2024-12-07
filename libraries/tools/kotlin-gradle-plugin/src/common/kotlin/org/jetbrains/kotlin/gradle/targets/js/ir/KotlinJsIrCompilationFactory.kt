/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnostic
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinCompilationFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.DefaultKotlinCompilationFriendPathsResolver
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.DefaultKotlinCompilationPreConfigure
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory.*
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.plugin.mpp.sourcesJarTask
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory.KotlinCompilationImplFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory.KotlinJsCompilerOptionsFactory
import org.jetbrains.kotlin.gradle.targets.js.toCompilerTarget

class KotlinJsIrCompilationFactory internal constructor(
    override val target: KotlinJsIrTarget,
) : KotlinCompilationFactory<KotlinJsIrCompilation> {
    override val itemClass: Class<KotlinJsIrCompilation>
        get() = KotlinJsIrCompilation::class.java

    private val compilationImplFactory: KotlinCompilationImplFactory = KotlinCompilationImplFactory(
        compilerOptionsFactory = KotlinJsCompilerOptionsFactory,
        compilationFriendPathsResolver = DefaultKotlinCompilationFriendPathsResolver(
            friendArtifactResolver = { _ ->
                target.project.files()
            }
        ),
        compilationDependencyConfigurationsFactory = DefaultKotlinCompilationDependencyConfigurationsFactory.WithRuntime(
            withResourcesConfigurationExtending = { runtimeDependencyConfiguration, _ ->
                if (runtimeDependencyConfiguration == null) {
                    project.reportDiagnostic(
                        KotlinToolingDiagnostics.MissingRuntimeDependencyConfigurationForWasmTarget(target.name,)
                    )
                }
                runtimeDependencyConfiguration
            }
        ),
        preConfigureAction = DefaultKotlinCompilationPreConfigure + { compilation ->
            if (compilation.platformType == KotlinPlatformType.wasm && compilation.isMain()) {
                val artifactNameAppendix = (compilation.target as KotlinJsIrTarget).wasmDecamelizedDefaultNameOrNull()
                    ?: compilation.target.targetName.toLowerCaseAsciiOnly()
                sourcesJarTask(compilation, compilation.target.targetName, artifactNameAppendix)
            }
        },
    )

    override fun create(name: String): KotlinJsIrCompilation = target.project.objects.newInstance(
        itemClass, compilationImplFactory.create(target, name)
    ).also {
        it.wasmTarget = target.wasmTargetType?.toCompilerTarget()
    }
}
