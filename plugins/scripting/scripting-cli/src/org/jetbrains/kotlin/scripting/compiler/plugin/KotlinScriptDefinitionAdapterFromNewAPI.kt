/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import com.intellij.openapi.fileTypes.LanguageFileType
import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import kotlin.reflect.KClass
import kotlin.script.experimental.api.ScriptCompileConfigurationParams
import kotlin.script.experimental.api.ScriptDefinition
import kotlin.script.experimental.api.resultOrNull
import kotlin.script.experimental.dependencies.DependenciesResolver
import kotlin.script.experimental.jvm.impl.BridgeDependenciesResolver

class KotlinScriptDefinitionAdapterFromNewAPI(val scriptDefinition: ScriptDefinition) : KotlinScriptDefinition(scriptDefinition.baseClass) {

    override val name: String get() = scriptDefinition.selector.name

    // TODO: consider creating separate type (subtype? for kotlin scripts)
    override val fileType: LanguageFileType = KotlinFileType.INSTANCE

    override val annotationsForSamWithReceivers: List<String>
        get() = emptyList()

    override fun isScript(fileName: String): Boolean =
        fileName.endsWith("." + scriptDefinition.selector.fileExtension)

    override fun getScriptName(script: KtScript): Name {
        val fileBasedName = NameUtils.getScriptNameForFile(script.containingKtFile.name)
        return Name.identifier(scriptDefinition.selector.makeScriptName(fileBasedName.identifier))
    }

    override val dependencyResolver: DependenciesResolver by lazy {
        BridgeDependenciesResolver(scriptDefinition.configurator)
    }

    override val acceptedAnnotations: List<KClass<out Annotation>> by lazy {
        runBlocking {
            scriptDefinition.configurator.baseConfiguration(null)
        }.resultOrNull()?.getOrNull(ScriptCompileConfigurationParams.updateConfigurationOnAnnotations)?.toList()
        ?: emptyList()
    }
}


