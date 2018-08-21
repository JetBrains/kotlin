/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.impl

import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.defaultJvmScriptingEnvironment

fun ScriptingHostConfiguration.withDefaults(): ScriptingHostConfiguration =
    if (this == defaultJvmScriptingEnvironment) {
        this
    } else {
        ScriptingHostConfiguration(defaultJvmScriptingEnvironment, this)
    }
