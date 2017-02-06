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

    private var closure: Closure<*>? = null

    open fun arguments(closure: Closure<*>) {
        this.closure = closure
    }

    fun getAdditionalArguments(project: Project, variantData: Any?, androidExtension: Any?): Map<String, String> {
        val closureToExecute = closure ?: return emptyMap()

        val executor = KaptAdditionalArgumentsDelegate(project, variantData, androidExtension)
        executor.execute(closureToExecute)
        return executor.args
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
open class KaptAdditionalArgumentsDelegate(
        @Suppress("unused") open val project: Project,
        @Suppress("unused") open val variant: Any?,
        @Suppress("unused") open val android: Any?
) {
    internal val args = LinkedHashMap<String, String>()

    @Suppress("unused")
    open fun arg(name: Any, vararg values: Any) {
        args.put(name.toString(), values.joinToString(" "))
    }

    fun execute(closure: Closure<*>) {
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.delegate = this
        closure.call()
    }

}