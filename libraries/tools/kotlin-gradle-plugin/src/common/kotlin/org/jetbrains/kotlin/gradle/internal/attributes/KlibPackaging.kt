/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.artifacts.internal

import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.attributes.*
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.utils.setAttribute
import java.io.Serializable

internal enum class KlibPackaging : Named, Serializable {
    PACKED,
    NON_PACKED,
    ;

    override fun toString() = name
    override fun getName() = name

    internal companion object {
        internal const val ATTRIBUTE_NAME = "org.jetbrains.kotlin.klib.packaging"
        internal const val NON_PACKED_KLIB_VARIANT_NAME = "non-packed-klib"
        internal val attribute = Attribute.of(ATTRIBUTE_NAME, KlibPackaging::class.java)

        fun setAttributeTo(project: Project, attributes: AttributeContainer, nonPacked: Boolean) {
            if (project.kotlinPropertiesProvider.useNonPackedKlibs) {
                attributes.setAttribute(attribute, if (nonPacked) NON_PACKED else PACKED)
            } else {
                error("$ATTRIBUTE_NAME should not be set when non-packed klibs usage is disabled")
            }
        }

        fun setupAttributesMatchingStrategy(attributesSchema: AttributesSchema) {
            attributesSchema.attribute(attribute) { strategy ->
                strategy.disambiguationRules.add(KlibPackagingDisambiguationRule::class.java)
                strategy.compatibilityRules.add(KlibPackagingCompatibilityRule::class.java)
            }
        }
    }
}

private class KlibPackagingCompatibilityRule : AttributeCompatibilityRule<KlibPackaging> {
    override fun execute(details: CompatibilityCheckDetails<KlibPackaging>) = with(details) {
        if (consumerValue == KlibPackaging.NON_PACKED && producerValue?.name == KlibPackaging.PACKED.name) {
            compatible()
        }
    }
}

private class KlibPackagingDisambiguationRule : AttributeDisambiguationRule<KlibPackaging> {
    override fun execute(details: MultipleCandidatesDetails<KlibPackaging>) = with(details) {
        when {
            KlibPackaging.NON_PACKED in details.candidateValues -> details.closestMatch(KlibPackaging.NON_PACKED)
            KlibPackaging.PACKED in details.candidateValues -> details.closestMatch(KlibPackaging.PACKED)
        }
    }
}