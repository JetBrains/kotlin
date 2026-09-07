/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.SingleActionPerProject

/**
 * The [NodeJsToolchainService] implementation the build uses to provision Node.js distributions.
 *
 * Selected with the `kotlin.js.nodejs.toolchain` Gradle property.
 */
internal enum class NodeJsToolchainMode {

    /**
     * Node.js distributions are downloaded and installed by [DefaultNodeJsToolchainService].
     */
    DOWNLOAD,

    /**
     * A pre-installed Node.js is used by [NodeJsFromSystemPathToolchainService], and nothing is downloaded.
     */
    SYSTEM_PATH,
    DISABLE,
    ;
}

private val serviceClass = NodeJsToolchainService::class.java
internal val nodeJsServiceName = "${serviceClass.name}_${serviceClass.classLoader.hashCode()}"


private fun registerIfAbsent(
    project: Project,
): Provider<out NodeJsToolchainService<out NodeJsToolchainService.Parameters>> {
    project.gradle.sharedServices.registrations.findByName(nodeJsServiceName)?.let {
        @Suppress("UNCHECKED_CAST")
        return it.service as Provider<NodeJsToolchainService<out NodeJsToolchainService.Parameters>>
    }

    return when (project.kotlinPropertiesProvider.nodeJsToolchainMode) {
        NodeJsToolchainMode.DOWNLOAD -> DefaultNodeJsToolchainService.registerIfAbsent(project)
        NodeJsToolchainMode.SYSTEM_PATH -> NodeJsFromSystemPathToolchainService.registerIfAbsent(project)
        NodeJsToolchainMode.DISABLE -> DisabledNodeJsToolchainService.registerIfAbsent(project)
    }
}



/**
 * Registers the [NodeJsToolchainService] selected for the build, and makes every
 * [UsesNodeJsToolchainService] task in [project] use it.
 */
internal fun registerNodeJsToolchainServiceIfAbsent(
    project: Project,
) {
    val serviceProvider = registerIfAbsent(project)

    SingleActionPerProject.run(project, UsesNodeJsToolchainService::class.java.name) {
        project.tasks.withType<UsesNodeJsToolchainService>().configureEach { task ->
            task.nodeJsToolchainService.value(serviceProvider).disallowChanges()
            task.usesService(serviceProvider)
        }
    }
}

