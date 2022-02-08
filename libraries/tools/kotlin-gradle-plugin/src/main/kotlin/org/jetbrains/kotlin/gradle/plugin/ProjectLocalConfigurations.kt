/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinWithJavaTarget

/**
 * This attribute is registered in the schema only for the case if some 3rd-party consumer or published
 * module still has it or relies on it. The Kotlin Gradle plugin should not add this attribute to any variant.
 * TODO: examine and remove
 */
object ProjectLocalConfigurations {
    val ATTRIBUTE: Attribute<String> = Attribute.of("org.jetbrains.kotlin.localToProject", String::class.java)

    const val PUBLIC_VALUE = "public"

    fun setupAttributesMatchingStrategy(attributesSchema: AttributesSchema) = with(attributesSchema) {
        attribute(ATTRIBUTE) {
            it.compatibilityRules.add(ProjectLocalCompatibility::class.java)
            it.disambiguationRules.add(ProjectLocalDisambiguation::class.java)
        }
    }

    class ProjectLocalCompatibility : AttributeCompatibilityRule<String> {
        override fun execute(details: CompatibilityCheckDetails<String>) {
            details.compatible()
        }
    }

    class ProjectLocalDisambiguation : AttributeDisambiguationRule<String> {
        override fun execute(details: MultipleCandidatesDetails<String?>) = with(details) {
            if (candidateValues.contains(PUBLIC_VALUE)) {
                closestMatch(PUBLIC_VALUE)
            }
        }
    }
}

internal fun Configuration.setupAsLocalTargetSpecificConfigurationIfSupported(target: KotlinTarget) {
    // don't setup in old MPP common modules, as their output configurations with KotlinPlatformType attribute would
    // fail to resolve as transitive dependencies of the platform modules, just as we don't mark their
    // `api/RuntimeElements` with the KotlinPlatformType
    if ((target !is KotlinWithJavaTarget<*> || target.platformType != KotlinPlatformType.common)) {
        usesPlatformOf(target)
    }
}
