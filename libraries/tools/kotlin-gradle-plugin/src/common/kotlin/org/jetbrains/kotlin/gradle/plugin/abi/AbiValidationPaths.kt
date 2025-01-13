/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.abi

/**
 * The paths used in the Application Binary Interface (ABI) validation's default configuration.
 */
internal object AbiValidationPaths {
    /**
     * The default directory for reference legacy dump files.
     */
    internal const val LEGACY_DEFAULT_REFERENCE_DUMP_DIR = "api"

    /**
     * The directory for actual legacy dump files.
     */
    internal const val LEGACY_ACTUAL_DUMP_DIR = "kotlin/abi-legacy"

    /**
     * The default file extension for legacy dumps for Kotlin/JVM or Kotlin Android targets.
     */
    internal const val LEGACY_JVM_DUMP_EXTENSION = ".api"

    /**
     * The default file extension for legacy dumps for Kotlin Multiplatform targets.
     */
    internal const val LEGACY_KLIB_DUMP_EXTENSION = ".klib.api"

    /**
     * The default directory for the reference dump file.
     */
    internal const val DEFAULT_REFERENCE_DUMP_DIR = "abi"

    /**
     * The default file name for the reference dump.
     */
    internal const val DEFAULT_REFERENCE_DUMP_FILE = "abi.dump"
}