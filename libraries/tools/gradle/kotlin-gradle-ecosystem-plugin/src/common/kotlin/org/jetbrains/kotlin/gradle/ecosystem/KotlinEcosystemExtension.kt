/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.ecosystem

import org.gradle.api.Action
import org.gradle.api.initialization.Settings
import org.jetbrains.kotlin.gradle.ExperimentalNodeJsToolchainDsl
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.toolchain.nodejs.NodeJsToolchainManagementDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools.WasmToolsToolchainManagementDsl
import org.jetbrains.kotlin.gradle.targets.wasm.wasmtools.toolchain.WasmToolsToolchainManagementDslImpl
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
 *
 * @since 2.5.0
 */
interface KotlinEcosystemExtension {

    /**
     * Configures toolchains provisioned for the whole build.
     *
     * @since 2.5.0
     */
    @ExperimentalNodeJsToolchainDsl
    fun toolchainManagement(configure: KotlinToolchainManagementDsl.() -> Unit)

    /**
     * [Action] based version of [toolchainManagement] above.
     *
     * @since 2.5.0
     */
    @ExperimentalNodeJsToolchainDsl
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
 *
 * @since 2.5.0
 */
interface KotlinToolchainManagementDsl {

    /**
     * Configures the Node.js toolchain service used by the whole build.
     *
     * At most one service may be selected inside the block: call only one of
     * [NodeJsToolchainManagementDsl.toolchainService] or [NodeJsToolchainManagementDsl.disable].
     * If the block is omitted, Node.js toolchain service will be disabled.
     *
     * Use the default download-based service with custom parameters:
     *  ```
     * // settings.gradle.kts
     * kotlin {
     *     toolchainManagement {
     *         nodeJs {
     *             toolchainService {
     *                 //default download-based Node.js toolchain service will be used
     *             }
     *         }
     *     }
     * }
     * ```
     * Default `https://nodejs.org/dist` Node.js URL will be used if non is specified.
     * Default installation directory `<user.home>/.kotlin/toolchains/nodejs` will be used if non is specified.
     *
     * ```
     * // settings.gradle.kts
     * kotlin {
     *     toolchainManagement {
     *         nodeJs {
     *             toolchainService {
     *             //all parameters could be set with custom values
     *                 downloadBaseUrl.set("https://my.mirror.example/nodejs/dist")
     *                 installationDir.set(File("/opt/toolchains/nodejs"))
     *             }
     *         }
     *     }
     * }
     * ```
     *
     * Use a Node.js executable that is already installed on the machine:
     * ```
     * // settings.gradle.kts
     * kotlin {
     *     toolchainManagement {
     *         nodeJs {
     *             toolchainService(PreInstalledNodeJsToolchainService::class) {
     *                 nodeJsExecutable.set("/usr/local/bin/node") //or a default `node` will be used if non is specified
     *             }
     *         }
     *     }
     * }
     * ```
     *
     * Disable Node.js provisioning entirely (tasks that need Node.js will fail):
     * ```
     * // settings.gradle.kts
     * kotlin {
     *     toolchainManagement {
     *         nodeJs {
     *             disable()
     *         }
     *     }
     * }
     * ```
     *
     * @since 2.5.0
     */
    @ExperimentalNodeJsToolchainDsl
    fun nodeJs(configure: NodeJsToolchainManagementDsl.() -> Unit)

    /**
     * [Action] based version of [nodeJs] above.
     *
     * @since 2.5.0
     */
    @ExperimentalNodeJsToolchainDsl
    fun nodeJs(configure: Action<NodeJsToolchainManagementDsl>) {
        nodeJs {
            configure.execute(this)
        }
    }

    /**
     * Configures the wasm-tools toolchain service used by the whole build.
     *
     * At most one service may be selected inside the block: call only one of
     * [WasmToolsToolchainManagementDsl.toolchainService] or [WasmToolsToolchainManagementDsl.disable].
     * If the block is omitted, the mode is taken from the `kotlin.wasm.wasmtools.toolchain` Gradle property,
     * which defaults to downloading wasm-tools. This block wins over that property.
     *
     * Use the default download-based service with custom parameters:
     * ```
     * // settings.gradle.kts
     * kotlin {
     *     toolchainManagement {
     *         wasmTools {
     *             toolchainService {
     *                 //default download-based wasm-tools toolchain service will be used
     *             }
     *         }
     *     }
     * }
     * ```
     * Default `https://github.com/bytecodealliance/wasm-tools/releases/download` wasm-tools URL will be used if non is specified.
     * Default installation directory `<user.home>/.kotlin/toolchains/wasm-tools` will be used if non is specified.
     *
     * ```
     * // settings.gradle.kts
     * kotlin {
     *     toolchainManagement {
     *         wasmTools {
     *             toolchainService {
     *             //all parameters could be set with custom values
     *                 downloadBaseUrl.set("https://my.mirror.example/wasm-tools")
     *                 installationDir.set(File("/opt/toolchains/wasm-tools"))
     *             }
     *         }
     *     }
     * }
     * ```
     *
     * Use a wasm-tools executable that is already installed on the machine:
     * ```
     * // settings.gradle.kts
     * kotlin {
     *     toolchainManagement {
     *         wasmTools {
     *             toolchainService(PreInstalledWasmToolsToolchainService::class) {
     *                 wasmToolsExecutable.set("/usr/local/bin/wasm-tools") //or a default `wasm-tools` will be used if non is specified
     *             }
     *         }
     *     }
     * }
     * ```
     *
     * Disable wasm-tools provisioning entirely (tasks that need wasm-tools will fail):
     * ```
     * // settings.gradle.kts
     * kotlin {
     *     toolchainManagement {
     *         wasmTools {
     *             disable()
     *         }
     *     }
     * }
     * ```
     *
     * @since 2.5.0
     */
    @ExperimentalWasmDsl
    fun wasmTools(configure: WasmToolsToolchainManagementDsl.() -> Unit)

    /**
     * [Action] based version of [wasmTools] above.
     *
     * @since 2.5.0
     */
    @ExperimentalWasmDsl
    fun wasmTools(configure: Action<WasmToolsToolchainManagementDsl>) {
        wasmTools {
            configure.execute(this)
        }
    }
}

// Open, as Gradle generates a decorated subclass when registering the extension.
internal open class KotlinEcosystemExtensionImpl(settings: Settings) : KotlinEcosystemExtension {

    @OptIn(InternalKotlinGradlePluginApi::class, ExperimentalWasmDsl::class)
    @ExperimentalNodeJsToolchainDsl
    private val toolchainManagementDsl = KotlinToolchainManagementDslImpl(
        NodeJsToolchainManagementDslImpl(settings.gradle),
        WasmToolsToolchainManagementDslImpl(settings.gradle),
    )

    @ExperimentalNodeJsToolchainDsl
    override fun toolchainManagement(configure: KotlinToolchainManagementDsl.() -> Unit) {
        toolchainManagementDsl.configure()
    }
}

internal class KotlinToolchainManagementDslImpl(
    private val nodeJsDsl: NodeJsToolchainManagementDsl,
    private val wasmToolsDsl: WasmToolsToolchainManagementDsl,
) : KotlinToolchainManagementDsl {

    @ExperimentalNodeJsToolchainDsl
    override fun nodeJs(configure: NodeJsToolchainManagementDsl.() -> Unit) {
        nodeJsDsl.configure()
    }

    @ExperimentalWasmDsl
    override fun wasmTools(configure: WasmToolsToolchainManagementDsl.() -> Unit) {
        wasmToolsDsl.configure()
    }
}
