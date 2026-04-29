/*
* Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
* Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
*/

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationImpl
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.webTargetVariant
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.web.nodejs.BaseNodeJsRootExtension
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import java.io.File
import javax.inject.Inject

abstract class KotlinJsIrCompilation @Inject internal constructor(
    compilation: KotlinCompilationImpl,
) : KotlinJsCompilation(compilation) {


    internal abstract val npmToolingDependenciesDir: DirectoryProperty

    var wasmTarget: WasmTarget? = null
        internal set
}

internal fun KotlinJsIrCompilation.npmToolingDir(): Provider<Directory> {
    val npmToolingDir: Provider<File> = webTargetVariant(
        { npmProject.dir.map { it.asFile } },
        { (nodeJsRoot() as WasmNodeJsRootExtension).npmTooling.map { it.dir } },
    )

    return project.objects.directoryProperty().fileProvider(npmToolingDir)
}

internal fun KotlinJsIrCompilation.nodeJsRoot(): BaseNodeJsRootExtension {
    return webTargetVariant(
        { NodeJsRootPlugin.apply(project.rootProject) },
        { WasmNodeJsRootPlugin.apply(project.rootProject) },
    )
}
