/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.typeProviders.json

import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.FileBasedScriptSource
import kotlin.script.experimental.typeProviders.AnnotationBasedTypeProvider
import kotlin.script.experimental.typeProviders.generatedCode.*
import kotlin.script.experimental.typeProviders.json.inference.generate
import kotlin.script.experimental.typeProviders.json.inference.infer

@Target(AnnotationTarget.FILE)
@Repeatable
@Retention(AnnotationRetention.SOURCE)
annotation class JSON(val filePath: String, val typeName: String = "")

object JSONTypeProvider : AnnotationBasedTypeProvider<JSON> {

    override fun ScriptCompilationConfiguration.Builder.prepare() {
        defaultImports(
            "kotlin.script.experimental.typeProviders.json.parse",
            "kotlin.script.experimental.typeProviders.json.parseFromFile",
            "kotlin.script.experimental.typeProviders.json.parseList",
            "kotlin.script.experimental.typeProviders.json.parseListFromFile",
        )
    }

    override fun invoke(
        collectedAnnotations: List<ScriptSourceAnnotation<JSON>>,
        context: AnnotationBasedTypeProvider.Context
    ): ResultWithDiagnostics<GeneratedCode> {
        val scriptFile = (context.script as? FileBasedScriptSource)?.file
        val baseDirectory = scriptFile?.parentFile
        return collectedAnnotations
            .mapSuccess { (annotation, location) -> this(annotation, location, baseDirectory) }
            .onSuccess {
                GeneratedCode(it).asSuccess()
            }
    }

    operator fun invoke(
        annotation: JSON,
        location: SourceCode.LocationWithId?,
        baseDirectory: File?
    ): ResultWithDiagnostics<GeneratedCode> {
        return annotation.infer(baseDirectory, location).onSuccess { it.generate(baseDirectory).asSuccess() }
    }

}