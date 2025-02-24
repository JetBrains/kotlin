/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.tooling.internal.consumer.ConnectorServices
import org.junit.jupiter.api.AfterEach

@DaemonsGradlePluginTests
abstract class KGPDaemonsBaseTest : KGPBaseTest() {
    /**
     * It's acceptable not to clean up this directory. The run files are removed by the daemon via [java.io.File.deleteOnExit]
     */
    private val kotlinDaemonRunFilesDir get() = kgpTestInfraWorkingDirectory.resolve("kotlin-daemon-run-files")

    override val defaultBuildOptions: BuildOptions =
        super.defaultBuildOptions.copy(customKotlinDaemonRunFilesDirectory = kotlinDaemonRunFilesDir.toFile())

    @AfterEach
    internal open fun tearDown() {
        // Stops Gradle and initiates Kotlin daemon termination, so new run will pick up new jvm arguments
        ConnectorServices.reset()
        awaitKotlinDaemonTermination(kotlinDaemonRunFilesDir)
    }
}
