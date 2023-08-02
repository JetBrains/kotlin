/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import com.android.build.gradle.api.BaseVariant
import org.jetbrains.kotlin.gradle.plugin.hierarchy.KotlinSourceSetTreeClassifier
import org.jetbrains.kotlin.gradle.plugin.hierarchy.KotlinSourceSetTreeClassifier.Property
import org.jetbrains.kotlin.gradle.plugin.hierarchy.sourceSetTreeClassifier
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.DefaultKotlinCompilationFriendPathsResolver
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinAndroidCompilationAssociator
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory.AndroidCompilationSourceSetsContainerFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory.KotlinCompilationImplFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory.KotlinJvmCompilerOptionsFactory
import org.jetbrains.kotlin.gradle.plugin.sources.android.AndroidVariantType
import org.jetbrains.kotlin.gradle.plugin.sources.android.type

class KotlinJvmAndroidCompilationFactory internal constructor(
    override val target: KotlinAndroidTarget,
    private val variant: BaseVariant,
) : KotlinCompilationFactory<KotlinJvmAndroidCompilation> {

    override val itemClass: Class<KotlinJvmAndroidCompilation>
        get() = KotlinJvmAndroidCompilation::class.java

    private val compilationImplFactory: KotlinCompilationImplFactory = KotlinCompilationImplFactory(
        compilerOptionsFactory = KotlinJvmCompilerOptionsFactory,
        compilationFriendPathsResolver = DefaultKotlinCompilationFriendPathsResolver(
            friendArtifactResolver = DefaultKotlinCompilationFriendPathsResolver.FriendArtifactResolver.composite(
                DefaultKotlinCompilationFriendPathsResolver.DefaultFriendArtifactResolver,
                DefaultKotlinCompilationFriendPathsResolver.AdditionalAndroidFriendArtifactResolver
            )
        ),
        compilationAssociator = KotlinAndroidCompilationAssociator,
        compilationSourceSetsContainerFactory = AndroidCompilationSourceSetsContainerFactory(target, variant),
    )

    override fun create(name: String): KotlinJvmAndroidCompilation {
        return project.objects.newInstance(itemClass, compilationImplFactory.create(target, name), variant).also { compilation ->
            configureSourceSetTreeClassifier(compilation)
        }
    }

    private fun configureSourceSetTreeClassifier(compilation: KotlinJvmAndroidCompilation) {
        compilation.sourceSetTreeClassifier = when (variant.type) {
            AndroidVariantType.Main -> Property(target.mainVariant.sourceSetTree)
            AndroidVariantType.UnitTest -> Property(target.unitTestVariant.sourceSetTree)
            AndroidVariantType.InstrumentedTest -> Property(target.instrumentedTestVariant.sourceSetTree)
            AndroidVariantType.Unknown -> KotlinSourceSetTreeClassifier.None
        }
    }
}
