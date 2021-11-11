/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources.kpm

import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

class FragmentMappedKotlinSourceSetFactory constructor(private val project: Project) :
    NamedDomainObjectFactory<KotlinSourceSet> {

    override fun create(name: String): FragmentMappedKotlinSourceSet {
        val locator = SourceSetMappedFragmentLocator.get(project)
        val location = locator.locateFragmentForSourceSet(project, name) ?: error("Couldn't map the source set name $name to KPM fragment")
        val (moduleName, fragmentName) = location
        val modules = project.multiplatformExtension.kpmModules
        val fragment = modules.maybeCreate(moduleName).fragments.maybeCreate(fragmentName)
        /** TODO setup JS-specific attributes similar to [org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSetFactory]*/
        return FragmentMappedKotlinSourceSet(project, fragment)
    }
}