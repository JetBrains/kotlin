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

import groovy.lang.Closure
import org.gradle.api.Task
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal

interface KotlinJsCompile : KotlinCompile<KotlinJsOptions>

interface KotlinJvmCompile : KotlinCompile<KotlinJvmOptions>

interface KotlinCommonCompile : KotlinCompile<KotlinMultiplatformCommonOptions>

interface KotlinJsDce : Task {
    @get:Internal
    val dceOptions: KotlinJsDceOptions

    @get:Input
    val keep: MutableList<String>

    fun dceOptions(fn: KotlinJsDceOptions.() -> Unit) {
        dceOptions.fn()
    }

    fun dceOptions(fn: Closure<*>) {
        fn.delegate = dceOptions
        fn.call()
    }

    fun keep(vararg fqn: String)
}