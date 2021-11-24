/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages.consumerApiUsage
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages.consumerRuntimeUsage
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages.producerApiUsage
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages.producerRuntimeUsage
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.ComputedCapability
import org.jetbrains.kotlin.gradle.plugin.usageByName

val DefaultKotlinCompileDependenciesSetup = KotlinConfigurationsSetup(
    KotlinPlatformAttributesSetup, KotlinConsumerApiUsageAttributesSetup
)

val DefaultKotlinApiElementsSetup = KotlinConfigurationsSetup(
    KotlinPlatformAttributesSetup, KotlinModuleCapabilitySetup, KotlinProducerApiUsageAttributesSetup
)

val DefaultKotlinRuntimeDependenciesSetup = KotlinConfigurationsSetup(
    KotlinPlatformAttributesSetup, KotlinConsumerRuntimeUsageAttributesSetup
)

val DefaultKotlinRuntimeElementsSetup = KotlinConfigurationsSetup(
    KotlinPlatformAttributesSetup, KotlinProducerRuntimeUsageAttributesSetup
)

val DefaultHostSpecificMetadataElementsSetup = KotlinConfigurationsSetup(
    KotlinPlatformAttributesSetup, KonanTargetAttributesSetup, KotlinMetadataUsageAttributeSetup
)

interface KotlinConfigurationsSetup<in T : KotlinGradleFragment> {
    fun configure(fragment: T, configuration: Configuration)
}

object KotlinPlatformAttributesSetup : KotlinConfigurationsSetup<KotlinGradleVariant> {
    override fun configure(fragment: KotlinGradleVariant, configuration: Configuration) {
        configuration.attributes.attribute(KotlinPlatformType.attribute, fragment.platformType)
    }
}

object KotlinConsumerApiUsageAttributesSetup : KotlinConfigurationsSetup<KotlinGradleVariant> {
    override fun configure(fragment: KotlinGradleVariant, configuration: Configuration) {
        configuration.attributes.attribute(USAGE_ATTRIBUTE, consumerApiUsage(fragment.project, fragment.platformType))
    }
}

object KotlinProducerApiUsageAttributesSetup : KotlinConfigurationsSetup<KotlinGradleVariant> {
    override fun configure(fragment: KotlinGradleVariant, configuration: Configuration) {
        configuration.attributes.attribute(USAGE_ATTRIBUTE, producerApiUsage(fragment.project, fragment.platformType))
    }
}

object KotlinConsumerRuntimeUsageAttributesSetup : KotlinConfigurationsSetup<KotlinGradleVariant> {
    override fun configure(fragment: KotlinGradleVariant, configuration: Configuration) {
        configuration.attributes.attribute(USAGE_ATTRIBUTE, consumerRuntimeUsage(fragment.project, fragment.platformType))
    }
}

object KotlinProducerRuntimeUsageAttributesSetup : KotlinConfigurationsSetup<KotlinGradleVariant> {
    override fun configure(fragment: KotlinGradleVariant, configuration: Configuration) {
        configuration.attributes.attribute(USAGE_ATTRIBUTE, producerRuntimeUsage(fragment.project, fragment.platformType))
    }
}

object KotlinMetadataUsageAttributeSetup : KotlinConfigurationsSetup<KotlinGradleVariant> {
    override fun configure(fragment: KotlinGradleVariant, configuration: Configuration) {
        configuration.attributes.attribute(Usage.USAGE_ATTRIBUTE, fragment.project.usageByName(KotlinUsages.KOTLIN_METADATA))
    }
}

object KonanTargetAttributesSetup : KotlinConfigurationsSetup<KotlinNativeVariantInternal> {
    override fun configure(fragment: KotlinNativeVariantInternal, configuration: Configuration) {
        configuration.attributes.attribute(KotlinNativeTarget.konanTargetAttribute, fragment.konanTarget.name)
    }
}

object KotlinModuleCapabilitySetup : KotlinConfigurationsSetup<KotlinGradleVariant> {
    override fun configure(fragment: KotlinGradleVariant, configuration: Configuration) {
        setModuleCapability(configuration, fragment.containingModule)
    }

    internal fun setModuleCapability(configuration: Configuration, module: KotlinGradleModule) {
        if (module.moduleClassifier != null) {
            configuration.outgoing.capability(ComputedCapability.fromModule(module))
        }
    }
}

operator fun <T : KotlinGradleFragment> KotlinConfigurationsSetup<T>.plus(
    other: KotlinConfigurationsSetup<T>
): KotlinConfigurationsSetup<T> {
    if (this is CompositeKotlinConfigurationsSetup && other is CompositeKotlinConfigurationsSetup) {
        return CompositeKotlinConfigurationsSetup(this.setups + other.setups)
    }

    if (this is CompositeKotlinConfigurationsSetup) {
        return CompositeKotlinConfigurationsSetup(this.setups + other)
    }

    if (other is CompositeKotlinConfigurationsSetup) {
        return CompositeKotlinConfigurationsSetup(listOf(this) + other.setups)
    }

    return CompositeKotlinConfigurationsSetup(listOf(this, other))
}

fun <T : KotlinGradleFragment> KotlinConfigurationsSetup(
    vararg configurators: KotlinConfigurationsSetup<T>
): KotlinConfigurationsSetup<T> {
    return CompositeKotlinConfigurationsSetup(configurators.toList())
}

private class CompositeKotlinConfigurationsSetup<in T : KotlinGradleFragment>(
    val setups: List<KotlinConfigurationsSetup<T>>
) : KotlinConfigurationsSetup<T> {
    override fun configure(fragment: T, configuration: Configuration) {
        setups.forEach { setup -> setup.configure(fragment, configuration) }
    }
}
