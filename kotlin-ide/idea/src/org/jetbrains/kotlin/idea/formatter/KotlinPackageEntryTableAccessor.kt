/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter

import com.intellij.application.options.codeStyle.properties.CodeStylePropertiesUtil
import com.intellij.application.options.codeStyle.properties.ValueListPropertyAccessor
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings
import org.jetbrains.kotlin.idea.core.formatter.KotlinPackageEntry
import org.jetbrains.kotlin.idea.core.formatter.KotlinPackageEntryTable
import java.lang.reflect.Field

class KotlinPackageEntryTableAccessor(kotlinCodeStyle: KotlinCodeStyleSettings, field: Field) :
    ValueListPropertyAccessor<KotlinPackageEntryTable>(kotlinCodeStyle, field) {
    override fun valueToString(value: List<String>): String = CodeStylePropertiesUtil.toCommaSeparatedString(value)

    override fun fromExternal(extVal: List<String>): KotlinPackageEntryTable = KotlinPackageEntryTable(
        extVal.asSequence().map(String::trim).map(::readPackageEntry).toMutableList()
    )

    override fun toExternal(value: KotlinPackageEntryTable): List<String> = value.getEntries().map(::writePackageEntry)

    companion object {
        private const val ALIAS_CHAR = "$"
        private const val OTHER_CHAR = "*"

        private fun readPackageEntry(string: String): KotlinPackageEntry = when {
            string == ALIAS_CHAR -> KotlinPackageEntry.ALL_OTHER_ALIAS_IMPORTS_ENTRY
            string == OTHER_CHAR -> KotlinPackageEntry.ALL_OTHER_IMPORTS_ENTRY
            string.endsWith("**") -> KotlinPackageEntry(string.substring(0, string.length - 1), true)
            else -> KotlinPackageEntry(string, false)
        }

        private fun writePackageEntry(entry: KotlinPackageEntry): String = when (entry) {
            KotlinPackageEntry.ALL_OTHER_ALIAS_IMPORTS_ENTRY -> ALIAS_CHAR
            KotlinPackageEntry.ALL_OTHER_IMPORTS_ENTRY -> OTHER_CHAR
            else -> "${entry.packageName}.*" + if (entry.withSubpackages) "*" else ""
        }
    }
}

