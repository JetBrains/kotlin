/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvm

import java.io.File
import kotlin.script.experimental.api.*

object JvmScriptCompileConfigurationParams {
    val javaHomeDir by typedKey<File>()

    open class Builder : ScriptCompileConfigurationParams.Builder() {
        fun dependencies(vararg classpath: Iterable<File>) =
            add(ScriptCompileConfigurationParams.dependencies to classpath.map(::JvmDependency))
    }
}

inline fun jvmScriptConfiguration(from: ChainedPropertyBag = ChainedPropertyBag(), body: JvmScriptCompileConfigurationParams.Builder.() -> Unit) =
    JvmScriptCompileConfigurationParams.Builder().build(from, body)

class JvmDependency(val classpath: Iterable<File>) : ScriptDependency

