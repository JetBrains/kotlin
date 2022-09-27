/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import java.io.File

interface GradleKpmSourceDirectoriesConfigurator<in T : GradleKpmFragment>: GradleKpmFragmentFactory.FragmentConfigurator<T>

object GradleKpmDefaultSourceDirectoriesConfigurator : GradleKpmSourceDirectoriesConfigurator<GradleKpmFragment> {
    override fun configure(fragment: GradleKpmFragment) {
        fragment.kotlinSourceRoots.srcDir(
            fragment.project.provider {
                defaultSourceFolder(
                    fragment,
                    type = "kotlin"
                )
            }
        )
    }

    private fun fragmentDirectoryName(fragment: GradleKpmFragment): String {
        return lowerCamelCaseName(fragment.name, fragment.containingModule.name)
    }

    fun defaultSourceFolder(fragment: GradleKpmFragment, type: String): File {
        val fragmentDirectoryName = fragmentDirectoryName(fragment)
        return fragment.project.file("src/$fragmentDirectoryName/$type")
    }
}
