package org.jetbrains.kotlin.tools.projectWizard.core

import org.jetbrains.kotlin.tools.projectWizard.core.entity.PipelineTask
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import kotlin.test.Test
import kotlin.test.assertEquals

class TaskSorterTest {
    private fun sortTasks(tasksCount: Int, dependencies: Map<Int, List<Int>>): TaskResult<List<Int>> {
        val tasks =
            List(tasksCount) { i ->
                PipelineTask(
                    path = i.toString(),
                    action = { UNIT_SUCCESS },
                    before = emptyList(),
                    after = emptyList(),
                    phase = GenerationPhase.FIRST_STEP,
                    checker = Checker.ALWAYS_AVAILABLE,
                    title = null
                )
            }
        val taskToIndex = tasks.withIndex().associateBy { it.value }.mapValues { it.value.index }

        val dependenciesMap = dependencies.map { (key, value) ->
            tasks[key] to value.map { tasks[it] }
        }.toMap()
        return TaskSorter().sort(tasks, dependenciesMap)
            .map { list -> list.map { taskToIndex.getValue(it) }.reversed() }
    }

    private fun performTest(tasksCount: Int, dependencies: Map<Int, List<Int>>, expected: TaskResult<List<Int>>) {
        val actual = sortTasks(tasksCount, dependencies)
        assertEquals(expected, actual)
    }

    @Test
    fun `it should correctly sort empty list of tasks`() {
        performTest(
            0,
            emptyMap(),
            Success(emptyList())
        )
    }

    @Test
    fun `it should correctly sort list of tasks with no dependencies`() {
        performTest(
            10,
            emptyMap(),
            Success((9 downTo 0).toList())
        )
    }

    @Test
    fun `it should correctly sort linear dependency`() {
        performTest(
            3,
            mapOf(
                0 to listOf(1),
                1 to listOf(2)
            ),
            Success((0..2).toList())
        )
    }

    @Test
    fun `it should correctly sort tree-like dependency list`() {
        performTest(
            8,
            mapOf(
                0 to listOf(1, 2),
                2 to listOf(3, 4),
                3 to listOf(5),
                5 to listOf(6, 7)
            ),
            Success(listOf(0, 2, 4, 3, 5, 7, 6, 1))
        )
    }

    @Test
    fun `it should correctly sort DAG`() {
        performTest(
            8,
            mapOf(
                0 to listOf(1, 2, 7),
                1 to listOf(4),
                2 to listOf(3, 4),
                3 to listOf(5),
                4 to listOf(6),
                5 to listOf(6, 7),
                6 to listOf(7)
            ),
            Success(listOf(0, 2, 3, 5, 1, 4, 6, 7))
        )
    }

    @Test
    fun `it should fail on a self-loop`() {
        performTest(
            1,
            mapOf(
                0 to listOf(0)
            ),
            Failure(
                CircularTaskDependencyError("0")
            )
        )
    }

    @Test
    fun `it should fail on direct loop`() {
        performTest(
            2,
            mapOf(
                0 to listOf(1),
                1 to listOf(0)
            ),
            Failure(
                CircularTaskDependencyError("0"),
                CircularTaskDependencyError("1")
            )
        )
    }

    @Test
    fun `it should fail on transitive loop`() {
        performTest(
            3,
            mapOf(
                0 to listOf(1),
                1 to listOf(2),
                2 to listOf(0)
            ),
            Failure(
                CircularTaskDependencyError("0"),
                CircularTaskDependencyError("1"),
                CircularTaskDependencyError("2")
            )
        )
    }
}