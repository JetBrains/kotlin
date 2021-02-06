package org.jetbrains.kotlin.tools.projectWizard.core

import org.junit.Test
import kotlin.test.assertEquals

class ResultTest {
    @Test
    fun `traverse should be success on empty list`() {
        assertEquals(
            emptyList<TaskResult<Unit>>().sequence(),
            Success(emptyList())
        )
    }

    @Test
    fun `traverse should be traverse the structure for non-empty list`() {
        assertEquals(
            listOf(Success(1), Success(2), Success(3)).sequence(),
            Success(listOf(1, 2, 3))
        )
    }

    @Test
    fun `traverse should collect errors`() {
        assertEquals(
            listOf(
                Success(1),
                Failure(
                    ParseError("1"),
                    ParseError("2")
                ),
                Success(3),
                Failure(ParseError("3"))
            ).sequence(),
            Failure(
                ParseError("1"),
                ParseError("2"),
                ParseError("3")
            )
        )
    }

    @Test
    fun `compute should support nested calls`() {
        assertEquals(
            compute {
                val (x) = Success(1)
                val (y) = compute {
                    val (z) = Success(2)
                    x + z
                }
                y + 3
            },
            Success(6)
        )
    }

    @Test
    fun `compute should not fail on empty computation`() {
        assertEquals(
            compute {},
            Success(Unit)
        )
    }

    @Test
    fun `compute should fail computation on fail call`() {
        assertEquals(
            compute {
                val (_) = Success(1)
                fail(ParseError("1"))
            },
            Failure(ParseError("1"))
        )
    }
}