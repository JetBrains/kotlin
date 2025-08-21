/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.model.ObjectFactory
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmApiExecution

class Yarn internal constructor(
    execOps: ExecOperations,
    objects: ObjectFactory,
) : NpmApiExecution<YarnEnvironment> by YarnWorkspaces(
    execOps,
    objects,
) {
    /**
     * Manually creating new instances of this class is deprecated.
     *
     * An instance of [Yarn] can be found from the extensions
     * [WasmYarnRootExtension][org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootExtension]
     * and
     * [YarnRootExtension][org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension].
     *
     * @see org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootExtension.packageManager
     * @see org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension.packageManager
     */
    @Deprecated(
        message = "Manually creating instances of this class is deprecated. " +
                "An instance can be obtained via WasmYarnRootExtension or YarnRootExtension. " +
                "Scheduled for removal in Kotlin 2.4.",
        level = DeprecationLevel.ERROR,
    )
    @Suppress("UNREACHABLE_CODE", "unused")
    constructor() : this(
        execOps = error("Cannot create instance of Npm. Constructor is deprecated."),
        objects = error("Cannot create instance of Npm. Constructor is deprecated."),
    )
}