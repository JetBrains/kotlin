/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.definitions

import java.io.File
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.impl.tryGetResourcePathForClass
import kotlin.script.experimental.jvm.util.matchMaybeVersionedFile

class AutoloadedScriptDefinitions(
    private val hostConfiguration: ScriptingHostConfiguration,
    private val baseClassloader: ClassLoader,
    private val messageReporter: MessageReporter
) : ScriptDefinitionsSource {

    private val basePath by lazy(LazyThreadSafetyMode.PUBLICATION) {
        tryGetResourcePathForClass(AutoloadedScriptDefinitions::class.java)?.parentFile?.takeIf { it.isDirectory } ?: File(".")
    }

    override val definitions: Sequence<ScriptDefinition> = sequence {
        val mainKtsJars = basePath.listFiles { f: File ->
            MAIN_KTS_JARS.any { expected ->
                f.matchMaybeVersionedFile(expected) && f.extension == "jar"
            }
        }?.toList()
        if (mainKtsJars != null && mainKtsJars.size >= MAIN_KTS_JARS.size) {
            yieldAll(
                loadScriptTemplatesFromClasspath(
                    listOf("org.jetbrains.kotlin.mainKts.MainKtsScript"),
                    mainKtsJars,
                    emptyList<File>(),
                    baseClassloader,
                    hostConfiguration,
                    messageReporter
                )
            )
        }
    }

    companion object {
        private val MAIN_KTS_JARS = listOf("kotlin-main-kts", "kotlin-stdlib", "kotlin-script-runtime", "kotlin-reflect")
    }
}