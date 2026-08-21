/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.artifacts.type.ArtifactTypeDefinition

plugins {
    jacoco
    id("kgp-jacoco-instrumenter")
}

val versionCatalog = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
val jacocoVersion = versionCatalog.findVersion("jacoco").get().requiredVersion

jacoco {
    toolVersion = jacocoVersion
}

val testCoverageEnabled = kotlinBuildProperties.kgpTestCoverageEnabled.get()
tasks.withType<Test>().configureEach {
    ignoreFailures = testCoverageEnabled
    extensions.configure<JacocoTaskExtension> {
        isEnabled = testCoverageEnabled
    }
}

if (testCoverageEnabled) {
    // `mainSourceElements` (consumed by jacoco-report-aggregation for source discovery) doesn't
    // include the `common` source set by default — add it so it shows up in reports.
    plugins.withId("java") {
        configurations.findByName("mainSourceElements")?.let { mainSourceElements ->
            sourceSets.findByName("common")?.allSource?.srcDirs?.forEach { srcDir ->
                mainSourceElements.outgoing.artifact(srcDir) {
                    type = ArtifactTypeDefinition.DIRECTORY_TYPE
                }
            }
        }
    }
}
