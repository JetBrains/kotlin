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

package org.jetbrains.kotlin.script.util.templates

import org.jetbrains.kotlin.script.ScriptTemplateDefinition
import org.jetbrains.kotlin.script.util.ContextAndAnnotationsBasedResolver
import org.jetbrains.kotlin.script.util.ContextBasedResolver

@ScriptTemplateDefinition(resolver = ContextBasedResolver::class, scriptFilePattern = ".*\\.kts")
abstract class StandardScriptTemplate(val args: Array<String>)

@ScriptTemplateDefinition(resolver = ContextAndAnnotationsBasedResolver::class, scriptFilePattern = ".*\\.kts")
abstract class StandardScriptTemplateWithAnnotatedResolving(val args: Array<String>)

@ScriptTemplateDefinition(resolver = ContextBasedResolver::class, scriptFilePattern = ".*\\.kts")
abstract class ScriptTemplateWithBindings(val bindings: Map<String, Any?>)

@ScriptTemplateDefinition(resolver = ContextAndAnnotationsBasedResolver::class, scriptFilePattern = ".*\\.kts")
abstract class ScriptTemplateWithBindingsAndAnnotatedResolving(val bindings: Map<String, Any?>)
