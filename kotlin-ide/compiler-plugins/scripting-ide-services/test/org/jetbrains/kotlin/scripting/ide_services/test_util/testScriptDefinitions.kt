/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.ide_services.test_util

import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.createCompilationConfigurationFromTemplate
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.experimental.jvm.util.classpathFromClass

@KotlinScript(fileExtension = "simplescript.kts")
abstract class SimpleScript

val simpleScriptCompilationConfiguration =
    createJvmCompilationConfigurationFromTemplate<SimpleScript> {
        updateClasspath(classpathFromClass<SimpleScript>())
    }

val simpleScriptEvaluationConfiguration = ScriptEvaluationConfiguration()

@KotlinScript(fileExtension = "withboth.kts", compilationConfiguration = ReceiverAndPropertiesConfiguration::class)
abstract class ScriptWithBoth

@KotlinScript(fileExtension = "withproperties.kts", compilationConfiguration = ProvidedPropertiesConfiguration::class)
abstract class ScriptWithProvidedProperties

@KotlinScript(fileExtension = "withreceiver.kts", compilationConfiguration = ImplicitReceiverConfiguration::class)
abstract class ScriptWithImplicitReceiver

object ReceiverAndPropertiesConfiguration : ScriptCompilationConfiguration(
    {
        updateClasspath(classpathFromClass<ScriptWithBoth>())

        providedProperties("providedString" to String::class)

        implicitReceivers(ImplicitReceiverClass::class)
    }
)

object ProvidedPropertiesConfiguration : ScriptCompilationConfiguration(
    {
        updateClasspath(classpathFromClass<ScriptWithProvidedProperties>())

        providedProperties("providedString" to String::class)
    }
)

object ImplicitReceiverConfiguration : ScriptCompilationConfiguration(
    {
        updateClasspath(classpathFromClass<ScriptWithImplicitReceiver>())

        implicitReceivers(ImplicitReceiverClass::class)
    }
)

class ImplicitReceiverClass(
    @Suppress("unused")
    val receiverString: String
)


inline fun <reified T : Any> createJvmCompilationConfigurationFromTemplate(
    hostConfiguration: ScriptingHostConfiguration = defaultJvmScriptingHostConfiguration,
    noinline body: ScriptCompilationConfiguration.Builder.() -> Unit = {}
): ScriptCompilationConfiguration =
    createCompilationConfigurationFromTemplate(
        KotlinType(T::class),
        hostConfiguration,
        ScriptCompilationConfiguration::class,
        body
    )