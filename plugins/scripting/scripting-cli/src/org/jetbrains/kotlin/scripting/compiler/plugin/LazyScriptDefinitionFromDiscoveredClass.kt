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
import kotlin.script.experimental.annotations.KotlinScriptFileExtension
import kotlin.script.experimental.api.*
import kotlin.script.experimental.definitions.ScriptDefinitionFromAnnotatedBaseClass
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.JvmGetScriptingClass

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

    override val scriptDefinition: ScriptDefinition by lazy {
        messageCollector.report(
            CompilerMessageSeverity.LOGGING,
            "Configure scripting: loading script definition class $className using classpath $classpath\n.  ${Thread.currentThread().stackTrace}"
        )
        try {
            ScriptDefinitionFromAnnotatedBaseClass(
                ScriptingEnvironment(
                    ScriptingEnvironmentProperties.baseClass to KotlinType(className),
                    ScriptingEnvironmentProperties.configurationDependencies to listOf(JvmDependency(classpath)),
                    ScriptingEnvironmentProperties.getScriptingClass to JvmGetScriptingClass()
                )
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

    override val scriptFileExtensionWithDot: String by lazy {
        val ext = annotationsFromAsm.find { it.name == KotlinScriptFileExtension::class.simpleName!! }?.args?.first()
                ?: scriptDefinition.properties.let {
                    it.getOrNull(ScriptDefinitionProperties.fileExtension) ?: "kts"
                }
        ".$ext"
    }

    override val name: String by lazy {
        annotationsFromAsm.find { it.name == KotlinScript::class.simpleName!! }?.args?.first()
                ?: super.name
    }
}

object InvalidScriptDefinition : ScriptDefinition {
    override val properties: ScriptDefinitionPropertiesBag = ScriptDefinitionPropertiesBag()
    override val compilationConfigurator: ScriptCompilationConfigurator = object : ScriptCompilationConfigurator {
        override val defaultConfiguration: ScriptCompileConfiguration = ScriptDefinitionPropertiesBag()
    }
    override val evaluator: ScriptEvaluator<*>? = null
}

