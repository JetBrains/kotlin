/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

internal object DefaultCommonizerSettings : CommonizerSettings {
    override fun <T : Any> getSetting(key: CommonizerSettings.Key<T>): T {
        return key.defaultValue
    }
}

internal class MapBasedCommonizerSettings private constructor(
    private val settings: Map<CommonizerSettings.Key<*>, Any>
) : CommonizerSettings {
    constructor(vararg settings: Setting<*>) : this(settings.associate { /** STATISTICS ON DESTRUCTURING - type: Lambdas, destructured variable total amount: 2, destructured variable amount without '_': 2, classId: org/jetbrains/kotlin/commonizer/MapBasedCommonizerSettings.Setting,  */
                                                                         (k, v) -> k to v })

    override fun <T : Any> getSetting(key: CommonizerSettings.Key<T>): T {
        @Suppress("UNCHECKED_CAST")
        return settings[key] as? T ?: key.defaultValue
    }

    internal data class Setting<T : Any>(
        internal val key: CommonizerSettings.Key<T>,
        internal val settingValue: T,
    )
}
