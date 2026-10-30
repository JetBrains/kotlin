/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.ecosystem

import org.gradle.api.Action
import org.gradle.api.initialization.Settings
import org.jetbrains.kotlin.gradle.ExperimentalKotlinToolchainDsl
import org.jetbrains.kotlin.gradle.ExperimentalNodeJsToolchainDsl
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.toolchain.ToolchainManagementDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.binaryen.BinaryenToolchainManagementDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.d8.D8ToolchainManagementDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.nodejs.NodeJsToolchainManagementDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.swc.SwcToolchainManagementDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtime.WasmtimeToolchainManagementDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasiadapter.WasiAdapterToolchainManagementDsl
import org.jetbrains.kotlin.gradle.targets.wasm.wasiadapter.toolchain.WasiAdapterToolchainManagementDslImpl
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools.WasmToolsToolchainManagementDsl
import org.jetbrains.kotlin.gradle.targets.wasm.wasmtools.toolchain.WasmToolsToolchainManagementDslImpl
import org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain.NodeJsToolchainManagementDslImpl
import org.jetbrains.kotlin.gradle.toolchains.BinaryenToolchainManagementDslImpl
import org.jetbrains.kotlin.gradle.toolchains.D8ToolchainManagementDslImpl
import org.jetbrains.kotlin.gradle.toolchains.SwcToolchainManagementDslImpl
import org.jetbrains.kotlin.gradle.toolchains.WasmtimeToolchainManagementDslImpl

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
     * Configures the WASI adapter toolchain service used by the whole build.
     *
     * Kotlin compiles Wasm WASI code into a core WebAssembly module that imports the legacy
     * `wasi_snapshot_preview1` interface, while a WebAssembly component is expected to import the WASI preview2 world.
     * The WASI adapter is the module, published with Wasmtime releases, that bridges these two worlds: it is passed to
     * `wasm-tools component new --adapt wasi_snapshot_preview1=<adapter>` when the compiled module is turned into a component.
     *
     * At most one service may be selected inside the block: call only one of
     * [WasiAdapterToolchainManagementDsl.toolchainService] or [WasiAdapterToolchainManagementDsl.disable].
     * If the block is omitted, WASI adapter toolchain service will be resolved from Gradle properties
     * or default to download mode.
     *
     * Use the default download-based service with custom parameters:
     * ```
     * // settings.gradle.kts
     * kotlin {
     *     toolchainManagement {
     *         wasiAdapter {
     *             toolchainService {
     *                 // default download-based WASI adapter toolchain service will be used
     *             }
     *         }
     *     }
     * }
     * ```
     * Default `https://github.com/bytecodealliance/wasmtime/releases/download` URL will be used if none is specified.
     * Default installation directory `<user.home>/.kotlin/toolchains/wasi-adapter` will be used if none is specified.
     *
     * ```
     * // settings.gradle.kts
     * kotlin {
     *     toolchainManagement {
     *         wasiAdapter {
     *             toolchainService {
     *                 // all parameters could be set with custom values
     *                 downloadBaseUrl.set("https://my.mirror.example/wasmtime/releases/download")
     *                 installationDir.set(File("/opt/toolchains/wasi-adapter"))
     *             }
     *         }
     *     }
     * }
     * ```
     *
     * Use a WASI adapter file that is already available on the machine:
     * ```
     * // settings.gradle.kts
     * kotlin {
     *     toolchainManagement {
     *         wasiAdapter {
     *             toolchainService(PreInstalledWasiAdapterToolchainService::class) {
     *                 adapterFile.set(file("/opt/adapters/wasi_snapshot_preview1.command.wasm"))
     *             }
     *         }
     *     }
     * }
     * ```
     *
     * Disable WASI adapter provisioning entirely (tasks that need a WASI adapter will fail):
     * ```
     * // settings.gradle.kts
     * kotlin {
     *     toolchainManagement {
     *         wasiAdapter {
     *             disable()
     *         }
     *     }
     * }
     * ```
     *
     * @since 2.5.20
     */
    @ExperimentalWasmDsl
    fun wasiAdapter(configure: WasiAdapterToolchainManagementDsl.() -> Unit)

    /**
     * [Action] based version of [wasiAdapter] above.
     *
     * @since 2.5.20
     */
    @ExperimentalWasmDsl
    fun wasiAdapter(configure: Action<WasiAdapterToolchainManagementDsl>) {
        wasiAdapter {
            configure.execute(this)
        }
    }

    /**
     * Configures the wasm-tools toolchain service used by the whole build.
     *
     * Kotlin compiles Wasm code into a core WebAssembly module, which knows nothing about WIT worlds,
     * while a WebAssembly component is a core module with the WIT declarations embedded into it and wrapped
     * into the component format. `wasm-tools` is the command-line tool of the Bytecode Alliance that performs
     * this conversion: `wasm-tools component embed` puts the WIT world into the module and
     * `wasm-tools component new` turns the result into a component.
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
     * @since 2.5.20
     */
    @ExperimentalWasmDsl
    fun wasmTools(configure: WasmToolsToolchainManagementDsl.() -> Unit)

    /**
     * [Action] based version of [wasmTools] above.
     *
     * @since 2.5.20
     */
    @ExperimentalWasmDsl
    fun wasmTools(configure: Action<WasmToolsToolchainManagementDsl>) {
        wasmTools {
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

    @OptIn(InternalKotlinGradlePluginApi::class, ExperimentalWasmDsl::class, ExperimentalKotlinToolchainDsl::class)
    @ExperimentalNodeJsToolchainDsl
    @ExperimentalWasmDsl
    private val toolchainManagementDsl = KotlinToolchainManagementDslImpl(
        nodeJsDsl = NodeJsToolchainManagementDslImpl(settings.gradle),
        wasiAdapterDsl = WasiAdapterToolchainManagementDslImpl(settings.gradle),
        wasmToolsDsl = WasmToolsToolchainManagementDslImpl(settings.gradle),
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
    private val wasiAdapterDsl: WasiAdapterToolchainManagementDsl,
    private val wasmToolsDsl: WasmToolsToolchainManagementDsl,
    private val d8Dsl: D8ToolchainManagementDsl,
    private val binaryenDsl: BinaryenToolchainManagementDsl,
    private val wasmtimeDsl: WasmtimeToolchainManagementDsl,
    private val swcDsl: SwcToolchainManagementDsl,
) : KotlinToolchainManagementDsl {

    @ExperimentalNodeJsToolchainDsl
    override fun nodeJs(configure: NodeJsToolchainManagementDsl.() -> Unit) {
        nodeJsDsl.configure()
    }

    @ExperimentalWasmDsl
    override fun wasiAdapter(configure: WasiAdapterToolchainManagementDsl.() -> Unit) {
        wasiAdapterDsl.configure()
    }

    @ExperimentalWasmDsl
    override fun wasmTools(configure: WasmToolsToolchainManagementDsl.() -> Unit) {
        wasmToolsDsl.configure()
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
