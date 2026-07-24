/*
* Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
* Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
*/

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.file.Directory
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

open class KotlinJsIrCompilation @Inject internal constructor(
    compilation: KotlinCompilationImpl,
) : KotlinJsCompilation(compilation) {
    /**
     * The target for the compilation.
     *
     * If `null` this compilation is used for Kotlin/JS.
     */
    // This property is a `var` because @Inject can't inject `null` values and fails with "Null value provided in parameters".
    var wasmTarget: WasmTarget? = null
        internal set
}

/**
 * The NPM project directory used for executing operations for the current compilation.
 *
 * It is used to discover Node modules, installed in `node_modules` directory, for the current compilation.
 * See [org.jetbrains.kotlin.gradle.targets.js.npm.NpmProjectModules].
 *
 * This function can only be called for projects that have JS or WasmJS targets.
 *
 * ### JS vs WasmJS
 *
 * Note: the name is misleading for JS, which does not have a separate 'npm tooling directory'.
 * For JS targets KGP's tooling dependencies and user-based dependencies are combined.
 *
 * Be aware the directory is different for JS and WasmJS.
 * - For WasmJS, the directory is in the user's home directory.
 *   It is shared between multiple Gradle builds.
 *
 * - For JS, the directory is a nested workspace within the root npm build directory.
 *   The directory is not shared with other compilations, but the parent directory
 *   is shared with all compilations in a single Gradle build.
 *
 * ### Custom location
 *
 * It is possible for users to override the directory,
 * allowing them to manually control KGP's npm tooling dependencies for WasmJS targets.
 * However, this is not widely used. It's not documented and is challenging to set up.
 */
internal fun KotlinJsIrCompilation.npmToolingDir(): Provider<Directory> {
    val npmToolingDir: Provider<File> = webTargetVariant(
        jsVariant = { npmProject.dir.map { it.asFile } },
        wasmVariant = { (nodeJsRoot() as WasmNodeJsRootExtension).npmTooling.map { it.dir } },
    )

    return project.objects.directoryProperty().fileProvider(npmToolingDir)
}

internal fun KotlinJsIrCompilation.nodeJsRoot(): BaseNodeJsRootExtension {
    return webTargetVariant(
        { NodeJsRootPlugin.apply(project.rootProject) },
        { WasmNodeJsRootPlugin.apply(project.rootProject) },
    )
}

/**
 * Returns `true` if the npm tooling directory for this compilation is in a global shared location between multiple Gradle builds.
 *
 * Currently, only WasmJS targets use a shared location KT-75714.
 */
internal fun KotlinJsIrCompilation.hasSharedNpmToolingDir(): Boolean {
    return when (wasmTarget) {
        /* JS */ null -> false
        WasmTarget.WASI -> false // WASI does not have npm dependencies
        WasmTarget.JS -> true
    }
}
