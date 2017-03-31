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

package org.jetbrains.kotlin.js.dce

import org.jetbrains.kotlin.js.backend.ast.JsFunction
import org.jetbrains.kotlin.js.backend.ast.JsInvocation
import org.jetbrains.kotlin.js.backend.ast.JsNode
import org.jetbrains.kotlin.js.dce.Context.Node

interface AnalysisResult {
    val nodeMap: Map<JsNode, Node>

    val astNodesToEliminate: Set<JsNode>

    val astNodesToSkip: Set<JsNode>

    val functionsToEnter: Set<JsFunction>

    val invocationsToSkip: Set<JsInvocation>
}
