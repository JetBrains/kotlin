/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.repl.k2

import kotlin.reflect.KType
import kotlin.script.experimental.api.ResultValue

// Dummy. Should only be used for developing the API and should be replaced by
// the real one.
class ReplState {
    fun setProperty(name: String, value: Any, type: KType) {
        // Set the property value somehow
    }

    fun <T: Any> getProperty(name: String): T? {
        // Read the property value somehow
        return null
    }

    fun getOutput(snippetNo: Int): ResultValue? {
        // Return the output for some code cell or Unit if no output exists
        return null
    }

    fun setOutput(snippetNo: Int, value: Any?) {
        // Store output some way
    }
}

inline fun <reified T: Any> ReplState.getProperty(name: String): T? {
    return getProperty<T>(name)
}
