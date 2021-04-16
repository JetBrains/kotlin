/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension

internal inline fun <reified T : Any> ExtraPropertiesExtension.getOrPut(key: String, provideValue: () -> T): T {
    return synchronized(this) {
        if (!has(key)) {
            set(key, provideValue())
        }
        get(key) as T
    }
}

internal inline fun <reified T : Any> Project.getOrPutRootProjectProperty(key: String, provideValue: () -> T) =
    rootProject.extensions.extraProperties.getOrPut(key, provideValue)