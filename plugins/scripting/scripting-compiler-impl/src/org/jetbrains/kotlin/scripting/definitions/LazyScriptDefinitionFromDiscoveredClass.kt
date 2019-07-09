/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.definitions

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import java.io.File
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.KotlinType
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.fileExtension
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.configurationDependencies
import kotlin.script.experimental.host.createCompilationConfigurationFromTemplate
import kotlin.script.experimental.host.createEvaluationConfigurationFromTemplate
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration

class LazyScriptDefinitionFromDiscoveredClass internal constructor(
    baseHostConfiguration: ScriptingHostConfiguration,
    private val annotationsFromAsm: ArrayList<BinAnnData>,
    private val className: String,
    private val classpath: List<File>,
    private val messageReporter: MessageReporter
) : ScriptDefinition.FromConfigurationsBase() {

    constructor(
        baseHostConfiguration: ScriptingHostConfiguration,
        classBytes: ByteArray,
        className: String,
        classpath: List<File>,
        messageReporter: MessageReporter
    ) : this(baseHostConfiguration, loadAnnotationsFromClass(classBytes), className, classpath, messageReporter)

    override val hostConfiguration: ScriptingHostConfiguration by lazy(LazyThreadSafetyMode.PUBLICATION) {
        ScriptingHostConfiguration(baseHostConfiguration) {
            configurationDependencies.append(JvmDependency(classpath))
        }
    }

    private val configurations by lazy(LazyThreadSafetyMode.PUBLICATION) {
        messageReporter(
            CompilerMessageSeverity.LOGGING,
            "Configure scripting: loading script definition class $className using classpath $classpath\n.  ${Thread.currentThread().stackTrace}"
        )
        try {
            val compileCfg =
                createCompilationConfigurationFromTemplate(
                    KotlinType(className),
                    hostConfiguration,
                    LazyScriptDefinitionFromDiscoveredClass::class
                )
            val evalCfg =
                createEvaluationConfigurationFromTemplate(
                    KotlinType(className),
                    hostConfiguration,
                    LazyScriptDefinitionFromDiscoveredClass::class
                )
            compileCfg to evalCfg
        } catch (ex: ClassNotFoundException) {
            messageReporter(CompilerMessageSeverity.ERROR, "Cannot find script definition class $className")
            InvalidScriptDefinition
        } catch (ex: Exception) {
            messageReporter(
                CompilerMessageSeverity.ERROR,
                "Error processing script definition class $className: ${ex.message}\nclasspath:\n${classpath.joinToString("\n", "    ")}"
            )
            InvalidScriptDefinition
        }
    }

    override val compilationConfiguration: ScriptCompilationConfiguration get() = configurations.first
    override val evaluationConfiguration: ScriptEvaluationConfiguration get() = configurations.second

    override val fileExtension: String by lazy(LazyThreadSafetyMode.PUBLICATION) {
        annotationsFromAsm.find { it.name == KotlinScript::class.simpleName }?.args
            ?.find { it.name == "fileExtension" }?.value
            ?: compilationConfiguration.let {
                it[ScriptCompilationConfiguration.fileExtension] ?: super.fileExtension
            }
    }

    override val name: String by lazy(LazyThreadSafetyMode.PUBLICATION) {
        annotationsFromAsm.find { it.name == KotlinScript::class.simpleName!! }?.args?.find { it.name == "name" }?.value
            ?: super.name
    }
}

val InvalidScriptDefinition = ScriptCompilationConfiguration() to ScriptEvaluationConfiguration()
