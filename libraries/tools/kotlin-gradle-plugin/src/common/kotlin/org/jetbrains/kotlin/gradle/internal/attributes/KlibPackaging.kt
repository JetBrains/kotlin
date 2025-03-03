/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.attributes

import org.gradle.api.Project
import org.gradle.api.attributes.*
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.attributes.KlibPackaging
import org.jetbrains.kotlin.gradle.utils.named

internal fun KlibPackaging.Companion.setAttributeTo(project: Project, attributes: AttributeContainer, packed: Boolean) {
    if (project.kotlinPropertiesProvider.useNonPackedKlibs) {
        attributes.attribute(
            ATTRIBUTE,
            project.objects.named(if (packed) PACKED else NON_PACKED)
        )
    } else {
        error("${ATTRIBUTE.name} should not be set when non-packed klibs usage is disabled")
    }
}

internal fun KlibPackaging.Companion.setupAttributesMatchingStrategy(attributesSchema: AttributesSchema) {
    attributesSchema.attribute(ATTRIBUTE) { strategy ->
        strategy.disambiguationRules.add(KlibPackagingDisambiguationRule::class.java)
        strategy.compatibilityRules.add(KlibPackagingCompatibilityRule::class.java)
    }
}

private class KlibPackagingCompatibilityRule : AttributeCompatibilityRule<KlibPackaging> {
    override fun execute(details: CompatibilityCheckDetails<KlibPackaging>) = with(details) {
        if (consumerValue?.name == KlibPackaging.NON_PACKED && producerValue?.name == KlibPackaging.PACKED) {
            compatible()
        }
    }
}

private class KlibPackagingDisambiguationRule : AttributeDisambiguationRule<KlibPackaging> {
    override fun execute(details: MultipleCandidatesDetails<KlibPackaging>) = with(details) {
        val candidateNames = getCandidateNames()
        val consumerValue = consumerValue
        when {
            consumerValue != null && consumerValue in candidateValues -> closestMatch(consumerValue)
            KlibPackaging.PACKED in candidateNames -> chooseCandidateByName(KlibPackaging.PACKED)
            KlibPackaging.NON_PACKED in candidateNames -> chooseCandidateByName(KlibPackaging.NON_PACKED)
        }
    }
}