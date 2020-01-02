package org.jetbrains.kotlin.tools.projectWizard.core

import org.jetbrains.kotlin.tools.projectWizard.core.entity.PropertyReference
import org.jetbrains.kotlin.tools.projectWizard.core.entity.Task1
import org.jetbrains.kotlin.tools.projectWizard.core.entity.Task1Reference
import org.jetbrains.kotlin.tools.projectWizard.core.service.ServicesManager

class TaskRunningContext(
    context: Context,
    servicesManager: ServicesManager,
    isUnitTestMode: Boolean
) : ValuesReadingContext(context, servicesManager, isUnitTestMode) {
    fun <A, B : Any> Task1Reference<A, B>.execute(value: A): TaskResult<B> {
        @Suppress("UNCHECKED_CAST")
        val task = context.taskContext.getEntity(this) as Task1<A, B>
        return task.action(this@TaskRunningContext, value)
    }

    inline fun <reified T : Any> PropertyReference<T>.update(
        crossinline updater: suspend ComputeContext<*>.(T) -> TaskResult<T>
    ): TaskResult<Unit> = compute {
        val (newValue) = updater(propertyValue)
        context.propertyContext[this@update] = newValue
        Unit
    }

    fun <T : Any> PropertyReference<List<T>>.addValues(
        vararg values: T
    ): TaskResult<Unit> = update { oldValues -> success(oldValues + values) }

    fun <T : Any> PropertyReference<List<T>>.addValues(
        values: List<T>
    ): TaskResult<Unit> = update { oldValues -> success(oldValues + values) }
}
