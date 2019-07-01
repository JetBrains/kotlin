/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.impl

import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration

fun ScriptingHostConfiguration.withDefaults(): ScriptingHostConfiguration =
    if (this == defaultJvmScriptingHostConfiguration) {
        this
    } else {
        ScriptingHostConfiguration(defaultJvmScriptingHostConfiguration, this)
    }
