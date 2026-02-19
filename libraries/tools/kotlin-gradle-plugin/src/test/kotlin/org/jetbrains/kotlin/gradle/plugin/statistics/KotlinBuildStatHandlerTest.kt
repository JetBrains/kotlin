/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics

import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.statistics.metrics.StringAnonymizationPolicy
import org.jetbrains.kotlin.statistics.metrics.StringMetrics
import kotlin.test.Test

class KotlinBuildStatHandlerTest {

    @Test // Checks that all KonanTarget names are presented in MPP_PLATFORMS statistic's report validator
    fun mppPlatformsShouldContainAllKonanTargetsTest() {
        val regex = Regex(StringMetrics.MPP_PLATFORMS.anonymization.validationRegexp())

        val konanTargetsMissedInMppPlatforms = KonanTarget::class.sealedSubclasses
            .mapNotNull { sealedClass -> sealedClass.objectInstance }
            .filter { sealedClass -> !regex.matches(sealedClass.name) }

        assert(konanTargetsMissedInMppPlatforms.isEmpty()) {
            "There are platforms $konanTargetsMissedInMppPlatforms which are not presented in MPP_PLATFORMS regex"
        }
    }

    @Test // Checks that all KotlinPlatformType names are presented in MPP_PLATFORMS statistic's report validator
    fun mppPlatformsShouldContainAllKotlinPlatformTypeTest() {
        val regex = Regex(StringMetrics.MPP_PLATFORMS.anonymization.validationRegexp())

        val kotlinPlatformTypesMissedInMppPlatforms = KotlinPlatformType.values()
            .map { platformType -> platformType.name }
            .filter { platformTypeName -> !regex.matches(platformTypeName) }

        assert(kotlinPlatformTypesMissedInMppPlatforms.isEmpty()) {
            "There are platform types $kotlinPlatformTypesMissedInMppPlatforms which are not presented in MPP_PLATFORMS regex"
        }
    }


    @Test // Checks that only values listed in KotlinPlatformType and KonanTarget are included in MPP_PLATFORMS
    fun mppPlatformsShouldContainOnlyKonanTargetsAndKotlinPlatformTypeTest() {
        val allowedMppValues =
            (StringMetrics.MPP_PLATFORMS.anonymization as StringAnonymizationPolicy.AllowedListAnonymizer)
                .allowedValues

        val kotlinPlatformTypesMissedInMppPlatforms = KotlinPlatformType.values()
            .map { platformType -> platformType.name }

        val konanTargetsMissedInMppPlatforms = KonanTarget::class.sealedSubclasses
            .mapNotNull { sealedClass -> sealedClass.objectInstance }
            .map { koltinTarget -> koltinTarget.name }


        val extraValues = allowedMppValues - kotlinPlatformTypesMissedInMppPlatforms - konanTargetsMissedInMppPlatforms
        assert(extraValues.isEmpty()) {
            "There are platforms $extraValues which are presented in MPP_PLATFORMS regex," +
                    " but they are presented neither in konan targets nor in kotlin platform types"
        }
    }
}