/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.sharding

import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MatrixTestShardingTest {

    @Test
    fun `empty matrix is rejected`() {
        val exception = assertFailsWith<IllegalStateException> {
            MatrixTestSharding(emptyList(), totalShards = 2)
        }
        assertEquals("Matrix should have at least one row", exception.message)
    }

    @Test
    fun `matrix without columns is rejected`() {
        val exception = assertFailsWith<IllegalStateException> {
            MatrixTestSharding(listOf(emptyList()), totalShards = 2)
        }
        assertEquals("Matrix should have at least one column", exception.message)
    }

    @Test
    fun `row shorter than the first row is rejected`() {
        val exception = assertFailsWith<IllegalStateException> {
            MatrixTestSharding(
                listOf(
                    listOf("a", "b"),
                    listOf("c"),
                ),
                totalShards = 2,
            )
        }
        assertEquals("Matrix should have only size of 2 columns", exception.message)
    }

    @Test
    fun `row longer than the first row is rejected`() {
        val exception = assertFailsWith<IllegalStateException> {
            MatrixTestSharding(
                listOf(
                    listOf("a"),
                    listOf("b", "c"),
                ),
                totalShards = 2,
            )
        }
        assertEquals("Matrix should have only size of 1 columns", exception.message)
    }

    @Test
    fun `zero shards are rejected`() {
        val exception = assertFailsWith<IllegalStateException> {
            MatrixTestSharding(listOf(listOf("a")), totalShards = 0)
        }
        assertEquals("Invalid 'totalShards': 0; Expected >= 1", exception.message)
    }

    @Test
    fun `too large matrix is rejected`() {
        val matrix = mutableListOf<List<Any>>()
        for (i in 0 until 50_000) {
            matrix.add(listOf("item-$i"))
        }

        val exception = assertFailsWith<IllegalStateException> {
            MatrixTestSharding(matrix, totalShards = 50_000)
        }
        assertEquals("Matrix is too large: 50000 rows on 50000 shards", exception.message)
    }

    @Test
    fun `rows and columns are counted`() {
        val sharding = MatrixTestSharding(
            listOf(
                listOf("a", "b", "c"),
                listOf("d", "e", "f"),
            ),
            totalShards = 2,
        )
        assertEquals(2, sharding.rows)
        assertEquals(3, sharding.columns)
    }

    @Test
    fun `single shard gets all rows`() {
        val sharding = MatrixTestSharding(
            listOf(
                listOf("a", "x"),
                listOf("a", "y"),
                listOf("b", "x"),
                listOf("b", "y"),
            ),
            totalShards = 1,
        )
        assertEquals(1, sharding.getShardOfRow(0))
        assertEquals(1, sharding.getShardOfRow(1))
        assertEquals(1, sharding.getShardOfRow(2))
        assertEquals(1, sharding.getShardOfRow(3))
    }

    @Test
    fun `single row can land on every shard`() {
        val matrix = listOf(
            listOf("a", "b"),
        )

        val usedShards = mutableSetOf<Int>()
        for (i in 0 until 40) {
            val sharding = MatrixTestSharding(matrix, salt = "template-$i".encodeToByteArray(), totalShards = 4)
            val shard = sharding.getShardOfRow(0)
            assertTrue(shard in 1..4, "Got shard $shard")
            usedShards.add(shard)
        }
        assertEquals(setOf(1, 2, 3, 4), usedShards)
    }

    @Test
    fun `every row is assigned to a valid shard`() {
        val matrix = mutableListOf<List<Any>>()
        for (i in 0 until 50) {
            matrix.add(listOf("group-${i % 3}", "item-$i"))
        }

        for (totalShards in 1..7) {
            val sharding = MatrixTestSharding(matrix, totalShards = totalShards)
            for (row in 0 until sharding.rows) {
                val shard = sharding.getShardOfRow(row)
                assertTrue(shard in 1..totalShards, "Row $row got shard $shard; totalShards=$totalShards")
            }
        }
    }

    @Test
    fun `as many rows as shards gives one row per shard`() {
        val matrix = listOf(
            listOf("a"),
            listOf("b"),
            listOf("c"),
            listOf("d"),
        )

        for (i in 0 until 10) {
            val sharding = MatrixTestSharding(matrix, salt = "template-$i".encodeToByteArray(), totalShards = 4)
            val usedShards = setOf(
                sharding.getShardOfRow(0),
                sharding.getShardOfRow(1),
                sharding.getShardOfRow(2),
                sharding.getShardOfRow(3),
            )
            assertEquals(setOf(1, 2, 3, 4), usedShards, "Salt 'template-$i'")
        }
    }

    @Test
    fun `rows are split evenly between shards`() {
        val matrix = mutableListOf<List<Any>>()
        for (i in 0 until 10) {
            matrix.add(listOf("item-$i"))
        }

        for (i in 0 until 10) {
            val sharding = MatrixTestSharding(matrix, salt = "template-$i".encodeToByteArray(), totalShards = 3)

            val rowsPerShard = IntArray(3)
            for (row in 0 until 10) {
                rowsPerShard[sharding.getShardOfRow(row) - 1]++
            }

            /* 10 rows on 3 shards: 3 or 4 rows each */
            assertTrue(rowsPerShard.all { it == 3 || it == 4 }, "Rows per shard: ${rowsPerShard.toList()}; salt 'template-$i'")
        }
    }

    @Test
    fun `small templates are balanced across many templates`() {
        val matrix = listOf(
            listOf("Gradle 8.14"),
            listOf("Gradle 9.7.0"),
        )

        val rowsPerShard = IntArray(3)
        for (i in 0 until 300) {
            val sharding = MatrixTestSharding(matrix, salt = "template-$i".encodeToByteArray(), totalShards = 3)
            rowsPerShard[sharding.getShardOfRow(0) - 1]++
            rowsPerShard[sharding.getShardOfRow(1) - 1]++
        }

        /* 600 rows on 3 shards: 200 each would be perfect */
        assertTrue(rowsPerShard.all { it in 150..250 }, "Rows per shard: ${rowsPerShard.toList()}")
    }

    @Test
    fun `same inputs give the same shards`() {
        val matrix = mutableListOf<List<Any>>()
        for (i in 0 until 20) {
            matrix.add(listOf("group-${i % 4}", "item-$i"))
        }

        val first = MatrixTestSharding(matrix, salt = "template".encodeToByteArray(), totalShards = 5)
        val second = MatrixTestSharding(matrix, salt = "template".encodeToByteArray(), totalShards = 5)

        for (row in 0 until 20) {
            assertEquals(first.getShardOfRow(row), second.getShardOfRow(row), "Row $row")
        }
    }

    @Test
    fun `order of rows does not matter`() {
        val matrix = listOf(
            listOf("a", "x"),
            listOf("a", "y"),
            listOf("b", "x"),
            listOf("c", "z"),
            listOf("d", "x"),
        )
        val reversedMatrix = matrix.reversed()

        val sharding = MatrixTestSharding(matrix, salt = "template".encodeToByteArray(), totalShards = 3)
        val reversedSharding = MatrixTestSharding(reversedMatrix, salt = "template".encodeToByteArray(), totalShards = 3)

        assertEquals(sharding.getShardOfRow(0), reversedSharding.getShardOfRow(4))
        assertEquals(sharding.getShardOfRow(1), reversedSharding.getShardOfRow(3))
        assertEquals(sharding.getShardOfRow(2), reversedSharding.getShardOfRow(2))
        assertEquals(sharding.getShardOfRow(3), reversedSharding.getShardOfRow(1))
        assertEquals(sharding.getShardOfRow(4), reversedSharding.getShardOfRow(0))
    }

    @Test
    fun `duplicate rows are counted as separate rows`() {
        val sharding = MatrixTestSharding(
            listOf(
                listOf("a", "x"),
                listOf("a", "x"),
            ),
            totalShards = 2,
        )
        assertEquals(setOf(1, 2), setOf(sharding.getShardOfRow(0), sharding.getShardOfRow(1)))
    }

    @Test
    fun `rows with the same first key stay on neighbouring shards`() {
        val matrix = mutableListOf<List<Any>>()
        /* 'b' comes first on purpose: rows are sorted by their keys */
        for (i in 0 until 20) {
            matrix.add(listOf("b", "item-$i"))
        }
        for (i in 0 until 20) {
            matrix.add(listOf("a", "item-$i"))
        }

        for (i in 0 until 10) {
            val sharding = MatrixTestSharding(matrix, salt = "template-$i".encodeToByteArray(), totalShards = 4)

            val shardsOfB = mutableSetOf<Int>()
            for (row in 0 until 20) {
                shardsOfB.add(sharding.getShardOfRow(row))
            }
            assertEquals(setOf(3, 4), shardsOfB, "Salt 'template-$i'")

            val shardsOfA = mutableSetOf<Int>()
            for (row in 20 until 40) {
                shardsOfA.add(sharding.getShardOfRow(row))
            }
            assertEquals(setOf(1, 2), shardsOfA, "Salt 'template-$i'")
        }
    }

    @Test
    fun `few rows keep the same range of shards for every salt`() {
        val matrix = listOf(
            listOf("Gradle 8.14"),
            listOf("Gradle 9.7.0"),
        )

        val shardsOfMin = mutableSetOf<Int>()
        val shardsOfMax = mutableSetOf<Int>()
        for (i in 0 until 40) {
            val sharding = MatrixTestSharding(matrix, salt = "template-$i".encodeToByteArray(), totalShards = 4)
            shardsOfMin.add(sharding.getShardOfRow(0))
            shardsOfMax.add(sharding.getShardOfRow(1))
        }

        /* Two rows on four shards: each row owns two shards, the salt picks one of them */
        assertEquals(setOf(1, 2), shardsOfMin)
        assertEquals(setOf(3, 4), shardsOfMax)
    }

    @Test
    fun `second key does not break up the first key`() {
        val matrix = listOf(
            listOf("Gradle 8.14", "JDK 1.8"),
            listOf("Gradle 9.7.0", "JDK 1.8"),
            listOf("Gradle 8.14", "JDK 21"),
            listOf("Gradle 9.7.0", "JDK 21"),
        )

        for (i in 0 until 10) {
            val sharding = MatrixTestSharding(matrix, salt = "template-$i".encodeToByteArray(), totalShards = 2)
            assertEquals(1, sharding.getShardOfRow(0), "Salt 'template-$i'")
            assertEquals(2, sharding.getShardOfRow(1), "Salt 'template-$i'")
            assertEquals(1, sharding.getShardOfRow(2), "Salt 'template-$i'")
            assertEquals(2, sharding.getShardOfRow(3), "Salt 'template-$i'")
        }
    }

    @Test
    fun `test different salt`() {
        val matrix = listOf(
            listOf("a"),
            listOf("b"),
            listOf("c"),
        )

        val shardingA = MatrixTestSharding(matrix, salt = "salt_A".encodeToByteArray(), totalShards = 10)
        val shardingB = MatrixTestSharding(matrix, salt = "salt_B".encodeToByteArray(), totalShards = 10)
        val shardingC = MatrixTestSharding(matrix, salt = "salt_C".encodeToByteArray(), totalShards = 10)

        listOf(shardingA, shardingB, shardingC).forEach { sharding ->
            assertTrue(sharding.getShardOfRow(0) < sharding.getShardOfRow(1))
            assertTrue(sharding.getShardOfRow(1) < sharding.getShardOfRow(2))
        }
    }

    @Test
    fun `comparable values are sorted by their natural order`() {
        /* As strings, the order would be "10", "2", "9" */
        val matrix = listOf(
            listOf(10),
            listOf(2),
            listOf(9),
        )

        for (i in 0 until 10) {
            /* As many rows as shards: the n-th row (in sorted order) lands on the n-th shard, independent of the salt */
            val sharding = MatrixTestSharding(matrix, salt = "template-$i".encodeToByteArray(), totalShards = 3)
            assertEquals(3, sharding.getShardOfRow(0), "Salt 'template-$i'")
            assertEquals(1, sharding.getShardOfRow(1), "Salt 'template-$i'")
            assertEquals(2, sharding.getShardOfRow(2), "Salt 'template-$i'")
        }
    }

    @Test
    fun `enum values are sorted by their natural order`() {
        val matrix = listOf(
            listOf(Priority.High),
            listOf(Priority.Low),
            listOf(Priority.Medium),
        )

        val sharding = MatrixTestSharding(matrix, salt = "template".encodeToByteArray(), totalShards = 3)
        assertEquals(3, sharding.getShardOfRow(0))
        assertEquals(1, sharding.getShardOfRow(1))
        assertEquals(2, sharding.getShardOfRow(2))
    }

    @Test
    fun `enum values with constant bodies are sorted by their natural order`() {
        /* Stage.Z and Stage.A declare a body, so their class is a subclass of Stage, while Stage.M is an instance of Stage itself */
        val stages = listOf(Stage.Z, Stage.M, Stage.A)

        for (permutation in stages.permutations()) {
            /* As many rows as shards: the n-th stage (by ordinal) lands on the n-th shard, independent of the order of the rows */
            val sharding = MatrixTestSharding(permutation.map { listOf(it) }, salt = "template".encodeToByteArray(), totalShards = 3)
            permutation.forEachIndexed { row, stage ->
                assertEquals(stage.ordinal + 1, sharding.getShardOfRow(row), "$stage in $permutation")
            }
        }
    }

    @Test
    fun `rows are grouped by enum values with constant bodies`() {
        val matrix = Stage.entries.flatMap { stage -> (0 until 10).map { i -> listOf(stage, i) } }

        for (i in 0 until 10) {
            /* The order in which the rows are provided must not matter */
            val shuffledMatrix = matrix.shuffled(Random(i))
            val sharding = MatrixTestSharding(shuffledMatrix, salt = "template-$i".encodeToByteArray(), totalShards = 3)

            /* Each stage takes exactly a third of the rows: the n-th stage (by ordinal) fills the n-th shard */
            shuffledMatrix.forEachIndexed { row, values ->
                val stage = values.first() as Stage
                assertEquals(stage.ordinal + 1, sharding.getShardOfRow(row), "$values with seed $i")
            }
        }
    }

    @Test
    fun `values without natural order are sorted by their string representation`() {
        val matrix = listOf(
            listOf(Version("b")),
            listOf(Version("c")),
            listOf(Version("a")),
        )

        val sharding = MatrixTestSharding(matrix, salt = "template".encodeToByteArray(), totalShards = 3)
        assertEquals(2, sharding.getShardOfRow(0))
        assertEquals(3, sharding.getShardOfRow(1))
        assertEquals(1, sharding.getShardOfRow(2))
    }

    @Test
    fun `equal values without natural order are grouped`() {
        val matrix = mutableListOf<List<Any>>()
        /* Version("b") comes first on purpose: rows are sorted by their values */
        for (i in 0 until 20) {
            matrix.add(listOf(Version("b"), i))
        }
        for (i in 0 until 20) {
            matrix.add(listOf(Version("a"), i))
        }

        for (i in 0 until 10) {
            val sharding = MatrixTestSharding(matrix, salt = "template-$i".encodeToByteArray(), totalShards = 4)
            assertEquals(setOf(3, 4), (0 until 20).map { sharding.getShardOfRow(it) }.toSet(), "Salt 'template-$i'")
            assertEquals(setOf(1, 2), (20 until 40).map { sharding.getShardOfRow(it) }.toSet(), "Salt 'template-$i'")
        }
    }

    @Test
    fun `order of rows with mixed value types does not matter`() {
        val matrix = listOf(
            listOf("Gradle 8.14", 21, Priority.Low),
            listOf("Gradle 8.14", 8, Priority.High),
            listOf("Gradle 9.7.0", 21, Priority.Medium),
            listOf("Gradle 9.7.0", 8, Priority.Low),
            listOf("Gradle 9.7.0", 17, Version("a")),
        )
        val reversedMatrix = matrix.reversed()

        val sharding = MatrixTestSharding(matrix, salt = "template".encodeToByteArray(), totalShards = 3)
        val reversedSharding = MatrixTestSharding(reversedMatrix, salt = "template".encodeToByteArray(), totalShards = 3)

        for (row in matrix.indices) {
            assertEquals(sharding.getShardOfRow(row), reversedSharding.getShardOfRow(matrix.lastIndex - row), "Row $row")
        }
    }

    /* Declared in a different order than the alphabetical one: sorted by their ordinal */
    private enum class Priority { Low, Medium, High }

    /* Declared in a different order than the alphabetical one: sorted by their ordinal, even though some constants have a body */
    private enum class Stage {
        Z {
            override val label: String get() = "last"
        },
        M,
        A {
            override val label: String get() = "first"
        };

        open val label: String get() = name
    }

    /* Not comparable: sorted by 'toString()' */
    private data class Version(val name: String)

    private fun <T> List<T>.permutations(): List<List<T>> {
        if (size <= 1) return listOf(this)
        return indices.flatMap { index ->
            (take(index) + drop(index + 1)).permutations().map { rest -> listOf(this[index]) + rest }
        }
    }
}
