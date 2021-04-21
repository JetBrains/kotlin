/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import kotlin.test.*

class CommonizerIT : BaseGradleIT() {
    override val defaultGradleVersion: GradleVersionRequired = GradleVersionRequired.FOR_MPP_SUPPORT

    @Test
    fun testCommonizeNativeDistributionWithIosLinuxWindows() {
        with(Project("commonizeNativeDistributionWithIosLinuxWindows")) {
            build(":p1:runCommonizer") {
                assertContains("Some Kotlin/Native targets cannot be built")
                assertSuccessful()
            }
        }
    }

    @Test
    fun `test KT-46234 intermediate source set with only one native target`() {

        val posixInImplementationMetadataConfigurationRegex = Regex(""".*implementationMetadataConfiguration:.*([pP])osix""")

        fun CompiledProject.containsPosixInImplementationMetadataConfiguration(): Boolean =
            output.lineSequence().any { line ->
                line.matches(posixInImplementationMetadataConfigurationRegex)
            }

        with(Project("commonize-kt-46234-singleNativeTarget")) {
            build(":p1:listNativePlatformMainDependencies", "-Pkotlin.mpp.enableIntransitiveMetadataConfiguration=false") {
                assertSuccessful()

                assertTrue(
                    containsPosixInImplementationMetadataConfiguration(),
                    "Expected dependency on posix in implementationMetadataConfiguration"
                )
            }

            build("assemble") {
                assertSuccessful()
            }
        }
    }
}
