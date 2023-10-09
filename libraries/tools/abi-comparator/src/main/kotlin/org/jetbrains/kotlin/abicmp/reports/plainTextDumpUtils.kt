/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.reports

import java.io.PrintWriter

class TextTreeBuilderContext(val ident: String, val out: PrintWriter)

private fun String.addIdent(ident: String) = "${ident}${replace("\n", "\n$ident")}"

fun dumpTree(out: PrintWriter, body: TextTreeBuilderContext.() -> Unit) {
    TextTreeBuilderContext("", out).also(body)
    out.flush()
}

fun TextTreeBuilderContext.node(header: String, body: TextTreeBuilderContext.() -> Unit = {}) {
    out.println(header.addIdent(ident))
    TextTreeBuilderContext(ident + "\t", out).also(body)
}

fun TextTreeBuilderContext.appendDiffEntries(header1: String, header2: String, diffs: ArrayList<DiffEntry>) {
    if (diffs.isNotEmpty()) {
        for (diff in diffs) {
            node(header1) {
                node(diff.value1)
            }
            node(header2) {
                node(diff.value2)
            }
        }
    }
}

fun TextTreeBuilderContext.appendNamedDiffEntries(header1: String, header2: String, diffEntries: ArrayList<NamedDiffEntry>, title: String) {
    if (diffEntries.isNotEmpty()) {
        for (entry in diffEntries) {
            node("$title: ${entry.name}") {
                node(header1) {
                    node(entry.value1)
                }
                node(header2) {
                    node(entry.value2)
                }
            }
        }
    }
}