/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.artifacts

import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.attributes.*
import org.jetbrains.kotlin.gradle.plugin.KotlinNativeTargetConfigurator
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import java.io.Serializable

enum class KlibPackaging : Named, Serializable {
    PACKED, UNPACKED;

    override fun toString(): String = name
    override fun getName(): String = name

    internal class KotlinPackedKlibCompatibilityRule: AttributeCompatibilityRule<KlibPackaging> {
        override fun execute(details: CompatibilityCheckDetails<KlibPackaging>) = details.compatible()
    }

    internal class KotlinUnpackedKlibDisambiguationRule: AttributeDisambiguationRule<KlibPackaging> {
        override fun execute(details: MultipleCandidatesDetails<KlibPackaging>) {
            val consumerValue = details.consumerValue
            when {
                consumerValue != null && consumerValue in details.candidateValues -> details.closestMatch(consumerValue)
                UNPACKED in details.candidateValues -> details.closestMatch(UNPACKED)
                PACKED in details.candidateValues -> details.closestMatch(PACKED)
            }
        }
    }

    companion object {
        const val ATTRIBUTE_NAME = "org.jetbrains.kotlin.klib.packaging"

        val attribute = Attribute.of(ATTRIBUTE_NAME, KlibPackaging::class.java)

        internal fun setupAttributesMatchingStrategy(project: Project) {
            val attributesSchema = project.dependencies.attributesSchema
            attributesSchema.getMatchingStrategy(attribute).run {
                compatibilityRules.add(KotlinPackedKlibCompatibilityRule::class.java)
                disambiguationRules.add(KotlinUnpackedKlibDisambiguationRule::class.java)
            }

            // By default consider klib is packed
            val artifactType = project.dependencies.artifactTypes.maybeCreate(KotlinNativeTargetConfigurator.NativeArtifactFormat.KLIB)
            artifactType.attributes.attribute(attribute, PACKED)
        }

        internal fun setAttributeTo(project: Project, attributes: AttributeContainer) {
            if (project.kotlinPropertiesProvider.enableUnpackedKlibs) {
                attributes.attribute(attribute, UNPACKED)
            } else {
                attributes.attribute(attribute, PACKED)
            }
        }
    }
}