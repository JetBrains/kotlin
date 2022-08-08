/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources.android.checker

import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.sources.android.KotlinAndroidSourceSetLayout
import org.jetbrains.kotlin.gradle.plugin.sources.android.multiplatformAndroidSourceSetLayoutV2
import org.jetbrains.kotlin.gradle.utils.SingleWarningPerBuild

/**
 * Promotes MultiplatformAndroidSourceSetLayoutV2 when requirements are met
 */
internal object MultiplatformLayoutV1PromoteV2Checker : KotlinAndroidSourceSetLayoutChecker {
    private val logger = Logging.getLogger(this.javaClass)

    override fun checkBeforeLayoutApplied(target: KotlinAndroidTarget, layout: KotlinAndroidSourceSetLayout) {
        if (target.project.kotlinPropertiesProvider.ignoreMppAndroidSourceSetLayoutVersion) return
        runCatching {
            if (MultiplatformLayoutV2AgpRequirementChecker.isAgpRequirementMet()) {
                SingleWarningPerBuild.show(
                    target.project, logger,
                    """ 
                        w: ${layout.name} is deprecated. Use ${multiplatformAndroidSourceSetLayoutV2.name} instead. 
                        To enable ${multiplatformAndroidSourceSetLayoutV2.name}: put the following in your gradle.properties: 
                        ${PropertyNames.KOTLIN_MPP_ANDROID_SOURCE_SET_LAYOUT_VERSION}=2
                        
                        To suppress this warning: put the following in your gradle.properties:
                        ${PropertyNames.KOTLIN_MPP_ANDROID_SOURCE_SET_LAYOUT_VERSION_1_NO_WARN}=true
                    """.trimIndent()
                )
            }
        }
    }
}
