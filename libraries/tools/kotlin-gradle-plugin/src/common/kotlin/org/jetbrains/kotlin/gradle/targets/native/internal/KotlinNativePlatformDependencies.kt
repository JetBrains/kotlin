/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.ide.ideaImportDependsOn
import org.jetbrains.kotlin.gradle.targets.metadata.isKotlinGranularMetadataEnabled

/**
 * Function signature needs to be kept stable since this is used during import
 * in IDEs (KotlinCommonizerModelBuilder) < 222
 *
 * IDEs >= will use the [ideaImportDependsOn] infrastructure
 */
@JvmName("isAllowCommonizer")
internal fun Project.isAllowCommonizer(): Boolean {
    assert(state.executed) { "'isAllowCommonizer' can only be called after project evaluation" }
    multiplatformExtensionOrNull ?: return false

    return multiplatformExtension.targets.any { it.platformType == KotlinPlatformType.native }
            && isKotlinGranularMetadataEnabled
}
