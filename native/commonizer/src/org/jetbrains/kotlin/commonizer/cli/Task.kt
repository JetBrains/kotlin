/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cli

import org.jetbrains.kotlin.commonizer.*
import org.jetbrains.kotlin.commonizer.MapBasedCommonizerSettings
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
            prologue = null,
            epilogue = null,
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

    protected fun getSettings(): CommonizerSettings {
        val passedSettings = ADDITIONAL_COMMONIZER_SETTINGS.map { settingOptionType ->
            settingOptionType.toCommonizerSetting()
        }

        return MapBasedCommonizerSettings(*passedSettings.toTypedArray())
    }

    private fun <T : Any> CommonizerSettingOptionType<T>.toCommonizerSetting(): MapBasedCommonizerSettings.Setting<T> {
        val key = commonizerSettingKey

        @Suppress("UNCHECKED_CAST")
        val settingValue = options.singleOrNull { option -> option.type == this }?.value as? T ?: key.defaultValue

        return MapBasedCommonizerSettings.Setting(key, settingValue)
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
