/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.dukat

import org.xml.sax.InputSource
import java.io.File
import javax.xml.xpath.XPathFactory

private val LINE_SEPARATOR = System.lineSeparator()

private fun readCopyrightNoticeFromProfile(copyrightProfile: File): String {
    val template = copyrightProfile.reader().use { reader ->
        XPathFactory.newInstance().newXPath().evaluate(
            "/component/copyright/option[@name='notice']/@value",
            InputSource(reader)
        )
    }
    val yearTemplate = "&#36;today.year"
    val year = java.time.LocalDate.now().year.toString()
    assert(yearTemplate in template)

    return template.replace(yearTemplate, year).lines()
        .joinToString("", prefix = "/*$LINE_SEPARATOR", postfix = " */$LINE_SEPARATOR") {
            " * $it$LINE_SEPARATOR"
        }
}

internal fun getHeader(): String {
    val copyrightNotice = readCopyrightNoticeFromProfile(
        File("../../../.idea/copyright/apache.xml")
    )
    val note = "// NOTE: THIS FILE IS AUTO-GENERATED, DO NOT EDIT!$LINE_SEPARATOR" +
            "// See github.com/kotlin/dukat for details$LINE_SEPARATOR"
    return copyrightNotice + LINE_SEPARATOR + note + LINE_SEPARATOR
}

internal fun launch(outputDirectory: String, dynamicAsType: Boolean, useStaticGetters: Boolean) {
    val input = "../../stdlib/js/idl/org.w3c.dom.idl"

    val args = mutableListOf<String>()
    args.add("-d")
    args.add(outputDirectory)
    args.add(input)
    if (dynamicAsType) {
        args.add("--dynamic-as-type")
    }
    if (useStaticGetters) {
        args.add("--use-static-getters")
    }

    org.jetbrains.dukat.cli.main(*args.toTypedArray())

    for (file in File(outputDirectory).listFiles { name ->
        name.extension == "kt"
    }.orEmpty()) {
        file.writeBytes((getHeader() + file.readText()).toByteArray())
    }
}