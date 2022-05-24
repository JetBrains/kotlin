/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.api.attributes.java.TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages.consumerApiUsage
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages.consumerRuntimeUsage
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages.producerApiUsage
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages.producerRuntimeUsage
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.utils.isGradleVersionAtLeast

val KotlinFragmentPlatformAttributes = FragmentAttributes<KpmGradleVariant> {
    if (isGradleVersionAtLeast(7, 0) && fragment.platformType == KotlinPlatformType.jvm) {
        namedAttribute(TARGET_JVM_ENVIRONMENT_ATTRIBUTE, TargetJvmEnvironment.STANDARD_JVM)
    }

    attribute(KotlinPlatformType.attribute, fragment.platformType)
}

val KotlinFragmentConsumerApiUsageAttribute = FragmentAttributes<KpmGradleVariant> {
    attribute(USAGE_ATTRIBUTE, consumerApiUsage(project, fragment.platformType))
}

val KotlinFragmentProducerApiUsageAttribute = FragmentAttributes<KpmGradleVariant> {
    attribute(USAGE_ATTRIBUTE, producerApiUsage(fragment.project, fragment.platformType))
}

val KotlinFragmentConsumerRuntimeUsageAttribute = FragmentAttributes<KpmGradleVariant> {
    attribute(USAGE_ATTRIBUTE, consumerRuntimeUsage(fragment.project, fragment.platformType))
}

val KotlinFragmentProducerRuntimeUsageAttribute = FragmentAttributes<KpmGradleVariant> {
    attribute(USAGE_ATTRIBUTE, producerRuntimeUsage(fragment.project, fragment.platformType))
}

val KotlinFragmentMetadataUsageAttribute = FragmentAttributes<KpmGradleFragment> {
    attribute(USAGE_ATTRIBUTE, fragment.project.usageByName(KotlinUsages.KOTLIN_METADATA))
}

val KotlinFragmentKonanTargetAttribute = FragmentAttributes<KpmNativeVariantInternal> {
    attributes.attribute(KotlinNativeTarget.konanTargetAttribute, fragment.konanTarget.name)
}
