/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvm

import java.io.File
import kotlin.script.experimental.api.ScriptDependency
import kotlin.script.experimental.api.TypedKey

object JvmScriptCompileConfigurationParams {

    val javaHomeDir = TypedKey<File>("javaHomeDir")
}

class JvmDependency(val classpath: Iterable<File>): ScriptDependency

