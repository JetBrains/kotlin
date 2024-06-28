package org.jetbrains.kotlin.gradle.plugin.mpp

import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class SortSourceSetsByDependsOnRelationTest {

    @Test
    fun simpleCase() {
        val dependsOnRelations = mapOf(
            "a" to emptySet(),
            "b" to setOf("a"),
            "c" to setOf("b"),
        )

        val expected = listOf("c", "b", "a")
        assertEquals(expected, sortSourceSetsByDependsOnRelation(setOf("a", "b", "c"), dependsOnRelations))
        assertEquals(expected, sortSourceSetsByDependsOnRelation(setOf("c", "b", "a"), dependsOnRelations))
        assertEquals(expected, sortSourceSetsByDependsOnRelation(setOf("c", "a", "b"), dependsOnRelations))
    }

    @Test
    fun sourceSetsWithoutRelationsBetweenEachOther() {
        val dependsOnRelations = mapOf(
            "a" to emptySet(),
            "b" to setOf("a"),
            "c" to setOf("b"),
            "d" to setOf("a"),
        )

        // So only these two requirements should stay true: c -> b -> a; d -> a;
        val possibleOutputs = listOf(
            listOf("d", "c", "b", "a"),
            listOf("c", "d", "b", "a"),
            listOf("c", "b", "d", "a"),
        )

        val allPermutations = listOf("a", "b", "c", "d").permutations()
        for (permutation in allPermutations) {
            val actual = sortSourceSetsByDependsOnRelation(permutation.toSet(), dependsOnRelations)
            if (actual !in possibleOutputs) fail("Unexpected $actual for $permutation")
        }
    }
}


private fun List<String>.permutations(): List<List<String>> {
    val solutions = mutableListOf<List<String>>()
    permutationsRecursive(toMutableList(), 0, solutions)
    return solutions
}

private fun permutationsRecursive(input: MutableList<String>, index: Int, answers: MutableList<List<String>>) {
    if (index == input.lastIndex) answers.add(input.toList())
    for (i in index .. input.lastIndex) {
        Collections.swap(input, index, i)
        permutationsRecursive(input, index + 1, answers)
        Collections.swap(input, i, index)
    }
}
