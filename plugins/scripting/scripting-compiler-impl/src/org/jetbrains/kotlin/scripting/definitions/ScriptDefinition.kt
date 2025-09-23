/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.definitions

import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.util.io.FileUtilRt
import kotlin.reflect.KClass
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.createScriptDefinitionFromTemplate
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.jvm
import kotlin.script.templates.standard.ScriptTemplateWithArgs

// Transitional class/implementation - migrating to the new API
// TODO: name could be confused with KotlinScriptDefinition, discuss naming
abstract class ScriptDefinition : UserDataHolderBase() {

    @Suppress("DEPRECATION")
    @Deprecated("Use configurations instead")
    abstract val hostConfiguration: ScriptingHostConfiguration
    abstract val compilationConfiguration: ScriptCompilationConfiguration
    abstract val evaluationConfiguration: ScriptEvaluationConfiguration?

    abstract fun isScript(script: SourceCode): Boolean
    abstract val fileExtension: String
    abstract val name: String
    open val defaultClassName: String = "Script"

    // TODO: used in settings, find out the reason and refactor accordingly
    abstract val definitionId: String

    abstract val contextClassLoader: ClassLoader?

    // Target platform for script, ex. "JVM", "JS", "NATIVE"
    open val platform: String
        get() = "JVM"

    open val isDefault = false

    // Store IDE-related settings in script definition
    var order: Int = Integer.MAX_VALUE
    open val canAutoReloadScriptConfigurationsBeSwitchedOff: Boolean get() = true
    open val canDefinitionBeSwitchedOff: Boolean get() = true

    abstract val baseClassType: KotlinType
    abstract val compilerOptions: Iterable<String>
    abstract val annotationsForSamWithReceivers: List<String>

    override fun toString(): String {
        return "ScriptDefinition($name)"
    }

    abstract class FromConfigurationsBase : ScriptDefinition() {
        val filePathPattern by lazy {
            compilationConfiguration[ScriptCompilationConfiguration.filePathPattern]?.takeIf { it.isNotBlank() }
        }
        val fileNamePattern by lazy {
            @Suppress("DEPRECATION_ERROR")
            compilationConfiguration[ScriptCompilationConfiguration.fileNamePattern]?.takeIf { it.isNotBlank() }
        }

        override fun isScript(script: SourceCode): Boolean {
            val extension = ".$fileExtension"
            val location = script.locationId ?: return false
            val systemIndependentName = FileUtilRt.toSystemIndependentName(location)

            if (script.name?.endsWith(extension) != true && !location.endsWith(extension)) return false

            if (filePathPattern != null) return Regex(filePathPattern!!).matches(systemIndependentName)
            if (fileNamePattern != null) return Regex(fileNamePattern!!).matches(systemIndependentName.substringAfterLast('/'))

            return true
        }

        override val fileExtension: String get() = compilationConfiguration[ScriptCompilationConfiguration.fileExtension]!!

        override val name: String
            get() =
                compilationConfiguration[ScriptCompilationConfiguration.displayName]?.takeIf { it.isNotBlank() }
                    ?: compilationConfiguration[ScriptCompilationConfiguration.baseClass]!!.typeName.substringAfterLast('.')

        override val defaultClassName: String
            get() = compilationConfiguration[ScriptCompilationConfiguration.defaultIdentifier] ?: super.defaultClassName

        override val definitionId: String get() = compilationConfiguration[ScriptCompilationConfiguration.baseClass]!!.typeName

        override val contextClassLoader: ClassLoader? by lazy {
            compilationConfiguration[ScriptCompilationConfiguration.baseClass]?.fromClass?.java?.classLoader
                ?: hostConfiguration[ScriptingHostConfiguration.jvm.baseClassLoader]
        }

        override val platform: String
            get() = compilationConfiguration[ScriptCompilationConfiguration.platform] ?: super.platform

        override val baseClassType: KotlinType
            get() = compilationConfiguration[ScriptCompilationConfiguration.baseClass]!!

        override val compilerOptions: Iterable<String>
            get() = compilationConfiguration[ScriptCompilationConfiguration.compilerOptions].orEmpty()

        override val annotationsForSamWithReceivers: List<String>
            get() = compilationConfiguration[ScriptCompilationConfiguration.annotationsForSamWithReceivers].orEmpty().map { it.typeName }

        override fun equals(other: Any?): Boolean = this === other ||
                (other as? FromConfigurationsBase)?.let {
                    compilationConfiguration == it.compilationConfiguration && evaluationConfiguration == it.evaluationConfiguration
                } == true

        override fun hashCode(): Int = compilationConfiguration.hashCode() + 37 * (evaluationConfiguration?.hashCode() ?: 0)
    }

    open class FromConfigurations(
        override val hostConfiguration: ScriptingHostConfiguration,
        override val compilationConfiguration: ScriptCompilationConfiguration,
        override val evaluationConfiguration: ScriptEvaluationConfiguration?,
    ) : FromConfigurationsBase()

    open class FromNewDefinition(
        private val baseHostConfiguration: ScriptingHostConfiguration,
        private val definition: kotlin.script.experimental.host.ScriptDefinition,
    ) : FromConfigurationsBase() {
        override val hostConfiguration: ScriptingHostConfiguration
            get() = definition.compilationConfiguration[ScriptCompilationConfiguration.hostConfiguration] ?: baseHostConfiguration

        override val compilationConfiguration: ScriptCompilationConfiguration get() = definition.compilationConfiguration
        override val evaluationConfiguration: ScriptEvaluationConfiguration get() = definition.evaluationConfiguration
    }

    open class FromTemplate(
        baseHostConfiguration: ScriptingHostConfiguration,
        template: KClass<*>,
        contextClass: KClass<*> = ScriptCompilationConfiguration::class,
    ) : FromNewDefinition(
        baseHostConfiguration,
        createScriptDefinitionFromTemplate(KotlinType(template), baseHostConfiguration, contextClass),
    )

    companion object {
        fun getDefault(hostConfiguration: ScriptingHostConfiguration) =
            object : FromTemplate(hostConfiguration, ScriptTemplateWithArgs::class) {
                override val isDefault = true
            }
    }
}
