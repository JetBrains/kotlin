/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.impl

import kotlin.script.experimental.api.ScriptCompilationConfigurationKeys
import kotlin.script.experimental.util.PropertiesCollection

/**
 * Set by the compiler: the language version used for script compilation
 */
val ScriptCompilationConfigurationKeys._languageVersion by PropertiesCollection.key<String>()
