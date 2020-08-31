/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.typeProviders.generatedCode

import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.typeProviders.generatedCode.internal.visit
import kotlin.script.experimental.typeProviders.generatedCode.internal.visitor.*
import kotlin.script.experimental.typeProviders.generatedCode.internal.visitor.FileBasedPersistentValuesSerializer
import kotlin.script.experimental.typeProviders.generatedCode.internal.visitor.ImportCollector
import kotlin.script.experimental.typeProviders.generatedCode.internal.visitor.ScriptCreator

fun GeneratedCode.artifacts(): GeneratedCodeArtifacts = GeneratedCodeArtifacts.Builder()
    .apply { visit(this@artifacts, 0) }
    .build()

/**
 * Includes all the scripts and imports that should be added to a script based on the Generated Code
 *
 * **Note:** All the scripts files will be deleted upon exit of the program.
 *  If you need to persist these across executions, copy them to another location you control yourself.
 */
class GeneratedCodeArtifacts private constructor(
    val importScripts: List<SourceCode>,
    val imports: Set<String>
) {
    internal class Builder private constructor(
        private val scriptCreator: ScriptCreator,
        private val importCollector: ImportCollector,
        private val scriptCollector: ScriptCollector
    ) : GeneratedCodeVisitor by GeneratedCodeVisitor(scriptCreator, importCollector, scriptCollector) {
        constructor() : this(
            ScriptCreator(FileBasedPersistentValuesSerializer(), ImportCollector()),
            ImportCollector(),
            ScriptCollector()
        )

        fun build(): GeneratedCodeArtifacts {
            val directory = createTempDir().apply { deleteOnExit() }
            val importScript = scriptCreator.build(directory)

            return GeneratedCodeArtifacts(
                importScripts = listOfNotNull(importScript) + scriptCollector.build(),
                imports = importCollector.build()
            )
        }
    }
}