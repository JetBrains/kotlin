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

package org.jetbrains.kotlin.gradle.dsl

import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments

internal class KotlinJsOptionsImpl : KotlinJsOptionsBase() {
    override var freeCompilerArgs: List<String> = listOf()

    var sourceMapBaseDirs: FileCollection? = null

    override fun updateArguments(args: K2JSCompilerArguments) {
        super.updateArguments(args)
        copyFreeCompilerArgsToArgs(args)
        sourceMapBaseDirs?.let {
            args.sourceMapBaseDirs = it.asPath
        }
    }
}
