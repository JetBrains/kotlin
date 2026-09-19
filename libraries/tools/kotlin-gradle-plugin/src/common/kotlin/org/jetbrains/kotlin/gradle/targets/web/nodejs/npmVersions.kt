/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.nodejs

import org.jetbrains.kotlin.gradle.targets.js.NpmVersions
import org.jetbrains.kotlin.gradle.targets.js.internal.jsToolingProject
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.webTargetVariant
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.web.npm.isolated.isIsolatedNpmResolutionEnabled
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin.Companion.kotlinNodeJsRootExtension as wasmKotlinNodeJsRootExtension

/**
 * Versions of the npm packages used by the Kotlin Gradle Plugin tooling.
 *
 * The only place where the storage of [NpmVersions] is decided:
 * the legacy pipeline keeps using the (user-configurable) instance of the root project's
 * `nodeJs` extension, while the Isolated Projects compatible pipeline uses the project-local
 * instance of [BaseNodeJsEnvSpec].
 */
internal val KotlinJsIrCompilation.npmVersions: NpmVersions
    get() = if (project.isIsolatedNpmResolutionEnabled) {
        nodeJsEnvSpec.versions
    } else {
        webTargetVariant(
            jsVariant = { project.jsToolingProject().kotlinNodeJsRootExtension.versions },
            wasmVariant = { project.jsToolingProject().wasmKotlinNodeJsRootExtension.versions },
        )
    }

/**
 * @see npmVersions
 */
internal val KotlinJsIrTarget.npmVersions: NpmVersions
    get() = if (project.isIsolatedNpmResolutionEnabled) {
        webTargetVariant(
            jsVariant = { NodeJsPlugin.apply(project).versions },
            wasmVariant = { WasmNodeJsPlugin.apply(project).versions },
        )
    } else {
        webTargetVariant(
            jsVariant = { project.jsToolingProject().kotlinNodeJsRootExtension.versions },
            wasmVariant = { project.jsToolingProject().wasmKotlinNodeJsRootExtension.versions },
        )
    }
