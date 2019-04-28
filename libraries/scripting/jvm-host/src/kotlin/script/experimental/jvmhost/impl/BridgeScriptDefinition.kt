/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.impl

import com.intellij.openapi.fileTypes.LanguageFileType
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.scripting.definitions.KotlinScriptDefinition
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.starProjectedType
import kotlin.script.dependencies.ScriptContents
import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.DependenciesResolver
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.getScriptingClass
import kotlin.script.experimental.jvm.impl.BridgeDependenciesResolver
import kotlin.script.experimental.util.getOrError

// A bridge to the current scripting
// mostly copies functionality from KotlinScriptDefinitionAdapterFromNewAPI[Base]
// reusing it requires structural changes that doesn't seem justified now, since the internals of the scripting should be reworked soon anyway
// TODO: either finish refactoring of the scripting internals or reuse KotlinScriptDefinitionAdapterFromNewAPI[Base] here
// NOTE: since KotlinScriptDefinition is not designed to separate static (script definition related) and dynamic (actual script compilation
// configuration) parameters, the implementation is quite hacky, especially for cases as REPL
// TODO: finish refactoring and replace KotlinScriptDefinition with right abstractions
internal class BridgeScriptDefinition(
    val scriptCompilationConfiguration: ScriptCompilationConfiguration,
    val hostConfiguration: ScriptingHostConfiguration,
    dynamicState: BridgeScriptDefinitionDynamicState
) : KotlinScriptDefinition(Any::class) {

    val baseClass: KClass<*> by lazy(LazyThreadSafetyMode.PUBLICATION) {
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

    override val fileExtension: String
        get() = scriptCompilationConfiguration[ScriptCompilationConfiguration.fileExtension] ?: super.fileExtension

    override val acceptedAnnotations = run {
        val cl = this::class.java.classLoader
        scriptCompilationConfiguration[ScriptCompilationConfiguration.refineConfigurationOnAnnotations]?.annotations
            ?.map { (cl.loadClass(it.typeName) as Class<out Annotation>).kotlin }
            ?: emptyList()
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

    override val dependencyResolver: DependenciesResolver =
        BridgeDependenciesResolver(
            scriptCompilationConfiguration,
            dynamicState::updateConfiguration,
            dynamicState::getScriptSource
        )

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

// TODO: consider synchronization, since it is mutable (or finish the refactoring after all)
internal class BridgeScriptDefinitionDynamicState {

    private val _sources = linkedSetOf<SourceCode>()
    private val _configurations = hashMapOf<SourceCode, ScriptCompilationConfiguration>()
    private var _baseScriptCompilationConfiguration: ScriptCompilationConfiguration? = null

    val sources: Set<SourceCode> get() = _sources

    val configurations: Map<SourceCode, ScriptCompilationConfiguration> get() = _configurations

    val baseScriptCompilationConfiguration: ScriptCompilationConfiguration get() = _baseScriptCompilationConfiguration!!

    val mainScript: SourceCode get() = sources.first()

    val mainScriptCompilationConfiguration: ScriptCompilationConfiguration
        get() = configurations[mainScript] ?: baseScriptCompilationConfiguration

    fun configureFor(script: SourceCode, scriptCompilationConfiguration: ScriptCompilationConfiguration) {
        _sources.clear()
        _sources.add(script)
        _configurations.clear()
        _baseScriptCompilationConfiguration = scriptCompilationConfiguration
    }

    fun updateConfiguration(script: SourceCode, updatedConfiguration: ScriptCompilationConfiguration) {
        _configurations[script] = updatedConfiguration
        updatedConfiguration[ScriptCompilationConfiguration.importScripts]?.let {
            _sources.addAll(it)
        }
    }

    fun getScriptSource(scriptContents: ScriptContents): SourceCode? {
        val name = scriptContents.file?.name
        return sources.find {
            // TODO: consider using merged text (likely should be cached)
            // on the other hand it may become obsolete when scripting internals will be redesigned properly
            (name != null && name == it.scriptFileName(
                sources.first(),
                mainScriptCompilationConfiguration
            )) || it.text == scriptContents.text
        }
    }
}