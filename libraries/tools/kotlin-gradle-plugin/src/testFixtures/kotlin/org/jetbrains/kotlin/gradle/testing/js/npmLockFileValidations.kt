/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testing.js

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.LockFileMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmExtension
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.npm.WasmNpmExtension
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootExtension
import org.jetbrains.kotlin.gradle.targets.web.npm.ObsoleteNpmDependencyModel

/**
 * Disable lock file validations for both JS and Wasm.
 */
@ObsoleteNpmDependencyModel
fun Project.disableNpmLockFileValidations() {
    plugins.withType(YarnPlugin::class.java) { _ ->
        extensions.getByType(YarnRootExtension::class.java).apply {
            yarnLockMismatchReportProperty.set(YarnLockMismatchReport.NONE)
            yarnLockAutoReplaceProperty.set(true)
        }
    }
    plugins.withType(WasmYarnPlugin::class.java) { _ ->
        extensions.getByType(WasmYarnRootExtension::class.java).apply {
            yarnLockMismatchReportProperty.set(YarnLockMismatchReport.NONE)
            yarnLockAutoReplaceProperty.set(true)
        }
    }
    plugins.withType(NodeJsRootPlugin::class.java) { _ ->
        extensions.getByType(NpmExtension::class.java).apply {
            packageLockMismatchReport.set(LockFileMismatchReport.NONE)
            packageLockAutoReplace.set(true)
        }
    }
    plugins.withType(WasmNodeJsRootPlugin::class.java) { _ ->
        extensions.getByType(WasmNpmExtension::class.java).apply {
            packageLockMismatchReport.set(LockFileMismatchReport.NONE)
            packageLockAutoReplace.set(true)
        }
    }
}
