/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.name.Name

/**
 * Name corresponding to an entity in the original Kotlin code.
 *
 * This is a little more general than the `Name` type in Kotlin: we also use this
 * to represent getters and setters, for example.
 */
sealed interface KotlinName : MangledName

data class SimpleKotlinName(val name: Name) : KotlinName {
    override val mangled: String = name.asStringStripSpecialMarkers()
}

abstract class PrefixedKotlinName(prefix: String, name: Name) : KotlinName {
    override val mangled: String = "${prefix}_${name.asStringStripSpecialMarkers()}"
}

data class FunctionKotlinName(val name: Name) : PrefixedKotlinName("fun", name)
data class MemberKotlinName(val name: Name) : PrefixedKotlinName("member", name)
data class GetterKotlinName(val name: Name) : PrefixedKotlinName("getter", name)
data class SetterKotlinName(val name: Name) : PrefixedKotlinName("setter", name)
data class ExtensionSetterKotlinName(val name: Name) : PrefixedKotlinName("ext_setter", name)
data class ExtensionGetterKotlinName(val name: Name) : PrefixedKotlinName("ext_getter", name)

data class ClassKotlinName(val name: Name) : KotlinName {
    override val mangled: String = "class_${name.asStringStripSpecialMarkers()}"
}

data object ConstructorKotlinName : KotlinName {
    override val mangled: String = "constructor"
}

