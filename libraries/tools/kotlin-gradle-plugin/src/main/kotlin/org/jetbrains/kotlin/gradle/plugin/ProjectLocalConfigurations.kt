/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinWithJavaTarget
import org.jetbrains.kotlin.gradle.utils.isGradleVersionAtLeast

internal object ProjectLocalConfigurations {
    val ATTRIBUTE = Attribute.of("org.jetbrains.kotlin.localToProject", String::class.java)

    fun setupAttributesMatchingStrategy(attributesSchema: AttributesSchema) = with(attributesSchema) {
        attribute(ATTRIBUTE) {
            if (gradleVersionSupportsAttributeRules) {
                it.compatibilityRules.add(ProjectLocalCompatibility::class.java)
                it.disambiguationRules.add(ProjectLocalDisambiguation::class.java)
            }
        }
    }

    class ProjectLocalCompatibility : AttributeCompatibilityRule<String> {
        override fun execute(details: CompatibilityCheckDetails<String>) {
            details.compatible()
        }
    }

    class ProjectLocalDisambiguation : AttributeDisambiguationRule<String> {
        override fun execute(details: MultipleCandidatesDetails<String?>) = with(details) {
            if (candidateValues.contains(null)) {
                @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
                closestMatch(null as String?)
            }
        }
    }
}

internal fun Configuration.setupAsLocalTargetSpecificConfigurationIfSupported(target: KotlinTarget) {
    if (gradleVersionSupportsAttributeRules &&
        // don't setup in old MPP common modules, as their output configurations with KotlinPlatformType attribute would
        // fail to resolve as transitive dependencies of the platform modules, just as we don't mark their
        // `api/RuntimeElements` with the KotlinPlatformType
        (target !is KotlinWithJavaTarget<*> || target.platformType != KotlinPlatformType.common)
    ) {
        usesPlatformOf(target)
        attributes.attribute(ProjectLocalConfigurations.ATTRIBUTE, target.project.path)
    }
}

internal val gradleVersionSupportsAttributeRules = isGradleVersionAtLeast(4, 0)