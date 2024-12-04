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

package org.jetbrains.kotlin.config

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.cli.common.arguments.Freezable

open class CompilerSettings : Freezable() {
    open var additionalArguments: String = DEFAULT_ADDITIONAL_ARGUMENTS
        set(value) {
            checkFrozen()
            field = value
        }
    open var scriptTemplates: String = ""
        set(value) {
            checkFrozen()
            field = value
        }
    open var scriptTemplatesClasspath: String = ""
        set(value) {
            checkFrozen()
            field = value
        }
    open var copyJsLibraryFiles = true
        set(value) {
            checkFrozen()
            field = value
        }
    open var outputDirectoryForJsLibraryFiles: String = DEFAULT_OUTPUT_DIRECTORY
        set(value) {
            checkFrozen()
            field = value
        }

    companion object {
        val DEFAULT_ADDITIONAL_ARGUMENTS = ""
        val DEFAULT_OUTPUT_DIRECTORY = "lib"
    }

    override fun copyOf(): Freezable = copyCompilerSettings(this, CompilerSettings())
}

val CompilerSettings.additionalArgumentsAsList: List<String>
    get() = splitArgumentString(additionalArguments)

fun splitArgumentString(arguments: String) = StringUtil.splitHonorQuotes(arguments, ' ').map {
    if (it.startsWith('"')) StringUtil.unescapeChar(StringUtil.unquoteString(it), '"') else it
}