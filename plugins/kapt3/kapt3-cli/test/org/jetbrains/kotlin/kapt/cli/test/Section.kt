/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.cli.test

import org.jetbrains.kotlin.kapt.cli.test.Section.Companion.SECTION_INDICATOR
import java.io.File

class Section(val name: String, val content: String) {
    companion object {
        const val SECTION_INDICATOR = "# "

        fun parse(file: File): List<Section> {
            val sections = mutableListOf<Section>()

            var currentName = ""
            val currentContent = StringBuilder()

            fun saveCurrent() {
                if (currentName.isNotEmpty()) {
                    sections += Section(currentName.trim(), currentContent.toString().trim())
                }

                currentName = ""
                currentContent.clear()
            }

            file.forEachLine { line ->
                if (line.startsWith(SECTION_INDICATOR)) {
                    assert(line.length > 2)
                    saveCurrent()
                    currentName = line.drop(2)
                } else {
                    currentContent.appendln(line)
                }
            }

            saveCurrent()
            return sections
        }
    }
}

fun List<Section>.render(): String = buildString {
    for (section in this@render) {
        append(SECTION_INDICATOR).appendln(section.name)
        appendln(section.content).appendln()
    }
}.trim()

fun List<Section>.replacingSection(name: String, newContent: String): List<Section> {
    val result = mutableListOf<Section>()
    var found = false

    for (section in this) {
        result += if (section.name == name) {
            found = true
            Section(name, newContent)
        } else {
            section
        }
    }

    assert(found) { "Section $name not found" }
    return result
}