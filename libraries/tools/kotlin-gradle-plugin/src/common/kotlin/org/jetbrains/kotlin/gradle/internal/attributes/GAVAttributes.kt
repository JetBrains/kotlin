/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.attributes

import org.gradle.api.attributes.*

internal val PUBLISH_COORDINATES_TYPE_ATTRIBUTE = Attribute.of("org.jetbrains.kotlin.publish.publish-coordinates-type", String::class.java)

internal const val WITHOUT_PUBLISH_COORDINATES = "without-publish-coordinates"
internal const val WITH_PUBLISH_COORDINATES = "with-publish-coordinates"

internal fun setupGavAttributesMatchingStrategy(attributesSchema: AttributesSchema) {
    attributesSchema.attribute(PUBLISH_COORDINATES_TYPE_ATTRIBUTE) { strategy ->
        strategy.compatibilityRules.add(WithArtifactIdAttributeCompatibilityRule::class.java)
        strategy.disambiguationRules.add(WithArtifactIdAttributeDisambiguationRule::class.java)
    }
}

internal class WithArtifactIdAttributeCompatibilityRule : AttributeCompatibilityRule<String> {
    override fun execute(details: CompatibilityCheckDetails<String>) = with(details) {
        if (consumerValue == WITH_PUBLISH_COORDINATES && producerValue == WITHOUT_PUBLISH_COORDINATES) {
            compatible()
        }

        if (consumerValue == WITHOUT_PUBLISH_COORDINATES && producerValue == null) {
            compatible()
        }
    }
}

internal class WithArtifactIdAttributeDisambiguationRule : AttributeDisambiguationRule<String> {
    override fun execute(details: MultipleCandidatesDetails<String>) = with(details) {
        if (consumerValue == null){
            closestMatch(WITHOUT_PUBLISH_COORDINATES)
        }
        consumerValue?.let { closestMatch(it) } ?: return@with
    }
}