/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.script

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.core.script.ScriptDefinitionSourceAsContributor
import org.jetbrains.kotlin.idea.core.script.loadDefinitionsFromTemplates
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionsFromClasspathDiscoverySource
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.intellij.ScriptDefinitionsProvider
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration

class BridgeScriptDefinitionsContributor(private val project: Project) : ScriptDefinitionSourceAsContributor {
    override val id: String = "BridgeScriptDefinitionsContributor"

    override val definitions: Sequence<ScriptDefinition>
        get() {
            val extensions = Extensions.getArea(project).getExtensionPoint(ScriptDefinitionsProvider.EP_NAME).extensions
            return extensions.asSequence().flatMap { provider ->
                val explicitClasses = provider.getDefinitionClasses().toList()
                val classPath = provider.getDefinitionsClassPath().toList()
                val hostConfiguration = defaultJvmScriptingHostConfiguration
                val explicitDefinitions =
                    if (explicitClasses.isEmpty()) emptySequence()
                    else loadDefinitionsFromTemplates(explicitClasses, classPath, hostConfiguration).asSequence()
                val discoveredDefinitions =
                    if (provider.useDiscovery())
                        ScriptDefinitionsFromClasspathDiscoverySource(
                            classPath,
                            hostConfiguration,
                            ::loggingReporter
                        ).definitions
                    else emptySequence()
                explicitDefinitions + discoveredDefinitions
            }
        }
}

fun loggingReporter(severity: ScriptDiagnostic.Severity, message: String) {
    val log = Logger.getInstance("ScriptDefinitionsProviders")
    when (severity) {
        ScriptDiagnostic.Severity.FATAL,
        ScriptDiagnostic.Severity.ERROR -> log.error(message)
        ScriptDiagnostic.Severity.WARNING,
        ScriptDiagnostic.Severity.INFO -> log.info(message)
    }
}

