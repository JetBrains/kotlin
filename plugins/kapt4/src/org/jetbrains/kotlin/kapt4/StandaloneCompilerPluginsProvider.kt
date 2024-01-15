/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt4

import org.jetbrains.kotlin.analysis.project.structure.KtCompilerPluginsProvider
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.extensions.internal.InternalNonStableExtensionPoints
import org.jetbrains.kotlin.resolve.extensions.AssignResolutionAltererExtension

internal class StandaloneCompilerPluginsProvider : KtCompilerPluginsProvider() {
    override fun <T : Any> getRegisteredExtensions(module: KtSourceModule, extensionType: ProjectExtensionDescriptor<T>): List<T> {
        return extensionType.getInstances(module.project)
    }

    @OptIn(InternalNonStableExtensionPoints::class)
    override fun isPluginOfTypeRegistered(module: KtSourceModule, pluginType: CompilerPluginType): Boolean {
        val extension = when (pluginType) {
            CompilerPluginType.ASSIGNMENT -> AssignResolutionAltererExtension
            else -> return false
        }
        return extension.getInstances(module.project).isNotEmpty()
    }
}