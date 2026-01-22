/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Project
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.TestSuiteName
import org.gradle.api.attributes.VerificationType
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.named

/**
 * Registers a consumable outgoing configuration that exposes JaCoCo `.exec` files to
 * `jacoco-report-aggregation` consumers.
 */
fun Project.registerKgpTestCoverageDataVariant(
    configurationName: String,
    suiteName: String,
    execFile: Provider<RegularFile>,
    testTask: TaskProvider<*>,
) {
    configurations.consumable(configurationName) {
        attributes {
            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.VERIFICATION))
            attribute(VerificationType.VERIFICATION_TYPE_ATTRIBUTE, objects.named(VerificationType.JACOCO_RESULTS))
            attribute(TestSuiteName.TEST_SUITE_NAME_ATTRIBUTE, objects.named(suiteName))
        }
        outgoing.artifact(execFile) {
            type = ArtifactTypeDefinition.BINARY_DATA_TYPE
            builtBy(testTask)
        }
    }
}
