/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import org.gradle.testkit.runner.GradleRunner
import org.jetbrains.kotlin.testFederation.NightlyTest
import java.io.File
import kotlin.test.Test

@NightlyTest
class BootstrapLocalTest {

    /**
     * Tests if simple 'local bootstrapping' works by
     * - calling 'publish' first
     * - compiling the repository with '-Pbootstrap.local=true'
     */
    @Test
    fun `bootstrap local`() {
        val runner = GradleRunner.create()
            .withProjectDir(File("").absoluteFile)
            .withEnvironment(System.getenv())
            .forwardOutput()
            .withTestKitDir(File(System.getProperty("gradle.user.home") ?: error("Missing 'gradle.user.home'")))

        try {
            runner
                .withArguments("publish", "-x", "mvnPublish")
                .build()

            runner
                .withArguments("compileAll", "-Pbootstrap.local=true")
                .build()
        } finally {
            runner
                .withArguments(":clean")
                .build()
        }
    }
}
