/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources.android

import org.jetbrains.kotlin.gradle.plugin.AndroidGradlePluginVersion
import org.jetbrains.kotlin.gradle.plugin.compareTo
import org.jetbrains.kotlin.gradle.plugin.sources.android.checker.*
import org.jetbrains.kotlin.gradle.plugin.sources.android.configurator.*

internal data class KotlinAndroidSourceSetLayout(
    val name: String,
    val naming: KotlinAndroidSourceSetNaming,
    val sourceSetConfigurator: KotlinAndroidSourceSetConfigurator,
    val checker: KotlinAndroidSourceSetLayoutChecker
) {
    override fun toString(): String = "KotlinAndroidSourceSetLayout: $name"
}

internal val singleTargetAndroidSourceSetLayout = KotlinAndroidSourceSetLayout(
    name = "Kotlin/Android-SourceSetLayout",
    naming = SingleTargetKotlinAndroidSourceSetNaming,
    sourceSetConfigurator = KotlinAndroidSourceSetConfigurator(
        KotlinAndroidSourceSetInfoConfigurator,
        AndroidKaptSourceSetConfigurator,
        GradleConventionAddKotlinSourcesToAndroidSourceSetConfigurator,
        Agp7AddKotlinSourcesToAndroidSourceSetConfigurator
            .onlyIf { AndroidGradlePluginVersion.current >= "7.0.0" },

        SingleTargetSourceDirConfigurator,
    ),
    checker = KotlinAndroidSourceSetLayoutChecker()
)

internal val multiplatformAndroidSourceSetLayoutV1 = KotlinAndroidSourceSetLayout(
    name = "Multiplatform/Android-V1-SourceSetLayout",
    naming = MultiplatformLayoutV1KotlinAndroidSourceSetNaming,
    sourceSetConfigurator = KotlinAndroidSourceSetConfigurator(
        KotlinAndroidSourceSetInfoConfigurator,
        AndroidKaptSourceSetConfigurator,
        GradleConventionAddKotlinSourcesToAndroidSourceSetConfigurator,
        Agp7AddKotlinSourcesToAndroidSourceSetConfigurator
            .onlyIf { AndroidGradlePluginVersion.current >= "7.0.0" },
        MultiplatformAndroidResourceDirConfigurator,
        MultiplatformLayoutV1DependsOnConfigurator,
        MultiplatformLayoutV1SourceDirConfigurator
    ),
    checker = KotlinAndroidSourceSetLayoutChecker(
        MultiplatformLayoutV1DeprecationChecker
    )
)

internal val multiplatformAndroidSourceSetLayoutV2 = KotlinAndroidSourceSetLayout(
    name = "Multiplatform/Android-V2-SourceSetLayout",
    naming = MultiplatformLayoutV2KotlinAndroidSourceSetNaming,
    sourceSetConfigurator = KotlinAndroidSourceSetConfigurator(
        KotlinAndroidSourceSetInfoConfigurator,
        AndroidKaptSourceSetConfigurator,
        MultiplatformAndroidResourceDirConfigurator,
        MultiplatformLayoutV2DependsOnConfigurator,
        Agp7AddKotlinSourcesToAndroidSourceSetConfigurator
            .onlyIf { AndroidGradlePluginVersion.current >= "7.0.0" },
        MultiplatformLayoutV2SourceDirConfigurator,
        MultiplatformLayoutV2DefaultManifestLocationConfigurator
    ),
    checker = KotlinAndroidSourceSetLayoutChecker(
        MultiplatformLayoutV2AgpRequirementChecker,
        MultiplatformLayoutV2AndroidStyleSourceDirUsageChecker,
        MultiplatformLayoutV2MultiplatformLayoutV1StyleSourceDirUsageChecker
    )
)
