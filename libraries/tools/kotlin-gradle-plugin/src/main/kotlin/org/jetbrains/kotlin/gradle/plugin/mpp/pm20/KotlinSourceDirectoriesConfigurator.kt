/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import java.io.File

interface KotlinSourceDirectoriesConfigurator<in T : KotlinGradleFragment>: KotlinGradleFragmentFactory.FragmentConfigurator<T>

object DefaultKotlinSourceDirectoriesConfigurator : KotlinSourceDirectoriesConfigurator<KotlinGradleFragment> {
    override fun configure(fragment: KotlinGradleFragment) {
        fragment.kotlinSourceRoots.srcDir(
            defaultSourceFolder(
                project = fragment.project,
                moduleName = fragment.containingModule.name,
                fragmentName = fragment.fragmentName,
                type = "kotlin"
            )
        )
    }

    fun defaultSourceFolder(project: Project, moduleName: String, fragmentName: String, type: String): File {
        return project.file("src/${lowerCamelCaseName(fragmentName, moduleName)}/$type")
    }
}
