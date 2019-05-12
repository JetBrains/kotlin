/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.definitions

import com.intellij.openapi.fileTypes.LanguageFileType
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.psi.KtScript
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.starProjectedType
import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.DependenciesResolver
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.getScriptingClass
import kotlin.script.experimental.jvm.compat.mapToLegacyExpectedLocations
import kotlin.script.experimental.jvm.impl.BridgeDependenciesResolver
import kotlin.script.experimental.location.ScriptExpectedLocation
import kotlin.script.experimental.util.getOrError

// temporary trick with passing Any as a template and overwriting it below, TODO: fix after introducing new script definitions hierarchy
abstract class KotlinScriptDefinitionAdapterFromNewAPIBase : KotlinScriptDefinition(Any::class) {

    abstract val scriptCompilationConfiguration: ScriptCompilationConfiguration

    abstract val hostConfiguration: ScriptingHostConfiguration

    open val baseClass: KClass<*> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        getScriptingClass(scriptCompilationConfiguration.getOrError(ScriptCompilationConfiguration.baseClass))
    }

    override val template: KClass<*> get() = baseClass

    override val name: String
        get() = scriptCompilationConfiguration[ScriptCompilationConfiguration.displayName] ?: "Kotlin Script"

    override val fileType: LanguageFileType = KotlinFileType.INSTANCE

    override fun isScript(fileName: String): Boolean =
        fileName.endsWith(".$fileExtension")

    override fun getScriptName(script: KtScript): Name {
        val fileBasedName = NameUtils.getScriptNameForFile(script.containingKtFile.name)
        return Name.identifier(fileBasedName.identifier.removeSuffix(".$fileExtension"))
    }

    override val annotationsForSamWithReceivers: List<String>
        get() = emptyList()

    override val dependencyResolver: DependenciesResolver by lazy(LazyThreadSafetyMode.PUBLICATION) {
        BridgeDependenciesResolver(scriptCompilationConfiguration)
    }

    override val acceptedAnnotations: List<KClass<out Annotation>> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        scriptCompilationConfiguration[ScriptCompilationConfiguration.refineConfigurationOnAnnotations]?.annotations
            .orEmpty()
            .map {
                @Suppress("UNCHECKED_CAST")
                getScriptingClass(it) as KClass<out Annotation>
            }
    }

    override val implicitReceivers: List<KType> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        scriptCompilationConfiguration[ScriptCompilationConfiguration.implicitReceivers]
            .orEmpty()
            .map { getScriptingClass(it).starProjectedType }
    }

    override val providedProperties: List<Pair<String, KType>> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        scriptCompilationConfiguration[ScriptCompilationConfiguration.providedProperties]
            ?.map { (k, v) -> k to getScriptingClass(v).starProjectedType }.orEmpty()
    }

    override val additionalCompilerArguments: List<String>
        get() = scriptCompilationConfiguration[ScriptCompilationConfiguration.compilerOptions]
            .orEmpty()

    @Suppress("DEPRECATION")
    override val scriptExpectedLocations: List<ScriptExpectedLocation>
        get() = scriptCompilationConfiguration[ScriptCompilationConfiguration.ide.acceptedLocations]?.mapToLegacyExpectedLocations()
            ?: listOf(ScriptExpectedLocation.SourcesOnly, ScriptExpectedLocation.TestsOnly)

    private val scriptingClassGetter by lazy(LazyThreadSafetyMode.PUBLICATION) {
        hostConfiguration[ScriptingHostConfiguration.getScriptingClass]
            ?: throw IllegalArgumentException("Expecting 'getScriptingClass' property in the scripting environment")
    }

    private fun getScriptingClass(type: KotlinType) =
        scriptingClassGetter(
            type,
            KotlinScriptDefinition::class, // Assuming that the KotlinScriptDefinition class is loaded in the proper classloader
            hostConfiguration
        )
}


class KotlinScriptDefinitionAdapterFromNewAPI(
    override val scriptCompilationConfiguration: ScriptCompilationConfiguration,
    override val hostConfiguration: ScriptingHostConfiguration
) : KotlinScriptDefinitionAdapterFromNewAPIBase() {

    override val name: String get() = scriptCompilationConfiguration[ScriptCompilationConfiguration.displayName] ?: super.name

    override val fileExtension: String
        get() = scriptCompilationConfiguration[ScriptCompilationConfiguration.fileExtension] ?: super.fileExtension
}
