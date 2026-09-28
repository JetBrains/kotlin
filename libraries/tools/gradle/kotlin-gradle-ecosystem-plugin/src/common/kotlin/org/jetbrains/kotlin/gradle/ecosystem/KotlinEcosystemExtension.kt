/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.ecosystem

import org.gradle.api.Action
import org.gradle.api.initialization.Settings
import org.jetbrains.kotlin.gradle.ExperimentalNodeJsToolchainDsl
import org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain.NodeJsToolchainManagementDsl
import org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain.NodeJsToolchainManagementDslImpl

/**
 * Entry point of the `org.jetbrains.kotlin.ecosystem` settings plugin DSL, exposed as `kotlin { ... }`
 * in `settings.gradle.kts`.
 *
 * ```
 * kotlin {
 *     toolchainManagement {
 *         nodeJs {
 *             toolchainService { ... }
 *         }
 *     }
 * }
 * ```
 */
interface KotlinEcosystemExtension {

    /**
     * Configures toolchains provisioned for the whole build.
     */
    fun toolchainManagement(configure: KotlinToolchainManagementDsl.() -> Unit)

    /**
     * [Action] based version of [toolchainManagement] above.
     */
    fun toolchainManagement(configure: Action<KotlinToolchainManagementDsl>) {
        toolchainManagement {
            configure.execute(this)
        }
    }
}

/**
 * `toolchainManagement { ... }` block of [KotlinEcosystemExtension].
 *
 * **Note:** This interface is not intended for implementation by build script or plugin authors.
 */
interface KotlinToolchainManagementDsl {

    /**
     * Selects and configures the Node.js toolchain service, see [NodeJsToolchainManagementDsl].
     */
    @ExperimentalNodeJsToolchainDsl
    fun nodeJs(configure: NodeJsToolchainManagementDsl.() -> Unit)

    /**
     * [Action] based version of [nodeJs] above.
     */
    @ExperimentalNodeJsToolchainDsl
    fun nodeJs(configure: Action<NodeJsToolchainManagementDsl>) {
        nodeJs {
            configure.execute(this)
        }
    }
}

// Open, as Gradle generates a decorated subclass when registering the extension.
internal open class KotlinEcosystemExtensionImpl(settings: Settings) : KotlinEcosystemExtension {

    @ExperimentalNodeJsToolchainDsl
    private val toolchainManagementDsl = KotlinToolchainManagementDslImpl(NodeJsToolchainManagementDslImpl(settings.gradle))

    @ExperimentalNodeJsToolchainDsl
    override fun toolchainManagement(configure: KotlinToolchainManagementDsl.() -> Unit) {
        toolchainManagementDsl.configure()
    }
}

internal class KotlinToolchainManagementDslImpl(
    private val nodeJsDsl: NodeJsToolchainManagementDsl,
) : KotlinToolchainManagementDsl {

    @ExperimentalNodeJsToolchainDsl
    override fun nodeJs(configure: NodeJsToolchainManagementDsl.() -> Unit) {
        nodeJsDsl.configure()
    }
}
