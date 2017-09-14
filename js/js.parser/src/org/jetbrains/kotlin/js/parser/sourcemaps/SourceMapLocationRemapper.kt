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

import org.jetbrains.kotlin.js.backend.ast.*

class SourceMapLocationRemapper(private val sourceMap: SourceMap) {
    fun remap(node: JsNode) {
        val listCollector = JsNodeFlatListCollector()
        node.accept(listCollector)
        applySourceMap(listCollector.nodeList)
    }

    private fun applySourceMap(nodes: List<JsNode>) {
        var lastGroup: SourceMapGroup? = null
        var lastGroupIndex = 0
        var lastSegment: SourceMapSegment? = null
        var lastSegmentIndex = 0

        fun findCorrespondingSegment(node: SourceInfoAwareJsNode): SourceMapSegment? {
            val source = node.source as? JsLocation ?: return null
            val group = sourceMap.groups.getOrElse(source.startLine) { return null }

            if (lastGroup != group) {
                if (lastGroup != null) {
                    val segmentsToSkip = lastGroup!!.segments.drop(lastSegmentIndex).toMutableList()
                    if (lastGroupIndex + 1 < source.startLine) {
                        segmentsToSkip += sourceMap.groups.subList((lastGroupIndex + 1), source.startLine).flatMap { it.segments }
                    }

                    segmentsToSkip.lastOrNull()?.let { lastSegment = it }
                }
                lastGroup = group
                lastGroupIndex = source.startLine
                lastSegmentIndex = 0
            }

            while (lastSegmentIndex < group.segments.size) {
                val segment = group.segments[lastSegmentIndex]
                if (segment.generatedColumnNumber > source.startChar) break

                lastSegment = segment
                lastSegmentIndex++
            }

            return lastSegment
        }


        for (node in nodes.asSequence().filterIsInstance<SourceInfoAwareJsNode>()) {
            val segment = findCorrespondingSegment(node)
            val sourceFileName = segment?.sourceFileName
            node.source = if (sourceFileName != null) {
                val location = JsLocation(segment.sourceFileName, segment.sourceLineNumber, segment.sourceColumnNumber)
                JsLocationWithEmbeddedSource(location, null) { sourceMap.sourceContentResolver(segment.sourceFileName) }
            }
            else {
                null
            }
        }
    }

    internal class JsNodeFlatListCollector : RecursiveJsVisitor() {
        val nodeList = mutableListOf<JsNode>()

        override fun visitDoWhile(x: JsDoWhile) {
            nodeList += x
            accept(x.body)
            accept(x.condition)
        }

        override fun visitBinaryExpression(x: JsBinaryOperation) = handleNode(x, x.arg1, x.arg2)

        override fun visitConditional(x: JsConditional) = handleNode(x, x.testExpression, x.thenExpression, x.elseExpression)

        override fun visitArrayAccess(x: JsArrayAccess) = handleNode(x, x.arrayExpression, x.indexExpression)

        override fun visitArray(x: JsArrayLiteral) = handleNode(x, *x.expressions.toTypedArray())

        override fun visitPrefixOperation(x: JsPrefixOperation) = handleNode(x, x.arg)

        override fun visitPostfixOperation(x: JsPostfixOperation) = handleNode(x, x.arg)

        override fun visitNameRef(nameRef: JsNameRef) = handleNode(nameRef, nameRef.qualifier)

        override fun visitInvocation(invocation: JsInvocation) =
                handleNode(invocation, invocation.qualifier, *invocation.arguments.toTypedArray())

        override fun visitFunction(x: JsFunction) {
            x.parameters.forEach { accept(it) }
            x.body.statements.forEach { accept(it) }
            nodeList += x
        }

        override fun visitElement(node: JsNode) {
            nodeList += node
            node.acceptChildren(this)
        }

        private fun handleNode(node: JsNode, vararg children: JsNode?) {
            val nonNullChildren = children.mapNotNull { it }

            if (nonNullChildren.isEmpty()) {
                nodeList += node
            }
            else {
                val firstChild = nonNullChildren.first()
                if (node.isNotBefore(firstChild)) {
                    accept(firstChild)
                    nodeList += node
                    nonNullChildren.drop(1).forEach { accept(it) }
                }
                else {
                    nodeList += node
                    nonNullChildren.forEach { accept(it) }
                }
            }
        }

        private fun JsNode.isNotBefore(other: JsNode): Boolean {
            val first = (source as? JsLocation ?: return false)
            val second = (other.source as? JsLocation ?: return false)
            if (first.file != second.file) return false
            return first.startLine > second.startLine || (first.startLine == second.startLine && first.startChar >= second.startChar)
        }
    }
}