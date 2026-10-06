/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.dsl.toolchain.nodejs

import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.ExperimentalNodeJsToolchainDsl

/**
 * A [NodeJsToolchainService] that uses a pre-installed Node.js instead of downloading one.
 *
 * The Node.js installation is not provisioned;
 * it must be available on the machine that runs the build.
 * Configure it via [Parameters].
 */
@ExperimentalNodeJsToolchainDsl
interface PreInstalledNodeJsToolchainService : NodeJsToolchainService<PreInstalledNodeJsToolchainService.Parameters> {

    /**
     * Parameters of [PreInstalledNodeJsToolchainService], specifying which already installed Node.js to run.
     * If [nodeJsExecutable] is left unset, `node` from the `PATH` is used.
     */
    abstract class Parameters : NodeJsToolchainService.Parameters {
        /**
         * The command used to run the pre-installed Node.js, for example `node` or a full path.
         */
        abstract val nodeJsExecutable: Property<String>
    }
}
