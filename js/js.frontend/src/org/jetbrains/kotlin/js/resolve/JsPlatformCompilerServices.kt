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

package org.jetbrains.kotlin.js.resolve

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.storage.StorageManager

object JsPlatformCompilerServices : PlatformDependentCompilerServices() {
    override fun computePlatformSpecificDefaultImports(storageManager: StorageManager, result: MutableList<ImportPath>) {
        result.add(ImportPath.fromString("kotlin.js.*"))
    }

    override val platformConfigurator: PlatformConfigurator = JsPlatformConfigurator

    val builtIns: KotlinBuiltIns
        get() = DefaultBuiltIns.Instance

    override val excludedImports: List<FqName> =
        listOf("Promise", "Date", "Console", "Math", "RegExp", "RegExpMatch", "Json", "json").map { FqName("kotlin.js.$it") }
}
