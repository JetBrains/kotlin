/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model

interface KpmVariant : KpmFragment {
    val variantAttributes: Map<KotlinAttributeKey, String>
}

class KpmBasicVariant(
    containingModule: KpmModule, fragmentName: String, languageSettings: LanguageSettings? = null
) : KpmBasicFragment(
    containingModule, fragmentName, languageSettings
), KpmVariant {
    override val variantAttributes: MutableMap<KotlinAttributeKey, String> = mutableMapOf()
    override fun toString(): String = "variant $fragmentName"
}
