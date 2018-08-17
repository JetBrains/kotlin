/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.impl

import kotlin.script.experimental.api.ScriptingEnvironment
import kotlin.script.experimental.jvm.defaultJvmScriptingEnvironment

fun ScriptingEnvironment.withDefaults(): ScriptingEnvironment =
    if (this == defaultJvmScriptingEnvironment || defaultJvmScriptingEnvironment.properties.all {
            this.properties.containsKey(it.key)
        }) {
        this
    } else {
        ScriptingEnvironment(defaultJvmScriptingEnvironment, this)
    }
