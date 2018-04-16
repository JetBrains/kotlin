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

package org.jetbrains.kotlin.j2k

import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.j2k.tree.JKElement
import org.jetbrains.kotlin.j2k.tree.prettyDebugPrintTree

class NewJavaToKotlinConverter(
    private val project: Project,
    private val settings: ConverterSettings
) {

    private fun List<JKElement>.prettyPrintTrees() = buildString {
        for (tree in this@prettyPrintTrees) {
            appendln()
            appendln(tree.prettyDebugPrintTree())
            appendln()
        }
    }

    fun filesToKotlin(files: List<PsiJavaFile>, progressIndicator: ProgressIndicator = EmptyProgressIndicator()): List<String> {
        val treeBuilder = JavaToJKTreeBuilder()
        val fileTrees = files.mapNotNull(treeBuilder::buildTree)

        println(fileTrees.prettyPrintTrees())

        ConversionsRunner.doApply(fileTrees)

        val resultTree = fileTrees.prettyPrintTrees()

        println(resultTree)

        return fileTrees.map { NewCodeBuilder().run { printCodeOut(it) } }
    }
}