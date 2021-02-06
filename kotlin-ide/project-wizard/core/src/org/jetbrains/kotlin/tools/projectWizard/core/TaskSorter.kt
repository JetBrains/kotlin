package org.jetbrains.kotlin.tools.projectWizard.core

import org.jetbrains.kotlin.tools.projectWizard.core.entity.PipelineTask

class TaskSorter {
    private enum class State {
        VISITED, NOT_VISITED, ADDED
    }

    fun sort(
        tasks: List<PipelineTask>,
        taskToDependencies: Map<PipelineTask, List<PipelineTask>>
    ): TaskResult<List<PipelineTask>> {
        val states = Array(tasks.size) { State.NOT_VISITED }
        val result = mutableListOf<PipelineTask>()
        val taskToIndex = tasks.indices.associateBy { tasks[it] }
        fun dfs(index: Int): TaskResult<Unit> {
            when (states[index]) {
                State.ADDED -> return UNIT_SUCCESS
                State.VISITED -> return Failure(
                    CircularTaskDependencyError(tasks[index].path)
                )
                State.NOT_VISITED -> {
                    states[index] = State.VISITED
                }
            }
            taskToDependencies[tasks[index]].orEmpty().mapNotNull { dependent ->
                dfs(taskToIndex[dependent] ?: return@mapNotNull null)
            }.sequence().raise { return it }

            result += tasks[index]
            states[index] = State.ADDED
            return UNIT_SUCCESS
        }

        return tasks.indices.map { index ->
            dfs(index)
        }.sequence().map { result }
    }
}