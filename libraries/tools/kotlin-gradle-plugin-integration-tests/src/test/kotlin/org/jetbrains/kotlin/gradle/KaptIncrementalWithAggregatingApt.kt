/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KaptIncrementalWithAggregatingApt : KaptIncrementalIT() {

    override fun getProject() =
        Project(
            "kaptIncrementalCompilationProject",
            GradleVersionRequired.None
        ).apply {
            setupIncrementalAptProject("AGGREGATING")
        }

    override fun defaultBuildOptions(): BuildOptions =
        super.defaultBuildOptions().copy(
            incremental = true,
            kaptOptions = KaptOptions(
                verbose = true,
                useWorkers = true,
                incrementalKapt = true,
                includeCompileClasspath = false
            )
        )

    @Test
    fun testIncrementalChanges() {
        val project = getProject()

        var aptTimestamp = 0L

        project.build("clean", "build") {
            assertSuccessful()

            val classpathHistory =
                fileInWorkingDir("build/kotlin/kaptGenerateStubsKotlin/classpath-fq-history").listFiles().asList().single()
            val stubsTimestamp = classpathHistory.name.toLong()

            aptTimestamp = fileInWorkingDir("build/tmp/kapt3/incApCache/main/last-build-ts.bin").readText().toLong()
            assertTrue(stubsTimestamp < aptTimestamp)
        }

        project.projectFile("useB.kt").modify { current -> "$current\nfun otherFunction() {}" }
        project.build("build") {
            assertSuccessful()

            val newAptTimestamp = fileInWorkingDir("build/tmp/kapt3/incApCache/main/last-build-ts.bin").readText().toLong()
            assertTrue(aptTimestamp < newAptTimestamp)

            assertEquals(
                setOf(
                    fileInWorkingDir("build/tmp/kapt3/stubs/main/bar/UseBKt.java").absolutePath,
                    fileInWorkingDir("build/tmp/kapt3/stubs/main/bar/B.java").absolutePath,
                    fileInWorkingDir("build/tmp/kapt3/stubs/main/baz/UtilKt.java").absolutePath,
                    fileInWorkingDir("build/tmp/kapt3/stubs/main/foo/A.java").absolutePath
                ), getProcessedSources(output)
            )
        }

        project.projectFile("JavaClass.java").modify { current ->
            val lastBrace = current.lastIndexOf("}")
            current.substring(0, lastBrace) + "private void anotherFun() {}\n }"
        }
        project.build("build") {
            assertSuccessful()
            assertEquals(
                setOf(
                    project.projectFile("JavaClass.java").absolutePath,
                    fileInWorkingDir("build/tmp/kapt3/stubs/main/bar/UseBKt.java").absolutePath,
                    fileInWorkingDir("build/tmp/kapt3/stubs/main/bar/B.java").absolutePath,
                    fileInWorkingDir("build/tmp/kapt3/stubs/main/baz/UtilKt.java").absolutePath,
                    fileInWorkingDir("build/tmp/kapt3/stubs/main/foo/A.java").absolutePath
                ),
                getProcessedSources(output)
            )
        }
    }
}