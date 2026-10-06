/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.nodejs

import org.gradle.api.Task
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.gradle.ExperimentalNodeJsToolchainDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.nodejs.NodeJsToolchainService

/**
 * A [Task] that uses a [NodeJsToolchainService] to provision Node.js.
 *
 * The Kotlin Gradle Plugin will set [nodeJsToolchainService] to the [UsesNodeJsToolchainService] tasks.
 */
@ExperimentalNodeJsToolchainDsl
interface UsesNodeJsToolchainService : Task {
    /**
     * The [NodeJsToolchainService] is used for Node.js provisioning.
     */
    @get:Internal
    val nodeJsToolchainService: Property<NodeJsToolchainService<*>>
}
