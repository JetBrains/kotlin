/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok

import org.jetbrains.kotlin.test.utils.verifyDiagnostics
import org.junit.jupiter.api.Test
import org.jetbrains.kotlin.lombok.generators.Singulars
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEquals

class LombokUnitTest {
    @Test
    fun verifyDiagnostics() {
        verifyDiagnostics(LombokCliDiagnostics)
        verifyDiagnostics(LombokFirDiagnostics)
    }

    @Test
    fun testAutoSingularize() {
        val pluralToSingularPairs = mapOf(
            "quizzes" to "quiz",
            "matrices" to "matrix",
            "indices" to "index",
            "vertices" to "vertex",
            "statuses" to "status",
            "aliases" to "alias",
            "alias" to null,
            "species" to null,
            "axes" to null,
            "pickaxes" to "pickaxe", // -axes
            "sexes" to "sex",
            "testes" to "testis",
            "movies" to "movie",
            "octopodes" to "octopus",
            "buses" to "bus",
            "mice" to "mouse",
            "lice" to "louse",
            "news" to null,
            "men" to "man",
            "women" to "woman",
            "minutiae" to "minutia",
            "shoes" to "shoe",
            "synopses" to "synopsis",
            "prognoses" to "prognosis",
            "theses" to "thesis",
            "diagnoses" to "diagnosis",
            "bases" to "base",
            "analyses" to "analysis",
            "crises" to "crisis",
            "children" to "child",
            "moves" to "move",
            "zombies" to "zombie",
            "colloquies" to "colloquy", // -quies
            "cactus" to null, // -us
            "crisis" to null, // -is
            "series" to null,
            "babies" to "baby", // -ies
            "tomatoes" to "tomato", // -oes
            "hives" to "hive",
            "alternatives" to "alternative", // -tives
            "bosses" to "boss", // -sses
            "matches" to "match", // -ches
            "boxes" to "box", // -xes
            "dishes" to "dish", // -shes
            "wolves" to "wolf", // -lves
            "scarves" to "scarf", // -rves
            "saves" to "save",
            "leaves" to "leaf",
            "knives" to null, // -ves
            "class" to null, // -ss
            "genius" to null, // -us
            "desks" to "desk", // -s

            "штуки" to null, // Don't handle non-English words
        )

        for ((plural = key, singular = value) in pluralToSingularPairs) {
            assertEquals(singular, Singulars.autoSingularize(plural))
        }
    }
}
