/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.extensions

import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.CollectAdditionalSourcesExtension
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.compiler.plugin.dependencies.collectScriptsCompilationDependencies

class ScriptingCollectAdditionalSourcesExtension(val project: MockProject) : CollectAdditionalSourcesExtension {
    override fun collectAdditionalSourcesAndUpdateConfiguration(
        knownSources: Collection<KtFile>,
        configuration: CompilerConfiguration,
        project: Project
    ): Collection<KtFile> {
        val (newSourcesClasspath, newSources, _) = collectScriptsCompilationDependencies(
            configuration,
            project,
            knownSources
        )
        configuration.addJvmClasspathRoots(newSourcesClasspath)
        return newSources
    }
}
