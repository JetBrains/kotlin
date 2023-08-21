/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal

import org.gradle.api.Describable
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import java.util.*

/**
 * Allow loading a properties file content in configuration cache and project isolation compatible way by specifying
 * [Parameters.propertiesFile].
 *
 * Returned type is `Map<String, String>` due to the following [bug in Gradle](https://github.com/gradle/gradle/pull/24846) which
 * prevents proper serialization of [Properties] type.
 *
 * If the file does not exist - returned provider will be empty.
 *
 * Usage:
 * ```
 * project
 *     .providers
 *     .of(CustomFileValueSource::class.java) {
 *         it.parameters.propertiesFile.set(project.layout.projectDirectory.file("my-properties-file-to-load"))
 *     }
 * ```
 */
internal abstract class CustomPropertiesFileValueSource : ValueSource<Map<String, String>, CustomPropertiesFileValueSource.Parameters>,
    Describable {

    interface Parameters : ValueSourceParameters {
        val propertiesFile: RegularFileProperty
    }

    override fun getDisplayName(): String = "properties file ${parameters.propertiesFile.get().asFile.absolutePath}"

    override fun obtain(): Map<String, String>? {
        val customFile = parameters.propertiesFile.get().asFile
        return if (customFile.exists()) {
            customFile.bufferedReader().use {
                @Suppress("UNCHECKED_CAST")
                Properties().apply { load(it) }.toMap() as Map<String, String>
            }
        } else {
            null
        }
    }
}