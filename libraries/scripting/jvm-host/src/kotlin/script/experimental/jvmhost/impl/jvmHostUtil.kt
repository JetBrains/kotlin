/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.impl

import kotlin.script.experimental.api.ScriptCompileConfiguration
import kotlin.script.experimental.api.ScriptingEnvironment
import kotlin.script.experimental.jvm.defaultJvmScriptingEnvironment

internal fun mergeConfigurations(vararg configurations: ScriptCompileConfiguration?): ScriptCompileConfiguration? {
    val nonEmptyConfigurations = configurations.filter { it != null && it.properties.isNotEmpty() }
    return when {
        nonEmptyConfigurations.isEmpty() -> null
        nonEmptyConfigurations.size == 1 -> nonEmptyConfigurations.first()!!
        else -> ScriptCompileConfiguration.create {
            for (configuration in nonEmptyConfigurations) {
                include(configuration!!)
            }
        }
    }
}

fun ScriptingEnvironment.withDefaults(): ScriptingEnvironment =
    if (this == defaultJvmScriptingEnvironment || defaultJvmScriptingEnvironment.properties.all {
            this.properties.containsKey(it.key)
        }) {
        this
    } else {
        ScriptingEnvironment.create {
            include(defaultJvmScriptingEnvironment)
            include(this)
        }
    }
