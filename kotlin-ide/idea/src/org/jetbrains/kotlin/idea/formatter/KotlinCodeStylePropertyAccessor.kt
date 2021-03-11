/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter

import com.intellij.application.options.codeStyle.properties.CodeStyleChoiceList
import com.intellij.application.options.codeStyle.properties.CodeStylePropertyAccessor
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings
import org.jetbrains.kotlin.idea.util.applyKotlinCodeStyle

class KotlinCodeStylePropertyAccessor(private val kotlinCodeStyle: KotlinCodeStyleSettings) :
    CodeStylePropertyAccessor<String>(),
    CodeStyleChoiceList {
    override fun set(extVal: String): Boolean = applyKotlinCodeStyle(extVal, kotlinCodeStyle.container)
    override fun get(): String? = kotlinCodeStyle.container.kotlinCodeStyleDefaults()
    override fun parseString(string: String): String = string
    override fun valueToString(value: String): String = value
    override fun getChoices(): List<String> = listOf(KotlinStyleGuideCodeStyle.CODE_STYLE_ID, KotlinObsoleteCodeStyle.CODE_STYLE_ID)
    override fun getPropertyName(): String = "code_style_defaults"
}