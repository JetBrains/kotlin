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

package org.jetbrains.kotlin.js.coroutine

import org.jetbrains.kotlin.js.backend.ast.JsDebugger
import org.jetbrains.kotlin.js.backend.ast.JsExpressionStatement
import org.jetbrains.kotlin.js.backend.ast.metadata.MetadataProperty

var JsDebugger.targetBlock: CoroutineBlock? by MetadataProperty(default = null)
var JsDebugger.targetExceptionBlock: CoroutineBlock? by MetadataProperty(default = null)
var JsDebugger.finallyPath: List<CoroutineBlock>? by MetadataProperty(default = null)

var JsExpressionStatement.targetBlock by MetadataProperty(default = false)
var JsExpressionStatement.targetExceptionBlock by MetadataProperty(default = false)
var JsExpressionStatement.finallyPath by MetadataProperty(default = false)