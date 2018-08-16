/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.jvm

import kotlin.script.experimental.api.ScriptCompileConfigurationKeys
import kotlin.script.experimental.api.ScriptingEnvironment
import kotlin.script.experimental.util.PropertiesCollection

interface JvmScriptCompilationConfigurationKeys

open class JvmScriptCompilationConfigurationBuilder : JvmScriptCompilationConfigurationKeys, JvmScriptDefinition() {

    companion object :
        PropertiesCollection.Builder.BuilderExtension<JvmScriptCompilationConfigurationBuilder>,
        JvmScriptCompilationConfigurationKeys {

        override fun get() = JvmScriptCompilationConfigurationBuilder()
    }
}

val JvmScriptCompilationConfigurationKeys.javaHome by PropertiesCollection.keyCopy(ScriptingEnvironment.jvm.javaHome)

val ScriptCompileConfigurationKeys.jvm get() = JvmScriptCompilationConfigurationBuilder()
