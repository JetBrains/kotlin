/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.reports

import org.jetbrains.kotlin.abicmp.*
import java.io.PrintWriter

fun String.withTag(tagName: String) = "<$tagName>$this</$tagName>"

fun PrintWriter.propertyDiffTable(header1: String, header2: String, propertyDiffs: List<NamedDiffEntry>) {
    if (propertyDiffs.isNotEmpty()) {
        table {
            tableHeader("Property", header1, header2)
            for (pd in propertyDiffs) {
                tableData(pd.name, pd.value1.toHtmlString().withTag("code"), pd.value2.toHtmlString().withTag("code"))
            }
        }
        println("&nbsp;")
    }
}

fun PrintWriter.annotationDiffTable(header1: String, header2: String, annotationDiffs: List<NamedDiffEntry>) {
    if (annotationDiffs.isNotEmpty()) {
        table {
            tableHeader("Annotation", header1, header2)
            for (ad in annotationDiffs) {
                tableData(ad.name, ad.value1.escapeHtml().withTag("code"), ad.value2.escapeHtml().withTag("code"))
            }
        }
        println("&nbsp;")
    }
}

fun PrintWriter.listDiff(header1: String, header2: String, listDiffs: List<DiffEntry>) {
    if (listDiffs.isNotEmpty()) {
        table {
            tableHeader(header1, header2)
            for (me in listDiffs) {
                tableData(me.value1.escapeHtml().withTag("code"), me.value2.escapeHtml().withTag("code"))
            }
        }
        println("&nbsp;")
    }
}

fun ComparisonReport?.isNullOrEmpty() = this == null || isEmpty()

fun List<ComparisonReport>.areAllEmpty() = !any { it.isNotEmpty() }