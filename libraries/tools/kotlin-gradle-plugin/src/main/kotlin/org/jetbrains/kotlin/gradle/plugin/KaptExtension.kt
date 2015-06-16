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

public open class KaptExtension {

    public open var generateStubs: Boolean = false

    private val additionalCompilerArgs = arrayListOf<String>()

    public open fun arg(name: String, vararg values: String) {
        val valuesString = if (values.isNotEmpty()) values.joinToString(" ", prefix = "=") else ""
        additionalCompilerArgs.add("-A$name$valuesString")
    }

    public open fun getAdditionalCompilerArgs(): List<String> {
        return additionalCompilerArgs.toList()
    }

}