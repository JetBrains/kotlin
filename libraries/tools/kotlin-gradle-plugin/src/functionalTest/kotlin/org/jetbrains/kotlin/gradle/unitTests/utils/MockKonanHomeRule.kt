/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.utils

import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.io.File
import java.nio.file.Files

/**
 * A JUnit 5 Extension that mocks a Kotlin/Native (Konan) installation directory.
 *
 * This extension manages a temporary filesystem structure (`konan/`, `bin/`) required
 * by the Kotlin Native Gradle Plugin during tests. It allows configuring whether
 * to copy the real `konan.properties` or run with an empty setup.
 *
 * Usage:
 * Call [setup] within your test or helper method to initialize the directory.
 */
class MockKonanHomeExtension : BeforeEachCallback, AfterEachCallback {

    private lateinit var temporaryFolder: File

    /**
     * The root directory of the mocked Kotlin/Native installation.
     * Throws an error if accessed before [setup] is called.
     */
    lateinit var konanHome: File
        private set

    override fun beforeEach(context: ExtensionContext) {
        temporaryFolder = Files.createTempDirectory("mock-konan").toFile()
    }

    /**
     * Initializes the mock environment.
     * @param includeKonanProperties If true, copies the real konan.properties to the mock dir.
     */
    fun setup(includeKonanProperties: Boolean = true) {
        // Check if lateinit var is already set to prevent double initialization
        if (::konanHome.isInitialized) return

        konanHome = temporaryFolder.resolve("mock-konan-home").also { it.mkdirs() }

        // Create standard directory structure
        val konanDir = File(konanHome, "konan").apply { mkdirs() }
        File(konanHome, "bin").apply { mkdirs() }

        if (includeKonanProperties) {
            val path = KONAN_PROPERTIES_PATH ?: error("konanProperties system property is not set")
            val sourceProps = File(path)

            if (sourceProps.exists()) {
                sourceProps.copyTo(File(konanDir, "konan.properties"), overwrite = true)
            }
        }
    }

    override fun afterEach(context: ExtensionContext) {
        temporaryFolder.deleteRecursively()
    }

    companion object {
        private val KONAN_PROPERTIES_PATH: String?
            get() = System.getProperty("konanProperties")
    }
}
