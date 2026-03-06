/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.tooling.internal.consumer.ConnectorServices
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.RegisterExtension
import java.nio.file.Path

@DaemonsGradlePluginTests
abstract class KGPDaemonsBaseTest : KGPBaseTest() {
    /**
     * It's acceptable not to clean up this directory. The daemon will remove run files via [java.io.File.deleteOnExit].
     */
    private val kotlinDaemonRunFilesDir: Path
        get() = kgpTestInfraWorkingDirectory.resolve("kotlin-daemon-run-files")

    override val defaultBuildOptions: BuildOptions =
        super.defaultBuildOptions.copy(customKotlinDaemonRunFilesDirectory = kotlinDaemonRunFilesDir.toFile())

    @RegisterExtension
    private val afterTestExecutionCallback: AfterTestExecutionCallback =
        AfterTestExecutionCallback { context ->
            println("[KGPDaemonsBaseTest] Test '${context.displayName}' completed. Terminating Gradle and Kotlin daemons.")
            ConnectorServices.reset()
            awaitKotlinDaemonTermination(kotlinDaemonRunFilesDir)
        }
}
