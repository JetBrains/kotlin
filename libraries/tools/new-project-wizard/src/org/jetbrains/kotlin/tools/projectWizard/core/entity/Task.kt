package org.jetbrains.kotlin.tools.projectWizard.core.entity

import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import kotlin.reflect.KProperty1

typealias TaskReference = KProperty1<out Plugin, Task>
typealias Task1Reference<A, B> = KProperty1<out Plugin, Task1<A, B>>
typealias PipelineTaskReference = KProperty1<out Plugin, PipelineTask>

class TaskContext : EntityContext<Task, TaskReference>()

sealed class Task : EntityBase()

data class Task1<A, B : Any>(
    override val path: String,
    val action: TaskRunningContext.(A) -> TaskResult<B>
) : Task() {
    class Builder<A, B : Any>(private val name: String) {
        private var action: TaskRunningContext.(A) -> TaskResult<B> = { Failure() }

        fun withAction(action: TaskRunningContext.(A) -> TaskResult<B>) {
            this.action = action
        }

        fun build(): Task1<A, B> = Task1(name, action)
    }

    companion object {
        fun <A, B : Any> delegate(
            context: EntityContext<Task, TaskReference>,
            init: Builder<A, B>.() -> Unit
        ) = entityDelegate(context) { name ->
            Builder<A, B>(name).apply(init).build()
        }
    }
}

data class PipelineTask(
    override val path: String,
    val action: TaskRunningContext.() -> TaskResult<Unit>,
    val before: List<PipelineTaskReference>,
    val after: List<PipelineTaskReference>,
    val phase: GenerationPhase,
    val checker: Checker,
    val title: String?
) : Task() {
    class Builder(
        private val name: String,
        private val phase: GenerationPhase
    ) {
        private var action: TaskRunningContext.() -> TaskResult<Unit> = { UNIT_SUCCESS }
        private val before = mutableListOf<PipelineTaskReference>()
        private val after = mutableListOf<PipelineTaskReference>()

        var activityChecker: Checker = Checker.ALWAYS_AVAILABLE

        var title: String? = null

        fun withAction(action: TaskRunningContext.() -> TaskResult<Unit>) {
            this.action = action
        }

        fun runBefore(vararg before: PipelineTaskReference) {
            this.before.addAll(before)
        }

        fun runAfter(vararg after: PipelineTaskReference) {
            this.after.addAll(after)
        }

        fun build(): PipelineTask = PipelineTask(name, action, before, after, phase, activityChecker, title)
    }

    companion object {
        fun delegate(
            context: EntityContext<Task, TaskReference>,
            phase: GenerationPhase,
            init: Builder.() -> Unit
        ) = entityDelegate(context) { name ->
            Builder(name, phase).apply(init).build()
        }
    }
}