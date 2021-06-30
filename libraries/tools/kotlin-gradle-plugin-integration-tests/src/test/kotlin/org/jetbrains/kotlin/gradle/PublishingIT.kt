/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName

@DisplayName("Artifacts publication")
@SimpleGradlePluginTests
class PublishingIT : KGPBaseTest() {

    private val String.fullProjectName get() = "publishing/$this"

    @DisplayName("Should allow to publish library in project which is using BOM (KT-47444)")
    @GradleTest
    internal fun shouldPublishCorrectlyWithOmittedVersion(gradleVersion: GradleVersion) {
        project("withBom".fullProjectName, gradleVersion) {
            build("publishToMavenLocal")
        }
    }
}
