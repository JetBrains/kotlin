/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources.android

import org.jetbrains.kotlin.gradle.plugin.sources.android.configurator.*

internal data class KotlinAndroidSourceSetLayout(
    val name: String,
    val naming: KotlinAndroidSourceSetNaming,
    val sourceSetConfigurator: KotlinAndroidSourceSetConfigurator
) {
    override fun toString(): String = "KotlinAndroidSourceSetLayout: $name"
}

internal val singleTargetAndroidSourceSetLayout = KotlinAndroidSourceSetLayout(
    name = "Kotlin/Android",
    naming = SingleTargetKotlinAndroidSourceSetNaming,
    sourceSetConfigurator = KotlinAndroidSourceSetConfigurator(
        KotlinAndroidSourceSetInfoConfigurator,
        AndroidKaptSourceSetConfigurator,
        AndroidSourceSetConventionConfigurator,
        SingleTargetSourceDirConfigurator,
    )
)

internal val multiplatformAndroidSourceSetLayoutV1 = KotlinAndroidSourceSetLayout(
    name = "Multiplatform/Android V1",
    naming = MultiplatformLayoutV1KotlinAndroidSourceSetNaming,
    sourceSetConfigurator = KotlinAndroidSourceSetConfigurator(
        KotlinAndroidSourceSetInfoConfigurator,
        AndroidKaptSourceSetConfigurator,
        AndroidSourceSetConventionConfigurator,
        MultiplatformAndroidResourceDirConfigurator,
        MultiplatformLayoutV1DependsOnConfigurator,
        MultiplatformLayoutV1SourceDirConfigurator
    )
)

internal val multiplatformAndroidSourceSetLayoutV2 = KotlinAndroidSourceSetLayout(
    name = "Multiplatform/Android V2",
    naming = MultiplatformLayoutV2KotlinAndroidSourceSetNaming,
    sourceSetConfigurator = KotlinAndroidSourceSetConfigurator(
        KotlinAndroidSourceSetInfoConfigurator,
        AndroidKaptSourceSetConfigurator,
        MultiplatformAndroidResourceDirConfigurator,
        MultiplatformLayoutV2DependsOnConfigurator,
        MultiplatformLayoutV2SourceDirConfigurator,
        MultiplatformLayoutV2DefaultManifestLocationConfigurator
    )
)
