/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.typeProviders

import kotlin.script.experimental.api.*
import kotlin.script.experimental.typeProviders.generatedCode.GeneratedCode

/**
 * A TypeProvider is a custom construct that can create types based on data at compile-time.
 * TypeProvider's listen for the usage of annotations and generate types based on the contents of the annotations.
 *
 * Examples include:
 * - Generate data classes that would parse a JSON file.
 * - Generate a type-safe client for a GraphQL API based on the Server URL.
 * - Generate kotlin stubs to call C libraries.
 * - Generate a type-safe DSL for shell commands.
 *
 * To include a Type Provider in a Scripts definition see [ScriptCompilationConfiguration.Builder.typeProviders].
 */
interface AnnotationBasedTypeProvider<A : Annotation> {
    class Context internal constructor(
        val script: SourceCode,
        val compilationConfiguration: ScriptCompilationConfiguration
    )

    /**
     * Perform additional changes to the Script Compilation Configuration.
     *
     * This function is intended for committing global changes to the compilation configuration.
     * These changes are completely separate from providing types based on data.
     *
     * Examples include:
     * - Adding default imports that should be available to the script. i.e. extension functions.
     * - Including special libraries in the classpath
     */
    fun ScriptCompilationConfiguration.Builder.prepare() {}

    /**
     * Turns the annotations into the types that will be included in the calling script.
     * See [GeneratedCode] for more information the kind of types that you can create.
     *
     * @param collectedAnnotations Annotations that should be turned into code
     * @param context Context information regarding the calling script and base directory. See [AnnotationBasedTypeProvider.Context] for more information.
     */
    operator fun invoke(
        collectedAnnotations: List<ScriptSourceAnnotation<A>>,
        context: Context
    ): ResultWithDiagnostics<GeneratedCode>
}