/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

package org.jetbrains.kotlin.gradle.plugin.experimental

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.file.FileCollection
import org.gradle.language.ComponentDependencies
import org.jetbrains.kotlin.konan.target.KonanTarget

interface CInteropSettings {

    interface IncludeDirectories {
        fun allHeaders(vararg includeDirs: Any)
        fun allHeaders(includeDirs: Collection<Any>)

        fun headerFilterOnly(vararg includeDirs: Any)
        fun headerFilterOnly(includeDirs: Collection<Any>)
    }

    fun defFile(file: Any)

    fun packageName(value: String)

    fun header(file: Any) = headers(file)
    fun headers(vararg files: Any)
    fun headers(files: FileCollection)

    fun includeDirs(vararg values: Any)
    fun includeDirs(closure: Closure<Unit>)
    fun includeDirs(action: Action<IncludeDirectories>)
    fun includeDirs(configure: IncludeDirectories.() -> Unit)

    fun compilerOpts(vararg values: String)
    fun compilerOpts(values: List<String>)

    fun linkerOpts(vararg values: String)
    fun linkerOpts(values: List<String>)

    fun extraOpts(vararg values: Any)
    fun extraOpts(values: List<Any>)

    val dependencies: ComponentDependencies
    fun dependencies(action: ComponentDependencies.() -> Unit)
    fun dependencies(action: Closure<Unit>)
    fun dependencies(action: Action<ComponentDependencies>)
}

interface CInterop : Named, CInteropSettings {
    fun target(target: String): CInteropSettings
    fun target(target: KonanTarget): CInteropSettings

    fun target(target: String, action: CInteropSettings.() -> Unit)
    fun target(target: String, action: Closure<Unit>)
    fun target(target: String, action: Action<CInteropSettings>)

    fun target(target: KonanTarget, action: CInteropSettings.() -> Unit)
    fun target(target: KonanTarget, action: Closure<Unit>)
    fun target(target: KonanTarget, action: Action<CInteropSettings>)
}