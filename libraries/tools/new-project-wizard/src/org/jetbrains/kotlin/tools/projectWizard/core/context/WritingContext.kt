/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.core.context

import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.core.entity.PropertyReference
import org.jetbrains.kotlin.tools.projectWizard.core.entity.Task1
import org.jetbrains.kotlin.tools.projectWizard.core.entity.Task1Reference
import org.jetbrains.kotlin.tools.projectWizard.core.service.ServicesManager

open class WritingContext(
    context: Context,
    servicesManager: ServicesManager,
    isUnitTestMode: Boolean
) : ReadingContext(context, servicesManager, isUnitTestMode) {
    fun <A, B : Any> Task1Reference<A, B>.execute(value: A): TaskResult<B> {
        @Suppress("UNCHECKED_CAST")
        val task = context.taskContext.getEntity(this) as Task1<A, B>
        return task.action(this@WritingContext, value)
    }

    inline fun <reified T : Any> PropertyReference<T>.update(
        crossinline updater: suspend ComputeContext<*>.(T) -> TaskResult<T>
    ): TaskResult<Unit> = compute {
        val (newValue) = updater(propertyValue)
        `access$context`.propertyContext[this@update] = newValue
    }

    fun <T : Any> PropertyReference<List<T>>.addValues(
        vararg values: T
    ): TaskResult<Unit> = update { oldValues -> success(oldValues + values) }

    fun <T : Any> PropertyReference<List<T>>.addValues(
        values: List<T>
    ): TaskResult<Unit> = update { oldValues -> success(oldValues + values) }
}
