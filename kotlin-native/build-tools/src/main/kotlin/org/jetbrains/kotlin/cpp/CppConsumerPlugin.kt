/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cpp

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeDisambiguationRule
import org.gradle.api.attributes.MultipleCandidatesDetails
import org.jetbrains.kotlin.bitcode.CompileToBitcodePlugin
import org.jetbrains.kotlin.konan.target.TargetWithSanitizer

private class TargetDisambiguationRule : AttributeDisambiguationRule<TargetWithSanitizer> {
    override fun execute(details: MultipleCandidatesDetails<TargetWithSanitizer>) = details.run {
        if (consumerValue == null) {
            // If the consumer didn't want a specific target, provide host target if it's available.
            val default = TargetWithSanitizer.host
            if (candidateValues.contains(default)) {
                closestMatch(default)
            }
        }
    }
}

/**
 * Plugin for projects that depend upon C++-built projects.
 *
 * For building C++ use [CompileToBitcodePlugin].
 *
 * @see CompileToBitcodePlugin
 */
// TODO: Consider doing CppBasePlugin like standard gradle plugins that also
//       creates default configurations and lifecycle tasks.
class CppConsumerPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.dependencies.attributesSchema {
            attribute(TargetWithSanitizer.TARGET_ATTRIBUTE) {
                disambiguationRules.add(TargetDisambiguationRule::class.java)
            }
        }
    }

    companion object {
        internal fun moduleCapability(project: Project, moduleName: String) = "${project.group}:${project.name}-${moduleName}:${project.version}"
        internal fun testFixturesCapability(project: Project) = "${project.group}:${project.name}-test-fixtures:${project.version}"
        internal fun moduleTestFixturesCapability(project: Project, moduleName: String) = "${project.group}:${project.name}-${moduleName}-test-fixtures:${project.version}"
        internal fun testCapability(project: Project) = "${project.group}:${project.name}-test:${project.version}"
        internal fun moduleTestCapability(project: Project, moduleName: String) = "${project.group}:${project.name}-${moduleName}-test:${project.version}"
    }
}