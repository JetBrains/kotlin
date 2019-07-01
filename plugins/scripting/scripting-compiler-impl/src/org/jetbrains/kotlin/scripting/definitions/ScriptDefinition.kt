/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.definitions

import com.intellij.openapi.util.UserDataHolderBase
import org.jetbrains.kotlin.scripting.resolve.KotlinScriptDefinitionFromAnnotatedTemplate
import java.io.File
import kotlin.reflect.KClass
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.createCompilationConfigurationFromTemplate
import kotlin.script.experimental.host.createEvaluationConfigurationFromTemplate
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.jvm

// Transitional class/implementation - migrating to the new API
// TODO: deprecate KotlinScriptDefinition
// TODO: name could be confused with KotlinScriptDefinition, discuss naming
abstract class ScriptDefinition : UserDataHolderBase() {

    abstract val legacyDefinition: KotlinScriptDefinition
    abstract val hostConfiguration: ScriptingHostConfiguration
    abstract val compilationConfiguration: ScriptCompilationConfiguration
    abstract val evaluationConfiguration: ScriptEvaluationConfiguration

    abstract fun isScript(fileName: String): Boolean
    abstract val fileExtension: String
    abstract val name: String
    // TODO: used in settings, find out the reason and refactor accordingly
    abstract val definitionId: String

    abstract val contextClassLoader: ClassLoader?

    // Target platform for script, ex. "JVM", "JS", "NATIVE"
    open val platform: String
        get() = "JVM"

    open val isDefault = false

    abstract val baseClassType: KotlinType
    abstract val compilerOptions: Iterable<String>

    inline fun <reified T : KotlinScriptDefinition> asLegacyOrNull(): T? =
        if (this is FromLegacy) legacyDefinition as? T else null

    open class FromLegacy(
        override val hostConfiguration: ScriptingHostConfiguration,
        override val legacyDefinition: KotlinScriptDefinition
    ) : ScriptDefinition() {

        override val compilationConfiguration by lazy {
            ScriptCompilationConfigurationFromDefinition(
                hostConfiguration,
                legacyDefinition
            )
        }

        override val evaluationConfiguration by lazy {
            ScriptEvaluationConfigurationFromDefinition(
                hostConfiguration,
                legacyDefinition
            )
        }

        override fun isScript(fileName: String): Boolean = legacyDefinition.isScript(fileName)

        override val fileExtension: String get() = legacyDefinition.fileExtension

        override val name: String get() = legacyDefinition.name

        override val definitionId: String get() = legacyDefinition::class.qualifiedName ?: "unknown"

        override val platform: String
            get() = legacyDefinition.platform

        override val contextClassLoader: ClassLoader?
            get() = legacyDefinition.template.java.classLoader

        override val baseClassType: KotlinType
            get() = KotlinType(legacyDefinition.template)

        override val compilerOptions: Iterable<String>
            get() = legacyDefinition.additionalCompilerArguments ?: emptyList()

        override fun equals(other: Any?): Boolean = this === other || legacyDefinition == (other as? FromLegacy)?.legacyDefinition

        override fun hashCode(): Int = legacyDefinition.hashCode()
    }

    open class FromLegacyTemplate(
        hostConfiguration: ScriptingHostConfiguration,
        template: KClass<*>,
        templateClasspath: List<File> = emptyList()
    ) : FromLegacy(
        hostConfiguration,
        KotlinScriptDefinitionFromAnnotatedTemplate(
            template,
            hostConfiguration[ScriptingHostConfiguration.getEnvironment]?.invoke(),
            templateClasspath
        )
    )

    abstract class FromConfigurationsBase : ScriptDefinition() {

        override val legacyDefinition by lazy {
            KotlinScriptDefinitionAdapterFromNewAPI(
                compilationConfiguration,
                hostConfiguration
            )
        }

        override fun isScript(fileName: String): Boolean = fileName.endsWith(".$fileExtension")

        override val fileExtension: String get() = compilationConfiguration[ScriptCompilationConfiguration.fileExtension]!!

        override val name: String get() = compilationConfiguration[ScriptCompilationConfiguration.displayName]!!

        override val definitionId: String get() = compilationConfiguration[ScriptCompilationConfiguration.baseClass]!!.typeName

        override val contextClassLoader: ClassLoader? by lazy {
            compilationConfiguration[ScriptCompilationConfiguration.baseClass]?.fromClass?.java?.classLoader
                ?: hostConfiguration[ScriptingHostConfiguration.jvm.baseClassLoader]
        }

        override val baseClassType: KotlinType
            get() = compilationConfiguration[ScriptCompilationConfiguration.baseClass]!!

        override val compilerOptions: Iterable<String>
            get() = compilationConfiguration[ScriptCompilationConfiguration.compilerOptions].orEmpty()

        override fun equals(other: Any?): Boolean = this === other ||
                (other as? FromConfigurations)?.let {
                    compilationConfiguration == it.compilationConfiguration && evaluationConfiguration == it.evaluationConfiguration
                } == true

        override fun hashCode(): Int = compilationConfiguration.hashCode() + 37 * evaluationConfiguration.hashCode()
    }

    open class FromConfigurations(
        override val hostConfiguration: ScriptingHostConfiguration,
        override val compilationConfiguration: ScriptCompilationConfiguration,
        override val evaluationConfiguration: ScriptEvaluationConfiguration
    ) : FromConfigurationsBase()

    open class FromTemplate(
        hostConfiguration: ScriptingHostConfiguration,
        template: KClass<*>,
        contextClass: KClass<*> = ScriptCompilationConfiguration::class
    ) : FromConfigurations(
        hostConfiguration,
        createCompilationConfigurationFromTemplate(KotlinType(template), hostConfiguration, contextClass),
        createEvaluationConfigurationFromTemplate(KotlinType(template), hostConfiguration, contextClass)
    )

    companion object {
        fun getDefault(hostConfiguration: ScriptingHostConfiguration) =
            object : FromLegacy(hostConfiguration, StandardScriptDefinition) {
                override val isDefault = true
            }
    }
}