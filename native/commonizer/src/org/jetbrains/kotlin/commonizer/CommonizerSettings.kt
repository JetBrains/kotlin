/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import org.jetbrains.kotlin.commonizer.cli.CommonizerSettingOptionType
import org.jetbrains.kotlin.commonizer.cli.Task

/**
 * Optional configuration settings for commonization task
 */
interface CommonizerSettings {
    fun <T : Any> getSetting(setting: CommonizerSettingOptionType<T>): T
}

internal object DefaultCommonizerSettings : CommonizerSettings {
    override fun <T : Any> getSetting(setting: CommonizerSettingOptionType<T>): T {
        return setting.defaultValue
    }
}

internal class TaskBasedCommonizerSettings(
    private val task: Task
) : CommonizerSettings {
    override fun <T : Any> getSetting(setting: CommonizerSettingOptionType<T>): T {
        return task.getCommonizerSetting(setting)
    }
}

internal class MapBasedCommonizerSettings(
    vararg settings: CommonizerSetting<*>
) : CommonizerSettings {
    private val settings: Map<CommonizerSettingOptionType<*>, Any> = settings.associate { (k, v) -> k to v }

    override fun <T : Any> getSetting(setting: CommonizerSettingOptionType<T>): T {
        @Suppress("UNCHECKED_CAST")
        return settings[setting] as? T
            ?: DefaultCommonizerSettings.getSetting(setting)
    }
}

internal data class CommonizerSetting<T : Any>(
    internal val setting: CommonizerSettingOptionType<T>,
    internal val settingValue: T,
)
