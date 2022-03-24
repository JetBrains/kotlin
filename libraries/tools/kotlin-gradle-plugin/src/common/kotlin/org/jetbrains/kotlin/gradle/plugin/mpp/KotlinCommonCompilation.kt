 /*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformCommonOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformCommonOptionsImpl
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.sources.getVisibleSourceSetsFromAssociateCompilations
import org.jetbrains.kotlin.gradle.targets.metadata.isKotlinGranularMetadataEnabled
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon

interface KotlinMetadataCompilation<T : KotlinCommonOptions> : KotlinCompilation<T>

class KotlinCommonCompilation(
    compilationDetails: CompilationDetails<KotlinMultiplatformCommonOptions>
) : AbstractKotlinCompilation<KotlinMultiplatformCommonOptions>(compilationDetails),
    KotlinMetadataCompilation<KotlinMultiplatformCommonOptions> {

    override fun getName() =
        if (compilationDetails is MetadataMappedCompilationDetails) defaultSourceSetName else super.compilationPurpose

    override val compileKotlinTask: KotlinCompileCommon
        get() = super.compileKotlinTask as KotlinCompileCommon

    internal val isKlibCompilation: Boolean
        get() = target.project.isKotlinGranularMetadataEnabled && !forceCompilationToKotlinMetadata

    internal var forceCompilationToKotlinMetadata: Boolean = false
}
