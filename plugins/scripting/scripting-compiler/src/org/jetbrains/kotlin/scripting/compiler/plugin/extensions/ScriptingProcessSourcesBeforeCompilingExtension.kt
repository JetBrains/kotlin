/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.extensions

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.extensions.ProcessSourcesBeforeCompilingExtension
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionProvider
import org.jetbrains.kotlin.scripting.resolve.KtFileScriptSource
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.isStandalone

class ScriptingProcessSourcesBeforeCompilingExtension(val project: Project) : ProcessSourcesBeforeCompilingExtension {

    override fun processSources(sources: Collection<KtFile>, configuration: CompilerConfiguration): Collection<KtFile> {
        val versionSettings = configuration.languageVersionSettings
        val shouldSkipStandaloneScripts = versionSettings.supportsFeature(LanguageFeature.SkipStandaloneScriptsInSourceRoots)
        val definitionProvider by lazy(LazyThreadSafetyMode.NONE) { ScriptDefinitionProvider.getInstance(project) }
        val messageCollector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)

        fun KtFile.isStandaloneScript(): Boolean {
            val scriptDefinition = definitionProvider?.findDefinition(KtFileScriptSource(this))
            return scriptDefinition?.compilationConfiguration?.get(ScriptCompilationConfiguration.isStandalone) ?: true
        }

        if (configuration.getBoolean(CommonConfigurationKeys.ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS)) return sources
        // TODO: see comment at LazyScriptDefinitionProvider.Companion.getNonScriptFilenameSuffixes
        val nonScriptFilenameSuffixes = arrayOf(".${KotlinFileType.EXTENSION}", ".${JavaFileType.DEFAULT_EXTENSION}")
        // filter out scripts that are not suitable for source roots, according to the compiler configuration and script definitions
        return sources.filter { ktFile ->
            when {
                nonScriptFilenameSuffixes.any { ktFile.virtualFilePath.endsWith(it) } -> true
                !ktFile.isStandaloneScript() -> true
                else -> {
                    if (!shouldSkipStandaloneScripts) {
                        messageCollector.report(
                            CompilerMessageSeverity.WARNING,
                            "Script '${ktFile.name}' is not supposed to be used along with regular Kotlin sources, and will be ignored in the future versions by default. (Use -Xallow-any-scripts-in-source-roots command line option to opt-in for the old behavior.)"
                        )
                    }
                    !shouldSkipStandaloneScripts
                }
            }
        }
    }
}