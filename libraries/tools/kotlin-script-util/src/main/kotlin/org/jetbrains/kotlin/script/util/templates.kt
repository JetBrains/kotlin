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

@file:Suppress("unused") // an API

package org.jetbrains.kotlin.script.util

import org.jetbrains.kotlin.script.ScriptTemplateDefinition

@ScriptTemplateDefinition(resolver = DefaultKotlinResolver::class, scriptFilePattern = ".*\\.kts")
abstract class StandardScript(val args: Array<String>)

@ScriptTemplateDefinition(resolver = DefaultKotlinAnnotatedScriptDependenciesResolver::class, scriptFilePattern = ".*\\.kts")
abstract class StandardScriptWithAnnotatedResolving(val args: Array<String>)

@ScriptTemplateDefinition(resolver = DefaultKotlinResolver::class, scriptFilePattern = ".*\\.kts")
abstract class ScriptWithBindings(val bindings: Map<String, Any?>)

@ScriptTemplateDefinition(resolver = DefaultKotlinAnnotatedScriptDependenciesResolver::class, scriptFilePattern = ".*\\.kts")
abstract class ScriptWithBindingsAndAnnotatedResolving(val bindings: Map<String, Any?>)
