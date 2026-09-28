/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain

import org.gradle.api.Action
import org.gradle.api.invocation.Gradle
import org.jetbrains.kotlin.gradle.ExperimentalNodeJsToolchainDsl
import org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain.NodeJsToolchainService.Companion.nodeJsServiceName
import kotlin.reflect.KClass

/**
 * DSL for selecting and configuring the [NodeJsToolchainService] used by the whole build, exposed as
 * `toolchainManagement { nodeJs { ... } }` in `settings.gradle.kts` by the `org.jetbrains.kotlin.ecosystem` plugin.
 *
 * At most one of [toolchainService] or [disable] may be called - the [NodeJsToolchainService] is registered as a
 * shared [org.gradle.api.services.BuildService] right away, under the same name every Kotlin Gradle Plugin project
 * looks up when it registers the toolchain service for itself, so calling this more than once would silently keep
 * only the first registration.
 *
 * ```
 * kotlin {
 *     toolchainManagement {
 *         nodeJs {
 *             toolchainService(MyNodeJsToolchainService::class) { //specify a service class is optional, DefaultNodeJsToolchainService will be used by default
 *             }
 *             toolchainService(DefaultNodeJsToolchainService::class) {
 *                 downloadBaseUrl("custom-url") //official node js website will be used by default
 *                 installationDir("custom location") //default $KOTLIN_CACHE_DIR/toolchains/nodejs location will be used by default
 *             }
 *             toolchainService(PreInstalledNodeJsToolchainService::class) {
 *                 nodeJsExecutable("path_to_node_js_executable") //"node" will be used by default
 *             }
 *        }
 *     }
 * }
 * ```
 *
 * **Note:** This interface is not intended for implementation by build script or plugin authors.
 */
interface NodeJsToolchainManagementDsl {

    /**
     * Configures the built-in, download-based [DefaultNodeJsToolchainService].
     *
     * This is the service used by the build when [toolchainService] and [disable] are never called.
     */
    @ExperimentalNodeJsToolchainDsl
    fun toolchainService(configure: DefaultNodeJsToolchainService.Parameters.() -> Unit)

    /**
     * [Action] based version of [toolchainService] above.
     */
    @ExperimentalNodeJsToolchainDsl
    fun toolchainService(configure: Action<DefaultNodeJsToolchainService.Parameters>) {
        toolchainService {
            configure.execute(this)
        }
    }

    /**
     * Selects [serviceClass] as the [NodeJsToolchainService] used by the whole build, and configures its
     * [parameters][NodeJsToolchainService.Parameters].
     */
    @ExperimentalNodeJsToolchainDsl
    fun <P : NodeJsToolchainService.Parameters, T : NodeJsToolchainService<P>> toolchainService(
        serviceClass: KClass<T>,
        configure: P.() -> Unit,
    )

    /**
     * [Action] based version of [toolchainService] above.
     */
    @ExperimentalNodeJsToolchainDsl
    fun <P : NodeJsToolchainService.Parameters, T : NodeJsToolchainService<P>> toolchainService(
        serviceClass: KClass<T>,
        configure: Action<P>,
    ) {
        toolchainService(serviceClass) {
            configure.execute(this)
        }
    }

    /**
     * Disables Node.js provisioning for the whole build - tasks that need a Node.js executable will fail.
     *
     * A shorthand for `toolchainService(DisabledNodeJsToolchainService::class) { }`.
     */
    @ExperimentalNodeJsToolchainDsl
    fun disable() {
        toolchainService(DisabledNodeJsToolchainService::class) { }
    }
}

@ExperimentalNodeJsToolchainDsl
open class NodeJsToolchainManagementDslImpl(
    private val gradle: Gradle,
) : NodeJsToolchainManagementDsl {

    private var configured = false

    override fun toolchainService(configure: DefaultNodeJsToolchainService.Parameters.() -> Unit) {
        toolchainService(DefaultNodeJsToolchainService::class) {
            offline.set(gradle.startParameter.isOffline)
            configure()
        }
    }

    override fun <P : NodeJsToolchainService.Parameters, T : NodeJsToolchainService<P>> toolchainService(
        serviceClass: KClass<T>,
        configure: P.() -> Unit,
    ) {
        check(!configured) {
            "The Node.js toolchain service is already configured. " +
                    "Only one 'toolchainService(...)' or 'disable()' call is allowed inside 'toolchainManagement { nodeJs { ... } }'."
        }
        configured = true

        gradle.sharedServices.registerIfAbsent(nodeJsServiceName, serviceClass.java) { spec ->
            configure(spec.parameters)
        }
    }
}
