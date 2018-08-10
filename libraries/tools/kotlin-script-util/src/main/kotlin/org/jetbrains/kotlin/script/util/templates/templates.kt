/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import org.jetbrains.kotlin.script.util.FilesAndIvyResolver
import org.jetbrains.kotlin.script.util.FilesAndMavenResolver
import org.jetbrains.kotlin.script.util.LocalFilesResolver
import kotlin.script.templates.ScriptTemplateDefinition

@ScriptTemplateDefinition(resolver = LocalFilesResolver::class, scriptFilePattern = ".*\\.kts")
abstract class StandardArgsScriptTemplateWithLocalResolving(val args: Array<String>)

@ScriptTemplateDefinition(resolver = FilesAndMavenResolver::class, scriptFilePattern = ".*\\.kts")
abstract class StandardArgsScriptTemplateWithMavenResolving(val args: Array<String>)

@ScriptTemplateDefinition(resolver = FilesAndIvyResolver::class, scriptFilePattern = ".*\\.kts")
abstract class StandardArgsScriptTemplateWithIvyResolving(val args: Array<String>)

@ScriptTemplateDefinition(resolver = LocalFilesResolver::class, scriptFilePattern = ".*\\.kts")
abstract class BindingsScriptTemplateWithLocalResolving(val bindings: Map<String, Any?>)

@ScriptTemplateDefinition(resolver = FilesAndMavenResolver::class, scriptFilePattern = ".*\\.kts")
abstract class BindingsScriptTemplateWithMavenResolving(val bindings: Map<String, Any?>)
