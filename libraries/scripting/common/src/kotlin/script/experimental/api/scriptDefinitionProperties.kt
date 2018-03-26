/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.api


typealias ScriptDefinitionPropertiesBag = HeterogeneousMap


object ScriptDefinitionProperties {

    val name by typedKey<String>()

    val fileExtension by typedKey<String>()

    val makeTitle by typedKey<(String) -> String>()

    open class Builder : HeterogeneousMapBuilder() {
        inline fun <reified T> makeTitle(noinline fn: (String) -> String) {
            add(makeTitle to fn)
        }
    }
}

