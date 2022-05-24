/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.FragmentNameDisambiguation

typealias KotlinCommonFragmentFactory = KotlinGradleFragmentFactory<KpmGradleFragmentInternal>

fun KotlinCommonFragmentFactory(module: KpmGradleModule): KotlinCommonFragmentFactory =
    KotlinCommonFragmentFactory(KotlinCommonFragmentInstantiator(module))

fun KotlinCommonFragmentFactory(
    commonFragmentInstantiator: KotlinCommonFragmentInstantiator,
    commonFragmentConfigurator: KotlinCommonFragmentConfigurator = KotlinCommonFragmentConfigurator()
): KotlinGradleFragmentFactory<KpmGradleFragmentInternal> = KotlinGradleFragmentFactory(
    fragmentInstantiator = commonFragmentInstantiator,
    fragmentConfigurator = commonFragmentConfigurator
)

class KotlinCommonFragmentInstantiator(
    private val module: KpmGradleModule,
    private val dependencyConfigurationsFactory: KotlinFragmentDependencyConfigurationsFactory =
        DefaultKotlinFragmentDependencyConfigurationsFactory
) : KotlinGradleFragmentFactory.FragmentInstantiator<KpmGradleFragmentInternal> {
    override fun create(name: String): KpmGradleFragmentInternal {
        val names = FragmentNameDisambiguation(module, name)
        return KpmGradleFragmentInternal(module, name, dependencyConfigurationsFactory.create(module, names))
    }
}

class KotlinCommonFragmentConfigurator(
    private val sourceDirectoriesSetup: KotlinSourceDirectoriesConfigurator<KpmGradleFragmentInternal> =
        DefaultKotlinSourceDirectoriesConfigurator
) : KotlinGradleFragmentFactory.FragmentConfigurator<KpmGradleFragmentInternal> {
    override fun configure(fragment: KpmGradleFragmentInternal) {
        sourceDirectoriesSetup.configure(fragment)
    }
}
