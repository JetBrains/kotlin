/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.gradle.internal.prepareCompilerArguments
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmFragment
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.metadataCompilationRegistryByModuleId
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object IdeaKpmCompilerArgumentsResolver {
    fun resolve(fragment: GradleKpmFragment): CommonCompilerArguments? {
        val compileTaskName = fragment.project.metadataCompilationRegistryByModuleId[fragment.containingModule.moduleIdentifier]
            ?.getForFragmentOrNull(fragment)
            ?.compileKotlinTaskName ?: return null
        return fragment.project.tasks.findByName(compileTaskName)
            ?.safeAs<AbstractKotlinCompileTool<CommonCompilerArguments>>()
            ?.prepareCompilerArguments()
    }
}