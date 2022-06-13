/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kpm.idea.proto

import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmLanguageSettings
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmLanguageSettingsImpl
import java.io.File
import kotlin.test.Test

class LanguageSettingsTest : AbstractSerializationTest<IdeaKpmLanguageSettings>() {

    override fun serialize(value: IdeaKpmLanguageSettings): ByteArray {
        return value.toByteArray()
    }

    override fun deserialize(data: ByteArray): IdeaKpmLanguageSettings {
        return IdeaKpmLanguageSettings(data)
    }

    override fun normalize(value: IdeaKpmLanguageSettings): IdeaKpmLanguageSettings {
        value as IdeaKpmLanguageSettingsImpl
        return value.copy(
            compilerPluginClasspath = value.compilerPluginClasspath.map { it.absoluteFile }
        )
    }

    @Test
    fun `serialize - deserialize - sample 0`() = testSerialization(
        IdeaKpmLanguageSettingsImpl(
            languageVersion = "1.3",
            apiVersion = "1.4",
            isProgressiveMode = false,
            enabledLanguageFeatures = setOf("some.feature.1"),
            optInAnnotationsInUse = setOf("some.opt.in", "some.other.opt.in"),
            compilerPluginArguments = listOf("my.argument"),
            compilerPluginClasspath = listOf(File("classpath")),
            freeCompilerArgs = listOf("free.compiler.arg.1", "free.compiler.arg.2")
        )
    )

    @Test
    fun `serialize - deserialize - sample 1`() = testSerialization(
        IdeaKpmLanguageSettingsImpl(
            languageVersion = null,
            apiVersion = "1.7",
            isProgressiveMode = true,
            enabledLanguageFeatures = emptySet(),
            optInAnnotationsInUse = emptySet(),
            compilerPluginArguments = emptyList(),
            compilerPluginClasspath = emptyList(),
            freeCompilerArgs = emptyList()
        )
    )
}
