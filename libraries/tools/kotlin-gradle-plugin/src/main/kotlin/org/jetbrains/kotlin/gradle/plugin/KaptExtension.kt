/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.gradle.plugin

import groovy.lang.Closure
import org.gradle.api.Project
import java.util.*

open class KaptExtension {
    open var generateStubs: Boolean = false

    open var inheritedAnnotations: Boolean = true

    open var useLightAnalysis: Boolean = true

    open var correctErrorTypes: Boolean = false

    open var dumpDefaultParameterValues: Boolean = false

    open var mapDiagnosticLocations: Boolean = false

    open var strictMode: Boolean = false

    open var stripMetadata: Boolean = false
    
    open var showProcessorTimings: Boolean = false

    open var detectMemoryLeaks: String = "default"

    open var includeCompileClasspath: Boolean? = null

    @Deprecated("Use `annotationProcessor()` and `annotationProcessors()` instead")
    open var processors: String = ""

    /**
     * If true keeps annotation processors added via `annotationProcessor(..)` configuration for javac java-files compilation
     */
    open var keepJavacAnnotationProcessors: Boolean = false

    /** Opt-out switch for Kapt caching. Should be used when annotation processors used by this project are suspected of
     * using anything aside from the task inputs in their logic and are not guaranteed to produce the same
     * output on subsequent runs without input changes. */
    var useBuildCache: Boolean = true

    private val apOptionsActions =
        mutableListOf<(KaptAnnotationProcessorOptions) -> Unit>()

    private val javacOptionsActions =
        mutableListOf<(KaptJavacOptionsDelegate) -> Unit>()

    private var apOptionsClosure: Closure<*>? = null
    private var javacOptionsClosure: Closure<*>? = null

    @Suppress("DEPRECATION")
    open fun annotationProcessor(fqName: String) {
        val oldProcessors = this.processors
        this.processors = if (oldProcessors.isEmpty()) fqName else "$oldProcessors,$fqName"
    }

    open fun annotationProcessors(vararg fqName: String) {
        fqName.forEach(this::annotationProcessor)
    }

    open fun arguments(closure: Closure<*>) {
        apOptionsActions += { apOptions ->
            apOptions.execute(closure)
        }
    }

    open fun arguments(action: KaptAnnotationProcessorOptions.() -> Unit) {
        apOptionsActions += action
    }

    open fun javacOptions(closure: Closure<*>) {
        this.javacOptionsActions += { javacOptions ->
            javacOptions.execute(closure)
        }
    }

    open fun javacOptions(action: KaptJavacOptionsDelegate.() -> Unit) {
        javacOptionsActions += action
    }

    fun getJavacOptions(): Map<String, String> {
        val result = KaptJavacOptionsDelegate()
        javacOptionsActions.forEach { it(result) }
        return result.options
    }

    fun getAdditionalArguments(project: Project, variantData: Any?, androidExtension: Any?): Map<String, String> {
        val result = KaptAnnotationProcessorOptions(project, variantData, androidExtension)
        apOptionsActions.forEach { it(result) }
        return result.options
    }

    fun getAdditionalArgumentsForJavac(project: Project, variantData: Any?, androidExtension: Any?): List<String> {
        val javacArgs = mutableListOf<String>()
        for ((key, value) in getAdditionalArguments(project, variantData, androidExtension)) {
            javacArgs += "-A" + key + (if (value.isNotEmpty()) "=$value" else "")
        }
        return javacArgs
    }
}

/**
 * [project], [variant] and [android] properties are intended to be used inside the closure.
 */
open class KaptAnnotationProcessorOptions(
    @Suppress("unused") open val project: Project,
    @Suppress("unused") open val variant: Any?,
    @Suppress("unused") open val android: Any?
) {
    internal val options = LinkedHashMap<String, String>()

    @Suppress("unused")
    open fun arg(name: Any, vararg values: Any) {
        options.put(name.toString(), values.joinToString(" "))
    }

    fun execute(closure: Closure<*>) = executeClosure(closure)
}

open class KaptJavacOptionsDelegate {
    internal val options = LinkedHashMap<String, String>()

    open fun option(name: Any, value: Any) {
        options.put(name.toString(), value.toString())
    }

    open fun option(name: Any) {
        options.put(name.toString(), "")
    }

    fun execute(closure: Closure<*>) = executeClosure(closure)
}

private fun Any?.executeClosure(closure: Closure<*>) {
    closure.resolveStrategy = Closure.DELEGATE_FIRST
    closure.delegate = this
    closure.call()
}
