/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.api

class ProcessedScriptDataParams : HeterogeneousMapBuilder() {
    companion object {
        val annotations by typedKey<Iterable<Annotation>>()

        val fragments by typedKey<Iterable<ScriptSourceNamedFragment>>()
    }
}

inline
fun processedScriptData(from: HeterogeneousMap = HeterogeneousMap(), body: ProcessedScriptDataParams.() -> Unit) =
    ProcessedScriptDataParams().build(from, body)