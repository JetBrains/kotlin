/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cli

import java.util.concurrent.atomic.AtomicInteger

internal abstract class Task(private val options: Collection<Option<*>>) : Comparable<Task> {
    internal enum class Category(
        open val prologue: String? = null,
        open val epilogue: String? = null,
        open val logEachStep: Boolean = false
    ) {
        // Important: the order of entries affects that order of tasks execution
        INFORMATIONAL,
        COMMONIZATION(
            prologue = "\nKotlin KLIB commonizer: Please wait while preparing libraries.",
            epilogue = "\n",
            logEachStep = true
        )
    }

    abstract val category: Category
    private val submissionOrder = SUBMISSION_ORDER_GENERATOR.getAndIncrement()

    abstract fun execute(logPrefix: String = "")

    protected inline fun <reified T, reified O : OptionType<T>> getMandatory(nameFilter: (String) -> Boolean = { true }): T {
        val option = options.filter { it.type is O }.single { nameFilter(it.type.alias) }
        check(option.type.mandatory)

        @Suppress("UNCHECKED_CAST")
        return option.value as T
    }

    internal inline fun <reified T, reified O : OptionType<T>> getOptional(nameFilter: (String) -> Boolean = { true }): T? {
        val option = options.filter { it.type is O }.singleOrNull { nameFilter(it.type.alias) }
        if (option != null) check(!option.type.mandatory)

        @Suppress("UNCHECKED_CAST")
        return option?.value as T?
    }

    override fun compareTo(other: Task): Int {
        category.compareTo(other.category).let {
            if (it != 0) return it
        }

        this::class.java.name.compareTo(other::class.java.name).let {
            if (it != 0) return it
        }

        return submissionOrder - other.submissionOrder
    }

    companion object {
        private val SUBMISSION_ORDER_GENERATOR = AtomicInteger(0)
    }
}
