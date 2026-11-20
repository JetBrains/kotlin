/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl.toolchain.binaryen

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.jetbrains.kotlin.gradle.ExperimentalExternalToolchainDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.ExternalToolchainPlatform
import javax.inject.Inject

/**
 * Describes the Binaryen distribution requested from a [BinaryenToolchainService].
 *
 * Instances are created by the Kotlin Gradle Plugin and read by the service implementation.
 */
@ExperimentalExternalToolchainDsl
abstract class BinaryenToolchainRequest @Inject internal constructor() {
    /** The Binaryen release version, for example `133`. Must not be blank. */
    @get:Input
    @get:Optional
    abstract val version: Property<String>

    /** The requested operating system and architecture, defaulting to the machine running the build. */
    @get:Nested
    @get:Optional
    abstract val platform: Property<ExternalToolchainPlatform>

    /** Sets [version] to a non-blank Binaryen release version. */
    fun version(version: String) {
        require(version.isNotBlank()) { "Binaryen version must not be blank" }
        this.version.set(version)
    }
}
