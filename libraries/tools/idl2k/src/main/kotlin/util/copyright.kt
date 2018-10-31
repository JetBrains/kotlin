/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.idl2k.util

import org.xml.sax.InputSource
import java.io.File
import javax.xml.xpath.XPathFactory

fun readCopyrightNoticeFromProfile(copyrightProfile: File): String {
    val template = copyrightProfile.reader().use { reader ->
        XPathFactory.newInstance().newXPath().evaluate("/component/copyright/option[@name='notice']/@value", InputSource(reader))
    }
    val yearTemplate = "&#36;today.year"
    val year = java.time.LocalDate.now().year.toString()
    assert(yearTemplate in template)

    return template.replace(yearTemplate, year).lines().joinToString("", prefix = "/*\n", postfix = " */\n") { " * $it\n" }
}