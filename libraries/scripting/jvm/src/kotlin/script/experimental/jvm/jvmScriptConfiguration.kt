/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.jvm

import java.io.File
import kotlin.script.experimental.api.PropertiesGroup
import kotlin.script.experimental.api.ScriptingProperties
import kotlin.script.experimental.util.typedKey

object JvmScriptCompileConfigurationProperties : PropertiesGroup {
    val javaHomeDir by typedKey<File>(File(System.getProperty("java.home")))
}

val ScriptingProperties.jvmCompileConfiguration get() = JvmScriptCompileConfigurationProperties
