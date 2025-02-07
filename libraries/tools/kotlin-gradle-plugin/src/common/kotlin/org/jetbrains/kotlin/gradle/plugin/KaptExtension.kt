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

    @Deprecated(
        "Use `annotationProcessor()` and `annotationProcessors()` instead. Scheduled for removal in Kotlin 2.3.",
        level = DeprecationLevel.ERROR
    )
    open var processors: String = ""

    override var keepJavacAnnotationProcessors: Boolean = false

    override var useBuildCache: Boolean = true

    private val apOptionsActions =
        mutableListOf<(KaptArguments) -> Unit>()

    private val javacOptionsActions =
        mutableListOf<(KaptJavacOption) -> Unit>()

    @Suppress("DEPRECATION_ERROR")
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
        val result = KaptJavacOptionsDelegate()
        javacOptionsActions.forEach { it(result) }
        return result.options
    }

    internal fun getAdditionalArguments(): Map<String, String> {
        val result = KaptAnnotationProcessorOptions()
        apOptionsActions.forEach { it(result) }
        return result.options
    }
}

private class KaptAnnotationProcessorOptions : KaptArguments {
    val options = LinkedHashMap<String, String>()

    override fun arg(name: String, vararg values: String) {
        options[name] = values.joinToString(" ")
    }
}

private class KaptJavacOptionsDelegate : KaptJavacOption {
    val options = LinkedHashMap<String, String>()

    override fun option(name: String, value: String) {
        options[name] = value
    }

    override fun option(name: String) {
        options[name] = ""
    }
}