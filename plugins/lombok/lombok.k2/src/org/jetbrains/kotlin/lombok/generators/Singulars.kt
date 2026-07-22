/*
 * Copyright 2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/*
 * Copyright (C) 2015 The Project Lombok Authors.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jetbrains.kotlin.lombok.generators

// The table is embedded as a source literal (rather than loaded as a classpath resource) so that
// this module is self-contained and doesn't depend on the real `lombok` library being present on
// the compiler's runtime classpath. To pick up an upstream update, just replace the contents of
// `SINGULARS_TXT` below with the new file's contents.

// Source: https://github.com/projectlombok/lombok/blob/c8f91b529497dfde33a72eef69a361d8bfbbd41d/src/core/lombok/core/handlers/singulars.txt

private const val SINGULARS_TXT = """
#Based on https://github.com/rails/rails/blob/efff6c1fd4b9e2e4c9f705a45879373cb34a5b0e/activesupport/lib/active_support/inflections.rb

quizzes = quiz
matrices = matrix
indices = index
vertices = vertex
statuses = status
aliases = alias
alias = !
species = !
Axes = !
-axes = axe
sexes = sex
Testes = testis
movies = movie
octopodes = octopus
buses = bus
Mice = mouse
Lice = louse
News = !
# We could add more detail (axemen, boatsmen, boogymen, cavemen, gentlemen, etc, but (A) there's stuff like 'cerumen', and (B) the 'men' ending is common in singulars and other languages.)
# Therefore, the odds of a mistake are too high, so other than these 2 well known cases, force the explicit singular.
Men = man
Women = woman
minutiae = minutia
shoes = shoe
synopses = synopsis
prognoses = prognosis
theses = thesis
diagnoses = diagnosis
bases = base
analyses = analysis
Crises = crisis
children = child
moves = move
zombies = zombie
-quies = quy
-us = !
-is = !
series = !
-ies = y
-oes = o
hives = hive
-tives = tive
-sses = ss
-ches = ch
-xes = x
-shes = sh
-lves = lf
-rves = rf
saves = save
Leaves = leaf
-ves = !
-ss = !
-us = !
-s =
"""

// Source: https://github.com/projectlombok/lombok/blob/c8f91b529497dfde33a72eef69a361d8bfbbd41d/src/core/lombok/core/handlers/Singulars.java

private val SINGULAR_STORE: List<String> = buildList {
    for (rawLine in SINGULARS_TXT.lineSequence()) {
        val line = rawLine.trim()
        if (line.startsWith('#') || line.isEmpty()) continue

        if (line.endsWith(" =")) {
            add(line.substring(0, line.length - 2))
            add("")
            continue
        }

        val idx = line.indexOf(" = ")
        add(line.substring(0, idx))
        add(line.substring(idx + 3))
    }
}

object Singulars {
    fun autoSingularize(word: String): String? {
        val inLen = word.length
        var i = 0
        while (i < SINGULAR_STORE.size) {
            val lastPart = SINGULAR_STORE[i]
            val wholeWord = lastPart[0].isUpperCase()
            val endingOnly = if (lastPart[0] == '-') 1 else 0
            val len = lastPart.length
            if (inLen < len) {
                i += 2
                continue
            }
            if (!word.regionMatches(inLen - len + endingOnly, lastPart, endingOnly, len - endingOnly, ignoreCase = true)) {
                i += 2
                continue
            }
            if (wholeWord && inLen != len && !word[inLen - len].isUpperCase()) {
                i += 2
                continue
            }

            val replacement = SINGULAR_STORE[i + 1]
            if (replacement == "!") return null

            val capitalizeFirst = replacement.isNotEmpty() && word[inLen - len + endingOnly].isUpperCase()
            val pre = word.substring(0, inLen - len + endingOnly)
            val post = if (capitalizeFirst) replacement[0].uppercaseChar() + replacement.substring(1) else replacement
            return pre + post
        }

        return null
    }
}
