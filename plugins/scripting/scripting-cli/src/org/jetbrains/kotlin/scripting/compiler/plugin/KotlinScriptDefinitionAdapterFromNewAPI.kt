/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import com.intellij.openapi.fileTypes.LanguageFileType
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.script.experimental.api.ScriptCompileConfigurationParams
import kotlin.script.experimental.api.ScriptDefinition
import kotlin.script.experimental.api.ScriptDefinitionProperties
import kotlin.script.experimental.dependencies.DependenciesResolver
import kotlin.script.experimental.jvm.impl.BridgeDependenciesResolver

class KotlinScriptDefinitionAdapterFromNewAPI(val scriptDefinition: ScriptDefinition) :
    KotlinScriptDefinition(scriptDefinition.compilationConfigurator.defaultConfiguration[ScriptCompileConfigurationParams.baseClass]) {

    override val name: String get() = scriptDefinition.properties[ScriptDefinitionProperties.name]

    // TODO: consider creating separate type (subtype? for kotlin scripts)
    override val fileType: LanguageFileType = KotlinFileType.INSTANCE

    override val annotationsForSamWithReceivers: List<String>
        get() = emptyList()

    private val scriptFileExtensionWithDot =
        "." + (scriptDefinition.properties.getOrNull(ScriptDefinitionProperties.fileExtension) ?: "kts")

    override fun isScript(fileName: String): Boolean =
        fileName.endsWith(scriptFileExtensionWithDot)

    override fun getScriptName(script: KtScript): Name {
        val fileBasedName = NameUtils.getScriptNameForFile(script.containingKtFile.name)
        return Name.identifier(fileBasedName.identifier.removeSuffix(scriptFileExtensionWithDot))
    }

    override val dependencyResolver: DependenciesResolver by lazy {
        BridgeDependenciesResolver(scriptDefinition.compilationConfigurator)
    }

    override val acceptedAnnotations: List<KClass<out Annotation>> by lazy {
        scriptDefinition.compilationConfigurator.defaultConfiguration.getOrNull(ScriptCompileConfigurationParams.updateConfigurationOnAnnotations)?.toList()
                ?: emptyList()
    }

    override val implicitReceivers: List<KType> by lazy {
        scriptDefinition.compilationConfigurator.defaultConfiguration.getOrNull(ScriptCompileConfigurationParams.scriptSignature)?.providedDeclarations?.implicitReceivers
                ?: emptyList()
    }
}


