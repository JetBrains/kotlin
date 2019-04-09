/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.test

import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate

@KotlinScript(fileExtension = "withboth.kts", compilationConfiguration = ReceiverAndPropertiesConfiguration::class)
abstract class ScriptWithBoth

@KotlinScript(fileExtension = "withproperties.kts", compilationConfiguration = ProvidedPropertiesConfiguration::class)
abstract class ScriptWithProvidedProperties

@KotlinScript(fileExtension = "withreceiver.kts", compilationConfiguration = ImplicitReceiverConfiguration::class)
abstract class ScriptWithImplicitReceiver

object ReceiverAndPropertiesConfiguration : ScriptCompilationConfiguration(
    {
        jvm { dependenciesFromCurrentContext(wholeClasspath = true) }

        providedProperties("providedString" to String::class)

        implicitReceivers(ImplicitReceiverClass::class)
    }
)

object ProvidedPropertiesConfiguration : ScriptCompilationConfiguration(
    {
        jvm { dependenciesFromCurrentContext(wholeClasspath = true) }

        providedProperties("providedString" to String::class)
    }
)

object ImplicitReceiverConfiguration : ScriptCompilationConfiguration(
    {
        jvm { dependenciesFromCurrentContext(wholeClasspath = true) }

        implicitReceivers(ImplicitReceiverClass::class)
    }
)

class ImplicitReceiverClass(val receiverString: String)

inline fun <reified T : Any> evalString(
    source: String,
    noinline configure: ScriptEvaluationConfiguration.Builder.() -> Unit
): ResultWithDiagnostics<EvaluationResult> {
    val actualConfiguration = createJvmCompilationConfigurationFromTemplate<T>()
    return BasicJvmScriptingHost()
        .eval(source.toScriptSource(), actualConfiguration, ScriptEvaluationConfiguration(configure))
}

