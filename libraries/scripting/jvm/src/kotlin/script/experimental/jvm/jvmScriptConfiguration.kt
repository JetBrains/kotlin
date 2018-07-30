/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.jvm

import kotlin.script.experimental.api.ScriptCompileConfiguration
import kotlin.script.experimental.util.PropertiesCollection

open class JvmScriptCompilationConfiguration : JvmScriptDefinition() {

    companion object : JvmScriptCompilationConfiguration()
}

val JvmScriptCompilationConfiguration.javaHome by PropertiesCollection.keyCopy(JvmScriptingEnvironment.javaHome)

val ScriptCompileConfiguration.jvm get() = JvmScriptCompilationConfiguration()
