/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.internal

import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.utils.getOrPut

internal val DeprecatedMppGradlePropertiesMigrationSetupAction = KotlinProjectSetupAction {
    if (KotlinVersion.CURRENT >= KotlinVersion(2, 1)) {
        logger.warn("Please remove [DeprecatedMppGradlePropertiesMigrationSetupAction] as it is no longer needed in Kotlin 2.1")
    }
    /*
    Since 233+ IDEA even bundled kotlin plugin do not relying on the "kotlin.mpp.enableGranularSourceSetsMetadata" property
    and HMPP enabled in IDE by default. But for previous IDE versions with bundled Kotlin plugin we have the code that
    will disable the HMPP if this property is not set to true.

    To mitigate that we should pass the "kotlin.mpp.enableGranularSourceSetsMetadata=true" for cases where KGP 1.9.20+ trying to
    open in the old IDE. This compatibility trick could be safely removed in 2.1

    Related issue: KT-68022
     */
    project.extensions.extraProperties.getOrPut("kotlin.mpp.enableGranularSourceSetsMetadata") { "true" }

    // Remove Unit when KT-21282 is fixed
    Unit
}