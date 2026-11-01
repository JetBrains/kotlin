/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.toolchain

import org.gradle.api.Task
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.gradle.ExperimentalExternalToolchainDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.binaryen.BinaryenToolchainService

/**
 * A [Task] that uses a [BinaryenToolchainService] to provision Binaryen.
 *
 * The Kotlin Gradle Plugin sets [binaryenToolchainService] on every [UsesBinaryenToolchainService] task.
 */
@ExperimentalExternalToolchainDsl
interface UsesBinaryenToolchainService : Task {
    /**
     * The [BinaryenToolchainService] used for Binaryen provisioning.
     */
    @get:Internal
    val binaryenToolchainService: Property<BinaryenToolchainService<*>>
}
