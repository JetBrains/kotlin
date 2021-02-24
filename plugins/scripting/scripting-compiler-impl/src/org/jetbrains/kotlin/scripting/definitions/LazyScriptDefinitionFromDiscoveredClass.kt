/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.definitions

import java.io.File
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.*
import kotlin.script.experimental.jvm.JvmDependency

class LazyScriptDefinitionFromDiscoveredClass internal constructor(
    private val baseHostConfiguration: ScriptingHostConfiguration,
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

    private val definition: kotlin.script.experimental.host.ScriptDefinition by lazy(LazyThreadSafetyMode.PUBLICATION) {
        messageReporter(
            ScriptDiagnostic.Severity.DEBUG,
            "Configure scripting: loading script definition class $className using classpath $classpath\n.  ${Thread.currentThread().stackTrace}"
        )
        try {
            createScriptDefinitionFromTemplate(
                KotlinType(className),
                baseHostConfiguration.with {
                    if (classpath.isNotEmpty()) {
                        configurationDependencies.append(JvmDependency(classpath))
                    }
                },
                LazyScriptDefinitionFromDiscoveredClass::class
            )
        } catch (ex: ClassNotFoundException) {
            messageReporter(ScriptDiagnostic.Severity.ERROR, "Cannot find script definition class $className")
            InvalidScriptDefinition
        } catch (ex: Exception) {
            messageReporter(
                ScriptDiagnostic.Severity.ERROR,
                "Error processing script definition class $className: ${ex.message}\nclasspath:\n${classpath.joinToString("\n", "    ")}"
            )
            InvalidScriptDefinition
        }
    }

    override val hostConfiguration: ScriptingHostConfiguration
        get() = definition.compilationConfiguration[ScriptCompilationConfiguration.hostConfiguration] ?: baseHostConfiguration

    override val compilationConfiguration: ScriptCompilationConfiguration get() = definition.compilationConfiguration
    override val evaluationConfiguration: ScriptEvaluationConfiguration get() = definition.evaluationConfiguration

    override val fileExtension: String by lazy(LazyThreadSafetyMode.PUBLICATION) {
        annotationsFromAsm.find { it.name == KotlinScript::class.java.simpleName }?.args
            ?.find { it.name == "fileExtension" }?.value
            ?: compilationConfiguration.let {
                it[ScriptCompilationConfiguration.fileExtension] ?: super.fileExtension
            }
    }

    override val name: String by lazy(LazyThreadSafetyMode.PUBLICATION) {
        annotationsFromAsm.find { it.name == KotlinScript::class.java.simpleName!! }?.args?.find { it.name == "name" }?.value
            ?: super.name
    }
}

val InvalidScriptDefinition =
    ScriptDefinition(ScriptCompilationConfiguration(), ScriptEvaluationConfiguration())
