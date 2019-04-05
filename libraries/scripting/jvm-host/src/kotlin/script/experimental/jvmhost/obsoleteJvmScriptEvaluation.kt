/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.jvmhost

import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.ScriptEvaluationConfigurationKeys
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.util.PropertiesCollection

@Deprecated("use the same definitions from kotlin.script.experimental.jvm package", level = DeprecationLevel.WARNING)
interface JvmScriptEvaluationConfigurationKeys

@Deprecated("use the same definitions from kotlin.script.experimental.jvm package", level = DeprecationLevel.WARNING)
open class JvmScriptEvaluationConfigurationBuilder : PropertiesCollection.Builder(), JvmScriptEvaluationConfigurationKeys {

    companion object : JvmScriptEvaluationConfigurationBuilder()
}

@Deprecated("use the same definitions from kotlin.script.experimental.jvm package", level = DeprecationLevel.ERROR)
val JvmScriptEvaluationConfigurationKeys.baseClassLoader get() = ScriptEvaluationConfiguration.jvm.baseClassLoader

@Deprecated("use the same definitions from kotlin.script.experimental.jvm package", level = DeprecationLevel.ERROR)
val ScriptEvaluationConfigurationKeys.jvm get() = ScriptEvaluationConfiguration.jvm

@Deprecated("use the same definitions from kotlin.script.experimental.jvm package", level = DeprecationLevel.ERROR)
open class BasicJvmScriptEvaluator : kotlin.script.experimental.jvm.BasicJvmScriptEvaluator()
