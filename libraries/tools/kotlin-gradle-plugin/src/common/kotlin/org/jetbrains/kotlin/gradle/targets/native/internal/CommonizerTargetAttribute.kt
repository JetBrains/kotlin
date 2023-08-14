/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.attributes.*
import org.jetbrains.kotlin.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.commonizer.parseCommonizerTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution

internal object CommonizerTargetAttribute {
    val attribute: Attribute<String> = Attribute.of("org.jetbrains.kotlin.native.commonizerTarget", String::class.java)

    fun setupAttributesMatchingStrategy(attributesSchema: AttributesSchema) {
        attributesSchema.attribute(attribute) { strategy ->
            strategy.compatibilityRules.add(CommonizerTargetCompatibilityRule::class.java)
            strategy.disambiguationRules.add(CommonizerTargetDisambiguationRule::class.java)
        }
    }

    /**
     * Generally all producers providing a superset of targets can be consumed!
     * e.g. a producer providing a commonizer target for ios and linux targets can be considered compatible
     * for consumers of just the ios targets
     */
    class CommonizerTargetCompatibilityRule : AttributeCompatibilityRule<String> {
        override fun execute(details: CompatibilityCheckDetails<String>) {
            val producerValue = parseCommonizerTarget(details.producerValue ?: return) as? SharedCommonizerTarget ?: return
            val consumerValue = parseCommonizerTarget(details.consumerValue ?: return) as? SharedCommonizerTarget ?: return

            if (producerValue.targets.containsAll(consumerValue.targets)) {
                details.compatible()
            }
        }
    }

    /**
     * Given the above [CommonizerTargetCompatibilityRule] we see that it is expected that every 'more common' cinterops
     * will be marked as compatible. However, in the commonizer output model we always just want to use a single
     * artifact to compile/analyze against. In the case of cinterops we always want the most specific cinterop
     * See [MetadataDependencyResolution.ChooseVisibleSourceSets.visibleSourceSetProvidingCInterops]
     *
     * e.g.
     * - given the consumer requests 'ios + linux'
     * - given the producer offers 'ios + macos + linux' and 'ios + macos + linux + windows'
     *
     * then: the more specific 'ios + macos + linux' (closer) shall be chosen
     */
    class CommonizerTargetDisambiguationRule : AttributeDisambiguationRule<String> {
        override fun execute(details: MultipleCandidatesDetails<String>) {
            details.closestMatch(details.candidateValues.sorted().minByOrNull { it.length } ?: return)
        }
    }
}