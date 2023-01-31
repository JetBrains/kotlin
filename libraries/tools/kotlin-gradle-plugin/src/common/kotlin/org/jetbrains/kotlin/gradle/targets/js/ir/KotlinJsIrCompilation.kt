/*
* Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
* Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
*/

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationImpl
import org.jetbrains.kotlin.gradle.tasks.configuration.BaseKotlinCompileConfig
import org.jetbrains.kotlin.gradle.tasks.configuration.Kotlin2JsCompileConfig
import javax.inject.Inject

open class KotlinJsIrCompilation @Inject internal constructor(compilation: KotlinCompilationImpl) : KotlinJsCompilation(compilation) {
    val compileDirectoryArtifactView = compilation.configurations
        .compileDependencyConfiguration
        .incoming
        .artifactView { artifactView ->
            artifactView.attributes.attribute(
                BaseKotlinCompileConfig.ARTIFACT_TYPE_ATTRIBUTE,
                Kotlin2JsCompileConfig.UNPACKED_KLIB_ARTIFACT_TYPE
            )
        }

    val compileDirectoryFiles
        get() = compileDirectoryArtifactView.files

    val runtimeDirectoryArtifactView = compilation.configurations
        .runtimeDependencyConfiguration!!
        .incoming
        .artifactView { artifactView ->
            artifactView.attributes.attribute(
                BaseKotlinCompileConfig.ARTIFACT_TYPE_ATTRIBUTE,
                Kotlin2JsCompileConfig.UNPACKED_KLIB_ARTIFACT_TYPE
            )
        }

    val runtimeDirectoryFiles
        get() = runtimeDirectoryArtifactView.files
}