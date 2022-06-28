/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.FragmentNameDisambiguation

typealias GradleKpmCommonFragmentFactory = GradleKpmFragmentFactory<GradleKpmFragmentInternal>

fun GradleKpmCommonFragmentFactory(module: GradleKpmModule): GradleKpmCommonFragmentFactory =
    GradleKpmCommonFragmentFactory(GradleKpmCommonFragmentInstantiator(module))

fun GradleKpmCommonFragmentFactory(
    commonFragmentInstantiator: GradleKpmCommonFragmentInstantiator,
    commonFragmentConfigurator: GradleKpmCommonFragmentConfigurator = GradleKpmCommonFragmentConfigurator()
): GradleKpmFragmentFactory<GradleKpmFragmentInternal> = GradleKpmFragmentFactory(
    fragmentInstantiator = commonFragmentInstantiator,
    fragmentConfigurator = commonFragmentConfigurator
)

class GradleKpmCommonFragmentInstantiator(
    private val module: GradleKpmModule,
    private val dependencyConfigurationsFactory: GradleKpmFragmentDependencyConfigurationsFactory =
        GradleKpmDefaultFragmentDependencyConfigurationsFactory
) : GradleKpmFragmentFactory.FragmentInstantiator<GradleKpmFragmentInternal> {
    override fun create(name: String): GradleKpmFragmentInternal {
        val names = FragmentNameDisambiguation(module, name)
        return module.project.objects.newInstance(
            GradleKpmFragmentInternal::class.java,
            module,
            name,
            dependencyConfigurationsFactory.create(module, names)
        )
    }
}

class GradleKpmCommonFragmentConfigurator(
    private val sourceDirectoriesSetup: GradleKpmSourceDirectoriesConfigurator<GradleKpmFragmentInternal> =
        GradleKpmDefaultSourceDirectoriesConfigurator
) : GradleKpmFragmentFactory.FragmentConfigurator<GradleKpmFragmentInternal> {
    override fun configure(fragment: GradleKpmFragmentInternal) {
        sourceDirectoriesSetup.configure(fragment)
    }
}
