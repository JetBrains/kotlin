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

package org.jetbrains.kotlin.js.parser.sourcemaps

import org.jetbrains.kotlin.js.backend.ast.JsLocation
import org.jetbrains.kotlin.js.backend.ast.JsNode
import org.jetbrains.kotlin.js.backend.ast.RecursiveJsVisitor
import org.jetbrains.kotlin.js.backend.ast.SourceInfoAwareJsNode

class SourceMapLocationRemapper(val sourceMaps: Map<String, SourceMap>) {
    fun remap(node: JsNode) {
        node.accept(visitor)
    }

    private val visitor = object : RecursiveJsVisitor() {
        private var lastSourceMap: SourceMap? = null
        private var lastGroup: SourceMapGroup? = null
        private var lastSegmentIndex = 0

        override fun visitElement(node: JsNode) {
            if (node is SourceInfoAwareJsNode) {
                if (!remapNode(node)) {
                    node.source = null
                }
            }
            super.visitElement(node)
        }

        private fun remapNode(node: SourceInfoAwareJsNode): Boolean {
            val source = node.source as? JsLocation ?: return false

            val sourceMap = sourceMaps[source.file] ?: return false
            val group = sourceMap.groups.getOrElse(source.startLine) { return false }
            if (group.segments.isEmpty()) return false

            if (lastSourceMap != sourceMap || lastGroup != group) {
                lastSegmentIndex = 0
            }
            if (group.segments[lastSegmentIndex].generatedColumnNumber > source.startChar) {
                if (lastSegmentIndex == 0) return false
                lastSegmentIndex = 0
            }

            while (lastSegmentIndex + 1 < group.segments.size) {
                val nextIndex = lastSegmentIndex + 1
                if (group.segments[nextIndex].generatedColumnNumber > source.startChar) break
                lastSegmentIndex = nextIndex
            }

            val segment = group.segments[lastSegmentIndex]
            node.source = JsLocation(segment.sourceFileName, segment.sourceLineNumber, segment.sourceColumnNumber)

            return true
        }
    }
}