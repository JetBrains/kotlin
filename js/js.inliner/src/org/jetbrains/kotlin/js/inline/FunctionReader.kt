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

package org.jetbrains.kotlin.js.inline

import org.jetbrains.kotlin.js.config.LibrarySourcesConfig
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.utils.*

import java.io.*

// TODO: add hash checksum to defineModule?
/**
 * Matches string like Kotlin.defineModule("stdlib", _)
 * Kotlin, _ can be renamed by minifier, quotes type can be changed too (" to ')
 */
private val DEFINE_MODULE_PATTERN = "(\\w+)\\.defineModule\\(\\s*(['\"])(\\w+)\\2\\s*,\\s*(\\w+)\\s*\\)".toRegex()

public class FunctionReader(private val context: TranslationContext) {
    /**
     * Maps module name to .js file content, that contains this module definition.
     * One file can contain more than one module definition.
     */
    private val moduleJsDefinition = hashMapOf<String, String>();

    {
        val config = context.getConfig() as LibrarySourcesConfig
        val libs = config.getLibraries().map { File(it) }
        val jsLibs = libs.filter { it.exists() && LibraryUtils.isKotlinJavascriptLibrary(it) }
        val files = LibraryUtils.readJsFiles(jsLibs.map { it.getPath() }.toList())

        for (file in files) {
            val matcher = DEFINE_MODULE_PATTERN.matcher(file)

            while (matcher.find()) {
                val moduleName = matcher.group(3)
                assert(moduleName !in moduleJsDefinition) { "Module is defined in more, than one file" }
                moduleJsDefinition[moduleName] = file
            }
        }
    }

}
