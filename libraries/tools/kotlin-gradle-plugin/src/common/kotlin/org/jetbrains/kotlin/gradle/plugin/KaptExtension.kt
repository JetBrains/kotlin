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
import org.jetbrains.kotlin.gradle.dsl.KaptArguments
import org.jetbrains.kotlin.gradle.dsl.KaptExtensionConfig
import org.jetbrains.kotlin.gradle.dsl.KaptJavacOption
import java.util.*

open class KaptExtension : KaptExtensionConfig {
    open var generateStubs: Boolean = false

    override var useLightAnalysis: Boolean = true

    override var correctErrorTypes: Boolean = false

    override var dumpDefaultParameterValues: Boolean = false

    override var mapDiagnosticLocations: Boolean = false

    override var strictMode: Boolean = false

    override var stripMetadata: Boolean = false

    override var showProcessorStats: Boolean = false

    override var detectMemoryLeaks: String = "default"

    override var includeCompileClasspath: Boolean? = null

    @Deprecated("Use `annotationProcessor()` and `annotationProcessors()` instead")
    open var processors: String = ""

    override var keepJavacAnnotationProcessors: Boolean = false

    override var useBuildCache: Boolean = true

    private val apOptionsActions =
        mutableListOf<(KaptArguments) -> Unit>()

    private val javacOptionsActions =
        mutableListOf<(KaptJavacOption) -> Unit>()

    @Suppress("DEPRECATION")
    override fun annotationProcessor(fqName: String) {
        val oldProcessors = this.processors
        this.processors = if (oldProcessors.isEmpty()) fqName else "$oldProcessors,$fqName"
    }

    override fun annotationProcessors(vararg fqName: String) {
        fqName.forEach(this::annotationProcessor)
    }

    override fun arguments(action: KaptArguments.() -> Unit) {
        apOptionsActions += action
    }

    override fun javacOptions(action: KaptJavacOption.() -> Unit) {
        javacOptionsActions += action
    }

    override fun getJavacOptions(): Map<String, String> {
        @Suppress("DEPRECATION")
        val result = KaptJavacOptionsDelegate()
        javacOptionsActions.forEach { it(result) }
        return result.options
    }

    @Suppress("UNUSED_PARAMETER")
    @Deprecated(message = "Scheduled for removal in Kotlin 2.2.")
    fun getAdditionalArguments(project: Project, variantData: Any?, androidExtension: Any?): Map<String, String> {
        return getAdditionalArguments()
    }

    internal fun getAdditionalArguments(): Map<String, String> {
        @Suppress("DEPRECATION")
        val result = KaptAnnotationProcessorOptions()
        apOptionsActions.forEach { it(result) }
        return result.options
    }

    @Suppress("UNUSED_PARAMETER")
    @Deprecated(message = "Scheduled for removal in Kotlin 2.2.")
    fun getAdditionalArgumentsForJavac(project: Project, variantData: Any?, androidExtension: Any?): List<String> {
        val javacArgs = mutableListOf<String>()
        for ((key, value) in getAdditionalArguments()) {
            javacArgs += "-A" + key + (if (value.isNotEmpty()) "=$value" else "")
        }
        return javacArgs
    }
}

@Deprecated("This is an internal class of KGP. Consider using the `KaptArguments` interface. Scheduled to be hidden in Kotlin 2.2.")
open class KaptAnnotationProcessorOptions() : KaptArguments {
    @Suppress("UNUSED_PARAMETER")
    @Deprecated(
        message = "This constructor with parameters is scheduled for removal in Kotlin 2.2. Consider migrating to the constructor without parameters.",
        replaceWith = ReplaceWith("KaptAnnotationProcessorOptions()")
    )
    constructor(
        project: Project,
        variant: Any?,
        android: Any?
    ) : this()

    internal val options = LinkedHashMap<String, String>()

    @Deprecated(
        message = "This function with Any parameters is scheduled for removal in Kotlin 2.2. Consider migrating to the function with String parameters.",
        replaceWith = ReplaceWith("arg(name.toString(), *values.map { it.toString() }.toTypedArray())")
    )
    override fun arg(name: Any, vararg values: Any) {
        arg(name.toString(), *values.map { it.toString() }.toTypedArray())
    }

    override fun arg(name: String, vararg values: String) {
        options[name] = values.joinToString(" ")
    }

    @Deprecated("Scheduled for removal in Kotlin 2.2.")
    fun execute(closure: Closure<*>) = executeClosure(closure)
}

@Deprecated("This is an internal class of KGP. Consider using the `KaptJavacOption` interface. Scheduled to be hidden in Kotlin 2.2.")
open class KaptJavacOptionsDelegate : KaptJavacOption {
    internal val options = LinkedHashMap<String, String>()

    @Deprecated(
        message = "This function with Any parameters is scheduled for removal in Kotlin 2.2. Consider migrating to the function with String parameters.",
        replaceWith = ReplaceWith("option(name.toString(), value.toString())")
    )
    override fun option(name: Any, value: Any) {
        option(name.toString(), value.toString())
    }

    override fun option(name: String, value: String) {
        options[name] = value
    }

    @Deprecated(
        message = "This function with Any parameter is scheduled for removal in Kotlin 2.2. Consider migrating to the function with String parameter.",
        replaceWith = ReplaceWith("option")
    )
    override fun option(name: Any) {
        option(name.toString())
    }

    override fun option(name: String) {
        options[name] = ""
    }

    @Deprecated("Scheduled for removal in Kotlin 2.2.")
    fun execute(closure: Closure<*>) = executeClosure(closure)
}

private fun Any?.executeClosure(closure: Closure<*>) {
    closure.resolveStrategy = Closure.DELEGATE_FIRST
    closure.delegate = this
    closure.call()
}
