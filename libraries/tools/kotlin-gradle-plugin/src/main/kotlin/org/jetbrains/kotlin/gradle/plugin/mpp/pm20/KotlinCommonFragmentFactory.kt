/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.FragmentNameDisambiguation

typealias KotlinCommonFragmentFactory = KotlinGradleFragmentFactory<KotlinGradleFragmentInternal>

fun KotlinCommonFragmentFactory(module: KotlinGradleModule): KotlinCommonFragmentFactory =
    KotlinCommonFragmentFactory(KotlinCommonFragmentInstantiator(module))

fun KotlinCommonFragmentFactory(
    commonFragmentInstantiator: KotlinCommonFragmentInstantiator,
    commonFragmentConfigurator: KotlinCommonFragmentConfigurator = KotlinCommonFragmentConfigurator()
): KotlinGradleFragmentFactory<KotlinGradleFragmentInternal> = KotlinGradleFragmentFactory(
    fragmentInstantiator = commonFragmentInstantiator,
    fragmentConfigurator = commonFragmentConfigurator
)

class KotlinCommonFragmentInstantiator(
    private val module: KotlinGradleModule,
    private val dependencyConfigurationsFactory: KotlinFragmentDependencyConfigurationsFactory =
        DefaultKotlinFragmentDependencyConfigurationsFactory
) : KotlinGradleFragmentFactory.FragmentInstantiator<KotlinGradleFragmentInternal> {
    override fun create(name: String): KotlinGradleFragmentInternal {
        val names = FragmentNameDisambiguation(module, name)
        return KotlinGradleFragmentInternal(module, name, dependencyConfigurationsFactory.create(module, names))
    }
}

class KotlinCommonFragmentConfigurator(
    private val sourceDirectoriesSetup: KotlinSourceDirectoriesConfigurator<KotlinGradleFragmentInternal> =
        DefaultKotlinSourceDirectoriesConfigurator
) : KotlinGradleFragmentFactory.FragmentConfigurator<KotlinGradleFragmentInternal> {
    override fun configure(fragment: KotlinGradleFragmentInternal) {
        sourceDirectoriesSetup.configure(fragment)
    }
}
