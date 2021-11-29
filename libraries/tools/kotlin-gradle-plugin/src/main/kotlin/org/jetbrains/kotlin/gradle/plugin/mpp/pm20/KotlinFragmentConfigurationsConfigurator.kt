/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.artifacts.Configuration
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

/**
 * Special 'Configurator' for Gradle configurations.
 * Such 'Configurator' depend on the concrete [KotlinGradleFragment] instance in order to run
 * This 'Configurator's can be composed using the [KotlinConfigurationsConfigurator] or [plus] function.
 */
fun interface KotlinFragmentConfigurationsConfigurator<in T : KotlinGradleFragment> {
    fun configure(fragment: T, configuration: Configuration)
}

fun <T : KotlinGradleFragment> KotlinConfigurationsConfigurator(
    vararg configurators: KotlinFragmentConfigurationsConfigurator<T>
): KotlinFragmentConfigurationsConfigurator<T> {
    return CompositeKotlinFragmentConfigurationsConfigurator(configurators.toList())
}

//region Simple default implementations

object KotlinFragmentPlatformAttributesConfigurator : KotlinFragmentConfigurationsConfigurator<KotlinGradleVariant> {
    override fun configure(fragment: KotlinGradleVariant, configuration: Configuration) {
        configuration.attributes.attribute(KotlinPlatformType.attribute, fragment.platformType)
    }
}

object KotlinFragmentConsumerApiUsageAttributesConfigurator : KotlinFragmentConfigurationsConfigurator<KotlinGradleVariant> {
    override fun configure(fragment: KotlinGradleVariant, configuration: Configuration) {
        configuration.attributes.attribute(USAGE_ATTRIBUTE, consumerApiUsage(fragment.project, fragment.platformType))
    }
}

object KotlinFragmentProducerApiUsageAttributesConfigurator : KotlinFragmentConfigurationsConfigurator<KotlinGradleVariant> {
    override fun configure(fragment: KotlinGradleVariant, configuration: Configuration) {
        configuration.attributes.attribute(USAGE_ATTRIBUTE, producerApiUsage(fragment.project, fragment.platformType))
    }
}

object KotlinFragmentConsumerRuntimeUsageAttributesConfigurator : KotlinFragmentConfigurationsConfigurator<KotlinGradleVariant> {
    override fun configure(fragment: KotlinGradleVariant, configuration: Configuration) {
        configuration.attributes.attribute(USAGE_ATTRIBUTE, consumerRuntimeUsage(fragment.project, fragment.platformType))
    }
}

object KotlinFragmentProducerRuntimeUsageAttributesConfigurator : KotlinFragmentConfigurationsConfigurator<KotlinGradleVariant> {
    override fun configure(fragment: KotlinGradleVariant, configuration: Configuration) {
        configuration.attributes.attribute(USAGE_ATTRIBUTE, producerRuntimeUsage(fragment.project, fragment.platformType))
    }
}

object KotlinFragmentMetadataUsageAttributeConfigurator : KotlinFragmentConfigurationsConfigurator<KotlinGradleVariant> {
    override fun configure(fragment: KotlinGradleVariant, configuration: Configuration) {
        configuration.attributes.attribute(USAGE_ATTRIBUTE, fragment.project.usageByName(KotlinUsages.KOTLIN_METADATA))
    }
}

object KonanTargetAttributesConfigurator : KotlinFragmentConfigurationsConfigurator<KotlinNativeVariantInternal> {
    override fun configure(fragment: KotlinNativeVariantInternal, configuration: Configuration) {
        configuration.attributes.attribute(KotlinNativeTarget.konanTargetAttribute, fragment.konanTarget.name)
    }
}

object KotlinFragmentModuleCapabilityConfigurator : KotlinFragmentConfigurationsConfigurator<KotlinGradleVariant> {
    override fun configure(fragment: KotlinGradleVariant, configuration: Configuration) {
        setModuleCapability(configuration, fragment.containingModule)
    }

    internal fun setModuleCapability(configuration: Configuration, module: KotlinGradleModule) {
        if (module.moduleClassifier != null) {
            configuration.outgoing.capability(ComputedCapability.fromModule(module))
        }
    }
}

//endregion

operator fun <T : KotlinGradleFragment> KotlinFragmentConfigurationsConfigurator<T>.plus(
    other: KotlinFragmentConfigurationsConfigurator<T>
): KotlinFragmentConfigurationsConfigurator<T> {
    if (this is CompositeKotlinFragmentConfigurationsConfigurator && other is CompositeKotlinFragmentConfigurationsConfigurator) {
        return CompositeKotlinFragmentConfigurationsConfigurator(this.setups + other.setups)
    }

    if (this is CompositeKotlinFragmentConfigurationsConfigurator) {
        return CompositeKotlinFragmentConfigurationsConfigurator(this.setups + other)
    }

    if (other is CompositeKotlinFragmentConfigurationsConfigurator) {
        return CompositeKotlinFragmentConfigurationsConfigurator(listOf(this) + other.setups)
    }

    return CompositeKotlinFragmentConfigurationsConfigurator(listOf(this, other))
}

private class CompositeKotlinFragmentConfigurationsConfigurator<in T : KotlinGradleFragment>(
    val setups: List<KotlinFragmentConfigurationsConfigurator<T>>
) : KotlinFragmentConfigurationsConfigurator<T> {
    override fun configure(fragment: T, configuration: Configuration) {
        setups.forEach { setup -> setup.configure(fragment, configuration) }
    }
}
