/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import java.io.File
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.configurationDependencies
import kotlin.script.experimental.host.createCompilationConfigurationFromTemplate
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration

class LazyScriptDefinitionFromDiscoveredClass internal constructor(
    private val annotationsFromAsm: ArrayList<BinAnnData>,
    private val className: String,
    private val classpath: List<File>,
    private val messageCollector: MessageCollector
) : KotlinScriptDefinitionAdapterFromNewAPIBase() {

    constructor(
        classBytes: ByteArray,
        className: String,
        classpath: List<File>,
        messageCollector: MessageCollector
    ) : this(loadAnnotationsFromClass(classBytes), className, classpath, messageCollector)

    override val hostConfiguration: ScriptingHostConfiguration by lazy(LazyThreadSafetyMode.PUBLICATION) {
        ScriptingHostConfiguration(defaultJvmScriptingHostConfiguration) {
            configurationDependencies.append(JvmDependency(classpath))
        }
    }

    override val scriptCompilationConfiguration: ScriptCompilationConfiguration by lazy(LazyThreadSafetyMode.PUBLICATION) {
        messageCollector.report(
            CompilerMessageSeverity.LOGGING,
            "Configure scripting: loading script definition class $className using classpath $classpath\n.  ${Thread.currentThread().stackTrace}"
        )
        try {
            createCompilationConfigurationFromTemplate(
                KotlinType(className),
                hostConfiguration,
                LazyScriptDefinitionFromDiscoveredClass::class
            )
        } catch (ex: ClassNotFoundException) {
            messageCollector.report(CompilerMessageSeverity.ERROR, "Cannot find script definition class $className")
            InvalidScriptDefinition
        } catch (ex: Exception) {
            messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "Error processing script definition class $className: ${ex.message}"
            )
            InvalidScriptDefinition
        }
    }

    override val fileExtension: String by lazy(LazyThreadSafetyMode.PUBLICATION) {
        annotationsFromAsm.find { it.name == KotlinScript::class.simpleName }?.args
            ?.find { it.name == "extension" }?.value
            ?: scriptCompilationConfiguration.let {
                it[ScriptCompilationConfiguration.fileExtension] ?: super.fileExtension
            }
    }

    override val name: String by lazy(LazyThreadSafetyMode.PUBLICATION) {
        annotationsFromAsm.find { it.name == KotlinScript::class.simpleName!! }?.args?.find { it.name == "name" }?.value
            ?: super.name
    }
}

val InvalidScriptDefinition = ScriptCompilationConfiguration()
