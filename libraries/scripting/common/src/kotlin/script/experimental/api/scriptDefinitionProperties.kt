/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.api

object ScriptDefinitionProperties : PropertiesBase<ScriptDefinitionProperties>() {

    val name by typedKey<String>()

    val fileExtension by typedKey<String>()

    open class Builder(parentBuilder: PropertyBagBuilder? = null) : PropertyBagBuilder(parentBuilder)
}

