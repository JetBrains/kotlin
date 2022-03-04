/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.dsl.topLevelExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationDefaultSourceSetName
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinCompilationData
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleModule
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleVariant
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.kpmModules
import org.jetbrains.kotlin.gradle.plugin.sources.kpm.FragmentMappedKotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.kpm.SourceSetMappedFragmentLocator
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

/**
 * If this [kotlinCompilationData] is owned by a variant or a fragment's metadata compilation that is represented by a
 * [KotlinCompilation] in the KPM-mapped model, returns the compilation object.
 */
internal fun findCompilationInKpmMappedModel(kotlinCompilationData: KotlinCompilationData<*>): KotlinCompilation<*>? {
    val ext = kotlinCompilationData.project.topLevelExtension as? KotlinMultiplatformExtension
        ?: return null
    ext.targets.forEach { target ->
        target.compilations.forEach { compilation ->
            val compilationDetails = (compilation as? AbstractKotlinCompilation)?.compilationDetails
            if (compilationDetails?.compilationData == kotlinCompilationData)
                return compilation
        }
    }
    return null
}

internal fun <V : KotlinGradleVariant, C : KotlinCompilation<*>> createAndEmbedMappedCompilation(
    target: KotlinTarget,
    compilationName: String,
    createVariant: (module: KotlinGradleModule, variantName: String) -> V,
    compilationFactory: (variant: V) -> C
): C {
    val project = target.project

    // TODO: for now, we assume that any new compilation, like "benchmark" or "integrationTest", goes to a corresponding module;
    //       this may not be the case with multi-variant targets like Android or JS; they won't be able to reuse this code directly if we
    //       extend model mapping to them; however, we don't really want that for now
    val module = target.project.kpmModules.maybeCreate(compilationName)

    // TODO: this way to determine the default source set name is consistent with JVM and Native compilations;
    //       The JS and Metadata compilations override the default source set name and should not be mapped with this function
    //       without an additional change (however, Metadata ones should not be mapped at all; JS ones need a big rework for mapping anyway)
    val fragmentLocation = SourceSetMappedFragmentLocator.get(project)
        .locateFragmentForSourceSet(compilationDefaultSourceSetName(target, compilationName))
        ?: error("Couldn't determine the location to create fragment for compilation $compilationName in $target")

    check(module.name == fragmentLocation.moduleName) {
        "Fragment locator result $fragmentLocation should be consistent with the compilation -> module mapping for $compilationName"
    }

    val preferredVariantName = fragmentLocation.fragmentName
    val backupVariantName = lowerCamelCaseName(preferredVariantName, "Variant")
    val variantName =
        if (module.fragments.findByName(preferredVariantName) == null)
            preferredVariantName
        else backupVariantName // TODO: deprecate such setups and report a warning, or provide proper support (without "Variant" suffix)

    val variant = createVariant(module, variantName).apply {
        if (variantName == backupVariantName) {
            refines(module.fragments.getByName(preferredVariantName))
        }
    }

    // For now, create a new KotlinSourceSet even if the preferred name is taken; this may be an additional source set "fooBarVariant"!
    // TODO: use just the single source set, don't create a new source set; instead, pass the existing source set to compilationFactory
    val defaultSourceSet = FragmentMappedKotlinSourceSet(variant.unambiguousNameInProject, variant)
    project.kotlinExtension.sourceSets.add(defaultSourceSet)

    return compilationFactory(variant)
}