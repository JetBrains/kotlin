/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

open class KaptExtension {

    open var generateStubs: Boolean = false

    open var inheritedAnnotations: Boolean = true

    private var closure: Closure<*>? = null

    open fun arguments(closure: Closure<*>) {
        this.closure = closure
    }

    fun getAdditionalArguments(project: Project, variant: Any?, android: Any?): List<String> {
        val closureToExecute = closure ?: return emptyList()

        val executor = KaptAdditionalArgumentsDelegate(project, variant, android)
        executor.execute(closureToExecute)
        return executor.additionalCompilerArgs
    }
}

open class KaptAdditionalArgumentsDelegate(
        open val project: Project,
        open val variant: Any?,
        open val android: Any?
) {

    val additionalCompilerArgs = arrayListOf<String>()

    open fun arg(name: Any, vararg values: Any) {
        val valuesString = if (values.isNotEmpty()) values.joinToString(" ", prefix = "=") else ""
        additionalCompilerArgs.add("-A$name$valuesString")
    }

    fun execute(closure: Closure<*>) {
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.delegate = this
        closure.call()
    }

}