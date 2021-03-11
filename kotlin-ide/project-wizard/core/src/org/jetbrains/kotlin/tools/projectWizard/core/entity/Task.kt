package org.jetbrains.kotlin.tools.projectWizard.core.entity


import org.jetbrains.annotations.Nls
import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import kotlin.reflect.KProperty1

sealed class Task : EntityBase()

data class Task1<A, B : Any>(
    override val path: String,
    val action: Writer.(A) -> TaskResult<B>
) : Task() {
    class Builder<A, B : Any>(private val name: String) {
        private var action: Writer.(A) -> TaskResult<B> = { Failure() }

        fun withAction(action: Writer.(A) -> TaskResult<B>) {
            this.action = action
        }

        fun build(): Task1<A, B> = Task1(name, action)
    }
}

data class PipelineTask(
    override val path: String,
    val action: Writer.() -> TaskResult<Unit>,
    val before: List<PipelineTask>,
    val after: List<PipelineTask>,
    val phase: GenerationPhase,
    val isAvailable: Checker,
    val title: String?
) : Task() {
    class Builder(
        private val name: String,
        private val phase: GenerationPhase
    ) {
        private var action: Writer.() -> TaskResult<Unit> = { UNIT_SUCCESS }
        private val before = mutableListOf<PipelineTask>()
        private val after = mutableListOf<PipelineTask>()

        var isAvailable: Checker = ALWAYS_AVAILABLE_CHECKER

        @Nls
        var title: String? = null

        fun withAction(action: Writer.() -> TaskResult<Unit>) {
            this.action = action
        }

        fun runBefore(vararg before: PipelineTask) {
            this.before.addAll(before)
        }

        fun runAfter(vararg after: PipelineTask) {
            this.after.addAll(after)
        }

        fun build(): PipelineTask = PipelineTask(name, action, before, after, phase, isAvailable, title)
    }
}