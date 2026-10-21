/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.ecosystem

import org.gradle.api.Action
import org.gradle.api.initialization.Settings
import org.jetbrains.kotlin.gradle.ExperimentalKotlinToolchainDsl
import org.jetbrains.kotlin.gradle.ExperimentalNodeJsToolchainDsl
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.toolchain.ToolchainManagementDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.binaryen.BinaryenToolchainManagementDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.d8.D8ToolchainManagementDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.nodejs.NodeJsToolchainManagementDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.swc.SwcToolchainManagementDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtime.WasmtimeToolchainManagementDsl
import org.jetbrains.kotlin.gradle.targets.js.swc.toolchain.SwcToolchainManagementDslImpl
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.toolchain.BinaryenToolchainManagementDslImpl
import org.jetbrains.kotlin.gradle.targets.wasm.d8.toolchain.D8ToolchainManagementDslImpl
import org.jetbrains.kotlin.gradle.targets.wasm.wasmtime.toolchain.WasmtimeToolchainManagementDslImpl
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
     * Configures the D8 toolchain service used by the whole build.
     *
     * D8 runs Kotlin/Wasm applications and tests. At most one service may be selected inside the block:
     * call only one of [ToolchainManagementDsl.toolchainService] or [ToolchainManagementDsl.disable].
     * If the block is omitted, the D8 toolchain service is disabled, and D8 is provisioned per project
     * by the D8 environment spec and the D8 setup task.
     *
     * ```
     * // settings.gradle.kts
     * kotlin {
     *     toolchainManagement {
     *         d8 {
     *             toolchainService {
     *                 installationDir.set(File("/opt/toolchains/d8"))
     *             }
     *         }
     *     }
     * }
     * ```
     *
     * @since 2.5.20
     */
    @ExperimentalKotlinToolchainDsl
    fun d8(configure: D8ToolchainManagementDsl.() -> Unit)

    /**
     * [Action] based version of [d8] above.
     *
     * @since 2.5.20
     */
    @ExperimentalKotlinToolchainDsl
    fun d8(configure: Action<D8ToolchainManagementDsl>) {
        d8 {
            configure.execute(this)
        }
    }

    /**
     * Configures the Binaryen toolchain service used by the whole build.
     *
     * Binaryen (`wasm-opt`) optimizes the Kotlin/Wasm binaries. The block behaves exactly like [d8].
     *
     * @since 2.5.20
     */
    @ExperimentalKotlinToolchainDsl
    fun binaryen(configure: BinaryenToolchainManagementDsl.() -> Unit)

    /**
     * [Action] based version of [binaryen] above.
     *
     * @since 2.5.20
     */
    @ExperimentalKotlinToolchainDsl
    fun binaryen(configure: Action<BinaryenToolchainManagementDsl>) {
        binaryen {
            configure.execute(this)
        }
    }

    /**
     * Configures the Wasmtime toolchain service used by the whole build.
     *
     * Wasmtime runs Kotlin/Wasm WASI applications and tests. The block behaves exactly like [d8].
     *
     * @since 2.5.20
     */
    @ExperimentalKotlinToolchainDsl
    fun wasmtime(configure: WasmtimeToolchainManagementDsl.() -> Unit)

    /**
     * [Action] based version of [wasmtime] above.
     *
     * @since 2.5.20
     */
    @ExperimentalKotlinToolchainDsl
    fun wasmtime(configure: Action<WasmtimeToolchainManagementDsl>) {
        wasmtime {
            configure.execute(this)
        }
    }

    /**
     * Configures the SWC toolchain service used by the whole build.
     *
     * SWC transpiles the JavaScript produced by the Kotlin compiler. The block behaves exactly like [d8].
     *
     * @since 2.5.20
     */
    @ExperimentalKotlinToolchainDsl
    fun swc(configure: SwcToolchainManagementDsl.() -> Unit)

    /**
     * [Action] based version of [swc] above.
     *
     * @since 2.5.20
     */
    @ExperimentalKotlinToolchainDsl
    fun swc(configure: Action<SwcToolchainManagementDsl>) {
        swc {
            configure.execute(this)
        }
    }
}

// Open, as Gradle generates a decorated subclass when registering the extension.
internal open class KotlinEcosystemExtensionImpl(settings: Settings) : KotlinEcosystemExtension {

    @OptIn(InternalKotlinGradlePluginApi::class, ExperimentalKotlinToolchainDsl::class)
    @ExperimentalNodeJsToolchainDsl
    private val toolchainManagementDsl = KotlinToolchainManagementDslImpl(
        nodeJsDsl = NodeJsToolchainManagementDslImpl(settings.gradle),
        d8Dsl = D8ToolchainManagementDslImpl(settings.gradle),
        binaryenDsl = BinaryenToolchainManagementDslImpl(settings.gradle),
        wasmtimeDsl = WasmtimeToolchainManagementDslImpl(settings.gradle),
        swcDsl = SwcToolchainManagementDslImpl(settings.gradle),
    )

    @ExperimentalNodeJsToolchainDsl
    override fun toolchainManagement(configure: KotlinToolchainManagementDsl.() -> Unit) {
        toolchainManagementDsl.configure()
    }
}

@OptIn(ExperimentalKotlinToolchainDsl::class)
internal class KotlinToolchainManagementDslImpl(
    private val nodeJsDsl: NodeJsToolchainManagementDsl,
    private val d8Dsl: D8ToolchainManagementDsl,
    private val binaryenDsl: BinaryenToolchainManagementDsl,
    private val wasmtimeDsl: WasmtimeToolchainManagementDsl,
    private val swcDsl: SwcToolchainManagementDsl,
) : KotlinToolchainManagementDsl {

    @ExperimentalNodeJsToolchainDsl
    override fun nodeJs(configure: NodeJsToolchainManagementDsl.() -> Unit) {
        nodeJsDsl.configure()
    }

    @ExperimentalKotlinToolchainDsl
    override fun d8(configure: D8ToolchainManagementDsl.() -> Unit) {
        d8Dsl.configure()
    }

    @ExperimentalKotlinToolchainDsl
    override fun binaryen(configure: BinaryenToolchainManagementDsl.() -> Unit) {
        binaryenDsl.configure()
    }

    @ExperimentalKotlinToolchainDsl
    override fun wasmtime(configure: WasmtimeToolchainManagementDsl.() -> Unit) {
        wasmtimeDsl.configure()
    }

    @ExperimentalKotlinToolchainDsl
    override fun swc(configure: SwcToolchainManagementDsl.() -> Unit) {
        swcDsl.configure()
    }
}
