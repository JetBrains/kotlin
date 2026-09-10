/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.script.experimental.api.ScriptCompilationConfiguration

object ReplSnippetConfigurationCodec {

    fun encode(configuration: ScriptCompilationConfiguration): ByteArray {
        val bytes = ByteArrayOutputStream()
        ObjectOutputStream(bytes).use { it.writeObject(configuration) }
        return bytes.toByteArray()
    }

    fun decode(bytes: ByteArray): ScriptCompilationConfiguration =
        ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() as ScriptCompilationConfiguration }

    fun writeTo(configuration: ScriptCompilationConfiguration, file: File) {
        file.parentFile?.mkdirs()
        file.writeBytes(encode(configuration))
    }

    fun readFrom(file: File): ScriptCompilationConfiguration = decode(file.readBytes())
}
